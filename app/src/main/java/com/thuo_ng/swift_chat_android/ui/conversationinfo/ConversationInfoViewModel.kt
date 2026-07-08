package com.thuo_ng.swift_chat_android.ui.conversationinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thuo_ng.swift_chat_android.core.network.NetworkResult
import com.thuo_ng.swift_chat_android.core.socket.SocketEvent
import com.thuo_ng.swift_chat_android.core.socket.SocketManager
import com.thuo_ng.swift_chat_android.core.storage.SecureStorage
import com.thuo_ng.swift_chat_android.domain.model.Conversation
import com.thuo_ng.swift_chat_android.domain.model.ConversationMember
import com.thuo_ng.swift_chat_android.domain.model.Message
import com.thuo_ng.swift_chat_android.domain.model.MessageAttachment
import com.thuo_ng.swift_chat_android.domain.model.MuteDuration
import com.thuo_ng.swift_chat_android.domain.repository.ChatRepository
import com.thuo_ng.swift_chat_android.domain.repository.ConversationRepository
import com.thuo_ng.swift_chat_android.domain.repository.FriendRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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

@HiltViewModel
class ConversationInfoViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val chatRepository: ChatRepository,
    private val friendRepository: FriendRepository,
    private val secureStorage: SecureStorage,
    private val socketManager: SocketManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ConversationInfoUiState(currentAccountId = secureStorage.getUserId())
    )
    val uiState: StateFlow<ConversationInfoUiState> = _uiState.asStateFlow()

    private val _effect = Channel<ConversationInfoEffect>(Channel.BUFFERED)
    val effect: Flow<ConversationInfoEffect> = _effect.receiveAsFlow()

    private var conversationJob: Job? = null
    private var messagesJob: Job? = null
    private var socketJob: Job? = null
    private var addMemberSearchJob: Job? = null
    private var activeConversationId: String? = null

    fun handleIntent(intent: ConversationInfoIntent) {
        when (intent) {
            is ConversationInfoIntent.Start -> start(intent.conversationId)
            ConversationInfoIntent.Refresh -> refresh()
            is ConversationInfoIntent.MuteSwitchChanged -> onMuteSwitchChanged(intent.checked)
            is ConversationInfoIntent.MuteDurationSelected -> mute(intent.duration)
            ConversationInfoIntent.DismissMuteDurationSheet -> {
                _uiState.update { it.copy(showMuteDurationSheet = false) }
            }
            ConversationInfoIntent.OpenAddMembers -> openAddMembers()
            ConversationInfoIntent.DismissAddMembers -> dismissAddMembers()
            is ConversationInfoIntent.AddMemberSearchChanged -> onAddMemberSearchChanged(intent.query)
            is ConversationInfoIntent.ToggleAddMemberSelection -> toggleAddMemberSelection(intent.accountId)
            ConversationInfoIntent.SubmitAddMembers -> submitAddMembers()
            ConversationInfoIntent.ShowAllMembers -> {
                _uiState.update { it.copy(showAllMembersSheet = true, allMembersSearchQuery = "") }
            }
            ConversationInfoIntent.DismissAllMembers -> {
                _uiState.update { it.copy(showAllMembersSheet = false, allMembersSearchQuery = "") }
            }
            is ConversationInfoIntent.AllMembersSearchChanged -> {
                _uiState.update { it.copy(allMembersSearchQuery = intent.query) }
            }
            is ConversationInfoIntent.KickMember -> kickMember(intent.accountId)
            is ConversationInfoIntent.ChangeMemberRole -> changeMemberRole(intent.accountId, intent.role)
            is ConversationInfoIntent.TransferLeadership -> transferLeadership(intent.accountId)
            ConversationInfoIntent.LeaveGroup -> leaveGroup()
            ConversationInfoIntent.DeleteConversation -> deleteConversation()
        }
    }

    private fun start(conversationId: String) {
        if (activeConversationId == conversationId) return

        activeConversationId = conversationId
        conversationJob?.cancel()
        messagesJob?.cancel()
        socketJob?.cancel()
        addMemberSearchJob?.cancel()

        _uiState.update {
            ConversationInfoUiState(
                conversationId = conversationId,
                currentAccountId = secureStorage.getUserId(),
                isLoading = true
            )
        }

        conversationJob = viewModelScope.launch {
            conversationRepository.observeConversation(conversationId).collect { conversation ->
                applyConversation(conversation)
            }
        }

        messagesJob = viewModelScope.launch {
            chatRepository.observeMessages(conversationId).collect { messages ->
                applyMediaPreview(messages)
            }
        }

        socketJob = viewModelScope.launch {
            socketManager.events.collect { event ->
                handleSocketEvent(event)
            }
        }

        refresh()
        syncLatestMessagesForMedia(conversationId)
    }

    private fun applyConversation(conversation: Conversation?) {
        _uiState.update {
            it.copy(
                conversation = conversation,
                isLoading = false,
                errorMessage = null
            )
        }
        if (conversation?.type.equals("group", ignoreCase = true) && _uiState.value.members.isEmpty()) {
            loadMembers()
        }
    }

    private fun refresh() {
        val conversationId = activeConversationId ?: return
        if (_uiState.value.isActionLoading(ConversationInfoActionKeys.Refresh)) return

        viewModelScope.launch {
            addAction(ConversationInfoActionKeys.Refresh)
            when (val result = conversationRepository.syncConversations(
                preserveConversationIds = setOf(conversationId)
            )) {
                is NetworkResult.Success -> {
                    if (_uiState.value.isGroup) loadMembers()
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message, isLoading = false) }
                    _effect.send(ConversationInfoEffect.ShowMessage(result.message))
                }
            }
            if (_uiState.value.conversationId == conversationId) {
                removeAction(ConversationInfoActionKeys.Refresh)
            }
        }
    }

    private fun loadMembers() {
        val conversationId = activeConversationId ?: return
        if (_uiState.value.isRefreshingMembers) return

        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingMembers = true, errorMessage = null) }
            when (val result = conversationRepository.getConversationMembers(conversationId)) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(members = result.data.sortedMembers(), isRefreshingMembers = false)
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(isRefreshingMembers = false, errorMessage = result.message)
                    }
                    _effect.send(ConversationInfoEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun syncLatestMessagesForMedia(conversationId: String) {
        viewModelScope.launch {
            when (val result = chatRepository.syncLatestMessages(conversationId)) {
                is NetworkResult.Success -> Unit
                is NetworkResult.Error -> Unit
            }
        }
    }

    private fun applyMediaPreview(messages: List<Message>) {
        val attachments = messages
            .filterNot { it.isDeleted || it.isUnsent }
            .flatMap { message ->
                message.attachments.map { attachment -> message to attachment }
            }
        val imageUrls = attachments
            .filter { (message, attachment) -> attachment.isImageAttachment(message) }
            .sortedByDescending { (message, _) -> message.createdAt }
            .map { (_, attachment) -> ConversationMediaPreview(attachment.url) }
            .distinctBy { it.url }
            .take(6)
        val documents = attachments.count { (message, attachment) ->
            !attachment.isImageAttachment(message)
        }

        _uiState.update {
            it.copy(
                mediaPreviews = imageUrls,
                documentCount = documents
            )
        }
    }

    private fun onMuteSwitchChanged(checked: Boolean) {
        if (checked) {
            _uiState.update { it.copy(showMuteDurationSheet = true) }
        } else {
            unmute()
        }
    }

    private fun mute(duration: MuteDuration) {
        val conversationId = activeConversationId ?: return
        if (_uiState.value.isActionLoading(ConversationInfoActionKeys.Mute)) return

        viewModelScope.launch {
            _uiState.update { it.copy(showMuteDurationSheet = false) }
            addAction(ConversationInfoActionKeys.Mute)
            when (val result = conversationRepository.muteConversation(conversationId, duration)) {
                is NetworkResult.Success -> _effect.send(ConversationInfoEffect.ShowMessage("Notifications muted"))
                is NetworkResult.Error -> _effect.send(ConversationInfoEffect.ShowMessage(result.message))
            }
            removeAction(ConversationInfoActionKeys.Mute)
        }
    }

    private fun unmute() {
        val conversationId = activeConversationId ?: return
        if (_uiState.value.isActionLoading(ConversationInfoActionKeys.Unmute)) return

        viewModelScope.launch {
            addAction(ConversationInfoActionKeys.Unmute)
            when (val result = conversationRepository.unmuteConversation(conversationId)) {
                is NetworkResult.Success -> _effect.send(ConversationInfoEffect.ShowMessage("Notifications unmuted"))
                is NetworkResult.Error -> _effect.send(ConversationInfoEffect.ShowMessage(result.message))
            }
            removeAction(ConversationInfoActionKeys.Unmute)
        }
    }

    private fun openAddMembers() {
        _uiState.update {
            it.copy(
                showAddMembersSheet = true,
                addMemberSearchQuery = "",
                addMemberSearchResults = emptyList(),
                selectedAddMemberIds = emptySet()
            )
        }
        loadFriendsForAddMembers()
    }

    private fun dismissAddMembers() {
        addMemberSearchJob?.cancel()
        _uiState.update {
            it.copy(
                showAddMembersSheet = false,
                addMemberSearchQuery = "",
                addMemberSearchResults = emptyList(),
                selectedAddMemberIds = emptySet(),
                isSearchingAddMembers = false
            )
        }
    }

    private fun loadFriendsForAddMembers() {
        if (_uiState.value.isLoadingFriends) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFriends = true) }
            when (val result = friendRepository.getFriends(limit = ADD_MEMBER_PAGE_SIZE, offset = 0)) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(friends = result.data.data, isLoadingFriends = false)
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoadingFriends = false) }
                    _effect.send(ConversationInfoEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun onAddMemberSearchChanged(query: String) {
        addMemberSearchJob?.cancel()
        _uiState.update {
            it.copy(
                addMemberSearchQuery = query,
                addMemberSearchResults = if (query.isBlank()) emptyList() else it.addMemberSearchResults,
                isSearchingAddMembers = query.isNotBlank()
            )
        }

        if (query.isBlank()) {
            _uiState.update { it.copy(isSearchingAddMembers = false) }
            return
        }

        addMemberSearchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            when (val result = friendRepository.searchUsers(query = query.trim(), scope = "all")) {
                is NetworkResult.Success -> _uiState.update {
                    if (it.addMemberSearchQuery.trim() == query.trim()) {
                        it.copy(addMemberSearchResults = result.data, isSearchingAddMembers = false)
                    } else {
                        it
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isSearchingAddMembers = false) }
                    _effect.send(ConversationInfoEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun toggleAddMemberSelection(accountId: String) {
        _uiState.update { state ->
            val nextSelection = if (accountId in state.selectedAddMemberIds) {
                state.selectedAddMemberIds - accountId
            } else {
                state.selectedAddMemberIds + accountId
            }
            state.copy(selectedAddMemberIds = nextSelection)
        }
    }

    private fun submitAddMembers() {
        val conversationId = activeConversationId ?: return
        val selectedIds = _uiState.value.selectedAddMemberIds.toList()
        if (selectedIds.isEmpty() || _uiState.value.isActionLoading(ConversationInfoActionKeys.AddMembers)) return

        viewModelScope.launch {
            addAction(ConversationInfoActionKeys.AddMembers)
            when (val result = conversationRepository.addMembers(conversationId, selectedIds)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            members = result.data.sortedMembers(),
                            showAddMembersSheet = false,
                            selectedAddMemberIds = emptySet(),
                            addMemberSearchQuery = "",
                            addMemberSearchResults = emptyList()
                        )
                    }
                    _effect.send(ConversationInfoEffect.ShowMessage("Members added"))
                }
                is NetworkResult.Error -> _effect.send(ConversationInfoEffect.ShowMessage(result.message))
            }
            removeAction(ConversationInfoActionKeys.AddMembers)
        }
    }

    private fun kickMember(accountId: String) {
        val conversationId = activeConversationId ?: return
        val actionKey = ConversationInfoActionKeys.kick(accountId)
        if (_uiState.value.isActionLoading(actionKey)) return

        viewModelScope.launch {
            addAction(actionKey)
            when (val result = conversationRepository.kickMember(conversationId, accountId)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(members = result.data.sortedMembers()) }
                    _effect.send(ConversationInfoEffect.ShowMessage("Member removed"))
                }
                is NetworkResult.Error -> _effect.send(ConversationInfoEffect.ShowMessage(result.message))
            }
            removeAction(actionKey)
        }
    }

    private fun changeMemberRole(accountId: String, role: String) {
        val conversationId = activeConversationId ?: return
        val actionKey = ConversationInfoActionKeys.role(accountId)
        if (_uiState.value.isActionLoading(actionKey)) return

        viewModelScope.launch {
            addAction(actionKey)
            when (val result = conversationRepository.changeMemberRole(conversationId, accountId, role)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(members = result.data.sortedMembers()) }
                    _effect.send(ConversationInfoEffect.ShowMessage("Role updated"))
                }
                is NetworkResult.Error -> _effect.send(ConversationInfoEffect.ShowMessage(result.message))
            }
            removeAction(actionKey)
        }
    }

    private fun transferLeadership(accountId: String) {
        val conversationId = activeConversationId ?: return
        val actionKey = ConversationInfoActionKeys.transfer(accountId)
        if (_uiState.value.isActionLoading(actionKey)) return

        viewModelScope.launch {
            addAction(actionKey)
            when (val result = conversationRepository.transferLeadership(conversationId, accountId)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(members = result.data.sortedMembers()) }
                    _effect.send(ConversationInfoEffect.ShowMessage("Leadership transferred"))
                }
                is NetworkResult.Error -> _effect.send(ConversationInfoEffect.ShowMessage(result.message))
            }
            removeAction(actionKey)
        }
    }

    private fun leaveGroup() {
        val conversationId = activeConversationId ?: return
        if (_uiState.value.isActionLoading(ConversationInfoActionKeys.LeaveGroup)) return

        viewModelScope.launch {
            addAction(ConversationInfoActionKeys.LeaveGroup)
            when (val result = conversationRepository.leaveGroup(conversationId)) {
                is NetworkResult.Success -> {
                    _effect.send(ConversationInfoEffect.ShowMessage("You left the group"))
                    _effect.send(ConversationInfoEffect.ConversationClosed)
                }
                is NetworkResult.Error -> _effect.send(ConversationInfoEffect.ShowMessage(result.message))
            }
            removeAction(ConversationInfoActionKeys.LeaveGroup)
        }
    }

    private fun deleteConversation() {
        val conversationId = activeConversationId ?: return
        if (_uiState.value.isActionLoading(ConversationInfoActionKeys.DeleteConversation)) return

        viewModelScope.launch {
            addAction(ConversationInfoActionKeys.DeleteConversation)
            val wasGroup = _uiState.value.isGroup
            when (val result = conversationRepository.deleteConversation(conversationId)) {
                is NetworkResult.Success -> {
                    _effect.send(
                        ConversationInfoEffect.ShowMessage(
                            if (wasGroup) "Group disbanded" else "Conversation hidden"
                        )
                    )
                    _effect.send(ConversationInfoEffect.ConversationClosed)
                }
                is NetworkResult.Error -> _effect.send(ConversationInfoEffect.ShowMessage(result.message))
            }
            removeAction(ConversationInfoActionKeys.DeleteConversation)
        }
    }

    private fun handleSocketEvent(event: SocketEvent) {
        val conversationId = activeConversationId ?: return
        when (event) {
            is SocketEvent.GroupDisbanded -> {
                if (event.conversationId == conversationId) {
                    viewModelScope.launch {
                        _effect.send(ConversationInfoEffect.ShowMessage("This group has been disbanded"))
                        _effect.send(ConversationInfoEffect.ConversationClosed)
                    }
                }
            }
            is SocketEvent.GroupMemberRemoved -> {
                if (event.conversationId == conversationId) {
                    if (event.removedUserId == secureStorage.getUserId()) {
                        viewModelScope.launch {
                            _effect.send(ConversationInfoEffect.ShowMessage("You were removed from this group"))
                            _effect.send(ConversationInfoEffect.ConversationClosed)
                        }
                    } else {
                        loadMembers()
                    }
                }
            }
            is SocketEvent.GroupInfoUpdated,
            is SocketEvent.GroupMemberAdded,
            is SocketEvent.GroupRoleChanged,
            is SocketEvent.GroupYouAdded -> {
                if (event.conversationId() == conversationId) {
                    refresh()
                    loadMembers()
                }
            }
            else -> Unit
        }
    }

    private fun addAction(actionKey: String) {
        _uiState.update { it.copy(actionKeysInProgress = it.actionKeysInProgress + actionKey) }
    }

    private fun removeAction(actionKey: String) {
        _uiState.update { it.copy(actionKeysInProgress = it.actionKeysInProgress - actionKey) }
    }

    private fun List<ConversationMember>.sortedMembers(): List<ConversationMember> =
        sortedWith(
            compareBy<ConversationMember> { member ->
                when (member.role.normalizedRole()) {
                    "leader" -> 0
                    "deputy" -> 1
                    else -> 2
                }
            }.thenBy { it.displayLabel.lowercase() }
        )

    private fun SocketEvent.conversationId(): String? =
        when (this) {
            is SocketEvent.GroupInfoUpdated -> conversationId
            is SocketEvent.GroupMemberAdded -> conversationId
            is SocketEvent.GroupRoleChanged -> conversationId
            is SocketEvent.GroupYouAdded -> conversationId
            else -> null
        }

    private fun MessageAttachment.isImageAttachment(message: Message): Boolean {
        if (message.type.equals("image", ignoreCase = true)) return true
        val cleanUrl = url.substringBefore('?').substringBefore('#').lowercase()
        return IMAGE_EXTENSIONS.any { cleanUrl.endsWith(it) }
    }

    private companion object {
        const val ADD_MEMBER_PAGE_SIZE = 50
        const val SEARCH_DEBOUNCE_MS = 300L
        val IMAGE_EXTENSIONS = setOf(
            ".jpg",
            ".jpeg",
            ".png",
            ".gif",
            ".webp",
            ".bmp",
            ".heic"
        )
    }
}
