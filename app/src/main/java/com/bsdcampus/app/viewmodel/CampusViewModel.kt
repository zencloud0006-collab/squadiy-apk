package com.bsdcampus.app.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdcampus.app.BuildConfig
import com.bsdcampus.app.data.AppUpdateDownloadState
import com.bsdcampus.app.data.AppUpdateManager
import com.bsdcampus.app.data.CampusRepository
import com.bsdcampus.app.data.MediaUtils
import com.bsdcampus.app.data.PreferencesRepository
import com.bsdcampus.app.data.StoryMediaEditor
import com.bsdcampus.app.data.StoryMusicTrack
import com.bsdcampus.app.model.AppUpdateConfig
import com.bsdcampus.app.model.CampusAnalytics
import com.bsdcampus.app.model.CampusConstants
import com.bsdcampus.app.model.CampusFollow
import com.bsdcampus.app.model.CampusNote
import com.bsdcampus.app.model.CampusNotification
import com.bsdcampus.app.model.CampusPost
import com.bsdcampus.app.model.CampusReel
import com.bsdcampus.app.model.CampusReport
import com.bsdcampus.app.model.CampusStory
import com.bsdcampus.app.model.CampusTab
import com.bsdcampus.app.model.CampusUser
import com.bsdcampus.app.model.ChatBackgroundPreset
import com.bsdcampus.app.model.ChatPreference
import com.bsdcampus.app.model.ConversationSummary
import com.bsdcampus.app.model.DirectMessage
import com.bsdcampus.app.model.HighlightMediaItem
import com.bsdcampus.app.model.NoteBubbleColor
import com.bsdcampus.app.model.PostImageRatio
import com.bsdcampus.app.model.StoryGroup
import com.bsdcampus.app.model.StoryHighlight
import com.bsdcampus.app.model.UiState
import com.bsdcampus.app.model.UserPrivateInfo
import com.bsdcampus.app.notifications.MediaUploadKind
import com.bsdcampus.app.notifications.MediaUploadTracker
import com.bsdcampus.app.notifications.PostDeleteWorker
import com.bsdcampus.app.notifications.PostReelUploadWorker
import com.bsdcampus.app.notifications.StagedDraftMediaInput
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class CampusViewModel(
    private val repository: CampusRepository,
    val currentUserId: String,
    val googleDisplayName: String,
    val googleEmail: String,
    val isEmailVerified: Boolean,
    val googleAvatar: String?,
    private val preferencesRepository: PreferencesRepository? = null,
    private val appContext: Context? = null
) : ViewModel() {

    private val _activeTab = MutableStateFlow(CampusTab.FEED)
    val activeTab: StateFlow<CampusTab> = _activeTab.asStateFlow()

    private val tabHistory = mutableListOf<CampusTab>()

    private val _viewingProfileUserId = MutableStateFlow<String?>(null)
    val viewingProfileUserId: StateFlow<String?> = _viewingProfileUserId.asStateFlow()

    private val _connectionsTarget = MutableStateFlow<Pair<String, Int>?>(null)
    val connectionsTarget: StateFlow<Pair<String, Int>?> = _connectionsTarget.asStateFlow()

    private val _activeChatUserId = MutableStateFlow<String?>(null)
    val activeChatUserId: StateFlow<String?> = _activeChatUserId.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _isRadialMenuOpen = MutableStateFlow(false)
    val isRadialMenuOpen: StateFlow<Boolean> = _isRadialMenuOpen.asStateFlow()

    private val _reportTarget = MutableStateFlow<Triple<String, String, String>?>(null)
    val reportTarget: StateFlow<Triple<String, String, String>?> = _reportTarget.asStateFlow()

    private val _adminEmailsMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val adminEmailsMap: StateFlow<Map<String, String>> = _adminEmailsMap.asStateFlow()

    private val _locallyReadNotificationIds = MutableStateFlow<Set<String>>(emptySet())

    val appUpdateConfig: StateFlow<AppUpdateConfig?> = repository.observeAppUpdateConfig()
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val _updateDownloadState = MutableStateFlow<AppUpdateDownloadState>(AppUpdateDownloadState.Idle)
    val updateDownloadState: StateFlow<AppUpdateDownloadState> = _updateDownloadState.asStateFlow()

    private val _dismissedUpdateVersionCode = MutableStateFlow<Int?>(null)
    private val _manualUpdateDialogRequested = MutableStateFlow(false)

    private val _isCheckingForUpdates = MutableStateFlow(false)
    val isCheckingForUpdates: StateFlow<Boolean> = _isCheckingForUpdates.asStateFlow()

    val showUpdateDialog: StateFlow<Boolean> = combine(
        appUpdateConfig,
        _updateDownloadState,
        _dismissedUpdateVersionCode,
        _manualUpdateDialogRequested
    ) { config, dlState, dismissedVer, manualReq ->
        val hasNewer = config != null && config.latestVersionCode > BuildConfig.VERSION_CODE
        val inProgress = dlState !is AppUpdateDownloadState.Idle
        when {
            inProgress -> true
            manualReq && config != null && hasNewer -> true
            hasNewer && (config!!.isMandatory || dismissedVer != config.latestVersionCode) -> true
            else -> false
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    private val rawProfiles: StateFlow<UiState<List<CampusUser>>> = repository.observeProfiles()
        .map<List<CampusUser>, UiState<List<CampusUser>>> { UiState.Success(it) }
        .catch { err ->
            val firestoreErr = unwrapFirestoreError(err)
            if (firestoreErr == null && err is CancellationException) throw err
            val root = firestoreErr ?: err
            Log.w(TAG, "Error observing profiles", root)
            emit(UiState.Error(root.message ?: "Failed to load campus directory"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val follows: StateFlow<List<CampusFollow>> = repository.observeFollows()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val privateInfo: StateFlow<UserPrivateInfo?> = repository.observePrivateInfo(currentUserId)
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val allUsersState: StateFlow<UiState<List<CampusUser>>> = combine(
        rawProfiles,
        follows,
        privateInfo,
        _adminEmailsMap
    ) { profState, followList, priv, emailMap ->
        when (profState) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> profState
            is UiState.Success -> {
                val followersByTarget = followList.groupBy { it.targetId }.mapValues { e -> e.value.map { it.followerId } }
                val followingByActor = followList.groupBy { it.followerId }.mapValues { e -> e.value.map { it.targetId } }
                val enriched = profState.data.map { u ->
                    val mergedEmail = when {
                        u.uid == currentUserId -> priv?.email?.takeIf { it.isNotBlank() } ?: googleEmail
                        else -> emailMap[u.uid] ?: ""
                    }
                    u.copy(
                        email = mergedEmail,
                        followers = followersByTarget[u.uid] ?: emptyList(),
                        following = followingByActor[u.uid] ?: emptyList()
                    )
                }
                UiState.Success(enriched)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val isVerifiedSuperAdmin: Boolean =
        googleEmail.equals(CampusConstants.SUPER_ADMIN_EMAIL, ignoreCase = true) && isEmailVerified

    private val hasAdminMarker: StateFlow<Boolean> = repository.observeIsAdminMarker(currentUserId)
        .catch { emit(false) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isCurrentUserAdmin: StateFlow<Boolean> = hasAdminMarker
        .map { markerExists -> markerExists || isVerifiedSuperAdmin }
        .stateIn(viewModelScope, SharingStarted.Eagerly, isVerifiedSuperAdmin)

    val currentUser: StateFlow<CampusUser?> = allUsersState.map { state ->
        (state as? UiState.Success)?.data?.firstOrNull { it.uid == currentUserId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val _isRefreshingFeed = MutableStateFlow(false)
    val isRefreshingFeed: StateFlow<Boolean> = _isRefreshingFeed.asStateFlow()

    private val _feedRefreshGeneration = MutableStateFlow(0L)
    private val _serverRefreshPosts = MutableStateFlow<List<CampusPost>?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val livePostsState: StateFlow<UiState<List<CampusPost>>> = _feedRefreshGeneration
        .flatMapLatest {
            repository.observePosts()
                .onEach { _serverRefreshPosts.value = null }
                .map<List<CampusPost>, UiState<List<CampusPost>>> { UiState.Success(it) }
                .catch { err ->
                    val firestoreErr = unwrapFirestoreError(err)
                    if (firestoreErr == null && err is CancellationException) throw err
                    emit(UiState.Error((firestoreErr ?: err).message ?: "Failed to load posts"))
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val pendingDeletedPostIds: StateFlow<Set<String>> = (
        preferencesRepository?.pendingDeletedPostIds ?: flowOf(emptySet())
        ).stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val postsState: StateFlow<UiState<List<CampusPost>>> = combine(
        livePostsState,
        _serverRefreshPosts,
        pendingDeletedPostIds,
        privateInfo
    ) { live, serverOverride, deletedIds, priv ->
        val blockedIds = priv?.blockedUserIds.orEmpty().toSet()
        val raw = if (serverOverride != null) UiState.Success(serverOverride) else live
        if (raw !is UiState.Success || (deletedIds.isEmpty() && blockedIds.isEmpty())) {
            raw
        } else {
            UiState.Success(
                raw.data.filterNot { deletedIds.contains(it.id) || blockedIds.contains(it.authorId) }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val rawReelsState: StateFlow<UiState<List<CampusReel>>> = repository.observeReels()
        .map<List<CampusReel>, UiState<List<CampusReel>>> { UiState.Success(it) }
        .catch { err ->
            val firestoreErr = unwrapFirestoreError(err)
            if (firestoreErr == null && err is CancellationException) throw err
            emit(UiState.Error((firestoreErr ?: err).message ?: "Failed to load reels"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val reelsState: StateFlow<UiState<List<CampusReel>>> = combine(
        rawReelsState,
        privateInfo
    ) { reelsUi, priv ->
        val blockedIds = priv?.blockedUserIds.orEmpty().toSet()
        if (reelsUi is UiState.Success && blockedIds.isNotEmpty()) {
            UiState.Success(reelsUi.data.filterNot { blockedIds.contains(it.authorId) })
        } else {
            reelsUi
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val rawStories: StateFlow<List<CampusStory>> = _feedRefreshGeneration
        .flatMapLatest { repository.observeStories() }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val highlights: StateFlow<List<StoryHighlight>> = repository.observeHighlights()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // All non-expired active stories indexed by authorId (used when viewing public user profiles directly)
    val storiesByAuthorId: StateFlow<Map<String, StoryGroup>> = combine(
        rawStories,
        allUsersState,
        privateInfo
    ) { storyList, usersState, priv ->
        val blockedIds = priv?.blockedUserIds.orEmpty().toSet()
        val users = (usersState as? UiState.Success)?.data.orEmpty()
        val userById = users.associateBy { it.uid }
        val now = System.currentTimeMillis()
        storyList
            .filter { story -> !story.isExpired(now) && !blockedIds.contains(story.authorId) }
            .groupBy { it.authorId }
            .mapNotNull { (authorId, authorStories) ->
                val author = userById[authorId] ?: return@mapNotNull null
                if (author.isSuspended && author.uid != currentUserId) return@mapNotNull null
                val ordered = authorStories.sortedBy { it.createdAt ?: Timestamp(0, 0) }
                val hasUnseen = ordered.any { story ->
                    story.authorId != currentUserId && !story.viewedBy.contains(currentUserId)
                }
                authorId to StoryGroup(
                    author = author,
                    stories = ordered,
                    hasUnseen = hasUnseen,
                    isOwn = authorId == currentUserId
                )
            }
            .toMap()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    // Strictly show stories ONLY from the current user and users that currentUser follows on Home Feed
    val storyGroups: StateFlow<List<StoryGroup>> = combine(
        storiesByAuthorId,
        currentUser
    ) { byAuthor, me ->
        val currentMe = me ?: return@combine emptyList()
        val allowedAuthors = (currentMe.following + currentUserId).toHashSet()
        byAuthor.values
            .filter { it.author.uid in allowedAuthors }
            .sortedWith(
                compareByDescending<StoryGroup> { it.isOwn }
                    .thenByDescending { it.hasUnseen }
                    .thenByDescending { group ->
                        group.stories.maxOfOrNull { it.createdAt?.toDate()?.time ?: 0L } ?: 0L
                    }
            )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val rawNotes: StateFlow<List<CampusNote>> = repository.observeCampusNotes()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Mutual-follow privacy for Chat Notes: show only own note OR mutual followers (I follow them & they follow me)
    val visibleNotes: StateFlow<List<CampusNote>> = combine(
        rawNotes,
        allUsersState,
        privateInfo
    ) { notesList, usersState, priv ->
        val blockedIds = priv?.blockedUserIds.orEmpty().toSet()
        val users = (usersState as? UiState.Success)?.data.orEmpty()
        val userById = users.associateBy { it.uid }
        val me = userById[currentUserId] ?: return@combine emptyList()
        val now = System.currentTimeMillis()
        notesList.filter { note ->
            if (note.isExpired(now)) return@filter false
            if (note.authorId == currentUserId) return@filter true
            if (blockedIds.contains(note.authorId)) return@filter false
            val author = userById[note.authorId] ?: return@filter false
            if (author.isSuspended) return@filter false
            me.following.contains(note.authorId) && me.followers.contains(note.authorId)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notifications: StateFlow<List<CampusNotification>> = combine(
        repository.observeNotifications(currentUserId),
        allUsersState,
        _locallyReadNotificationIds
    ) { notifList, usersState, localReadIds ->
        val users = (usersState as? UiState.Success)?.data.orEmpty()
        val userById = users.associateBy { it.uid }
        notifList.map { notif ->
            val liveSender = userById[notif.senderId]
            val resolvedName = when {
                liveSender?.isBlackSociety == true -> "Member - ${liveSender.blackSocietyNumber ?: 1}"
                liveSender != null -> liveSender.name.ifBlank { notif.senderName }
                else -> notif.senderName
            }
            val resolvedAvatar = when {
                liveSender?.isBlackSociety == true -> null
                liveSender != null -> liveSender.avatar
                else -> notif.senderAvatar
            }
            val resolvedRead = notif.read || (notif.id in localReadIds)
            notif.copy(
                senderName = resolvedName,
                senderAvatar = resolvedAvatar,
                read = resolvedRead
            )
        }
    }.catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val messages: StateFlow<List<DirectMessage>> = repository.observeMessages(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val typingMap: StateFlow<Map<String, Boolean>> = repository.observeTypingStatus(currentUserId)
        .catch { emit(emptyMap()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    val chatPreferences: StateFlow<Map<String, ChatPreference>> = combine(
        repository.observeChatPreferences(currentUserId).catch { emit(emptyMap()) },
        repository.observeSharedChatThreads(currentUserId).catch { emit(emptyMap()) }
    ) { legacy, shared ->
        legacy + shared
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    val reports: StateFlow<List<CampusReport>> = isCurrentUserAdmin
        .flatMapLatest { admin ->
            repository.observeReports(isAdmin = admin, uid = currentUserId)
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = notifications.map { list -> list.count { !it.read } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    val unreadMessagesCount: StateFlow<Int> = messages.map { list ->
        list.count { it.recipientId == currentUserId && !it.read }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    val conversations: StateFlow<List<ConversationSummary>> = combine(
        messages,
        allUsersState
    ) { msgList, usersState ->
        val users = (usersState as? UiState.Success)?.data.orEmpty()
        val userById = users.associateBy { it.uid }
        val grouped = msgList.groupBy { msg ->
            if (msg.senderId == currentUserId) msg.recipientId else msg.senderId
        }
        grouped.mapNotNull { (otherId, thread) ->
            val otherUser = userById[otherId] ?: return@mapNotNull null
            val last = thread.maxByOrNull { it.createdAt?.toDate()?.time ?: Long.MAX_VALUE } ?: return@mapNotNull null
            val unread = thread.count { it.recipientId == currentUserId && !it.read }
            ConversationSummary(otherUser = otherUser, lastMessage = last, unreadCount = unread)
        }.sortedByDescending { it.lastMessage.createdAt?.toDate()?.time ?: Long.MAX_VALUE }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val analytics: StateFlow<CampusAnalytics> = combine(
        allUsersState,
        postsState,
        reports
    ) { uState, pState, rList ->
        val users = (uState as? UiState.Success)?.data.orEmpty()
        val posts = (pState as? UiState.Success)?.data.orEmpty()
        val catCounts = CampusConstants.POST_CATEGORIES.map { cat ->
            cat to posts.count { it.category == cat }
        }
        CampusAnalytics(
            totalUsers = users.size,
            studentsCount = users.count { it.role == "student" },
            teachersCount = users.count { it.role == "teacher" && it.isApproved },
            pendingTeachersCount = users.count { it.role == "teacher" && !it.isApproved },
            activePostsCount = posts.size,
            totalCommentsCount = posts.sumOf { it.commentsCount },
            pendingReportsCount = rList.count { it.status == "pending" },
            categoryDistribution = catCounts
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CampusAnalytics())

    private var lastSeenWriteAtMs: Long = 0L

    fun touchLastSeen(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastSeenWriteAtMs < 25_000L) return
        lastSeenWriteAtMs = now
        viewModelScope.launch {
            repository.updateLastSeen()
        }
    }

    fun navigateTo(tab: CampusTab) {
        if (tab == CampusTab.ADMIN && !isCurrentUserAdmin.value) {
            showToast("Administrator access required")
            return
        }
        if (_activeTab.value != tab) {
            tabHistory.add(_activeTab.value)
            _activeTab.value = tab
        }
        if (tab != CampusTab.PROFILE && tab != CampusTab.CONNECTIONS) {
            _viewingProfileUserId.value = null
        }
        if (tab == CampusTab.ADMIN) {
            refreshAdminEmails()
        }
        touchLastSeen()
    }

    fun navigateBack(): Boolean {
        if (_activeChatUserId.value != null && _activeTab.value == CampusTab.CHAT) {
            _activeChatUserId.value = null
            return true
        }
        return if (tabHistory.isNotEmpty()) {
            _activeTab.value = tabHistory.removeAt(tabHistory.lastIndex)
            true
        } else if (_activeTab.value != CampusTab.FEED) {
            _activeTab.value = CampusTab.FEED
            true
        } else {
            false
        }
    }

    fun openUserProfile(userId: String) {
        _viewingProfileUserId.value = userId
        navigateTo(CampusTab.PROFILE)
    }

    fun openFollowConnections(userId: String, initialTabIndex: Int) {
        _connectionsTarget.value = userId to initialTabIndex.coerceIn(0, 1)
        navigateTo(CampusTab.CONNECTIONS)
    }

    fun openDirectChat(userId: String) {
        _activeChatUserId.value = userId
        navigateTo(CampusTab.CHAT)
    }

    fun clearActiveChat() {
        _activeChatUserId.value = null
    }

    fun setRadialMenuOpen(open: Boolean) {
        _isRadialMenuOpen.value = open
    }

    fun openReport(targetType: String, targetId: String, summary: String) {
        _reportTarget.value = Triple(targetType, targetId, summary)
    }

    fun closeReport() {
        _reportTarget.value = null
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun registerProfile(name: String, role: String, grade: String?) {
        viewModelScope.launch {
            val res = repository.registerProfile(name, googleEmail, role, grade, googleAvatar)
            res.onFailure { showToast(it.localizedMessage ?: "Failed to create campus profile") }
        }
    }

    fun updateProfile(name: String, grade: String?, avatar: String?, bio: String) {
        val me = currentUser.value ?: return
        viewModelScope.launch {
            val res = repository.updateOwnProfile(
                currentUser = me,
                isAdmin = isCurrentUserAdmin.value,
                newName = name,
                newGrade = grade,
                newAvatar = avatar,
                newBio = bio,
                context = appContext
            )
            if (res.isSuccess) {
                showToast("Profile updated!")
                navigateTo(CampusTab.PROFILE)
            } else {
                showToast(res.exceptionOrNull()?.localizedMessage ?: "Failed to update profile")
            }
        }
    }

    fun updateAvatar(newAvatar: String, onDone: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.updateOwnAvatar(
                newAvatar = newAvatar,
                isAdmin = isCurrentUserAdmin.value,
                context = appContext
            )
            if (res.isSuccess) {
                showToast("Profile photo updated!")
            } else {
                showToast(res.exceptionOrNull()?.localizedMessage ?: "Failed to save profile photo")
            }
            onDone(res)
        }
    }

    fun updatePrivateSettings(transform: (UserPrivateInfo) -> UserPrivateInfo) {
        val base = privateInfo.value ?: UserPrivateInfo(userId = currentUserId, email = googleEmail)
        val updated = transform(base)
        viewModelScope.launch {
            val res = repository.savePrivateSettings(updated)
            if (res.isFailure) {
                showToast(res.exceptionOrNull()?.localizedMessage ?: "Failed to save preference")
            }
        }
    }

    fun toggleBookmarkPost(postId: String) {
        updatePrivateSettings { info ->
            val saved = if (info.savedPostIds.contains(postId)) {
                info.savedPostIds - postId
            } else {
                (info.savedPostIds + postId).takeLast(200)
            }
            info.copy(savedPostIds = saved)
        }
    }

    fun toggleBookmarkReel(reelId: String) {
        updatePrivateSettings { info ->
            val saved = if (info.savedReelIds.contains(reelId)) {
                info.savedReelIds - reelId
            } else {
                (info.savedReelIds + reelId).takeLast(200)
            }
            info.copy(savedReelIds = saved)
        }
    }

    fun toggleFollow(targetUserId: String) {
        val me = currentUser.value ?: return
        if (me.isSuspended && !isCurrentUserAdmin.value) {
            showToast("Account is suspended")
            return
        }
        val currentlyFollowing = me.following.contains(targetUserId)
        viewModelScope.launch {
            repository.toggleFollow(targetUserId, currentlyFollowing, me)
        }
    }

    fun refreshFeed() {
        if (_isRefreshingFeed.value) return
        viewModelScope.launch {
            _isRefreshingFeed.value = true
            try {
                val postsResult = repository.fetchLatestPostsFromServer()
                repository.fetchLatestStoriesFromServer()
                if (postsResult.isSuccess) {
                    _serverRefreshPosts.value = postsResult.getOrNull()
                    _feedRefreshGeneration.value += 1
                    touchLastSeen(force = true)
                } else {
                    showToast(
                        postsResult.exceptionOrNull()?.localizedMessage
                            ?: "Could not refresh feed. Check your connection."
                    )
                }
            } finally {
                _isRefreshingFeed.value = false
            }
        }
    }

    fun createPost(
        content: String,
        category: String,
        images: List<String>,
        imageAspectRatio: String = PostImageRatio.FREE.key,
        isOfficial: Boolean,
        isPinned: Boolean,
        musicTrack: StoryMusicTrack? = null,
        musicStartSec: Int = 0,
        onSuccess: () -> Unit
    ) {
        val me = currentUser.value ?: return
        if (me.isSuspended && !isCurrentUserAdmin.value) {
            showToast("Suspended accounts cannot publish posts.")
            return
        }
        viewModelScope.launch {
            val res = repository.createPost(
                author = me,
                isAdmin = isCurrentUserAdmin.value,
                content = content,
                category = category,
                images = images,
                imageAspectRatio = imageAspectRatio,
                isOfficial = isOfficial,
                isPinned = isPinned,
                musicTrack = musicTrack,
                musicStartSec = musicStartSec
            )
            if (res.isSuccess) {
                showToast("Post published to Squadify!")
                onSuccess()
                navigateTo(CampusTab.FEED)
            } else {
                showToast(res.exceptionOrNull()?.localizedMessage ?: "Failed to publish post")
            }
        }
    }

    fun enqueueBackgroundPostUpload(
        context: Context,
        content: String,
        category: String,
        mediaItems: List<StagedDraftMediaInput>,
        imageAspectRatio: PostImageRatio,
        isOfficial: Boolean,
        isPinned: Boolean,
        musicTrack: StoryMusicTrack?,
        musicStartSec: Int,
        onAccepted: () -> Unit = {}
    ) {
        val me = currentUser.value ?: return
        if (me.isSuspended && !isCurrentUserAdmin.value) {
            showToast("Suspended accounts cannot publish posts.")
            return
        }
        val appCtx = context.applicationContext
        val isAdmin = isCurrentUserAdmin.value
        val uploadId = UUID.randomUUID().toString()
        MediaUploadTracker.start(
            uploadId = uploadId,
            kind = MediaUploadKind.POST,
            itemCount = mediaItems.size.coerceAtLeast(1)
        )
        onAccepted()
        navigateTo(CampusTab.FEED)

        viewModelScope.launch {
            val stagedDir = StoryMediaEditor.pendingUploadsDir(appCtx)
            val stagedFiles = mutableListOf<File>()
            val isVideos = mutableListOf<Boolean>()
            var stagingError: String? = null

            for ((idx, item) in mediaItems.withIndex()) {
                val itemId = "${uploadId}_$idx"
                val label = if (mediaItems.size > 1) {
                    "Preparing media ${idx + 1}/${mediaItems.size}..."
                } else {
                    "Preparing media..."
                }
                MediaUploadTracker.updateProgress(uploadId, 0.06f + idx * 0.02f, label)
                val res = if (item.isVideo) {
                    StoryMediaEditor.stageVideoToCache(
                        context = appCtx,
                        uri = item.uri,
                        uploadId = itemId,
                        isAdmin = isAdmin,
                        outputDir = stagedDir
                    )
                } else {
                    StoryMediaEditor.stageEditedImageToCache(
                        context = appCtx,
                        uri = item.uri,
                        uploadId = itemId,
                        isAdmin = isAdmin,
                        edit = item.editParams,
                        targetAspectRatio = imageAspectRatio.ratio,
                        outputDir = stagedDir
                    )
                }
                val file = res.getOrNull()
                if (file == null) {
                    stagingError = res.exceptionOrNull()?.localizedMessage ?: "Failed to prepare selected media."
                    break
                }
                stagedFiles.add(file)
                isVideos.add(item.isVideo)
                if (idx == 0 && !item.isVideo) {
                    MediaUploadTracker.updatePreviewPath(uploadId, file.absolutePath)
                }
            }

            if (stagingError != null) {
                stagedFiles.forEach { runCatching { it.delete() } }
                MediaUploadTracker.markFailure(uploadId, MediaUploadKind.POST, stagingError)
                showToast(stagingError)
                return@launch
            }

            PostReelUploadWorker.enqueue(
                context = appCtx,
                uploadId = uploadId,
                kind = MediaUploadKind.POST,
                stagedFiles = stagedFiles,
                isVideos = isVideos,
                content = content,
                category = category,
                imageAspectRatio = imageAspectRatio.key,
                isOfficial = isOfficial,
                isPinned = isPinned,
                isAdmin = isAdmin,
                musicTrack = musicTrack,
                musicStartSec = musicStartSec
            )
        }
    }

    fun toggleLikePost(post: CampusPost) {
        val me = currentUser.value ?: return
        viewModelScope.launch {
            repository.toggleLikePost(post, me)
        }
    }

    fun markPostViewed(post: CampusPost) {
        viewModelScope.launch {
            repository.markPostViewed(post)
        }
    }

    fun deletePost(post: CampusPost) {
        val isAdmin = isCurrentUserAdmin.value
        if (!isAdmin && (post.authorId != currentUserId || post.isProtectedAnnouncement)) {
            showToast("Only administrators can delete official announcements.")
            return
        }
        _serverRefreshPosts.value = _serverRefreshPosts.value?.filterNot { it.id == post.id }
        viewModelScope.launch {
            preferencesRepository?.addPendingDeletedPostId(post.id)
            appContext?.let { ctx ->
                PostDeleteWorker.enqueue(ctx, post.id, post.images, isAdmin)
            }
            val res = repository.deletePost(post, isAdmin, appContext)
            if (res.isSuccess) {
                preferencesRepository?.removePendingDeletedPostId(post.id)
                appContext?.let { ctx -> PostDeleteWorker.cancel(ctx, post.id) }
                showToast("Post deleted")
            } else {
                val err = res.exceptionOrNull()
                val isPermDenied = err is SecurityException ||
                    (err as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                if (isPermDenied) {
                    preferencesRepository?.removePendingDeletedPostId(post.id)
                    appContext?.let { ctx -> PostDeleteWorker.cancel(ctx, post.id) }
                    showToast(err?.localizedMessage ?: "Unable to delete post")
                } else {
                    showToast("Post deleted")
                }
            }
        }
    }

    fun updatePost(
        post: CampusPost,
        newContent: String,
        newCategory: String,
        newImageAspectRatio: String = post.imageAspectRatio,
        musicTrack: StoryMusicTrack? = null,
        musicStartSec: Int = 0
    ) {
        val isAdmin = isCurrentUserAdmin.value
        viewModelScope.launch {
            val res = repository.updatePost(
                post = post,
                isAdmin = isAdmin,
                newContent = newContent,
                newCategory = newCategory,
                newImageAspectRatio = newImageAspectRatio,
                musicTrack = musicTrack,
                musicStartSec = musicStartSec
            )
            if (res.isSuccess) {
                showToast("Post updated")
            } else {
                showToast(res.exceptionOrNull()?.localizedMessage ?: "Unable to update post")
            }
        }
    }

    fun togglePinPost(post: CampusPost) {
        val isAdmin = isCurrentUserAdmin.value
        if (!isAdmin) {
            showToast("Only administrators can pin posts.")
            return
        }
        viewModelScope.launch {
            repository.togglePinPost(post, isAdmin)
                .onFailure { showToast(it.localizedMessage ?: "Unable to update pin") }
        }
    }

    fun addComment(post: CampusPost, content: String, replyTo: com.bsdcampus.app.model.CampusComment? = null) {
        val me = currentUser.value ?: return
        if (me.isSuspended && !isCurrentUserAdmin.value) {
            showToast("Suspended accounts cannot comment.")
            return
        }
        viewModelScope.launch {
            repository.addComment(post, me, content, isCurrentUserAdmin.value, replyTo)
                .onFailure { showToast(it.localizedMessage ?: "Unable to post comment") }
        }
    }

    fun toggleLikeComment(post: CampusPost, comment: com.bsdcampus.app.model.CampusComment) {
        val me = currentUser.value ?: return
        if (me.isSuspended && !isCurrentUserAdmin.value) {
            showToast("Suspended accounts cannot like comments.")
            return
        }
        viewModelScope.launch {
            repository.toggleLikeComment(post.id, comment, me)
                .onFailure { showToast(it.localizedMessage ?: "Unable to like comment") }
        }
    }

    fun deleteComment(postId: String, commentId: String) {
        viewModelScope.launch {
            repository.deleteComment(postId, commentId)
        }
    }

    fun createReel(
        videoUrl: String,
        caption: String,
        category: String,
        audioTitle: String,
        videoAspectRatio: String = PostImageRatio.PORTRAIT_9_16.key,
        musicTrack: StoryMusicTrack? = null,
        musicStartSec: Int = 0,
        muteOriginalAudio: Boolean = false,
        onSuccess: () -> Unit
    ) {
        val me = currentUser.value ?: return
        if (me.isSuspended && !isCurrentUserAdmin.value) {
            showToast("Suspended accounts cannot publish reels.")
            return
        }
        viewModelScope.launch {
            val res = repository.createReel(
                author = me,
                isAdmin = isCurrentUserAdmin.value,
                videoUrl = videoUrl,
                videoAspectRatio = videoAspectRatio,
                caption = caption,
                category = category,
                audioTitle = audioTitle,
                musicTrack = musicTrack,
                musicStartSec = musicStartSec,
                muteOriginalAudio = muteOriginalAudio
            )
            if (res.isSuccess) {
                showToast("Reel published to Squadify!")
                onSuccess()
                navigateTo(CampusTab.REELS)
            } else {
                showToast(res.exceptionOrNull()?.localizedMessage ?: "Failed to publish reel")
            }
        }
    }

    fun enqueueBackgroundReelUpload(
        context: Context,
        videoUri: Uri,
        caption: String,
        category: String,
        audioTitle: String,
        videoAspectRatio: PostImageRatio = PostImageRatio.FREE,
        musicTrack: StoryMusicTrack?,
        musicStartSec: Int,
        muteOriginalAudio: Boolean,
        onAccepted: () -> Unit = {}
    ) {
        val me = currentUser.value ?: return
        if (me.isSuspended && !isCurrentUserAdmin.value) {
            showToast("Suspended accounts cannot publish reels.")
            return
        }
        val appCtx = context.applicationContext
        val isAdmin = isCurrentUserAdmin.value
        val uploadId = UUID.randomUUID().toString()
        MediaUploadTracker.start(
            uploadId = uploadId,
            kind = MediaUploadKind.REEL,
            itemCount = 1,
            statusLabel = "Preparing reel video..."
        )
        onAccepted()
        navigateTo(CampusTab.REELS)

        viewModelScope.launch {
            val stagedDir = StoryMediaEditor.pendingUploadsDir(appCtx)
            val res = StoryMediaEditor.stageVideoToCache(
                context = appCtx,
                uri = videoUri,
                uploadId = uploadId,
                isAdmin = isAdmin,
                outputDir = stagedDir
            )
            val file = res.getOrNull()
            if (file == null) {
                val msg = res.exceptionOrNull()?.localizedMessage ?: "Failed to prepare reel video."
                MediaUploadTracker.markFailure(uploadId, MediaUploadKind.REEL, msg)
                showToast(msg)
                return@launch
            }
            PostReelUploadWorker.enqueue(
                context = appCtx,
                uploadId = uploadId,
                kind = MediaUploadKind.REEL,
                stagedFiles = listOf(file),
                isVideos = listOf(true),
                content = caption,
                category = category,
                videoAspectRatio = videoAspectRatio.key,
                audioTitle = audioTitle,
                muteOriginalAudio = muteOriginalAudio,
                isAdmin = isAdmin,
                musicTrack = musicTrack,
                musicStartSec = musicStartSec
            )
        }
    }

    fun toggleLikeReel(reel: CampusReel) {
        val me = currentUser.value ?: return
        viewModelScope.launch {
            repository.toggleLikeReel(reel, me)
        }
    }

    fun markReelViewed(reel: CampusReel) {
        viewModelScope.launch {
            repository.markReelViewed(reel)
        }
    }

    fun updateReel(
        reel: CampusReel,
        newCaption: String,
        newCategory: String,
        newAudioTitle: String,
        newVideoAspectRatio: String = reel.videoAspectRatio,
        musicTrack: StoryMusicTrack? = null,
        musicStartSec: Int = 0,
        muteOriginalAudio: Boolean = false
    ) {
        val isAdmin = isCurrentUserAdmin.value
        viewModelScope.launch {
            val res = repository.updateReel(
                reel = reel,
                isAdmin = isAdmin,
                newCaption = newCaption,
                newCategory = newCategory,
                newAudioTitle = newAudioTitle,
                newVideoAspectRatio = newVideoAspectRatio,
                musicTrack = musicTrack,
                musicStartSec = musicStartSec,
                muteOriginalAudio = muteOriginalAudio
            )
            if (res.isSuccess) {
                showToast("Reel updated")
            } else {
                showToast(res.exceptionOrNull()?.localizedMessage ?: "Unable to update reel")
            }
        }
    }

    fun deleteReel(reel: CampusReel) {
        val isAdmin = isCurrentUserAdmin.value
        if (!isAdmin && reel.authorId != currentUserId) {
            showToast("Only the reel creator or an administrator can delete this reel.")
            return
        }
        viewModelScope.launch {
            val res = repository.deleteReel(reel, isAdmin, appContext)
            if (res.isSuccess) {
                showToast("Reel deleted")
            } else {
                showToast(res.exceptionOrNull()?.localizedMessage ?: "Unable to delete reel")
            }
        }
    }

    fun addReelComment(reel: CampusReel, content: String, replyTo: com.bsdcampus.app.model.ReelComment? = null) {
        val me = currentUser.value ?: return
        if (me.isSuspended && !isCurrentUserAdmin.value) {
            showToast("Suspended accounts cannot comment.")
            return
        }
        viewModelScope.launch {
            repository.addReelComment(reel, me, content, isCurrentUserAdmin.value, replyTo)
                .onFailure { showToast(it.localizedMessage ?: "Unable to post comment") }
        }
    }

    fun toggleLikeReelComment(reel: CampusReel, comment: com.bsdcampus.app.model.ReelComment) {
        val me = currentUser.value ?: return
        if (me.isSuspended && !isCurrentUserAdmin.value) {
            showToast("Suspended accounts cannot like comments.")
            return
        }
        viewModelScope.launch {
            repository.toggleLikeReelComment(reel.id, comment, me)
                .onFailure { showToast(it.localizedMessage ?: "Unable to like comment") }
        }
    }

    fun deleteReelComment(reelId: String, commentId: String) {
        viewModelScope.launch {
            repository.deleteReelComment(reelId, commentId)
        }
    }

    fun sendMessage(
        recipientId: String,
        content: String,
        mediaData: String? = null,
        audioData: String? = null,
        durationSec: Long? = null,
        replyToMessage: DirectMessage? = null,
        replyToSenderName: String? = null
    ) {
        val me = currentUser.value
        if (me?.isSuspended == true && !isCurrentUserAdmin.value) {
            showToast("Suspended accounts cannot send messages.")
            return
        }
        val blocked = privateInfo.value?.blockedUserIds.orEmpty()
        if (blocked.contains(recipientId)) {
            showToast("Unblock this user to send messages.")
            return
        }
        viewModelScope.launch {
            val res = repository.sendMessage(
                recipientId = recipientId,
                content = content,
                mediaData = mediaData,
                audioData = audioData,
                audioDurationSec = durationSec,
                replyToMessage = replyToMessage,
                replyToSenderName = replyToSenderName
            )
            if (res.isFailure) {
                showToast(res.exceptionOrNull()?.localizedMessage ?: "Failed to send message")
            }
        }
    }

    fun editMessage(messageId: String, newContent: String) {
        viewModelScope.launch { repository.editMessage(messageId, newContent) }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch { repository.deleteMessage(messageId) }
    }

    fun toggleMessageReaction(message: DirectMessage, emoji: String) {
        viewModelScope.launch { repository.toggleMessageReaction(message, emoji) }
    }

    fun markThreadRead(otherUserId: String) {
        val thread = messages.value.filter { it.senderId == otherUserId && it.recipientId == currentUserId && !it.read }
        if (thread.isEmpty()) return
        viewModelScope.launch { repository.markMessagesRead(thread) }
    }

    fun setTyping(recipientId: String, isTyping: Boolean) {
        viewModelScope.launch { repository.setTypingStatus(recipientId, isTyping) }
    }

    fun saveNote(
        text: String,
        bubbleColor: NoteBubbleColor,
        musicTrack: StoryMusicTrack?,
        musicStartSec: Int,
        onSuccess: () -> Unit
    ) {
        val me = currentUser.value ?: return
        if (me.isSuspended && !isCurrentUserAdmin.value) {
            showToast("Suspended accounts cannot post notes.")
            return
        }
        viewModelScope.launch {
            repository.upsertCampusNote(
                author = me,
                text = text,
                bubbleColor = bubbleColor,
                musicTrack = musicTrack,
                musicStartSec = musicStartSec
            )
                .onSuccess {
                    showToast("Note shared for 24h!")
                    onSuccess()
                }
                .onFailure {
                    showToast(it.localizedMessage ?: "Failed to share note")
                }
        }
    }

    fun deleteNote(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteCampusNote()
                .onSuccess {
                    showToast("Note removed")
                    onDone()
                }
                .onFailure {
                    showToast(it.localizedMessage ?: "Failed to remove note")
                }
        }
    }

    fun markNotificationsRead() {
        val unread = notifications.value.filter { !it.read && it.id.isNotBlank() }
        if (unread.isEmpty()) return
        val unreadIds = unread.map { it.id }.toSet()
        _locallyReadNotificationIds.value = _locallyReadNotificationIds.value + unreadIds
        viewModelScope.launch {
            val res = repository.markAllNotificationsRead(unread)
            if (res.isFailure) {
                _locallyReadNotificationIds.value = _locallyReadNotificationIds.value - unreadIds
            }
        }
    }

    fun submitReport(targetType: String, targetId: String, summary: String, reason: String, details: String) {
        viewModelScope.launch {
            val res = repository.submitReport(targetType, targetId, summary, reason, details)
            if (res.isSuccess) showToast("Report submitted to Admin")
            else showToast("Unable to submit report")
        }
    }

    fun setChatNickname(peerId: String, nickname: String?) {
        val existing = chatPreferences.value[peerId]
        val cleanNick = nickname?.trim()?.takeIf { it.isNotBlank() }?.take(40)
        viewModelScope.launch {
            val res = repository.saveChatPreference(
                peerId = peerId,
                nickname = cleanNick,
                backgroundPreset = existing?.backgroundPreset ?: ChatBackgroundPreset.DEFAULT.key,
                backgroundImage = existing?.backgroundImage,
                isAdmin = isCurrentUserAdmin.value,
                context = appContext
            )
            if (res.isSuccess) {
                showToast(if (cleanNick == null) "Nickname cleared" else "Nickname saved")
            } else {
                showToast(res.exceptionOrNull()?.localizedMessage ?: "Unable to save nickname")
            }
        }
    }

    fun setChatBackground(peerId: String, presetKey: String, imageUri: String?) {
        val existing = chatPreferences.value[peerId]
        val cleanImage = imageUri?.trim()?.takeIf { it.isNotBlank() }
        val cleanPreset = if (cleanImage != null) ChatBackgroundPreset.DEFAULT.key else ChatBackgroundPreset.fromKey(presetKey).key
        viewModelScope.launch {
            val res = repository.saveChatPreference(
                peerId = peerId,
                nickname = existing?.nickname,
                backgroundPreset = cleanPreset,
                backgroundImage = cleanImage,
                isAdmin = isCurrentUserAdmin.value,
                context = appContext
            )
            if (res.isSuccess) {
                val isReset = cleanPreset == ChatBackgroundPreset.DEFAULT.key && cleanImage == null
                showToast(if (isReset) "Chat background reset" else "Chat background updated")
            } else {
                showToast(res.exceptionOrNull()?.localizedMessage ?: "Unable to save chat background")
            }
        }
    }

    fun toggleBlockUser(targetUserId: String) {
        if (targetUserId == currentUserId) return
        val base = privateInfo.value ?: UserPrivateInfo(userId = currentUserId, email = googleEmail)
        val currentlyBlocked = base.blockedUserIds.contains(targetUserId)
        val updatedList = if (currentlyBlocked) {
            base.blockedUserIds - targetUserId
        } else {
            (base.blockedUserIds + targetUserId).distinct().takeLast(200)
        }
        viewModelScope.launch {
            val res = repository.savePrivateSettings(base.copy(blockedUserIds = updatedList))
            if (res.isSuccess) {
                showToast(if (currentlyBlocked) "User unblocked" else "User blocked")
            } else {
                showToast(res.exceptionOrNull()?.localizedMessage ?: "Unable to update block status")
            }
        }
    }

    fun submitChatReport(
        peer: CampusUser,
        reason: String,
        details: String,
        threadMessages: List<DirectMessage>
    ) {
        val peerLabel = if (peer.isBlackSociety) "Member - ${peer.blackSocietyNumber ?: 1}" else peer.name
        val myLabel = currentUser.value?.let {
            if (it.isBlackSociety) "Member - ${it.blackSocietyNumber ?: 1}" else it.name
        } ?: googleDisplayName.ifBlank { "Reporter" }
        val dateFmt = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        val lastFive = threadMessages
            .sortedBy { it.createdAt?.toDate()?.time ?: 0L }
            .takeLast(5)
            .map { msg ->
                val senderName = if (msg.senderId == currentUserId) "$myLabel (Reporter)" else peerLabel
                val timeLabel = msg.createdAt?.toDate()?.let { dateFmt.format(it) } ?: "Recent"
                val body = when {
                    msg.content.isNotBlank() && !msg.mediaData.isNullOrBlank() ->
                        "${msg.content.trim()} [+${if (MediaUtils.isVideoMediaUri(msg.mediaData)) "Video" else "Photo"}]"
                    msg.content.isNotBlank() -> msg.content.trim()
                    !msg.audioData.isNullOrBlank() -> "[Voice note ${msg.audioDurationSec ?: 0}s]"
                    MediaUtils.isVideoMediaUri(msg.mediaData) -> "[Video attachment]"
                    !msg.mediaData.isNullOrBlank() -> "[Photo attachment]"
                    !msg.storyId.isNullOrBlank() -> "[Story reply]"
                    else -> "[Message]"
                }
                "[$timeLabel] $senderName: $body".take(400)
            }
        val cleanDetails = details.trim()
        val combinedDetails = buildString {
            if (cleanDetails.isNotBlank()) {
                append(cleanDetails)
                if (lastFive.isNotEmpty()) append("\n\n")
            }
            if (lastFive.isNotEmpty()) {
                append("Last ${lastFive.size} message(s) forwarded to Admin:\n")
                lastFive.forEachIndexed { idx, line ->
                    append("${idx + 1}. $line")
                    if (idx < lastFive.lastIndex) append("\n")
                }
            }
        }.take(2500)
        viewModelScope.launch {
            val res = repository.submitReport(
                targetType = "chat",
                targetId = peer.uid,
                targetSummary = "Chat with $peerLabel (${lastFive.size} msg snapshot)".take(300),
                reason = reason.ifBlank { "Inappropriate chat behavior" },
                details = combinedDetails,
                reportedMessages = lastFive
            )
            if (res.isSuccess) {
                showToast("Report & ${lastFive.size} message(s) sent to Admin")
            } else {
                showToast(res.exceptionOrNull()?.localizedMessage ?: "Unable to submit report")
            }
        }
    }

    fun createStory(
        mediaUrl: String,
        mediaType: String,
        caption: String,
        musicTrack: com.bsdcampus.app.data.StoryMusicTrack? = null,
        musicStartSec: Int = 0,
        muteOriginalAudio: Boolean = false,
        onSuccess: () -> Unit
    ) {
        val me = currentUser.value ?: return
        if (me.isSuspended && !isCurrentUserAdmin.value) {
            showToast("Suspended accounts cannot share stories.")
            return
        }
        viewModelScope.launch {
            val res = repository.createStory(
                author = me,
                isAdmin = isCurrentUserAdmin.value,
                mediaUrl = mediaUrl,
                mediaType = mediaType,
                caption = caption,
                musicTrack = musicTrack,
                musicStartSec = musicStartSec,
                muteOriginalAudio = muteOriginalAudio
            )
            if (res.isSuccess) {
                showToast("Story shared for 24h!")
                onSuccess()
            } else {
                showToast(res.exceptionOrNull()?.localizedMessage ?: "Failed to share story")
            }
        }
    }

    fun sharePostToStory(
        post: CampusPost,
        note: String,
        musicTrack: com.bsdcampus.app.data.StoryMusicTrack? = null,
        musicStartSec: Int = 0,
        muteOriginalAudio: Boolean = false,
        onSuccess: () -> Unit
    ) {
        val me = currentUser.value ?: return
        if (me.isSuspended && !isCurrentUserAdmin.value) {
            showToast("Suspended accounts cannot share stories.")
            return
        }
        val firstMedia = post.images.firstOrNull()
        val mediaType = when {
            firstMedia == null -> "post_card"
            MediaUtils.isVideoMediaUri(firstMedia) -> "video"
            else -> "image"
        }
        val mediaUrl = firstMedia ?: "shared_text_post://${post.id}"
        val users = (allUsersState.value as? UiState.Success)?.data.orEmpty()
        val liveAuthor = users.firstOrNull { it.uid == post.authorId }
        val isAnon = liveAuthor?.isBlackSociety ?: post.authorIsBlackSociety
        val anonNum = liveAuthor?.blackSocietyNumber ?: post.authorBlackSocietyNumber
        val sourceAuthorName = if (isAnon) "Member - ${anonNum ?: 1}" else (liveAuthor?.name ?: post.authorName)
        val sourceAvatar = if (isAnon) null else (liveAuthor?.avatar ?: post.authorAvatar)

        viewModelScope.launch {
            val res = repository.createStory(
                author = me,
                isAdmin = isCurrentUserAdmin.value,
                mediaUrl = mediaUrl,
                mediaType = mediaType,
                caption = note,
                sharedSourceType = "post",
                sharedSourceId = post.id,
                sharedAuthorId = post.authorId,
                sharedAuthorName = sourceAuthorName,
                sharedAuthorAvatar = sourceAvatar,
                sharedSourceCaption = post.content,
                musicTrack = musicTrack,
                musicStartSec = musicStartSec,
                muteOriginalAudio = muteOriginalAudio
            )
            if (res.isSuccess) {
                showToast("Post added to your story!")
                onSuccess()
            } else {
                showToast(res.exceptionOrNull()?.localizedMessage ?: "Failed to share post to story")
            }
        }
    }

    fun shareReelToStory(
        reel: CampusReel,
        note: String,
        musicTrack: com.bsdcampus.app.data.StoryMusicTrack? = null,
        musicStartSec: Int = 0,
        muteOriginalAudio: Boolean = false,
        onSuccess: () -> Unit
    ) {
        val me = currentUser.value ?: return
        if (me.isSuspended && !isCurrentUserAdmin.value) {
            showToast("Suspended accounts cannot share stories.")
            return
        }
        val users = (allUsersState.value as? UiState.Success)?.data.orEmpty()
        val liveAuthor = users.firstOrNull { it.uid == reel.authorId }
        val isAnon = liveAuthor?.isBlackSociety ?: reel.authorIsBlackSociety
        val anonNum = liveAuthor?.blackSocietyNumber ?: reel.authorBlackSocietyNumber
        val sourceAuthorName = if (isAnon) "Member - ${anonNum ?: 1}" else (liveAuthor?.name ?: reel.authorName)
        val sourceAvatar = if (isAnon) null else (liveAuthor?.avatar ?: reel.authorAvatar)

        viewModelScope.launch {
            val res = repository.createStory(
                author = me,
                isAdmin = isCurrentUserAdmin.value,
                mediaUrl = reel.videoUrl,
                mediaType = "video",
                caption = note,
                sharedSourceType = "reel",
                sharedSourceId = reel.id,
                sharedAuthorId = reel.authorId,
                sharedAuthorName = sourceAuthorName,
                sharedAuthorAvatar = sourceAvatar,
                sharedSourceCaption = reel.caption,
                musicTrack = musicTrack,
                musicStartSec = musicStartSec,
                muteOriginalAudio = muteOriginalAudio
            )
            if (res.isSuccess) {
                showToast("Reel added to your story!")
                onSuccess()
            } else {
                showToast(res.exceptionOrNull()?.localizedMessage ?: "Failed to share reel to story")
            }
        }
    }

    fun markStoryViewed(story: CampusStory) {
        viewModelScope.launch {
            repository.markStoryViewed(story)
        }
    }

    fun deleteStory(story: CampusStory) {
        viewModelScope.launch {
            repository.deleteStory(story, isCurrentUserAdmin.value, appContext)
                .onSuccess { showToast("Story deleted") }
                .onFailure { showToast(it.localizedMessage ?: "Unable to delete story") }
        }
    }

    fun toggleLikeStory(story: CampusStory) {
        val me = currentUser.value ?: return
        if (me.isSuspended && !isCurrentUserAdmin.value) {
            showToast("Suspended accounts cannot like stories.")
            return
        }
        viewModelScope.launch {
            repository.toggleLikeStory(story, me)
                .onFailure { showToast(it.localizedMessage ?: "Unable to update story like") }
        }
    }

    fun createHighlight(
        title: String,
        coverAvatar: String?,
        coverEmoji: String?,
        items: List<HighlightMediaItem>,
        onDone: () -> Unit
    ) {
        val me = currentUser.value ?: return
        if (me.isSuspended && !isCurrentUserAdmin.value) {
            showToast("Suspended accounts cannot create highlights.")
            return
        }
        viewModelScope.launch {
            repository.createHighlight(title, coverAvatar, coverEmoji, items)
                .onSuccess {
                    showToast("Highlight added to your profile!")
                    onDone()
                }
                .onFailure { showToast(it.localizedMessage ?: "Unable to save highlight") }
        }
    }

    fun updateHighlight(
        highlightId: String,
        title: String,
        coverAvatar: String?,
        coverEmoji: String?,
        items: List<HighlightMediaItem>,
        onDone: () -> Unit
    ) {
        val me = currentUser.value ?: return
        if (me.isSuspended && !isCurrentUserAdmin.value) {
            showToast("Suspended accounts cannot edit highlights.")
            return
        }
        viewModelScope.launch {
            repository.updateHighlight(highlightId, title, coverAvatar, coverEmoji, items)
                .onSuccess {
                    showToast("Highlight updated")
                    onDone()
                }
                .onFailure { showToast(it.localizedMessage ?: "Unable to update highlight") }
        }
    }

    fun deleteHighlight(highlightId: String) {
        viewModelScope.launch {
            repository.deleteHighlight(highlightId)
                .onSuccess { showToast("Highlight deleted") }
                .onFailure { showToast(it.localizedMessage ?: "Unable to delete highlight") }
        }
    }

    fun replyToStory(story: CampusStory, authorName: String, replyText: String) {
        val trimmed = replyText.trim()
        if (trimmed.isBlank() || story.authorId == currentUserId) return
        val me = currentUser.value
        if (me?.isSuspended == true && !isCurrentUserAdmin.value) {
            showToast("Suspended accounts cannot send replies.")
            return
        }
        if (privateInfo.value?.blockedUserIds.orEmpty().contains(story.authorId)) {
            showToast("Unblock this user to reply.")
            return
        }
        viewModelScope.launch {
            repository.sendMessage(
                recipientId = story.authorId,
                content = trimmed,
                replyStory = story,
                replyStoryAuthorName = authorName
            )
                .onSuccess { showToast("Reply sent to ${authorName.substringBefore(" ")}!") }
                .onFailure { showToast(it.localizedMessage ?: "Failed to send story reply") }
        }
    }

    // Admin actions
    fun refreshAdminEmails() {
        if (!isCurrentUserAdmin.value) return
        viewModelScope.launch {
            _adminEmailsMap.value = repository.getAllPrivateEmailsForAdmin()
        }
    }

    fun claimSuperAdmin() {
        if (!isVerifiedSuperAdmin) {
            showToast("Admin claim denied by security rules.")
            return
        }
        val me = currentUser.value ?: return
        viewModelScope.launch {
            val res = repository.claimSuperAdmin(me)
            showToast(if (res.isSuccess) "Admin privileges claimed!" else "Admin claim denied by security rules.")
        }
    }

    fun approveTeacher(teacherId: String) {
        if (!isCurrentUserAdmin.value) return
        viewModelScope.launch {
            repository.approveTeacher(teacherId)
                .onSuccess { showToast("Teacher approved & verified ✓") }
                .onFailure { showToast(it.localizedMessage ?: "Failed to approve teacher") }
        }
    }

    fun rejectTeacher(teacherId: String) {
        if (!isCurrentUserAdmin.value) return
        viewModelScope.launch {
            repository.rejectTeacher(teacherId)
                .onSuccess { showToast("Teacher application removed") }
                .onFailure { showToast(it.localizedMessage ?: "Failed to remove application") }
        }
    }

    fun toggleSuspendUser(userId: String, suspend: Boolean) {
        if (!isCurrentUserAdmin.value) return
        viewModelScope.launch {
            repository.toggleSuspendUser(userId, suspend)
                .onSuccess { showToast(if (suspend) "Account suspended" else "Account reactivated") }
                .onFailure { showToast(it.localizedMessage ?: "Failed to update suspension status") }
        }
    }

    fun toggleVerifyUser(userId: String, verify: Boolean) {
        if (!isCurrentUserAdmin.value) return
        viewModelScope.launch {
            repository.toggleVerifyUser(userId, verify)
                .onSuccess { showToast(if (verify) "Blue Tick granted ✓" else "Blue Tick removed") }
                .onFailure { showToast(it.localizedMessage ?: "Failed to update verification badge") }
        }
    }

    fun updateUserRole(userId: String, email: String, newRole: String) {
        if (!isCurrentUserAdmin.value) return
        viewModelScope.launch {
            repository.updateUserRole(userId, email, newRole)
                .onSuccess { showToast("Role updated to $newRole") }
                .onFailure { showToast(it.localizedMessage ?: "Failed to update role") }
        }
    }

    fun toggleDarkSociety(userId: String, grant: Boolean) {
        if (!isCurrentUserAdmin.value) return
        val allUsers = (allUsersState.value as? UiState.Success)?.data.orEmpty()
        viewModelScope.launch {
            repository.toggleDarkSociety(userId, grant, allUsers)
                .onSuccess { showToast(if (grant) "Dark Society membership granted ♠" else "Dark Society membership revoked") }
                .onFailure { showToast(it.localizedMessage ?: "Failed to update Dark Society status") }
        }
    }

    fun assignClassRep(userId: String, gradeNum: Int, assign: Boolean) {
        if (!isCurrentUserAdmin.value) return
        viewModelScope.launch {
            repository.assignClassRepresentative(userId, gradeNum, assign)
                .onSuccess { showToast(if (assign) "Assigned Class $gradeNum Rep 👑" else "Revoked Class Representative") }
                .onFailure { showToast(it.localizedMessage ?: "Failed to update Class Rep") }
        }
    }

    fun resolveReport(reportId: String, status: String, actionTaken: String) {
        if (!isCurrentUserAdmin.value) return
        viewModelScope.launch {
            repository.updateReportStatus(reportId, status, actionTaken)
                .onSuccess { showToast("Report marked as $status") }
                .onFailure { showToast(it.localizedMessage ?: "Failed to update report") }
        }
    }

    fun broadcastAnnouncement(content: String, category: String, onDone: () -> Unit) {
        if (!isCurrentUserAdmin.value) {
            showToast("Only administrators can broadcast official notices.")
            return
        }
        val me = currentUser.value ?: return
        val allUsers = (allUsersState.value as? UiState.Success)?.data.orEmpty()
        viewModelScope.launch {
            val res = repository.broadcastAnnouncement(me, true, content, category, allUsers)
            if (res.isSuccess) {
                showToast("Official Campus Broadcast published!")
                onDone()
            } else {
                showToast("Failed to broadcast notice")
            }
        }
    }

    // --- In-App Update Actions ---

    fun checkForAppUpdates(userInitiated: Boolean) {
        if (_isCheckingForUpdates.value) return
        viewModelScope.launch {
            _isCheckingForUpdates.value = true
            try {
                val res = repository.fetchAppUpdateConfigFromServer()
                if (res.isSuccess) {
                    val cfg = res.getOrNull()
                    if (cfg != null && cfg.latestVersionCode > BuildConfig.VERSION_CODE) {
                        _dismissedUpdateVersionCode.value = null
                        if (userInitiated) {
                            _manualUpdateDialogRequested.value = true
                        }
                    } else if (userInitiated) {
                        _manualUpdateDialogRequested.value = false
                        showToast("You're on the latest Squadify (v${BuildConfig.VERSION_NAME})")
                    }
                } else if (userInitiated) {
                    showToast(res.exceptionOrNull()?.localizedMessage ?: "Unable to check for updates")
                }
            } finally {
                _isCheckingForUpdates.value = false
            }
        }
    }

    fun startAppUpdateDownload(context: Context) {
        val cfg = appUpdateConfig.value ?: return
        if (cfg.apkUrl.isBlank()) {
            showToast("Update download link is unavailable.")
            return
        }
        if (_updateDownloadState.value is AppUpdateDownloadState.Downloading) return
        val appCtx = context.applicationContext
        viewModelScope.launch {
            _updateDownloadState.value = AppUpdateDownloadState.Downloading(0f, 0L, -1L)
            val res = AppUpdateManager.downloadApk(appCtx, cfg.apkUrl) { progress, downloaded, total ->
                _updateDownloadState.value = AppUpdateDownloadState.Downloading(progress, downloaded, total)
            }
            if (res.isSuccess) {
                val apkFile = res.getOrThrow()
                if (AppUpdateManager.canInstallPackages(appCtx)) {
                    _updateDownloadState.value = AppUpdateDownloadState.ReadyToInstall(apkFile)
                    runCatching { AppUpdateManager.launchApkInstaller(appCtx, apkFile) }
                        .onFailure { err ->
                            _updateDownloadState.value = AppUpdateDownloadState.Error(
                                err.localizedMessage ?: "Unable to launch package installer."
                            )
                        }
                } else {
                    _updateDownloadState.value = AppUpdateDownloadState.NeedsInstallPermission(apkFile)
                }
            } else {
                _updateDownloadState.value = AppUpdateDownloadState.Error(
                    res.exceptionOrNull()?.localizedMessage ?: "Download failed. Please try again."
                )
            }
        }
    }

    fun installPendingApk(context: Context, requestPermissionIfNeeded: Boolean = true) {
        val appCtx = context.applicationContext
        val apkFile = when (val st = _updateDownloadState.value) {
            is AppUpdateDownloadState.ReadyToInstall -> st.apkFile
            is AppUpdateDownloadState.NeedsInstallPermission -> st.apkFile
            else -> return
        }
        if (!apkFile.exists()) {
            _updateDownloadState.value = AppUpdateDownloadState.Error("Downloaded APK is missing. Please download again.")
            return
        }
        if (AppUpdateManager.canInstallPackages(appCtx)) {
            _updateDownloadState.value = AppUpdateDownloadState.ReadyToInstall(apkFile)
            runCatching { AppUpdateManager.launchApkInstaller(appCtx, apkFile) }
                .onFailure { err ->
                    _updateDownloadState.value = AppUpdateDownloadState.Error(
                        err.localizedMessage ?: "Unable to open installer."
                    )
                }
        } else if (requestPermissionIfNeeded) {
            AppUpdateManager.openInstallPermissionSettings(appCtx)
        }
    }

    fun dismissUpdateDialog() {
        val cfg = appUpdateConfig.value
        val dlState = _updateDownloadState.value
        if (cfg?.isMandatory == true && cfg.latestVersionCode > BuildConfig.VERSION_CODE) {
            return
        }
        if (dlState is AppUpdateDownloadState.Downloading) return
        _dismissedUpdateVersionCode.value = cfg?.latestVersionCode
        _manualUpdateDialogRequested.value = false
        _updateDownloadState.value = AppUpdateDownloadState.Idle
    }

    fun publishAppUpdateConfig(
        versionCode: Int,
        versionName: String,
        apkUrl: String,
        releaseNotes: String,
        isMandatory: Boolean,
        onDone: () -> Unit
    ) {
        if (!isCurrentUserAdmin.value) {
            showToast("Only administrators can publish app updates.")
            return
        }
        viewModelScope.launch {
            repository.publishAppUpdateConfig(versionCode, versionName, apkUrl, releaseNotes, isMandatory)
                .onSuccess {
                    showToast("In-app update v$versionName published!")
                    onDone()
                }
                .onFailure {
                    showToast(it.localizedMessage ?: "Failed to publish update config")
                }
        }
    }

    private fun unwrapFirestoreError(err: Throwable): FirebaseFirestoreException? {
        return generateSequence(err) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
    }

    private companion object {
        const val TAG = "CampusViewModel"
    }
}
