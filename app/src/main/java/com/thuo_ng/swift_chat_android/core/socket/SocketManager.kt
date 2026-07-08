package com.thuo_ng.swift_chat_android.core.socket

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.thuo_ng.swift_chat_android.BuildConfig
import com.thuo_ng.swift_chat_android.core.network.TokenRefreshManager
import com.thuo_ng.swift_chat_android.core.storage.SecureStorage
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.json.JSONObject
import kotlin.coroutines.resume
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketManager @Inject constructor(
    private val secureStorage: SecureStorage,
    private val tokenRefreshManager: TokenRefreshManager
) {
    companion object {
        private const val TAG = "SocketManager"
        const val BASE_URL      = BuildConfig.BASE_URL
        const val NAMESPACE     = "chat"
        const val HEARTBEAT_INTERVAL_MS = 30_000L
        private val WRAPPER_KEYS = listOf("data", "message", "notification", "payload")
        private const val MAX_WRAPPER_DEPTH = 2
    }

    private var socket: Socket? = null
    private var activeToken: String? = null
    private var isRefreshingToken = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var heartbeatJob: Job? = null
    private val activeConversationRooms = mutableSetOf<String>()
    private val listConversationRooms = mutableSetOf<String>()

    // ── Expose Flows ─────────────────────────────────────────────
    private val _events = MutableSharedFlow<SocketEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<SocketEvent> = _events.asSharedFlow()

    private val _connectionState = MutableStateFlow<SocketConnectionState>(SocketConnectionState.Disconnected)
    val connectionState: StateFlow<SocketConnectionState> = _connectionState.asStateFlow()
    val isConnected: StateFlow<Boolean> = connectionState
        .map { it is SocketConnectionState.Connected }
        .stateIn(scope, SharingStarted.Eagerly, false)

    // ── App Foreground/Background Lifecycle ─────────────────────
    init {
        Log.d(TAG, "Initializing SocketManager and registering process lifecycle observer")
        observeAppLifecycle()
        observeTokenChanges()
    }

    private fun observeAppLifecycle() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    Log.d(TAG, "App entered FOREGROUND")
                    if (secureStorage.getAccessToken() != null) {
                        if (socket != null) {
                            Log.d(TAG, "Socket is already initialized.")
                            if (socket?.connected() == true) {
                                Log.d(TAG, "Socket is connected. Requesting rooms rejoin and starting heartbeat.")
                                emitRejoinRooms()
                                startHeartbeat()
                            } else {
                                Log.d(TAG, "Socket is reconnecting. Will rejoin when EVENT_CONNECT fires.")
                            }
                        } else {
                            Log.d(TAG, "Socket is not initialized. Triggering connect().")
                            connect()
                        }
                    } else {
                        Log.d(TAG, "Cannot auto-connect: access token is missing")
                    }
                }

                override fun onStop(owner: LifecycleOwner) {
                    Log.d(TAG, "App entered BACKGROUND. Stopping heartbeat.")
                    stopHeartbeat()
                }
            }
        )
    }

    private fun observeTokenChanges() {
        scope.launch {
            secureStorage.tokenFlow.collect { token ->
                when {
                    token == null -> disconnect()
                    socket != null && token != activeToken -> reconnectWithLatestToken()
                }
            }
        }
    }

    // ── Socket Connect/Disconnect ───────────────────────────────
    fun connect() {
        val token = secureStorage.getAccessToken()
        if (token == null) {
            Log.w(TAG, "Abort connect(): Access token is null")
            activeToken = null
            _connectionState.value = SocketConnectionState.Disconnected
            return
        }
        if (socket != null) {
            if (activeToken != token) {
                reconnectWithLatestToken()
            } else {
                Log.d(TAG, "Abort connect(): Socket is already initialized")
            }
            return
        }

        activeToken = token
        _connectionState.value = SocketConnectionState.Connecting

        Log.d(TAG, "Initiating socket connection to $BASE_URL$NAMESPACE")
        val options = IO.Options.builder()
            .setPath("/socket.io")
            .setAuth(mapOf("token" to token))
            .setExtraHeaders(mapOf("Authorization" to listOf("Bearer $token")))
            .setTransports(arrayOf("websocket"))
            .setReconnection(true)
            .setReconnectionAttempts(Int.MAX_VALUE)
            .setReconnectionDelay(2_000)
            .setReconnectionDelayMax(30_000)
            .build()

        socket = IO.socket(BASE_URL + NAMESPACE, options).apply {
            on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "EVENT_CONNECT: Connected to server successfully")
                _connectionState.value = SocketConnectionState.Connected
                emitRejoinRooms()
                rejoinTrackedRooms()
                startHeartbeat()
            }
            on(Socket.EVENT_DISCONNECT) {
                Log.d(TAG, "EVENT_DISCONNECT: Disconnected from server")
                _connectionState.value = SocketConnectionState.Disconnected
                stopHeartbeat()
            }
            on(Socket.EVENT_CONNECT_ERROR) { args ->
                val message = args.getOrNull(0)?.toString()
                Log.e(TAG, "EVENT_CONNECT_ERROR: Connection failed. Detail: $message")
                if (isAuthError(message)) {
                    handleAuthError(message)
                } else {
                    _connectionState.value = SocketConnectionState.NetworkError(message)
                }
            }
            on("error") { args ->
                val message = args.getOrNull(0)?.toString()
                Log.e(TAG, "EVENT_ERROR: Error occurred. Detail: $message")
                if (isAuthError(message)) {
                    handleAuthError(message)
                }
            }
            on("exception") { args ->
                val message = args.getOrNull(0)?.toString()
                Log.e(TAG, "EVENT_EXCEPTION: Server exception. Detail: $message")
                if (isAuthError(message)) {
                    handleAuthError(message)
                }
            }
            registerServerEvents()
            connect()
        }
    }

    fun disconnect() {
        Log.d(TAG, "Explicitly disconnecting socket")
        stopHeartbeat()
        socket?.disconnect()
        socket?.off()
        socket = null
        activeToken = null
        isRefreshingToken = false
        activeConversationRooms.clear()
        listConversationRooms.clear()
        _connectionState.value = SocketConnectionState.Disconnected
    }

    private fun reconnectWithLatestToken() {
        val token = secureStorage.getAccessToken()
        if (token == null) {
            disconnect()
            return
        }

        Log.d(TAG, "Access token changed. Reconnecting socket with latest token.")
        _connectionState.value = SocketConnectionState.Reconnecting
        stopHeartbeat()
        socket?.disconnect()
        socket?.off()
        socket = null
        activeToken = null
        connect()
    }

    private fun handleAuthError(message: String?) {
        _connectionState.value = SocketConnectionState.AuthError(message)
        if (isRefreshingToken) return

        isRefreshingToken = true
        scope.launch {
            val failedToken = activeToken
            val refreshed = tokenRefreshManager.refreshAccessToken(failedToken)
            isRefreshingToken = false

            if (refreshed && activeToken == failedToken) {
                reconnectWithLatestToken()
            }
        }
    }

    // ── Room management ─────────────────────────────────────────
    @Synchronized
    fun joinRoom(conversationId: String): Boolean {
        Log.d(TAG, "Joining chat room for conversation: $conversationId")
        activeConversationRooms += conversationId
        return emitJoinRoom(conversationId)
    }

    @Synchronized
    fun leaveRoom(conversationId: String): Boolean {
        Log.d(TAG, "Leaving chat room for conversation: $conversationId")
        activeConversationRooms -= conversationId
        return if (conversationId in listConversationRooms) {
            Log.d(TAG, "Keeping room joined because it is subscribed by conversation list: $conversationId")
            true
        } else {
            emitLeaveRoom(conversationId)
        }
    }

    @Synchronized
    fun subscribeConversationListRooms(conversationIds: Collection<String>) {
        val nextRooms = conversationIds
            .mapNotNull { it.takeIf(String::isNotBlank) }
            .toSet()
        val removedRooms = listConversationRooms - nextRooms

        listConversationRooms.clear()
        listConversationRooms += nextRooms

        nextRooms.forEach(::emitJoinRoom)
        removedRooms
            .filterNot { it in activeConversationRooms }
            .forEach(::emitLeaveRoom)
    }

    private fun emitRejoinRooms(): Boolean {
        Log.d(TAG, "Emitting chat:rejoin_rooms request to join active conversation rooms")
        return emit("chat:rejoin_rooms", JSONObject())
    }

    private fun rejoinTrackedRooms() {
        val trackedRooms = synchronized(this) {
            activeConversationRooms + listConversationRooms
        }
        if (trackedRooms.isEmpty()) return
        Log.d(TAG, "Rejoining tracked conversation rooms: ${trackedRooms.size}")
        trackedRooms.forEach(::emitJoinRoom)
    }

    private fun emitJoinRoom(conversationId: String): Boolean =
        emit("chat:join_room", JSONObject().put("conversationId", conversationId))

    private fun emitLeaveRoom(conversationId: String): Boolean =
        emit("chat:leave_room", JSONObject().put("conversationId", conversationId))

    // ── Chat actions ────────────────────────────────────────────
    fun sendMessage(payload: SendMessagePayload): Boolean {
        Log.d(TAG, "Emitting chat:send_message clientTempId=${payload.clientTempId}")
        return emit("chat:send_message", payload.toJsonObject())
    }

    suspend fun sendMessageWithAck(
        payload: SendMessagePayload,
        timeoutMs: Long = 10_000L
    ): Result<SocketAckResult> {
        Log.d(TAG, "Emitting chat:send_message with ack clientTempId=${payload.clientTempId}")
        return emitWithAck("chat:send_message", payload.toJsonObject(), timeoutMs)
    }

    fun sendTyping(conversationId: String): Boolean {
        Log.d(TAG, "Emitting typing state for conversation $conversationId")
        return emit("chat:typing", JSONObject().put("conversationId", conversationId))
    }

    fun sendStopTyping(conversationId: String): Boolean {
        Log.d(TAG, "Emitting stop typing state for conversation $conversationId")
        return emit("chat:stop_typing", JSONObject().put("conversationId", conversationId))
    }

    fun markRead(payload: MarkReadPayload): Boolean {
        Log.d(TAG, "Emitting read receipt messageId=${payload.messageId}")
        return emit("chat:mark_read", payload.toJsonObject())
    }

    fun unsendMessage(messageId: String, conversationId: String): Boolean {
        Log.d(TAG, "Emitting unsend message request messageId=$messageId")
        return emit("chat:unsend_message", MessageActionPayload(messageId, conversationId).toJsonObject())
    }
    
    fun deleteForMe(messageId: String, conversationId: String): Boolean {
        Log.d(TAG, "Emitting delete message for me messageId=$messageId")
        return emit("chat:delete_for_me", MessageActionPayload(messageId, conversationId).toJsonObject())
    }

    fun editMessage(payload: EditMessagePayload): Boolean {
        Log.d(TAG, "Emitting edit message request messageId=${payload.messageId}")
        return emit("chat:edit_message", payload.toJsonObject())
    }

    fun reactMessage(payload: ReactPayload): Boolean {
        Log.d(TAG, "Emitting reaction emoji=${payload.emoji} for messageId=${payload.messageId}")
        return emit("chat:react_message", payload.toJsonObject())
    }
    
    fun pinMessage(messageId: String, conversationId: String): Boolean {
        Log.d(TAG, "Emitting pin message request messageId=$messageId")
        return emit("chat:pin_message", MessageActionPayload(messageId, conversationId).toJsonObject())
    }
    
    fun unpinMessage(messageId: String, conversationId: String): Boolean {
        Log.d(TAG, "Emitting unpin message request messageId=$messageId")
        return emit("chat:unpin_message", MessageActionPayload(messageId, conversationId).toJsonObject())
    }

    // ── Heartbeat ───────────────────────────────────────────────
    private fun startHeartbeat() {
        Log.d(TAG, "Starting heartbeat loop")
        stopHeartbeat()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL_MS)
                emit("chat:heartbeat", JSONObject())
            }
        }
    }

    private fun stopHeartbeat() {
        if (heartbeatJob != null) {
            Log.d(TAG, "Stopping heartbeat loop")
            heartbeatJob?.cancel()
            heartbeatJob = null
        }
    }

    // ── Register server events ─────────────────────────────────
    private fun Socket.registerServerEvents() {
        onEvent("chat:receive_message")      { args ->
            parse<MessagePayload>(args) { it.hasMessageIdentity() }
                ?.let { _events.tryEmit(SocketEvent.ReceiveMessage(it)) }
        }
        onEvent("chat:user_typing")          { args -> parse<SocketEvent.UserTyping>(args)?.let { _events.tryEmit(it) } }
        onEvent("chat:user_stop_typing")     { args -> parse<SocketEvent.UserStopTyping>(args)?.let { _events.tryEmit(it) } }
        onEvent("chat:message_unsent")       { args -> parse<SocketEvent.MessageUnsent>(args)?.let { _events.tryEmit(it) } }
        onEvent("chat:message_deleted")      { args -> parse<SocketEvent.MessageUnsent>(args)?.let { _events.tryEmit(it) } }
        onEvent("chat:message_deleted_for_me") { args -> parse<SocketEvent.MessageDeletedForMe>(args)?.let { _events.tryEmit(it) } }
        onEvent("chat:message_edited")       { args -> parse<SocketEvent.MessageEdited>(args)?.let { _events.tryEmit(it) } }
        onEvent("chat:read_receipt")         { args -> parse<SocketEvent.ReadReceipt>(args)?.let { _events.tryEmit(it) } }
        onEvent("chat:reaction_updated")     { args -> parse<SocketEvent.ReactionUpdated>(args)?.let { _events.tryEmit(it) } }
        onEvent("chat:message_pinned")       { args -> parse<SocketEvent.MessagePinned>(args)?.let { _events.tryEmit(it) } }
        onEvent("chat:message_unpinned")     { args -> parse<SocketEvent.MessageUnpinned>(args)?.let { _events.tryEmit(it) } }
        
        onEvent("presence:status")           { args ->
            parse<SocketEvent.PresenceStatus>(args) { it.hasPresenceIdentity() && it.hasPresenceState() }
                ?.let { _events.tryEmit(it) }
        }
        onEvent("notification:new")          { args ->
            parse<SocketEvent.NewNotification>(args) { it.hasNotificationIdentity() }
                ?.let { _events.tryEmit(it) }
        }
        onEvent("friend:updated")            { _events.tryEmit(SocketEvent.FriendUpdated) }
        
        onEvent("group:info_updated")        { args -> parse<SocketEvent.GroupInfoUpdated>(args)?.let { _events.tryEmit(it) } }
        onEvent("group:member_added")        { args -> parse<SocketEvent.GroupMemberAdded>(args)?.let { _events.tryEmit(it) } }
        onEvent("group:member_removed")      { args -> parse<SocketEvent.GroupMemberRemoved>(args)?.let { _events.tryEmit(it) } }
        onEvent("group:disbanded")           { args -> parse<SocketEvent.GroupDisbanded>(args)?.let { _events.tryEmit(it) } }
        onEvent("group:role_changed")        { args -> parse<SocketEvent.GroupRoleChanged>(args)?.let { _events.tryEmit(it) } }
        onEvent("group:you_added")           { args -> parse<SocketEvent.GroupYouAdded>(args)?.let { _events.tryEmit(it) } }
    }

    // ── Helpers ────────────────────────────────────────────────
    private val gson = Gson()

    private fun Any.toJsonObject(): JSONObject = JSONObject(gson.toJson(this))

    private inline fun <reified T> parse(
        args: Array<Any>,
        noinline isValid: (T) -> Boolean = { true }
    ): T? {
        if (args.isEmpty()) return null
        val raw = args[0].toString()
        Log.d(TAG, "Received Event raw payload: $raw")
        for (candidate in payloadCandidates(raw)) {
            try {
                val parsed = gson.fromJson(candidate, T::class.java)
                if (isValid(parsed)) {
                    Log.d(TAG, "Parsed payload into class ${T::class.java.simpleName}: $parsed")
                    return parsed
                }
                Log.w(TAG, "Ignoring invalid ${T::class.java.simpleName} payload candidate: $candidate")
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing payload into ${T::class.java.simpleName}", e)
            }
        }
        return null
    }

    private fun payloadCandidates(raw: String): List<String> {
        val candidates = mutableListOf<String>()
        val root = runCatching { JsonParser.parseString(raw) }.getOrNull()
        val jsonObject = root?.takeIf { it.isJsonObject }?.asJsonObject
        if (jsonObject != null) {
            collectWrappedPayloads(jsonObject, candidates)
        }
        candidates += raw
        return candidates.distinct()
    }

    private fun collectWrappedPayloads(
        jsonObject: JsonObject,
        candidates: MutableList<String>,
        depth: Int = 0
    ) {
        if (depth >= MAX_WRAPPER_DEPTH) return
        WRAPPER_KEYS.forEach { key ->
            val child = jsonObject.objectValue(key) ?: return@forEach
            candidates += child.toString()
            collectWrappedPayloads(child, candidates, depth + 1)
        }
    }

    private fun JsonObject.objectValue(key: String) =
        takeIf { has(key) && !get(key).isJsonNull && get(key).isJsonObject }?.getAsJsonObject(key)

    private fun MessagePayload.hasMessageIdentity(): Boolean = runCatching {
        id.isNotBlank() && conversationId.isNotBlank() && senderId.isNotBlank() && type.isNotBlank()
    }.getOrDefault(false)

    private fun SocketEvent.PresenceStatus.hasPresenceIdentity(): Boolean =
        !accountId.isNullOrBlank() || !userId.isNullOrBlank()

    private fun SocketEvent.PresenceStatus.hasPresenceState(): Boolean =
        isOnline != null || !status.isNullOrBlank()

    private fun SocketEvent.NewNotification.hasNotificationIdentity(): Boolean = runCatching {
        id.isNotBlank() && type.isNotBlank()
    }.getOrDefault(false)

    private fun emit(event: String, data: JSONObject): Boolean {
        val currentSocket = socket
        if (currentSocket?.connected() != true) {
            Log.w(TAG, "Skip emit: socket is not connected. event='$event' payload=$data")
            return false
        }

        Log.d(TAG, "EMIT: event='$event' payload=$data")
        currentSocket.emit(event, data)
        return true
    }

    private suspend fun emitWithAck(
        event: String,
        data: JSONObject,
        timeoutMs: Long
    ): Result<SocketAckResult> {
        val currentSocket = socket
        if (currentSocket?.connected() != true) {
            val message = "Socket is not connected"
            Log.w(TAG, "Skip emit ack: $message. event='$event' payload=$data")
            return Result.failure(IllegalStateException(message))
        }

        return try {
            withTimeout(timeoutMs) {
                suspendCancellableCoroutine { continuation ->
                    Log.d(TAG, "EMIT_ACK: event='$event' payload=$data")
                    currentSocket.emit(
                        event,
                        data,
                        Ack { args ->
                            if (continuation.isActive) {
                                continuation.resume(parseAck(args))
                            }
                        }
                    )
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Ack timeout for event='$event'", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Ack emit failed for event='$event'", e)
            Result.failure(e)
        }
    }

    private fun parseAck(args: Array<Any>): Result<SocketAckResult> {
        if (args.isEmpty()) {
            return Result.success(SocketAckResult(status = "ok"))
        }

        return try {
            val first = args[0]
            val json = when (first) {
                is JSONObject -> first
                else -> JSONObject(first.toString())
            }
            val status = json.optString("status", json.optString("ok", "ok"))
            val messageId = json.optNullableString("messageId")
            val error = json.optNullableString("error") ?: json.optNullableString("message")
            val ack = SocketAckResult(
                status = status,
                messageId = messageId,
                error = error
            )

            if (status.equals("error", ignoreCase = true) || !error.isNullOrBlank()) {
                Result.failure(IllegalStateException(error ?: "Socket ack error"))
            } else {
                Result.success(ack)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse socket ack", e)
            Result.failure(e)
        }
    }

    private fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).takeIf { it.isNotBlank() }
    }

    private fun Socket.onEvent(event: String, handler: (Array<Any>) -> Unit) {
        on(event) { args ->
            Log.d(TAG, "ON_EVENT: event='$event' triggered")
            scope.launch { handler(args) }
        }
    }

    private fun isAuthError(message: String?): Boolean {
        if (message.isNullOrBlank()) return false
        return message.contains("auth", ignoreCase = true) ||
            message.contains("token", ignoreCase = true) ||
            message.contains("unauthorized", ignoreCase = true)
    }
}
