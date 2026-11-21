package com.bsdcampus.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.bsdcampus.app.data.AppThemeMode
import com.bsdcampus.app.data.AppUpdateDownloadState
import com.bsdcampus.app.data.AppUpdateManager
import com.bsdcampus.app.data.CampusRepository
import com.bsdcampus.app.data.PreferencesRepository
import com.bsdcampus.app.firebase.configureAppCheck
import com.bsdcampus.app.model.CampusPost
import com.bsdcampus.app.model.CampusReel
import com.bsdcampus.app.model.CampusTab
import com.bsdcampus.app.model.UiState
import com.bsdcampus.app.notifications.CampusNotificationCoordinator
import com.bsdcampus.app.notifications.CampusNotificationWorker
import com.bsdcampus.app.notifications.NotificationHelper
import com.bsdcampus.app.ui.auth.AuthPortalScreen
import com.bsdcampus.app.ui.auth.OnboardingProfileScreen
import com.bsdcampus.app.ui.auth.PendingTeacherApprovalScreen
import com.bsdcampus.app.ui.auth.SuspendedAccountScreen
import com.bsdcampus.app.ui.components.AppleTopBar
import com.bsdcampus.app.ui.components.InAppUpdateDialog
import com.bsdcampus.app.ui.components.RadialActionMenu
import com.bsdcampus.app.ui.components.ReportModal
import com.bsdcampus.app.ui.components.RepostToStoryDialog
import com.bsdcampus.app.ui.components.StoryCreatorDialog
import com.bsdcampus.app.ui.components.StoryViewerDialog
import com.bsdcampus.app.ui.components.ZenCampusTwilightNav
import com.bsdcampus.app.ui.screens.ActivityScreen
import com.bsdcampus.app.ui.screens.AdminScreen
import com.bsdcampus.app.ui.screens.ChatScreen
import com.bsdcampus.app.ui.screens.CreatePostScreen
import com.bsdcampus.app.ui.screens.EditProfileScreen
import com.bsdcampus.app.ui.screens.ExploreScreen
import com.bsdcampus.app.ui.screens.FeedScreen
import com.bsdcampus.app.ui.screens.FollowConnectionsScreen
import com.bsdcampus.app.ui.screens.ProfileScreen
import com.bsdcampus.app.ui.screens.ReelsScreen
import com.bsdcampus.app.ui.screens.SettingsScreen
import com.bsdcampus.app.ui.theme.BSDCampusTheme
import com.bsdcampus.app.viewmodel.CampusViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    private var pendingNotificationIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureAppCheck(this, intent)
        NotificationHelper.ensureChannels(this)
        pendingNotificationIntent = intent

        setContent {
            val context = LocalContext.current
            val prefsRepo = remember(context) { PreferencesRepository(context.applicationContext) }
            val themeMode by prefsRepo.themeMode.collectAsStateWithLifecycle(initialValue = AppThemeMode.SYSTEM)
            val systemInDark = isSystemInDarkTheme()
            val effectiveDarkMode = when (themeMode) {
                AppThemeMode.SYSTEM -> systemInDark
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
            }
            val scope = rememberCoroutineScope()

            BSDCampusTheme(darkTheme = effectiveDarkMode) {
                AppNavigation(
                    themeMode = themeMode,
                    onSelectThemeMode = { mode ->
                        scope.launch { prefsRepo.setThemeMode(mode) }
                    },
                    pendingNotificationIntent = pendingNotificationIntent,
                    onNotificationIntentHandled = { pendingNotificationIntent = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingNotificationIntent = intent
    }
}

@Composable
private fun AppNavigation(
    themeMode: AppThemeMode,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    pendingNotificationIntent: Intent?,
    onNotificationIntentHandled: () -> Unit,
    auth: FirebaseAuth = Firebase.auth
) {
    val authUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = authUser

    if (user == null) {
        AuthPortalScreen()
    } else {
        key(user.uid) {
            val viewModel: CampusViewModel = viewModel(
                key = user.uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        CampusViewModel(
                            repository = CampusRepository(db, auth),
                            currentUserId = user.uid,
                            googleDisplayName = user.displayName.orEmpty(),
                            googleEmail = user.email.orEmpty(),
                            isEmailVerified = user.isEmailVerified,
                            googleAvatar = user.photoUrl?.toString(),
                            preferencesRepository = PreferencesRepository(app.applicationContext),
                            appContext = app.applicationContext
                        )
                    }
                }
            )
            AuthenticatedCampusHost(
                viewModel = viewModel,
                themeMode = themeMode,
                onSelectThemeMode = onSelectThemeMode,
                pendingNotificationIntent = pendingNotificationIntent,
                onNotificationIntentHandled = onNotificationIntentHandled
            )
        }
    }
}

@Composable
private fun AuthenticatedCampusHost(
    viewModel: CampusViewModel,
    themeMode: AppThemeMode,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    pendingNotificationIntent: Intent?,
    onNotificationIntentHandled: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember(context) { CampusRepository(context.applicationContext) }

    val allUsersState by viewModel.allUsersState.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isCurrentUserAdmin.collectAsStateWithLifecycle()
    val privateInfo by viewModel.privateInfo.collectAsStateWithLifecycle()
    val postsState by viewModel.postsState.collectAsStateWithLifecycle()
    val isRefreshingFeed by viewModel.isRefreshingFeed.collectAsStateWithLifecycle()
    val reelsState by viewModel.reelsState.collectAsStateWithLifecycle()
    val storyGroups by viewModel.storyGroups.collectAsStateWithLifecycle()
    val storiesByAuthorId by viewModel.storiesByAuthorId.collectAsStateWithLifecycle()
    val highlights by viewModel.highlights.collectAsStateWithLifecycle()
    val visibleNotes by viewModel.visibleNotes.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val typingMap by viewModel.typingMap.collectAsStateWithLifecycle()
    val reports by viewModel.reports.collectAsStateWithLifecycle()
    val analytics by viewModel.analytics.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val viewingProfileUserId by viewModel.viewingProfileUserId.collectAsStateWithLifecycle()
    val connectionsTarget by viewModel.connectionsTarget.collectAsStateWithLifecycle()
    val activeChatUserId by viewModel.activeChatUserId.collectAsStateWithLifecycle()
    val unreadNotificationsCount by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle()
    val unreadMessagesCount by viewModel.unreadMessagesCount.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val isRadialMenuOpen by viewModel.isRadialMenuOpen.collectAsStateWithLifecycle()
    val reportTarget by viewModel.reportTarget.collectAsStateWithLifecycle()
    val appUpdateConfig by viewModel.appUpdateConfig.collectAsStateWithLifecycle()
    val updateDownloadState by viewModel.updateDownloadState.collectAsStateWithLifecycle()
    val isCheckingForUpdates by viewModel.isCheckingForUpdates.collectAsStateWithLifecycle()
    val showUpdateDialog by viewModel.showUpdateDialog.collectAsStateWithLifecycle()

    var isStoryCreatorOpen by remember { mutableStateOf(false) }
    var activeStoryAuthorId by remember { mutableStateOf<String?>(null) }
    var repostTargetPost by remember { mutableStateOf<CampusPost?>(null) }
    var repostTargetReel by remember { mutableStateOf<CampusReel?>(null) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            CampusNotificationWorker.schedulePeriodic(context)
        }
    }

    LaunchedEffect(Unit) {
        CampusNotificationCoordinator.init(context.applicationContext)
        CampusNotificationWorker.schedulePeriodic(context.applicationContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationHelper.hasNotificationPermission(context)
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(activeTab, activeChatUserId) {
        CampusNotificationCoordinator.activeChatPeerId =
            if (activeTab == CampusTab.CHAT) activeChatUserId else null
        CampusNotificationCoordinator.isActivityTabOpen = activeTab == CampusTab.ACTIVITY
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, currentUser?.uid) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START) {
                if (currentUser != null) {
                    viewModel.touchLastSeen(force = true)
                }
                if (event == Lifecycle.Event.ON_RESUME &&
                    viewModel.updateDownloadState.value is AppUpdateDownloadState.NeedsInstallPermission &&
                    AppUpdateManager.canInstallPackages(context)
                ) {
                    viewModel.installPendingApk(context, requestPermissionIfNeeded = false)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null) {
            while (true) {
                viewModel.touchLastSeen(force = true)
                delay(60_000L)
            }
        }
    }

    LaunchedEffect(Unit) {
        CampusNotificationCoordinator.inAppBanners.collect { banner ->
            viewModel.showToast(banner)
        }
    }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2800L)
            viewModel.clearToast()
        }
    }

    LaunchedEffect(pendingNotificationIntent, currentUser) {
        val intentToHandle = pendingNotificationIntent ?: return@LaunchedEffect
        val profileReady = currentUser != null
        if (!profileReady) return@LaunchedEffect
        when (intentToHandle.getStringExtra(NotificationHelper.EXTRA_OPEN_TAB)) {
            "chat" -> {
                val peerId = intentToHandle.getStringExtra(NotificationHelper.EXTRA_CHAT_USER_ID)
                if (!peerId.isNullOrBlank()) viewModel.openDirectChat(peerId)
                else viewModel.navigateTo(CampusTab.CHAT)
            }
            "activity" -> {
                viewModel.navigateTo(CampusTab.ACTIVITY)
            }
        }
        intentToHandle.removeExtra(NotificationHelper.EXTRA_OPEN_TAB)
        intentToHandle.removeExtra(NotificationHelper.EXTRA_CHAT_USER_ID)
        onNotificationIntentHandled()
    }

    when (val usersState = allUsersState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator()
                    Text("Loading Squadify...", style = MaterialTheme.typography.bodyMedium)
                }
            }
            return
        }
        is UiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(usersState.message, color = MaterialTheme.colorScheme.error)
            }
            return
        }
        is UiState.Success -> {
            val profile = currentUser
            if (profile == null) {
                OnboardingProfileScreen(
                    defaultName = viewModel.googleDisplayName,
                    email = viewModel.googleEmail,
                    isEmailVerified = viewModel.isEmailVerified,
                    onCreateProfile = { name, role, grade ->
                        viewModel.registerProfile(name, role, grade)
                    }
                )
                return
            }
            if (profile.isSuspended && !isAdmin) {
                SuspendedAccountScreen(user = profile)
                return
            }
            if (profile.role == "teacher" && !profile.isApproved && !isAdmin) {
                PendingTeacherApprovalScreen(user = profile)
                return
            }

            BackHandler(enabled = activeTab != CampusTab.FEED || activeChatUserId != null) {
                viewModel.navigateBack()
            }

            val allUsers = usersState.data
            val allPosts = (postsState as? UiState.Success)?.data.orEmpty()
            val allReels = (reelsState as? UiState.Success)?.data.orEmpty()

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useRail = maxWidth >= 840.dp

                Row(modifier = Modifier.fillMaxSize()) {
                    if (useRail) {
                        NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                            NavigationRailItem(
                                selected = activeTab == CampusTab.FEED,
                                onClick = { viewModel.navigateTo(CampusTab.FEED) },
                                icon = { Icon(if (activeTab == CampusTab.FEED) Icons.Filled.Home else Icons.Outlined.Home, contentDescription = "Feed") },
                                label = { Text("Feed") }
                            )
                            NavigationRailItem(
                                selected = activeTab == CampusTab.EXPLORE,
                                onClick = { viewModel.navigateTo(CampusTab.EXPLORE) },
                                icon = { Icon(if (activeTab == CampusTab.EXPLORE) Icons.Filled.Explore else Icons.Outlined.Explore, contentDescription = "Explore") },
                                label = { Text("Explore") }
                            )
                            NavigationRailItem(
                                selected = activeTab == CampusTab.CREATE,
                                onClick = { viewModel.navigateTo(CampusTab.CREATE) },
                                icon = { Icon(Icons.Filled.Add, contentDescription = "Create") },
                                label = { Text("Create") }
                            )
                            NavigationRailItem(
                                selected = activeTab == CampusTab.REELS,
                                onClick = { viewModel.navigateTo(CampusTab.REELS) },
                                icon = { Icon(if (activeTab == CampusTab.REELS) Icons.Filled.SmartDisplay else Icons.Outlined.SmartDisplay, contentDescription = "Reels") },
                                label = { Text("Reels") }
                            )
                            NavigationRailItem(
                                selected = activeTab == CampusTab.CHAT,
                                onClick = { viewModel.navigateTo(CampusTab.CHAT) },
                                icon = { Icon(Icons.Outlined.Forum, contentDescription = "Chat") },
                                label = { Text("Chat") }
                            )
                            NavigationRailItem(
                                selected = activeTab == CampusTab.ACTIVITY,
                                onClick = { viewModel.navigateTo(CampusTab.ACTIVITY) },
                                icon = { Icon(if (activeTab == CampusTab.ACTIVITY) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = "Activity") },
                                label = { Text("Activity") }
                            )
                            NavigationRailItem(
                                selected = activeTab == CampusTab.PROFILE,
                                onClick = { viewModel.openUserProfile(profile.uid) },
                                icon = { Icon(if (activeTab == CampusTab.PROFILE) Icons.Filled.Person else Icons.Outlined.Person, contentDescription = "Profile") },
                                label = { Text("Profile") }
                            )
                            if (isAdmin) {
                                NavigationRailItem(
                                    selected = activeTab == CampusTab.ADMIN,
                                    onClick = { viewModel.navigateTo(CampusTab.ADMIN) },
                                    icon = { Icon(Icons.Outlined.Shield, contentDescription = "Admin") },
                                    label = { Text("Admin") }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        val isInActiveChatThread = !useRail && activeTab == CampusTab.CHAT && activeChatUserId != null
                        val showTopBar = activeTab == CampusTab.FEED || activeTab == CampusTab.PROFILE
                        val showBottomNav = !useRail && !isInActiveChatThread && activeTab != CampusTab.CONNECTIONS
                        val hasBlockingOverlay = isStoryCreatorOpen ||
                            activeStoryAuthorId != null ||
                            repostTargetPost != null ||
                            repostTargetReel != null ||
                            reportTarget != null
                        val usersById = remember(allUsers) { allUsers.associateBy { it.uid } }
                        val viewedProfileUser = remember(viewingProfileUserId, allUsers, profile) {
                            viewingProfileUserId?.let { id -> allUsers.firstOrNull { it.uid == id } } ?: profile
                        }
                        val viewedProfileDisplayName = remember(viewedProfileUser) {
                            if (viewedProfileUser.isBlackSociety) {
                                "Member - ${viewedProfileUser.blackSocietyNumber ?: 1}"
                            } else {
                                viewedProfileUser.name
                            }
                        }

                        LaunchedEffect(activeTab, notifications) {
                            if (activeTab == CampusTab.ACTIVITY && notifications.any { !it.read }) {
                                viewModel.markNotificationsRead()
                            }
                        }

                        Scaffold(
                            containerColor = MaterialTheme.colorScheme.background,
                            contentWindowInsets = if (isInActiveChatThread) WindowInsets.statusBars else ScaffoldDefaults.contentWindowInsets,
                            topBar = {
                                if (showTopBar) {
                                    AppleTopBar(
                                        currentUser = profile,
                                        isAdmin = isAdmin,
                                        activeTab = activeTab,
                                        profileTitle = viewedProfileDisplayName,
                                        unreadNotificationsCount = unreadNotificationsCount,
                                        unreadMessagesCount = unreadMessagesCount,
                                        onNavigate = { viewModel.navigateTo(it) },
                                        onOpenNotifications = { viewModel.navigateTo(CampusTab.ACTIVITY) },
                                        onOpenChat = { viewModel.navigateTo(CampusTab.CHAT) }
                                    )
                                }
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                AnimatedContent(
                                    targetState = activeTab,
                                    transitionSpec = {
                                        (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 28 })
                                            .togetherWith(fadeOut(tween(140)))
                                    },
                                    label = "tabTransition"
                                ) { tab ->
                                    when (tab) {
                                        CampusTab.FEED -> FeedScreen(
                                            currentUser = profile,
                                            isAdmin = isAdmin,
                                            privateInfo = privateInfo,
                                            postsState = postsState,
                                            usersById = usersById,
                                            repository = repository,
                                            onToggleLike = { viewModel.toggleLikePost(it) },
                                            onToggleBookmark = { viewModel.toggleBookmarkPost(it) },
                                            onAddComment = { p, txt, replyTo -> viewModel.addComment(p, txt, replyTo) },
                                            onToggleCommentLike = { p, c -> viewModel.toggleLikeComment(p, c) },
                                            onDeleteComment = { pId, cId -> viewModel.deleteComment(pId, cId) },
                                            onMarkPostViewed = { viewModel.markPostViewed(it) },
                                            onTogglePin = { viewModel.togglePinPost(it) },
                                            onDeletePost = { viewModel.deletePost(it) },
                                            onOpenAuthorProfile = { viewModel.openUserProfile(it) },
                                            onMessageAuthor = { viewModel.openDirectChat(it) },
                                            onReportPost = { p -> viewModel.openReport("post", p.id, p.content) },
                                            onUpdatePost = { p, content, cat, ratio, track, startSec ->
                                                viewModel.updatePost(p, content, cat, ratio, track, startSec)
                                            },
                                            onAddPostToStory = { repostTargetPost = it },
                                            storyGroups = storyGroups,
                                            onOpenAddStory = { isStoryCreatorOpen = true },
                                            onOpenStory = { authorId -> activeStoryAuthorId = authorId },
                                            onNavigateCreate = { viewModel.navigateTo(CampusTab.CREATE) },
                                            isRefreshing = isRefreshingFeed,
                                            onRefresh = { viewModel.refreshFeed() },
                                            isScreenActive = activeTab == CampusTab.FEED && !hasBlockingOverlay
                                        )
                                        CampusTab.EXPLORE -> ExploreScreen(
                                            currentUser = profile,
                                            isAdmin = isAdmin,
                                            privateInfo = privateInfo,
                                            allUsersState = allUsersState,
                                            postsState = postsState,
                                            repository = repository,
                                            onOpenUserProfile = { viewModel.openUserProfile(it) },
                                            onToggleFollow = { viewModel.toggleFollow(it) },
                                            onToggleLike = { viewModel.toggleLikePost(it) },
                                            onToggleBookmark = { viewModel.toggleBookmarkPost(it) },
                                            onAddComment = { p, txt, replyTo -> viewModel.addComment(p, txt, replyTo) },
                                            onToggleCommentLike = { p, c -> viewModel.toggleLikeComment(p, c) },
                                            onDeleteComment = { pId, cId -> viewModel.deleteComment(pId, cId) },
                                            onMarkPostViewed = { viewModel.markPostViewed(it) },
                                            onTogglePin = { viewModel.togglePinPost(it) },
                                            onDeletePost = { viewModel.deletePost(it) },
                                            onMessageAuthor = { viewModel.openDirectChat(it) },
                                            onReportPost = { p -> viewModel.openReport("post", p.id, p.content) },
                                            onUpdatePost = { p, content, cat, ratio, track, startSec ->
                                                viewModel.updatePost(p, content, cat, ratio, track, startSec)
                                            },
                                            onAddPostToStory = { repostTargetPost = it },
                                            isScreenActive = activeTab == CampusTab.EXPLORE && !hasBlockingOverlay
                                        )
                                        CampusTab.CREATE -> CreatePostScreen(
                                            currentUser = profile,
                                            isAdmin = isAdmin,
                                            onPublish = { content, cat, mediaItems, ratio, official, pinned, track, startSec, onDone ->
                                                viewModel.enqueueBackgroundPostUpload(
                                                    context = context,
                                                    content = content,
                                                    category = cat,
                                                    mediaItems = mediaItems,
                                                    imageAspectRatio = ratio,
                                                    isOfficial = official,
                                                    isPinned = pinned,
                                                    musicTrack = track,
                                                    musicStartSec = startSec,
                                                    onAccepted = onDone
                                                )
                                            },
                                            onPublishReel = { videoUri, caption, cat, audio, videoRatio, track, startSec, muteOrig, onDone ->
                                                viewModel.enqueueBackgroundReelUpload(
                                                    context = context,
                                                    videoUri = videoUri,
                                                    caption = caption,
                                                    category = cat,
                                                    audioTitle = audio,
                                                    videoAspectRatio = videoRatio,
                                                    musicTrack = track,
                                                    musicStartSec = startSec,
                                                    muteOriginalAudio = muteOrig,
                                                    onAccepted = onDone
                                                )
                                            },
                                            onShowToast = { viewModel.showToast(it) }
                                        )
                                        CampusTab.REELS -> ReelsScreen(
                                            currentUser = profile,
                                            isAdmin = isAdmin,
                                            privateInfo = privateInfo,
                                            reelsState = reelsState,
                                            usersById = usersById,
                                            repository = repository,
                                            onToggleLike = { viewModel.toggleLikeReel(it) },
                                            onToggleBookmark = { viewModel.toggleBookmarkReel(it) },
                                            onToggleFollow = { viewModel.toggleFollow(it) },
                                            onAddComment = { r, txt, replyTo -> viewModel.addReelComment(r, txt, replyTo) },
                                            onToggleCommentLike = { r, c -> viewModel.toggleLikeReelComment(r, c) },
                                            onDeleteComment = { rId, cId -> viewModel.deleteReelComment(rId, cId) },
                                            onMarkViewed = { viewModel.markReelViewed(it) },
                                            onUpdateReel = { r, cap, cat, aud, videoRatio, track, startSec, muteOrig ->
                                                viewModel.updateReel(r, cap, cat, aud, videoRatio, track, startSec, muteOrig)
                                            },
                                            onDeleteReel = { viewModel.deleteReel(it) },
                                            onAddToStory = { repostTargetReel = it },
                                            onOpenAuthorProfile = { viewModel.openUserProfile(it) },
                                            onMessageAuthor = { viewModel.openDirectChat(it) },
                                            onReportReel = { r -> viewModel.openReport("reel", r.id, r.caption.ifBlank { "Campus Reel" }) },
                                            onCreateReel = { videoUri, caption, cat, audio, videoRatio, track, startSec, muteOrig, onDone ->
                                                viewModel.enqueueBackgroundReelUpload(
                                                    context = context,
                                                    videoUri = videoUri,
                                                    caption = caption,
                                                    category = cat,
                                                    audioTitle = audio,
                                                    videoAspectRatio = videoRatio,
                                                    musicTrack = track,
                                                    musicStartSec = startSec,
                                                    muteOriginalAudio = muteOrig,
                                                    onAccepted = onDone
                                                )
                                            },
                                            onShowToast = { viewModel.showToast(it) }
                                        )
                                        CampusTab.CHAT -> ChatScreen(
                                            currentUser = profile,
                                            allUsersState = allUsersState,
                                            conversations = conversations,
                                            messages = messages,
                                            notes = visibleNotes,
                                            typingMap = typingMap,
                                            activeChatUserId = activeChatUserId,
                                            onSelectChatUser = { viewModel.openDirectChat(it) },
                                            onClearChatUser = { viewModel.clearActiveChat() },
                                            onOpenProfile = { viewModel.openUserProfile(it) },
                                            onSendMessage = { rec, content, media, audio, dur, replyMsg, replyName ->
                                                viewModel.sendMessage(rec, content, media, audio, dur, replyMsg, replyName)
                                            },
                                            onEditMessage = { id, newText -> viewModel.editMessage(id, newText) },
                                            onDeleteMessage = { id -> viewModel.deleteMessage(id) },
                                            onToggleReaction = { msg, emoji -> viewModel.toggleMessageReaction(msg, emoji) },
                                            onMarkThreadRead = { viewModel.markThreadRead(it) },
                                            onSetTyping = { rec, typing -> viewModel.setTyping(rec, typing) },
                                            onSaveNote = { text, color, track, startSec, onDone ->
                                                viewModel.saveNote(text, color, track, startSec, onDone)
                                            },
                                            onDeleteNote = { onDone -> viewModel.deleteNote(onDone) },
                                            onShowToast = { viewModel.showToast(it) }
                                        )
                                        CampusTab.ACTIVITY -> ActivityScreen(
                                            notifications = notifications,
                                            usersById = usersById,
                                            onMarkAllRead = { viewModel.markNotificationsRead() },
                                            onOpenSenderProfile = { viewModel.openUserProfile(it) },
                                            onBack = { viewModel.navigateBack() }
                                        )
                                        CampusTab.PROFILE -> {
                                            val viewedUser = viewingProfileUserId?.let { id ->
                                                allUsers.firstOrNull { it.uid == id }
                                            } ?: profile
                                            val ownActiveStories = storyGroups.firstOrNull { it.isOwn }?.stories.orEmpty()
                                            val canViewProfileStories = viewedUser.uid == profile.uid ||
                                                !viewedUser.isPrivateAccount ||
                                                profile.following.contains(viewedUser.uid)
                                            val profileActiveStories = if (canViewProfileStories) {
                                                storiesByAuthorId[viewedUser.uid]?.stories.orEmpty()
                                            } else {
                                                emptyList()
                                            }
                                            ProfileScreen(
                                                currentUser = profile,
                                                isAdmin = isAdmin,
                                                profileUser = viewedUser,
                                                allUsers = allUsers,
                                                posts = allPosts,
                                                reels = allReels,
                                                highlights = highlights,
                                                ownActiveStories = ownActiveStories,
                                                profileActiveStories = profileActiveStories,
                                                onOpenProfileStory = {
                                                    if (profileActiveStories.isNotEmpty()) {
                                                        activeStoryAuthorId = viewedUser.uid
                                                    }
                                                },
                                                privateInfo = privateInfo,
                                                repository = repository,
                                                onNavigate = { viewModel.navigateTo(it) },
                                                onOpenUserProfile = { viewModel.openUserProfile(it) },
                                                onOpenConnections = { uid, idx -> viewModel.openFollowConnections(uid, idx) },
                                                onToggleFollow = { viewModel.toggleFollow(it) },
                                                onOpenDirectChat = { viewModel.openDirectChat(it) },
                                                onReportUser = { u -> viewModel.openReport("user", u.uid, u.name) },
                                                onToggleLike = { viewModel.toggleLikePost(it) },
                                                onToggleBookmark = { viewModel.toggleBookmarkPost(it) },
                                                onAddComment = { p, txt, replyTo -> viewModel.addComment(p, txt, replyTo) },
                                                onToggleCommentLike = { p, c -> viewModel.toggleLikeComment(p, c) },
                                                onDeleteComment = { pId, cId -> viewModel.deleteComment(pId, cId) },
                                                onMarkPostViewed = { viewModel.markPostViewed(it) },
                                                onTogglePin = { viewModel.togglePinPost(it) },
                                                onDeletePost = { viewModel.deletePost(it) },
                                                onReportPost = { p -> viewModel.openReport("post", p.id, p.content) },
                                                onUpdatePost = { p, content, cat, ratio, track, startSec ->
                                                    viewModel.updatePost(p, content, cat, ratio, track, startSec)
                                                },
                                                onAddPostToStory = { repostTargetPost = it },
                                                onToggleLikeReel = { viewModel.toggleLikeReel(it) },
                                                onToggleBookmarkReel = { viewModel.toggleBookmarkReel(it) },
                                                onAddReelComment = { r, txt, replyTo -> viewModel.addReelComment(r, txt, replyTo) },
                                                onToggleReelCommentLike = { r, c -> viewModel.toggleLikeReelComment(r, c) },
                                                onDeleteReelComment = { rId, cId -> viewModel.deleteReelComment(rId, cId) },
                                                onMarkReelViewed = { viewModel.markReelViewed(it) },
                                                onUpdateReel = { r, cap, cat, aud, videoRatio, track, startSec, muteOrig ->
                                                    viewModel.updateReel(r, cap, cat, aud, videoRatio, track, startSec, muteOrig)
                                                },
                                                onDeleteReel = { viewModel.deleteReel(it) },
                                                onReportReel = { r -> viewModel.openReport("reel", r.id, r.caption.ifBlank { "Campus Reel" }) },
                                                onAddReelToStory = { repostTargetReel = it },
                                                onCreateHighlight = { title, avatar, emoji, items, onDone ->
                                                    viewModel.createHighlight(title, avatar, emoji, items, onDone)
                                                },
                                                onUpdateHighlight = { hlId, title, avatar, emoji, items, onDone ->
                                                    viewModel.updateHighlight(hlId, title, avatar, emoji, items, onDone)
                                                },
                                                onDeleteHighlight = { viewModel.deleteHighlight(it) },
                                                onShowToast = { viewModel.showToast(it) },
                                                isScreenActive = activeTab == CampusTab.PROFILE && !hasBlockingOverlay
                                            )
                                        }
                                        CampusTab.CONNECTIONS -> {
                                            val (targetUid, initialTab) = connectionsTarget ?: (profile.uid to 0)
                                            val targetUser = allUsers.firstOrNull { it.uid == targetUid } ?: profile
                                            FollowConnectionsScreen(
                                                profileUser = targetUser,
                                                currentUser = profile,
                                                allUsers = allUsers,
                                                initialTabIndex = initialTab,
                                                onToggleFollow = { viewModel.toggleFollow(it) },
                                                onOpenUserProfile = { viewModel.openUserProfile(it) },
                                                onBack = { viewModel.navigateBack() }
                                            )
                                        }
                                        CampusTab.EDIT_PROFILE -> EditProfileScreen(
                                            currentUser = profile,
                                            isAdmin = isAdmin,
                                            googleEmail = viewModel.googleEmail,
                                            onSaveAvatar = { avatar, onDone ->
                                                viewModel.updateAvatar(avatar, onDone)
                                            },
                                            onSaveProfile = { name, grade, avatar, bio ->
                                                viewModel.updateProfile(name, grade, avatar, bio)
                                            },
                                            onShowToast = { viewModel.showToast(it) },
                                            onBack = { viewModel.navigateBack() }
                                        )
                                        CampusTab.SETTINGS -> SettingsScreen(
                                            currentUser = profile,
                                            isAdmin = isAdmin,
                                            googleEmail = viewModel.googleEmail,
                                            privateInfo = privateInfo,
                                            posts = allPosts,
                                            themeMode = themeMode,
                                            appUpdateConfig = appUpdateConfig,
                                            isCheckingForUpdates = isCheckingForUpdates,
                                            onSelectThemeMode = onSelectThemeMode,
                                            onUpdatePrivateSettings = { transform -> viewModel.updatePrivateSettings(transform) },
                                            onCheckForUpdates = { viewModel.checkForAppUpdates(userInitiated = true) },
                                            onNavigate = { viewModel.navigateTo(it) },
                                            onOpenReportApp = { viewModel.openReport("app", "bsd_campus_app", "General App Feedback") },
                                            onBack = { viewModel.navigateBack() }
                                        )
                                        CampusTab.ADMIN -> AdminScreen(
                                            currentUser = profile,
                                            isAdmin = isAdmin,
                                            googleEmail = viewModel.googleEmail,
                                            isEmailVerified = viewModel.isEmailVerified,
                                            analytics = analytics,
                                            allUsers = allUsers,
                                            posts = allPosts,
                                            reports = reports,
                                            appUpdateConfig = appUpdateConfig,
                                            onClaimSuperAdmin = { viewModel.claimSuperAdmin() },
                                            onApproveTeacher = { viewModel.approveTeacher(it) },
                                            onRejectTeacher = { viewModel.rejectTeacher(it) },
                                            onToggleSuspend = { id, susp -> viewModel.toggleSuspendUser(id, susp) },
                                            onToggleVerify = { id, ver -> viewModel.toggleVerifyUser(id, ver) },
                                            onUpdateRole = { id, em, role -> viewModel.updateUserRole(id, em, role) },
                                            onToggleDarkSociety = { id, grant -> viewModel.toggleDarkSociety(id, grant) },
                                            onAssignClassRep = { id, g, assign -> viewModel.assignClassRep(id, g, assign) },
                                            onTogglePin = { viewModel.togglePinPost(it) },
                                            onDeletePost = { viewModel.deletePost(it) },
                                            onResolveReport = { id, st, act -> viewModel.resolveReport(id, st, act) },
                                            onBroadcast = { text, cat, onDone -> viewModel.broadcastAnnouncement(text, cat, onDone) },
                                            onPublishAppUpdate = { code, verName, url, notes, mandatory, onDone ->
                                                viewModel.publishAppUpdateConfig(code, verName, url, notes, mandatory, onDone)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (showBottomNav) {
                            ZenCampusTwilightNav(
                                activeTab = activeTab,
                                unreadNotificationsCount = unreadNotificationsCount,
                                isAdmin = isAdmin,
                                currentUser = profile,
                                onSelectTab = { tab ->
                                    if (tab == CampusTab.PROFILE) viewModel.openUserProfile(profile.uid)
                                    else viewModel.navigateTo(tab)
                                },
                                onOpenRadialMenu = { viewModel.setRadialMenuOpen(true) },
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }

                        RadialActionMenu(
                            visible = isRadialMenuOpen,
                            onDismiss = { viewModel.setRadialMenuOpen(false) },
                            onNavigate = { viewModel.navigateTo(it) },
                            onOpenStoryCreator = { isStoryCreatorOpen = true }
                        )

                        androidx.compose.animation.AnimatedVisibility(
                            visible = toastMessage != null,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = if (isInActiveChatThread) 76.dp else 92.dp, start = 20.dp, end = 20.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.inverseSurface,
                                tonalElevation = 0.dp
                            ) {
                                Text(
                                    text = toastMessage.orEmpty(),
                                    color = MaterialTheme.colorScheme.inverseOnSurface,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }

            reportTarget?.let { (type, targetId, summary) ->
                ReportModal(
                    targetType = type,
                    targetSummary = summary,
                    onSubmit = { reason, details ->
                        viewModel.submitReport(type, targetId, summary, reason, details)
                    },
                    onDismiss = { viewModel.closeReport() }
                )
            }

            if (isStoryCreatorOpen) {
                StoryCreatorDialog(
                    currentUser = profile,
                    isAdmin = isAdmin,
                    onShowToast = { viewModel.showToast(it) },
                    onDismiss = { isStoryCreatorOpen = false }
                )
            }

            activeStoryAuthorId?.let { storyAuthorId ->
                val viewerGroups = remember(storyGroups, storiesByAuthorId, storyAuthorId) {
                    if (storyGroups.any { it.author.uid == storyAuthorId }) {
                        storyGroups
                    } else {
                        storiesByAuthorId[storyAuthorId]?.let { listOf(it) } ?: storyGroups
                    }
                }
                StoryViewerDialog(
                    storyGroups = viewerGroups,
                    initialAuthorId = storyAuthorId,
                    currentUser = profile,
                    isAdmin = isAdmin,
                    usersById = allUsers.associateBy { it.uid },
                    onMarkViewed = { viewModel.markStoryViewed(it) },
                    onToggleLikeStory = { viewModel.toggleLikeStory(it) },
                    onDeleteStory = { viewModel.deleteStory(it) },
                    onReplyToStory = { story, aName, reply ->
                        viewModel.replyToStory(story, aName, reply)
                    },
                    onOpenAuthorProfile = { viewModel.openUserProfile(it) },
                    onOpenAddStory = { isStoryCreatorOpen = true },
                    onDismiss = { activeStoryAuthorId = null }
                )
            }

            repostTargetPost?.let { target ->
                val liveAuthor = allUsers.firstOrNull { it.uid == target.authorId }
                val isAnon = liveAuthor?.isBlackSociety ?: target.authorIsBlackSociety
                val anonNum = liveAuthor?.blackSocietyNumber ?: target.authorBlackSocietyNumber
                val resolvedName = if (isAnon) "Member - ${anonNum ?: 1}" else (liveAuthor?.name ?: target.authorName)
                val resolvedAvatar = if (isAnon) null else (liveAuthor?.avatar ?: target.authorAvatar)
                RepostToStoryDialog(
                    sourceType = "post",
                    authorName = resolvedName,
                    authorAvatar = resolvedAvatar,
                    previewMediaUrl = target.images.firstOrNull(),
                    sourceCaption = target.content,
                    category = target.category,
                    onShareToStory = { note, music, startSec, muteOriginal, onSuccess ->
                        viewModel.sharePostToStory(
                            post = target,
                            note = note,
                            musicTrack = music,
                            musicStartSec = startSec,
                            muteOriginalAudio = muteOriginal,
                            onSuccess = onSuccess
                        )
                    },
                    onDismiss = { repostTargetPost = null }
                )
            }

            repostTargetReel?.let { target ->
                val liveAuthor = allUsers.firstOrNull { it.uid == target.authorId }
                val isAnon = liveAuthor?.isBlackSociety ?: target.authorIsBlackSociety
                val anonNum = liveAuthor?.blackSocietyNumber ?: target.authorBlackSocietyNumber
                val resolvedName = if (isAnon) "Member - ${anonNum ?: 1}" else (liveAuthor?.name ?: target.authorName)
                val resolvedAvatar = if (isAnon) null else (liveAuthor?.avatar ?: target.authorAvatar)
                RepostToStoryDialog(
                    sourceType = "reel",
                    authorName = resolvedName,
                    authorAvatar = resolvedAvatar,
                    previewMediaUrl = target.videoUrl,
                    sourceCaption = target.caption,
                    category = target.category,
                    onShareToStory = { note, music, startSec, muteOriginal, onSuccess ->
                        viewModel.shareReelToStory(
                            reel = target,
                            note = note,
                            musicTrack = music,
                            musicStartSec = startSec,
                            muteOriginalAudio = muteOriginal,
                            onSuccess = onSuccess
                        )
                    },
                    onDismiss = { repostTargetReel = null }
                )
            }

            if (showUpdateDialog) {
                appUpdateConfig?.let { updateCfg ->
                    InAppUpdateDialog(
                        config = updateCfg,
                        downloadState = updateDownloadState,
                        onStartDownload = { viewModel.startAppUpdateDownload(context) },
                        onInstallPending = { viewModel.installPendingApk(context, requestPermissionIfNeeded = true) },
                        onDismiss = { viewModel.dismissUpdateDialog() }
                    )
                }
            }
        }
    }
}
