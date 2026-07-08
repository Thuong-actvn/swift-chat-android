package com.thuo_ng.swift_chat_android.ui.friends

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thuo_ng.swift_chat_android.core.network.NetworkResult
import com.thuo_ng.swift_chat_android.core.socket.SocketEvent
import com.thuo_ng.swift_chat_android.core.socket.SocketManager
import com.thuo_ng.swift_chat_android.core.storage.SecureStorage
import com.thuo_ng.swift_chat_android.domain.model.FriendRequest
import com.thuo_ng.swift_chat_android.domain.model.OffsetPage
import com.thuo_ng.swift_chat_android.domain.model.PublicUserProfile
import com.thuo_ng.swift_chat_android.domain.model.isFriendRequestAcceptedType
import com.thuo_ng.swift_chat_android.domain.model.isFriendRequestReceivedType
import com.thuo_ng.swift_chat_android.domain.model.normalizeNotificationType
import com.thuo_ng.swift_chat_android.domain.repository.ConversationRepository
import com.thuo_ng.swift_chat_android.domain.repository.FriendRepository
import com.thuo_ng.swift_chat_android.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val friendRepository: FriendRepository,
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository,
    private val secureStorage: SecureStorage,
    private val socketManager: SocketManager
) : ViewModel() {

    private companion object {
        const val PAGE_SIZE = 20
        const val SEARCH_DEBOUNCE_MS = 300L
        const val AUTO_ACCEPTED = "AUTO_ACCEPTED"
    }

    private val _uiState = MutableStateFlow(
        FriendsUiState(
            myAccountId = secureStorage.getUserId(),
            friendsLimit = PAGE_SIZE
        )
    )
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    private val _effect = Channel<FriendsEffect>(Channel.BUFFERED)
    val effect: Flow<FriendsEffect> = _effect.receiveAsFlow()

    private var searchJob: Job? = null
    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            savedStateHandle.getStateFlow<String?>("initialSection", null).collect { sectionName ->
                sectionName?.let { name ->
                    val section = when (name) {
                        "Received" -> FriendsSection.Received
                        "Sent" -> FriendsSection.Sent
                        "Blocked" -> FriendsSection.Blocked
                        else -> FriendsSection.Friends
                    }
                    _uiState.update { it.copy(selectedSection = section) }
                }
            }
        }
        initialize()
        observeSocketEvents()
    }

    fun handleIntent(intent: FriendsIntent) {
        when (intent) {
            FriendsIntent.Refresh -> refreshAll(isRefresh = true)
            FriendsIntent.LoadMoreFriends -> loadMoreFriends()
            is FriendsIntent.SectionSelected -> {
                _uiState.update { it.copy(selectedSection = intent.section) }
                refreshSection(intent.section)
            }
            is FriendsIntent.SearchQueryChanged -> onSearchQueryChanged(intent.query)
            is FriendsIntent.SendFriendRequest -> sendFriendRequest(intent.accountId)
            is FriendsIntent.AcceptFriendRequest -> respondToRequest(
                actionKey = FriendActionKeys.accept(intent.requestId),
                successMessage = "Friend request accepted",
                call = { friendRepository.acceptFriendRequest(intent.requestId) }
            )
            is FriendsIntent.RejectFriendRequest -> respondToRequest(
                actionKey = FriendActionKeys.reject(intent.requestId),
                successMessage = "Friend request declined",
                call = { friendRepository.rejectFriendRequest(intent.requestId) }
            )
            is FriendsIntent.CancelFriendRequest -> respondToRequest(
                actionKey = FriendActionKeys.cancel(intent.requestId),
                successMessage = "Friend request canceled",
                call = { friendRepository.cancelFriendRequest(intent.requestId) }
            )
            is FriendsIntent.RemoveFriend -> respondToRequest(
                actionKey = FriendActionKeys.remove(intent.accountId),
                successMessage = "Friend removed",
                call = { friendRepository.removeFriend(intent.accountId) }
            )
            is FriendsIntent.BlockUser -> respondToRequest(
                actionKey = FriendActionKeys.block(intent.accountId),
                successMessage = "User blocked",
                refreshBlocked = true,
                call = { friendRepository.blockUser(intent.accountId) }
            )
            is FriendsIntent.UnblockUser -> respondToRequest(
                actionKey = FriendActionKeys.unblock(intent.accountId),
                successMessage = "User unblocked",
                refreshBlocked = true,
                call = { friendRepository.unblockUser(intent.accountId) }
            )
            is FriendsIntent.UserSelected -> onUserSelected(intent)
        }
    }

    private fun onUserSelected(intent: FriendsIntent.UserSelected) {
        if (!intent.isFriend) {
            viewModelScope.launch {
                _effect.send(FriendsEffect.OpenPublicProfile(intent.accountId, intent.displayName))
            }
            return
        }

        val actionKey = FriendActionKeys.open(intent.accountId)
        if (_uiState.value.isActionLoading(actionKey)) return

        viewModelScope.launch {
            addAction(actionKey)
            when (val result = conversationRepository.openOrCreateDirectConversation(intent.accountId)) {
                is NetworkResult.Success -> {
                    _effect.send(FriendsEffect.OpenConversation(result.data.id))
                }
                is NetworkResult.Error -> {
                    val localConversation = when (
                        val localResult = conversationRepository.findDirectConversationWith(intent.accountId)
                    ) {
                        is NetworkResult.Success -> localResult.data
                        is NetworkResult.Error -> null
                    }
                    if (localConversation != null) {
                        _effect.send(FriendsEffect.OpenConversation(localConversation.id))
                    } else {
                        _effect.send(FriendsEffect.ShowMessage(result.message))
                    }
                }
            }
            removeAction(actionKey)
        }
    }

    private fun FriendsIntent.UserSelected.toPendingDirectEffect(): FriendsEffect.OpenPendingDirectChat =
        FriendsEffect.OpenPendingDirectChat(
            partnerId = accountId,
            displayName = displayName,
            avatarUrl = avatarUrl
        )

    private fun initialize() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            ensureMyAccountId()
            refreshAllInternal(isRefresh = false)
        }
    }

    private suspend fun ensureMyAccountId() {
        if (secureStorage.getUserId() == null) {
            runCatching { userRepository.syncCurrentUser() }
        }
        _uiState.update { it.copy(myAccountId = secureStorage.getUserId()) }
    }

    private fun refreshAll(isRefresh: Boolean) {
        if (isRefresh && _uiState.value.isRefreshing) return
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            ensureMyAccountId()
            refreshAllInternal(isRefresh = isRefresh)
        }
    }

    private suspend fun refreshAllInternal(isRefresh: Boolean) {
        val showInitialLoading = !isRefresh && _uiState.value.friends.isEmpty()
        _uiState.update {
            it.copy(
                isLoading = showInitialLoading,
                isRefreshing = isRefresh,
                errorMessage = null
            )
        }

        val friendsError = syncFriendsFirstPage()
        val requestsError = syncFriendRequests()
        val blockedError = syncBlockedUsers()
        refreshVisibleSearch()

        val error = friendsError ?: requestsError ?: blockedError
        _uiState.update {
            it.copy(
                isLoading = false,
                isRefreshing = false,
                errorMessage = error
            )
        }
        if (error != null) {
            _effect.send(FriendsEffect.ShowMessage(error))
        }
    }

    private fun loadMoreFriends() {
        val state = _uiState.value
        if (state.isSearchMode || state.isLoadingMoreFriends || !state.hasMoreFriends) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreFriends = true) }
            when (val result = friendRepository.getFriends(limit = PAGE_SIZE, offset = state.nextFriendsOffset)) {
                is NetworkResult.Success -> {
                    val page = result.data
                    _uiState.update {
                        it.copy(
                            friends = it.friends + page.data,
                            totalFriends = page.total,
                            nextFriendsOffset = page.nextOffset,
                            hasMoreFriends = page.hasMore,
                            isLoadingMoreFriends = false
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoadingMoreFriends = false) }
                    _effect.send(FriendsEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun onSearchQueryChanged(query: String) {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                searchQuery = query,
                searchResults = if (query.isBlank()) emptyList() else it.searchResults,
                isSearching = query.isNotBlank() && it.selectedSection == FriendsSection.Friends
            )
        }

        if (query.isBlank()) {
            _uiState.update { it.copy(isSearching = false) }
            return
        }
        if (_uiState.value.selectedSection != FriendsSection.Friends) {
            _uiState.update { it.copy(isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            searchUsers(query)
        }
    }

    private suspend fun searchUsers(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return

        when (val result = friendRepository.searchUsers(query = trimmed, scope = "all")) {
            is NetworkResult.Success -> {
                _uiState.update {
                    if (it.searchQuery.trim() == trimmed) {
                        it.copy(searchResults = result.data.sortedForFriendsSearch(), isSearching = false)
                    } else {
                        it
                    }
                }
            }
            is NetworkResult.Error -> {
                _uiState.update { it.copy(isSearching = false) }
                _effect.send(FriendsEffect.ShowMessage(result.message))
            }
        }
    }

    private fun sendFriendRequest(accountId: String) {
        val actionKey = FriendActionKeys.send(accountId)
        if (_uiState.value.isActionLoading(actionKey)) return

        viewModelScope.launch {
            addAction(actionKey)
            when (val result = friendRepository.sendFriendRequest(accountId)) {
                is NetworkResult.Success -> {
                    val message = if (result.data.result == AUTO_ACCEPTED) {
                        "You are now friends"
                    } else {
                        "Friend request sent"
                    }
                    _effect.send(FriendsEffect.ShowMessage(message))
                    refreshAfterMutation(refreshBlocked = false)
                }
                is NetworkResult.Error -> _effect.send(FriendsEffect.ShowMessage(result.message))
            }
            removeAction(actionKey)
        }
    }

    private fun respondToRequest(
        actionKey: String,
        successMessage: String,
        refreshBlocked: Boolean = false,
        call: suspend () -> NetworkResult<Unit>
    ) {
        if (_uiState.value.isActionLoading(actionKey)) return

        viewModelScope.launch {
            addAction(actionKey)
            when (val result = call()) {
                is NetworkResult.Success -> {
                    _effect.send(FriendsEffect.ShowMessage(successMessage))
                    refreshAfterMutation(refreshBlocked = refreshBlocked)
                }
                is NetworkResult.Error -> _effect.send(FriendsEffect.ShowMessage(result.message))
            }
            removeAction(actionKey)
        }
    }

    private suspend fun refreshAfterMutation(refreshBlocked: Boolean) {
        syncFriendsFirstPage()
        syncFriendRequests()
        if (refreshBlocked) {
            syncBlockedUsers()
        }
        refreshVisibleSearch()
    }

    private fun refreshSection(section: FriendsSection) {
        viewModelScope.launch {
            when (section) {
                FriendsSection.Friends -> {
                    syncFriendsFirstPage()
                    refreshVisibleSearch()
                }
                FriendsSection.Received,
                FriendsSection.Sent -> syncFriendRequests()
                FriendsSection.Blocked -> syncBlockedUsers()
            }
        }
    }

    private suspend fun refreshVisibleSearch() {
        val query = _uiState.value.searchQuery
        if (query.isNotBlank() && _uiState.value.selectedSection == FriendsSection.Friends) {
            _uiState.update { it.copy(isSearching = true) }
            searchUsers(query)
        }
    }

    private suspend fun syncFriendsFirstPage(): String? {
        return when (val result = friendRepository.getFriends(limit = PAGE_SIZE, offset = 0)) {
            is NetworkResult.Success -> {
                applyFriendsPage(result.data)
                null
            }
            is NetworkResult.Error -> result.message
        }
    }

    private fun applyFriendsPage(page: OffsetPage<PublicUserProfile>) {
        _uiState.update {
            it.copy(
                friends = page.data,
                totalFriends = page.total,
                friendsLimit = page.limit,
                nextFriendsOffset = page.nextOffset,
                hasMoreFriends = page.hasMore
            )
        }
    }

    private suspend fun syncFriendRequests(): String? {
        return when (val result = friendRepository.getFriendRequests()) {
            is NetworkResult.Success -> {
                applyFriendRequests(result.data)
                null
            }
            is NetworkResult.Error -> result.message
        }
    }

    private fun applyFriendRequests(requests: List<FriendRequest>) {
        val accountId = _uiState.value.myAccountId
        _uiState.update {
            it.copy(
                incomingRequests = requests.filter { request -> request.receiverId == accountId },
                outgoingRequests = requests.filter { request -> request.senderId == accountId }
            )
        }
    }

    private suspend fun syncBlockedUsers(): String? {
        return when (val result = friendRepository.getBlockedUsers()) {
            is NetworkResult.Success -> {
                _uiState.update { it.copy(blockedUsers = result.data) }
                null
            }
            is NetworkResult.Error -> result.message
        }
    }

    private fun observeSocketEvents() {
        viewModelScope.launch {
            socketManager.events.collect { event ->
                when (event) {
                    SocketEvent.FriendUpdated -> refreshAfterMutation(refreshBlocked = true)
                    is SocketEvent.PresenceStatus -> applyPresence(event)
                    is SocketEvent.NewNotification -> {
                        if (event.type.isFriendRelatedNotificationType()) {
                            refreshAfterMutation(refreshBlocked = false)
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun applyPresence(event: SocketEvent.PresenceStatus) {
        val accountId = event.accountId?.takeIf { it.isNotBlank() }
            ?: event.userId?.takeIf { it.isNotBlank() }
            ?: return
        val isOnline = event.isOnline ?: event.status.equals("online", ignoreCase = true)
        _uiState.update {
            it.copy(
                friends = it.friends.map { friend ->
                    if (friend.id == accountId) {
                        friend.copy(isOnline = isOnline)
                    } else {
                        friend
                    }
                }
            )
        }
    }

    private fun addAction(actionKey: String) {
        _uiState.update { it.copy(actionKeysInProgress = it.actionKeysInProgress + actionKey) }
    }

    private fun removeAction(actionKey: String) {
        _uiState.update { it.copy(actionKeysInProgress = it.actionKeysInProgress - actionKey) }
    }

    private fun String.isFriendRelatedNotificationType(): Boolean {
        val normalized = normalizeNotificationType()
        return isFriendRequestReceivedType() ||
            isFriendRequestAcceptedType() ||
            normalized.startsWith("friend_") ||
            normalized.contains("friend_request")
    }

    private fun List<com.thuo_ng.swift_chat_android.domain.model.SearchUser>.sortedForFriendsSearch():
        List<com.thuo_ng.swift_chat_android.domain.model.SearchUser> {
        return withIndex()
            .sortedWith(
                compareBy<IndexedValue<com.thuo_ng.swift_chat_android.domain.model.SearchUser>> {
                    when {
                        it.value.isFriend == true -> 0
                        it.value.friendRequestStatus != null -> 1
                        else -> 2
                    }
                }.thenBy { it.index }
            )
            .map { it.value }
    }
}
