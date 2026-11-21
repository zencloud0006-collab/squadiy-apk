package com.bsdcampus.app.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import com.bsdcampus.app.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.bsdcampus.app.data.CampusChunkedVideoCache
import com.bsdcampus.app.data.CampusMediaRepository
import com.bsdcampus.app.data.CampusRepository
import com.bsdcampus.app.data.MediaUtils
import com.bsdcampus.app.data.MusicCacheManager
import com.bsdcampus.app.data.StoryMediaEditor
import com.bsdcampus.app.data.StoryMusicTrack
import com.bsdcampus.app.model.CampusConstants
import com.bsdcampus.app.model.CampusReel
import com.bsdcampus.app.model.CampusUser
import com.bsdcampus.app.model.PostImageRatio
import com.bsdcampus.app.model.ReelComment
import com.bsdcampus.app.model.UiState
import com.bsdcampus.app.model.UserPrivateInfo
import com.bsdcampus.app.notifications.MediaUploadKind
import com.bsdcampus.app.notifications.MediaUploadTracker
import com.bsdcampus.app.ui.components.AvatarSize
import com.bsdcampus.app.ui.components.CampusImage
import com.bsdcampus.app.ui.components.StoryMusicPickerSheet
import com.bsdcampus.app.ui.components.StoryMusicStickerPill
import com.bsdcampus.app.ui.components.UserAvatar
import com.bsdcampus.app.ui.components.VerifiedBadgeIcon
import com.bsdcampus.app.ui.components.formatRelativeTime
import com.bsdcampus.app.ui.theme.Emerald500
import com.bsdcampus.app.ui.theme.Gold300
import com.bsdcampus.app.ui.theme.Gold400
import com.bsdcampus.app.ui.theme.InstaBlue
import com.bsdcampus.app.ui.theme.InstaLikeRed
import com.bsdcampus.app.ui.theme.Slate900
import com.bsdcampus.app.ui.theme.Zinc700
import com.bsdcampus.app.ui.theme.Zinc900
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(UnstableApi::class)
@Composable
fun ReelsScreen(
    currentUser: CampusUser,
    isAdmin: Boolean,
    privateInfo: UserPrivateInfo?,
    reelsState: UiState<List<CampusReel>>,
    usersById: Map<String, CampusUser>,
    repository: CampusRepository,
    initialReelId: String? = null,
    onToggleLike: (CampusReel) -> Unit,
    onToggleBookmark: (String) -> Unit,
    onToggleFollow: (String) -> Unit,
    onAddComment: (CampusReel, String, ReelComment?) -> Unit,
    onToggleCommentLike: (CampusReel, ReelComment) -> Unit = { _, _ -> },
    onDeleteComment: (String, String) -> Unit,
    onMarkViewed: (CampusReel) -> Unit = {},
    onUpdateReel: (CampusReel, String, String, String, String, StoryMusicTrack?, Int, Boolean) -> Unit,
    onDeleteReel: (CampusReel) -> Unit,
    onAddToStory: (CampusReel) -> Unit,
    onOpenAuthorProfile: (String) -> Unit,
    onMessageAuthor: (String) -> Unit,
    onReportReel: (CampusReel) -> Unit,
    onCreateReel: (
        videoUri: Uri,
        caption: String,
        category: String,
        audioTitle: String,
        videoAspectRatio: PostImageRatio,
        musicTrack: StoryMusicTrack?,
        musicStartSec: Int,
        muteOriginalAudio: Boolean,
        onDone: () -> Unit
    ) -> Unit,
    onShowToast: (String) -> Unit
) {
    var isMuted by remember { mutableStateOf(false) }
    var showCreateReelDialog by remember { mutableStateOf(false) }
    val mediaUploadState by MediaUploadTracker.state.collectAsStateWithLifecycle()
    val reelUploadState = mediaUploadState?.takeIf { it.kind == MediaUploadKind.REEL }
    LaunchedEffect(reelUploadState?.isCompleted, reelUploadState?.uploadId) {
        if (reelUploadState?.isCompleted == true) {
            delay(3200L)
            MediaUploadTracker.clear()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when (reelsState) {
            is UiState.Loading -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Text("Loading BSD Reels...", color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodySmall)
                }
            }
            is UiState.Error -> {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Unable to load Reels", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(reelsState.message, color = InstaLikeRed, style = MaterialTheme.typography.bodySmall)
                }
            }
            is UiState.Success -> {
                val reels = reelsState.data
                if (reels.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SmartDisplay,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "No Campus Reels Yet",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Post the first short vertical video from BSD events, sports, labs, or campus life!",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = { showCreateReelDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = InstaBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.testTag("reels_empty_create_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create First Reel", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    val initialPage = remember(initialReelId, reels.size) {
                        if (initialReelId == null) 0
                        else reels.indexOfFirst { it.id == initialReelId }.coerceAtLeast(0)
                    }
                    val pagerState = rememberPagerState(
                        initialPage = initialPage,
                        pageCount = { reels.size }
                    )
                    val screenContext = LocalContext.current
                    LaunchedEffect(reels, pagerState.currentPage) {
                        val upcomingReels = reels.drop(pagerState.currentPage).take(3)
                        MusicCacheManager.prefetchUrls(
                            screenContext,
                            upcomingReels.mapNotNull { it.musicPreviewUrl?.takeIf { u -> u.isNotBlank() } }
                        )
                        upcomingReels.forEach { upcoming ->
                            if (MediaUtils.isChunkedMediaUri(upcoming.videoUrl)) {
                                launch {
                                    CampusChunkedVideoCache.prefetchVideoHead(screenContext, upcoming.videoUrl)
                                }
                            }
                        }
                    }

                    VerticalPager(
                        state = pagerState,
                        beyondViewportPageCount = 1,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("reels_vertical_pager")
                    ) { page ->
                        val reel = reels[page]
                        val isActivePage = pagerState.currentPage == page
                        val isSaved = privateInfo?.savedReelIds?.contains(reel.id) == true
                        ReelPageItem(
                            reel = reel,
                            isActivePage = isActivePage,
                            isMuted = isMuted,
                            onToggleMute = { isMuted = !isMuted },
                            currentUser = currentUser,
                            isAdmin = isAdmin,
                            isBookmarked = isSaved,
                            usersById = usersById,
                            repository = repository,
                            onToggleLike = { onToggleLike(reel) },
                            onToggleBookmark = { onToggleBookmark(reel.id) },
                            onToggleFollow = { onToggleFollow(reel.authorId) },
                            onAddComment = { txt, replyTo -> onAddComment(reel, txt, replyTo) },
                            onToggleCommentLike = { comment -> onToggleCommentLike(reel, comment) },
                            onDeleteComment = { cId -> onDeleteComment(reel.id, cId) },
                            onMarkViewed = { onMarkViewed(reel) },
                            onUpdateReel = { cap, cat, aud, videoRatio, track, startSec, muteOrig ->
                                onUpdateReel(reel, cap, cat, aud, videoRatio, track, startSec, muteOrig)
                            },
                            onDeleteReel = { onDeleteReel(reel) },
                            onAddToStory = { onAddToStory(reel) },
                            onOpenAuthorProfile = { onOpenAuthorProfile(reel.authorId) },
                            onMessageAuthor = { onMessageAuthor(reel.authorId) },
                            onReportReel = { onReportReel(reel) },
                            onOpenCreateReel = { showCreateReelDialog = true }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = reelUploadState != null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp, start = 14.dp, end = 14.dp)
        ) {
            reelUploadState?.let { uploadState ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xEA121216),
                    border = BorderStroke(
                        0.75.dp,
                        when {
                            uploadState.isFailed -> InstaLikeRed.copy(alpha = 0.75f)
                            uploadState.isCompleted -> Emerald500.copy(alpha = 0.75f)
                            else -> Color.White.copy(alpha = 0.2f)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reel_upload_banner")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    uploadState.isFailed -> Icons.Outlined.ErrorOutline
                                    uploadState.isCompleted -> Icons.Filled.CheckCircle
                                    else -> Icons.Filled.SmartDisplay
                                },
                                contentDescription = null,
                                tint = when {
                                    uploadState.isFailed -> InstaLikeRed
                                    uploadState.isCompleted -> Emerald500
                                    else -> InstaBlue
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uploadState.statusLabel,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!uploadState.errorMessage.isNullOrBlank()) {
                                    Text(
                                        text = uploadState.errorMessage,
                                        color = InstaLikeRed,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (uploadState.isFailed) {
                                IconButton(
                                    onClick = { MediaUploadTracker.clear() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Dismiss upload error", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            } else if (!uploadState.isCompleted) {
                                Text(
                                    text = "${(uploadState.progress * 100).toInt()}%",
                                    color = InstaBlue,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (!uploadState.isCompleted && !uploadState.isFailed) {
                            LinearProgressIndicator(
                                progress = { uploadState.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(CircleShape),
                                color = InstaBlue,
                                trackColor = Color.White.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateReelDialog) {
        CreateReelDialog(
            onPublishReel = { videoUri, caption, category, audioTitle, videoRatio, track, startSec, muteOrig, onDone ->
                onCreateReel(videoUri, caption, category, audioTitle, videoRatio, track, startSec, muteOrig) {
                    onDone()
                    showCreateReelDialog = false
                }
            },
            onDismiss = { showCreateReelDialog = false }
        )
    }
}

@Composable
fun ReelPlayerDialog(
    initialReel: CampusReel,
    allReels: List<CampusReel>,
    currentUser: CampusUser,
    isAdmin: Boolean,
    privateInfo: UserPrivateInfo?,
    usersById: Map<String, CampusUser>,
    repository: CampusRepository,
    onToggleLike: (CampusReel) -> Unit,
    onToggleBookmark: (String) -> Unit,
    onToggleFollow: (String) -> Unit,
    onAddComment: (CampusReel, String, ReelComment?) -> Unit,
    onToggleCommentLike: (CampusReel, ReelComment) -> Unit = { _, _ -> },
    onDeleteComment: (String, String) -> Unit,
    onMarkViewed: (CampusReel) -> Unit = {},
    onUpdateReel: (CampusReel, String, String, String, String, StoryMusicTrack?, Int, Boolean) -> Unit,
    onDeleteReel: (CampusReel) -> Unit,
    onAddToStory: (CampusReel) -> Unit,
    onOpenAuthorProfile: (String) -> Unit,
    onMessageAuthor: (String) -> Unit,
    onReportReel: (CampusReel) -> Unit,
    onDismiss: () -> Unit
) {
    val activeList = remember(allReels, initialReel.id) {
        if (allReels.any { it.id == initialReel.id }) allReels else listOf(initialReel)
    }
    var isMuted by remember { mutableStateOf(false) }
    val startIndex = remember(initialReel.id, activeList.size) {
        activeList.indexOfFirst { it.id == initialReel.id }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { activeList.size })
    val dialogContext = LocalContext.current
    LaunchedEffect(activeList, pagerState.currentPage) {
        val upcomingReels = activeList.drop(pagerState.currentPage).take(3)
        MusicCacheManager.prefetchUrls(
            dialogContext,
            upcomingReels.mapNotNull { it.musicPreviewUrl?.takeIf { u -> u.isNotBlank() } }
        )
        upcomingReels.forEach { upcoming ->
            if (MediaUtils.isChunkedMediaUri(upcoming.videoUrl)) {
                launch {
                    CampusChunkedVideoCache.prefetchVideoHead(dialogContext, upcoming.videoUrl)
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            VerticalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val reel = activeList[page]
                ReelPageItem(
                    reel = reel,
                    isActivePage = pagerState.currentPage == page,
                    isMuted = isMuted,
                    onToggleMute = { isMuted = !isMuted },
                    currentUser = currentUser,
                    isAdmin = isAdmin,
                    isBookmarked = privateInfo?.savedReelIds?.contains(reel.id) == true,
                    usersById = usersById,
                    repository = repository,
                    onToggleLike = { onToggleLike(reel) },
                    onToggleBookmark = { onToggleBookmark(reel.id) },
                    onToggleFollow = { onToggleFollow(reel.authorId) },
                    onAddComment = { txt, replyTo -> onAddComment(reel, txt, replyTo) },
                    onToggleCommentLike = { comment -> onToggleCommentLike(reel, comment) },
                    onDeleteComment = { cId -> onDeleteComment(reel.id, cId) },
                    onMarkViewed = { onMarkViewed(reel) },
                    onUpdateReel = { cap, cat, aud, videoRatio, track, startSec, muteOrig ->
                        onUpdateReel(reel, cap, cat, aud, videoRatio, track, startSec, muteOrig)
                    },
                    onDeleteReel = {
                        onDeleteReel(reel)
                        onDismiss()
                    },
                    onAddToStory = { onAddToStory(reel) },
                    onOpenAuthorProfile = {
                        onDismiss()
                        onOpenAuthorProfile(reel.authorId)
                    },
                    onMessageAuthor = {
                        onDismiss()
                        onMessageAuthor(reel.authorId)
                    },
                    onReportReel = { onReportReel(reel) },
                    onOpenCreateReel = null,
                    topEndCloseButton = {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Close reel", tint = Color.White)
                        }
                    }
                )
            }
        }
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@OptIn(UnstableApi::class)
@Composable
private fun ReelPageItem(
    reel: CampusReel,
    isActivePage: Boolean,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    currentUser: CampusUser,
    isAdmin: Boolean,
    isBookmarked: Boolean,
    usersById: Map<String, CampusUser>,
    repository: CampusRepository,
    onToggleLike: () -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleFollow: () -> Unit,
    onAddComment: (String, ReelComment?) -> Unit,
    onToggleCommentLike: (ReelComment) -> Unit = {},
    onDeleteComment: (String) -> Unit,
    onMarkViewed: () -> Unit = {},
    onUpdateReel: (String, String, String, String, StoryMusicTrack?, Int, Boolean) -> Unit,
    onDeleteReel: () -> Unit,
    onAddToStory: () -> Unit,
    onOpenAuthorProfile: () -> Unit,
    onMessageAuthor: () -> Unit,
    onReportReel: () -> Unit,
    onOpenCreateReel: (() -> Unit)?,
    topEndCloseButton: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isChunked = MediaUtils.isChunkedMediaUri(reel.videoUrl)
    val cachedLocalFile = remember(reel.videoUrl) {
        if (isChunked) {
            MediaUtils.extractAssetId(reel.videoUrl)?.let { CampusChunkedVideoCache.cachedMp4File(context, it) }
        } else null
    }
    var localFile by remember(reel.videoUrl) { mutableStateOf<File?>(cachedLocalFile) }
    var isLoadingVideo by remember(reel.videoUrl) { mutableStateOf(false) }
    var isBuffering by remember(reel.videoUrl) { mutableStateOf(false) }
    var videoError by remember(reel.videoUrl) { mutableStateOf<String?>(null) }
    var userPaused by remember(reel.id) { mutableStateOf(false) }
    var showHeartBurst by remember { mutableStateOf(false) }
    var showCommentsSheet by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var commentDraft by remember { mutableStateOf("") }
    var replyingToComment by remember(reel.id) { mutableStateOf<ReelComment?>(null) }
    var editCaption by remember(reel.id, reel.caption) { mutableStateOf(reel.caption) }
    var editCategory by remember(reel.id, reel.category) { mutableStateOf(reel.category) }
    var editAudio by remember(reel.id, reel.audioTitle) { mutableStateOf(reel.audioTitle) }
    var editVideoAspectRatio by remember(reel.id, reel.videoAspectRatio) { mutableStateOf(PostImageRatio.fromKey(reel.videoAspectRatio)) }
    val initialReelTrack = remember(reel.id, reel.musicPreviewUrl, reel.musicTitle, reel.musicArtist, reel.musicArtworkUrl) {
        val preview = reel.musicPreviewUrl?.takeIf { it.isNotBlank() }
        val title = reel.musicTitle?.takeIf { it.isNotBlank() }
        if (preview != null && title != null) {
            StoryMusicTrack(
                title = title,
                artist = reel.musicArtist.orEmpty().ifBlank { "Artist" },
                artworkUrl = reel.musicArtworkUrl.orEmpty(),
                previewUrl = preview
            )
        } else null
    }
    var editMusicTrack by remember(reel.id, initialReelTrack) { mutableStateOf(initialReelTrack) }
    var editMusicStartSec by remember(reel.id, reel.musicStartSec) { mutableIntStateOf(reel.musicStartSec) }
    var editMuteOriginalAudio by remember(reel.id, reel.muteOriginalAudio) { mutableStateOf(reel.muteOriginalAudio) }
    var showEditMusicPicker by remember { mutableStateOf(false) }

    LaunchedEffect(reel.id, isActivePage, currentUser.uid) {
        if (isActivePage && !reel.viewedBy.contains(currentUser.uid)) {
            onMarkViewed()
        }
    }

    val liveAuthor = usersById[reel.authorId]
    val effectiveIsBlackSociety = liveAuthor?.isBlackSociety ?: reel.authorIsBlackSociety
    val effectiveBlackSocietyNumber = liveAuthor?.blackSocietyNumber ?: reel.authorBlackSocietyNumber
    val effectiveIsClassRep = liveAuthor?.isClassRepresentative ?: reel.authorIsClassRepresentative
    val effectiveClassRepGrade = liveAuthor?.classRepresentativeGrade ?: reel.authorClassRepresentativeGrade
    val effectiveIsVerified = (liveAuthor?.isVerified ?: reel.authorIsVerified) ||
        effectiveIsClassRep ||
        (liveAuthor?.role == "admin" || reel.authorRole == "admin")
    val effectiveAuthorName = if (effectiveIsBlackSociety) {
        "Member - ${effectiveBlackSocietyNumber ?: 1}"
    } else {
        liveAuthor?.name?.takeIf { it.isNotBlank() } ?: reel.authorName
    }
    val effectiveAvatar = if (effectiveIsBlackSociety) null else (liveAuthor?.avatar ?: reel.authorAvatar)

    val isLiked = reel.likedBy.contains(currentUser.uid)
    val isFollowing = currentUser.following.contains(reel.authorId)
    val canEditOrDelete = isAdmin || reel.authorId == currentUser.uid

    val likeScale by animateFloatAsState(
        targetValue = if (isLiked) 1.24f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "reel_like_scale"
    )
    val bookmarkScale by animateFloatAsState(
        targetValue = if (isBookmarked) 1.16f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "reel_bookmark_scale"
    )

    LaunchedEffect(showHeartBurst) {
        if (showHeartBurst) {
            delay(650L)
            showHeartBurst = false
        }
    }

    LaunchedEffect(reel.videoUrl, isActivePage) {
        if (!isChunked && localFile == null && !isLoadingVideo && isActivePage) {
            isLoadingVideo = true
            videoError = null
            CampusMediaRepository.resolveVideoFile(context, reel.videoUrl)
                .onSuccess { localFile = it }
                .onFailure { videoError = it.localizedMessage ?: "Unable to load reel video" }
            isLoadingVideo = false
        }
    }

    val hasAttachedMusic = !reel.musicPreviewUrl.isNullOrBlank()
    val effectiveVideoVolume = if (isMuted || (reel.muteOriginalAudio && hasAttachedMusic)) 0f else 1f
    val shouldCreatePlayer = isActivePage && (isChunked || localFile != null)

    val exoPlayer = remember(reel.id, reel.videoUrl, shouldCreatePlayer, localFile) {
        if (!shouldCreatePlayer) return@remember null
        val fileToPlay = localFile
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(2_500, 15_000, 350, 800)
            .build()
        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build()
            .apply {
                if (fileToPlay != null) {
                    setMediaItem(MediaItem.fromUri(Uri.fromFile(fileToPlay)))
                } else if (isChunked) {
                    setMediaSource(
                        ProgressiveMediaSource.Factory(CampusChunkedVideoCache.Factory(context))
                            .createMediaSource(MediaItem.fromUri(reel.videoUrl))
                    )
                } else {
                    setMediaItem(MediaItem.fromUri(reel.videoUrl))
                }
                repeatMode = Player.REPEAT_MODE_ALL
                volume = effectiveVideoVolume
                prepare()
            }
    }
    var hasRenderedFirstFrame by remember(exoPlayer) { mutableStateOf(false) }

    val reelMusicUrl = reel.musicPreviewUrl?.takeIf { it.isNotBlank() }
    val musicPlayer = remember(reel.id, reelMusicUrl, isActivePage) {
        if (!isActivePage || reelMusicUrl == null) return@remember null
        val resolvedUri = MusicCacheManager.resolvePlayableUri(context, reelMusicUrl)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(2_000, 10_000, 250, 600)
            .build()
        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(resolvedUri))
                repeatMode = Player.REPEAT_MODE_ALL
                volume = if (isMuted) 0f else 1f
                if (reel.musicStartSec > 0) {
                    seekTo(reel.musicStartSec.coerceIn(0, 60) * 1000L)
                }
                prepare()
            }
    }

    LaunchedEffect(exoPlayer, musicPlayer, isActivePage, userPaused, showCommentsSheet, showShareSheet, showEditDialog, confirmDelete) {
        val shouldPlay = isActivePage && !userPaused && !showCommentsSheet && !showShareSheet && !showEditDialog && !confirmDelete
        exoPlayer?.let { player ->
            player.playWhenReady = shouldPlay
            if (shouldPlay) player.play() else player.pause()
        }
        musicPlayer?.let { mPlayer ->
            mPlayer.playWhenReady = shouldPlay
            if (shouldPlay) mPlayer.play() else mPlayer.pause()
        }
    }

    LaunchedEffect(exoPlayer, musicPlayer, isMuted, reel.muteOriginalAudio, hasAttachedMusic) {
        exoPlayer?.volume = if (isMuted || (reel.muteOriginalAudio && hasAttachedMusic)) 0f else 1f
        musicPlayer?.volume = if (isMuted) 0f else 1f
    }

    DisposableEffect(exoPlayer) {
        val player = exoPlayer
        if (player == null) {
            onDispose {}
        } else {
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    isBuffering = playbackState == Player.STATE_BUFFERING
                    if (playbackState == Player.STATE_READY) {
                        videoError = null
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    isBuffering = false
                    videoError = error.localizedMessage ?: "Unable to play reel"
                }

                override fun onRenderedFirstFrame() {
                    hasRenderedFirstFrame = true
                    isBuffering = false
                }
            }
            player.addListener(listener)
            onDispose {
                player.removeListener(listener)
                player.release()
            }
        }
    }

    DisposableEffect(musicPlayer) {
        val mPlayer = musicPlayer
        if (mPlayer == null) {
            onDispose {}
        } else {
            onDispose { mPlayer.release() }
        }
    }

    DisposableEffect(lifecycleOwner, exoPlayer, musicPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                exoPlayer?.pause()
                musicPlayer?.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val comments by remember(reel.id, showCommentsSheet) {
        if (showCommentsSheet) repository.observeReelComments(reel.id) else kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(reel.id, isLiked) {
                detectTapGestures(
                    onTap = { userPaused = !userPaused },
                    onDoubleTap = {
                        showHeartBurst = true
                        if (!isLiked) onToggleLike()
                    }
                )
            }
            .testTag("reel_page_${reel.id}")
    ) {
        val reelRatio = PostImageRatio.fromKey(reel.videoAspectRatio)
        val fitOriginalRatio = reelRatio == PostImageRatio.FREE
        val videoSurfaceModifier = if (reelRatio.ratio != null && reelRatio != PostImageRatio.PORTRAIT_9_16) {
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .aspectRatio(reelRatio.ratio)
                .clip(RoundedCornerShape(8.dp))
        } else {
            Modifier.fillMaxSize()
        }

        if (exoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    (LayoutInflater.from(ctx).inflate(
                        R.layout.campus_texture_player_view,
                        null,
                        false
                    ) as PlayerView).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = if (fitOriginalRatio) {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT
                        } else {
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        }
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    if (view.player !== exoPlayer) view.player = exoPlayer
                    val targetMode = if (fitOriginalRatio) {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                    } else {
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                    if (view.resizeMode != targetMode) {
                        view.resizeMode = targetMode
                    }
                },
                modifier = videoSurfaceModifier
            )
        }
        if (exoPlayer == null || !hasRenderedFirstFrame) {
            CampusImage(
                urlOrDataUri = reel.videoUrl,
                contentDescription = reel.caption.ifBlank { "Campus Reel" },
                contentScale = if (fitOriginalRatio) ContentScale.Fit else ContentScale.Crop,
                modifier = videoSurfaceModifier
            )
        }

        // Top and bottom legibility scrims
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))))
        )

        if (isLoadingVideo || (isBuffering && !hasRenderedFirstFrame)) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.5.dp,
                modifier = Modifier.align(Alignment.Center).size(36.dp)
            )
        } else if (videoError != null) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.72f),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(
                    text = videoError ?: "Unable to play reel",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        } else if (userPaused) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Paused",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = showHeartBurst,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(92.dp)
            )
        }

        // Top Bar Controls inside Reel Canvas
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f)
            ) {
                Text(
                    text = reel.category,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.48f))
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }

                if (onOpenCreateReel != null) {
                    Surface(
                        shape = CircleShape,
                        color = InstaBlue,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onOpenCreateReel)
                            .testTag("reels_create_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("Reel", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                topEndCloseButton?.invoke()
            }
        }

        // Bottom Overlay: Creator Info (left) + Vertical Instagram Action Rail (right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(start = 14.dp, end = 8.dp, bottom = 84.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Creator Metadata + Caption + Audio
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp, bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clickable(onClick = onOpenAuthorProfile),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UserAvatar(
                            src = effectiveAvatar,
                            name = effectiveAuthorName,
                            role = liveAuthor?.role ?: reel.authorRole,
                            isVerified = effectiveIsVerified,
                            isBlackSociety = effectiveIsBlackSociety,
                            blackSocietyNumber = effectiveBlackSocietyNumber,
                            isClassRepresentative = effectiveIsClassRep,
                            size = AvatarSize.MD
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = effectiveAuthorName,
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (effectiveIsVerified && !effectiveIsBlackSociety) {
                                VerifiedBadgeIcon(size = 14.dp)
                            }
                            if (effectiveIsClassRep) {
                                Surface(
                                    shape = CircleShape,
                                    color = Gold400,
                                    border = BorderStroke(0.5.dp, Gold300)
                                ) {
                                    Text(
                                        text = "C${effectiveClassRepGrade ?: 10}",
                                        color = Slate900,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            if (effectiveIsBlackSociety) {
                                Surface(
                                    shape = CircleShape,
                                    color = Zinc900,
                                    border = BorderStroke(0.5.dp, Zinc700)
                                ) {
                                    Text(
                                        text = "♠",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (reel.authorId != currentUser.uid) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isFollowing) Color.White.copy(alpha = 0.14f) else Color.Transparent,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.78f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onToggleFollow)
                        ) {
                            Text(
                                text = if (isFollowing) "Following" else "Follow",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (reel.caption.isNotBlank()) {
                    Text(
                        text = reel.caption,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val reelViewCount = reel.viewedBy.size
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.14f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MusicNote,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (!reel.musicTitle.isNullOrBlank()) {
                                    listOfNotNull(
                                        reel.musicTitle.takeIf { it.isNotBlank() },
                                        reel.musicArtist?.takeIf { it.isNotBlank() }
                                    ).joinToString(" · ")
                                } else {
                                    reel.audioTitle.ifBlank { "Original audio" }
                                },
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.14f),
                        modifier = Modifier.testTag("reel_views_${reel.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Visibility,
                                contentDescription = "Views",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (reelViewCount == 1) "1 view" else "$reelViewCount views",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Text(
                        text = formatRelativeTime(reel.createdAt),
                        color = Color.White.copy(alpha = 0.65f),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }

            // Right Vertical Action Rail
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onToggleLike,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("like_reel_${reel.id}")
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like reel",
                            tint = if (isLiked) InstaLikeRed else Color.White,
                            modifier = Modifier
                                .size(29.dp)
                                .scale(likeScale)
                        )
                    }
                    Text(
                        text = "${reel.likedBy.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { showCommentsSheet = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("comments_reel_${reel.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Reel comments",
                            tint = Color.White,
                            modifier = Modifier.size(27.dp)
                        )
                    }
                    Text(
                        text = "${reel.commentsCount}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(
                    onClick = { showShareSheet = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("share_reel_${reel.id}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "Share reel or add to story",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("bookmark_reel_${reel.id}")
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Save reel",
                        tint = Color.White,
                        modifier = Modifier
                            .size(26.dp)
                            .scale(bookmarkScale)
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Reel options",
                            tint = Color.White,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add to your story") },
                            leadingIcon = { Icon(Icons.Outlined.AddCircleOutline, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onAddToStory()
                            }
                        )
                        if (reel.authorId != currentUser.uid) {
                            DropdownMenuItem(
                                text = { Text("Message Creator") },
                                leadingIcon = { Icon(Icons.Outlined.Forum, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onMessageAuthor()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Report Reel") },
                            leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onReportReel()
                            }
                        )
                        if (canEditOrDelete) {
                            DropdownMenuItem(
                                text = { Text("Edit Reel") },
                                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    editCaption = reel.caption
                                    editCategory = reel.category
                                    editAudio = reel.audioTitle
                                    editMusicTrack = initialReelTrack
                                    editMusicStartSec = reel.musicStartSec
                                    editMuteOriginalAudio = reel.muteOriginalAudio
                                    showEditDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Reel", color = InstaLikeRed) },
                                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = InstaLikeRed) },
                                onClick = {
                                    menuExpanded = false
                                    confirmDelete = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showShareSheet) {
        ModalBottomSheet(
            onDismissRequest = { showShareSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Share Reel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = InstaBlue.copy(alpha = 0.12f),
                    border = BorderStroke(0.75.dp, InstaBlue.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            showShareSheet = false
                            onAddToStory()
                        }
                        .testTag("reel_share_sheet_add_to_story")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddCircleOutline,
                            contentDescription = null,
                            tint = InstaBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Add reel to your story",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = InstaBlue
                            )
                            Text(
                                text = "Repost ${effectiveAuthorName.substringBefore(" ")}'s reel for your followers",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (reel.authorId != currentUser.uid) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                showShareSheet = false
                                onMessageAuthor()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Outlined.Forum, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text(
                                text = "Send message to ${effectiveAuthorName.substringBefore(" ")}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            showShareSheet = false
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "$effectiveAuthorName posted a Reel on Squadify (${reel.category}):\n${reel.caption}"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Squadify Reel"))
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Share outside Squadify",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    if (showCommentsSheet) {
        var expandedReplyParentIds by remember(reel.id) { mutableStateOf(setOf<String>()) }
        val commentTree = remember(comments) {
            val byId = comments.associateBy { it.id }
            fun rootIdFor(c: ReelComment): String {
                var cur = c
                val seen = HashSet<String>()
                while (!cur.replyToCommentId.isNullOrBlank() && seen.add(cur.id)) {
                    val parent = byId[cur.replyToCommentId] ?: break
                    cur = parent
                }
                return cur.id
            }
            val topLevel = comments.filter {
                it.replyToCommentId.isNullOrBlank() || !byId.containsKey(it.replyToCommentId)
            }
            val repliesByRoot = comments.filter {
                !it.replyToCommentId.isNullOrBlank() && byId.containsKey(it.replyToCommentId)
            }.groupBy { rootIdFor(it) }
            Triple(topLevel, repliesByRoot, ::rootIdFor)
        }
        val topLevelComments = commentTree.first
        val repliesByParent = commentTree.second
        val resolveRootParentId = commentTree.third

        ModalBottomSheet(
            onDismissRequest = {
                showCommentsSheet = false
                replyingToComment = null
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Comments (${comments.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                if (topLevelComments.isEmpty()) {
                    Text(
                        text = "No comments yet. Be the first to comment!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(275.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(topLevelComments, key = { it.id }) { parentComment ->
                            val childReplies = repliesByParent[parentComment.id].orEmpty()
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                ReelCommentRow(
                                    comment = parentComment,
                                    currentUserId = currentUser.uid,
                                    liveUser = usersById[parentComment.authorId],
                                    isReply = false,
                                    canDelete = isAdmin || parentComment.authorId == currentUser.uid,
                                    onToggleLike = { onToggleCommentLike(parentComment) },
                                    onReply = { replyingToComment = parentComment },
                                    onDelete = { onDeleteComment(parentComment.id) },
                                    onClickAuthor = {
                                        showCommentsSheet = false
                                        onOpenAuthorProfile()
                                    }
                                )

                                if (childReplies.isNotEmpty()) {
                                    val isExpanded = expandedReplyParentIds.contains(parentComment.id)
                                    if (!isExpanded) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .padding(start = 40.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    expandedReplyParentIds = expandedReplyParentIds + parentComment.id
                                                }
                                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                                .testTag("view_reel_replies_${parentComment.id}")
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(24.dp)
                                                    .height(1.dp)
                                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (childReplies.size == 1) "View 1 reply" else "View ${childReplies.size} replies",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        Column(
                                            modifier = Modifier.padding(top = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            childReplies.forEach { reply ->
                                                ReelCommentRow(
                                                    comment = reply,
                                                    currentUserId = currentUser.uid,
                                                    liveUser = usersById[reply.authorId],
                                                    isReply = true,
                                                    canDelete = isAdmin || reply.authorId == currentUser.uid,
                                                    onToggleLike = { onToggleCommentLike(reply) },
                                                    onReply = { replyingToComment = reply },
                                                    onDelete = { onDeleteComment(reply.id) },
                                                    onClickAuthor = {
                                                        showCommentsSheet = false
                                                        onOpenAuthorProfile()
                                                    }
                                                )
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .padding(start = 40.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        expandedReplyParentIds = expandedReplyParentIds - parentComment.id
                                                    }
                                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                                                    .testTag("hide_reel_replies_${parentComment.id}")
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(24.dp)
                                                        .height(1.dp)
                                                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f))
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Hide replies",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                replyingToComment?.let { target ->
                    val targetUser = usersById[target.authorId]
                    val targetName = if (targetUser?.isBlackSociety ?: target.authorIsBlackSociety) {
                        "Member - ${(targetUser?.blackSocietyNumber ?: target.authorBlackSocietyNumber) ?: 1}"
                    } else {
                        targetUser?.name ?: target.authorName
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = InstaBlue.copy(alpha = 0.12f),
                        border = BorderStroke(0.5.dp, InstaBlue.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Replying to @$targetName",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = InstaBlue
                                )
                                Text(
                                    text = target.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { replyingToComment = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Cancel reply",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentDraft,
                        onValueChange = { commentDraft = it },
                        placeholder = {
                            Text(
                                text = if (replyingToComment != null) "Write a reply..." else "Add a comment...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            if (commentDraft.isNotBlank()) {
                                val target = replyingToComment
                                if (target != null) {
                                    expandedReplyParentIds = expandedReplyParentIds + resolveRootParentId(target)
                                }
                                onAddComment(commentDraft, target)
                                commentDraft = ""
                                replyingToComment = null
                            }
                        },
                        enabled = commentDraft.isNotBlank()
                    ) {
                        Text("Post", color = InstaBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            title = { Text("Edit Reel", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CampusConstants.POST_CATEGORIES.forEach { category ->
                            FilterChip(
                                selected = editCategory == category,
                                onClick = { editCategory = category },
                                label = { Text(category) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = editAudio,
                        onValueChange = { editAudio = it.take(120) },
                        label = { Text("Audio Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Reel Ratio",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PostImageRatio.entries.forEach { ratio ->
                            FilterChip(
                                selected = editVideoAspectRatio == ratio,
                                onClick = { editVideoAspectRatio = ratio },
                                label = { Text(ratio.label) }
                            )
                        }
                    }
                    val activeEditTrack = editMusicTrack
                    if (activeEditTrack == null) {
                        OutlinedButton(
                            onClick = { showEditMusicPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Song to Reel")
                        }
                    } else {
                        StoryMusicStickerPill(
                            title = activeEditTrack.title,
                            artist = buildString {
                                append(activeEditTrack.artist)
                                if (editMuteOriginalAudio) append(" · Video muted")
                            },
                            artworkUrl = activeEditTrack.artworkUrl,
                            isPlaying = false,
                            onClick = { showEditMusicPicker = true },
                            onRemove = {
                                editMusicTrack = null
                                editMusicStartSec = 0
                                editMuteOriginalAudio = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = editCaption,
                        onValueChange = { editCaption = it.take(2000) },
                        label = { Text("Caption") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showEditDialog = false
                    onUpdateReel(
                        editCaption,
                        editCategory,
                        editAudio,
                        editVideoAspectRatio.key,
                        editMusicTrack,
                        editMusicStartSec,
                        editMuteOriginalAudio
                    )
                }) {
                    Text("Save", color = InstaBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showEditMusicPicker) {
        StoryMusicPickerSheet(
            title = "Edit Reel Song",
            currentTrack = editMusicTrack,
            currentStartSec = editMusicStartSec,
            allowMuteVideoAudio = true,
            muteOriginalAudio = editMuteOriginalAudio,
            onApply = { track, startSec, muteOriginal ->
                editMusicTrack = track
                editMusicStartSec = startSec
                editMuteOriginalAudio = muteOriginal
            },
            onDismiss = { showEditMusicPicker = false }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            title = { Text("Delete Reel?") },
            text = { Text("This reel will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDeleteReel()
                }) {
                    Text("Delete", color = InstaLikeRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ReelCommentRow(
    comment: ReelComment,
    currentUserId: String,
    liveUser: CampusUser?,
    isReply: Boolean,
    canDelete: Boolean,
    onToggleLike: () -> Unit,
    onReply: () -> Unit,
    onDelete: () -> Unit,
    onClickAuthor: () -> Unit
) {
    val isBlackSociety = liveUser?.isBlackSociety ?: comment.authorIsBlackSociety
    val blackNumber = liveUser?.blackSocietyNumber ?: comment.authorBlackSocietyNumber
    val isClassRep = liveUser?.isClassRepresentative ?: comment.authorIsClassRepresentative
    val isVerified = (liveUser?.isVerified ?: comment.authorIsVerified) ||
        isClassRep ||
        (liveUser?.role == "admin" || comment.authorRole == "admin")
    val displayName = if (isBlackSociety) "Member - ${blackNumber ?: 1}" else (liveUser?.name ?: comment.authorName)
    val isLiked = comment.likedBy.contains(currentUserId)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isReply) 28.dp else 0.dp),
        verticalAlignment = Alignment.Top
    ) {
        UserAvatar(
            src = if (isBlackSociety) null else (liveUser?.avatar ?: comment.authorAvatar),
            name = displayName,
            role = liveUser?.role ?: comment.authorRole,
            isVerified = isVerified,
            isBlackSociety = isBlackSociety,
            blackSocietyNumber = blackNumber,
            isClassRepresentative = isClassRep,
            size = AvatarSize.SM,
            onClick = onClickAuthor
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                if (isVerified && !isBlackSociety) {
                    VerifiedBadgeIcon(size = 12.dp)
                }
                Text(
                    text = formatRelativeTime(comment.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!comment.replyToAuthorName.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = buildString {
                            append("Replying to @${comment.replyToAuthorName}")
                            comment.replyToContent?.takeIf { it.isNotBlank() }?.let {
                                append(": $it")
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = InstaBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(text = comment.content, style = MaterialTheme.typography.bodyMedium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = "Reply",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clickable(onClick = onReply)
                        .padding(vertical = 2.dp, horizontal = 2.dp)
                        .testTag("reply_reel_comment_${comment.id}")
                )
                if (comment.likedBy.isNotEmpty()) {
                    Text(
                        text = if (comment.likedBy.size == 1) "1 like" else "${comment.likedBy.size} likes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (canDelete) {
                IconButton(onClick = onDelete, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete comment", tint = InstaLikeRed, modifier = Modifier.size(15.dp))
                }
            }
            IconButton(
                onClick = onToggleLike,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("like_reel_comment_${comment.id}")
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isLiked) "Unlike comment" else "Like comment",
                    tint = if (isLiked) InstaLikeRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@kotlin.OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateReelDialog(
    onPublishReel: (
        videoUri: Uri,
        caption: String,
        category: String,
        audioTitle: String,
        videoAspectRatio: PostImageRatio,
        musicTrack: StoryMusicTrack?,
        musicStartSec: Int,
        muteOriginalAudio: Boolean,
        onDone: () -> Unit
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var videoAspectRatio by remember { mutableStateOf(PostImageRatio.FREE) }
    var caption by remember { mutableStateOf("") }
    var audioTitle by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }
    var selectedMusicTrack by remember { mutableStateOf<StoryMusicTrack?>(null) }
    var selectedMusicStartSec by remember { mutableIntStateOf(0) }
    var muteOriginalAudio by remember { mutableStateOf(false) }
    var showMusicPicker by remember { mutableStateOf(false) }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            videoUri = uri
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 560.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("New Squadify Reel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                OutlinedButton(
                    onClick = {
                        videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("dialog_pick_reel_video")
                ) {
                    Icon(Icons.Outlined.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (videoUri == null) "Select Reel Video" else "Video Selected • Change", fontWeight = FontWeight.SemiBold)
                }

                videoUri?.let { pickedUri ->
                    val naturalRatio by produceState(initialValue = 9f / 16f, key1 = pickedUri) {
                        value = StoryMediaEditor.readVideoAspectRatio(context, pickedUri)
                    }
                    val thumbBitmap by produceState<Bitmap?>(initialValue = null, key1 = pickedUri) {
                        value = StoryMediaEditor.extractVideoThumbnailBitmap(context, pickedUri)
                    }
                    val previewRatio = (videoAspectRatio.ratio ?: naturalRatio).coerceIn(0.45f, 1.91f)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .height(86.dp)
                                .aspectRatio(previewRatio)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF121216)),
                            contentAlignment = Alignment.Center
                        ) {
                            val bmp = thumbBitmap
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Reel preview",
                                    contentScale = if (videoAspectRatio == PostImageRatio.FREE) ContentScale.Fit else ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Reel Ratio",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PostImageRatio.entries.forEach { ratio ->
                                    FilterChip(
                                        selected = videoAspectRatio == ratio,
                                        onClick = { videoAspectRatio = ratio },
                                        label = { Text(ratio.label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = InstaBlue.copy(alpha = 0.16f),
                                            selectedLabelColor = InstaBlue
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it.take(1000) },
                    label = { Text("Reel caption") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = audioTitle,
                    onValueChange = { audioTitle = it.take(120) },
                    label = { Text("Audio name (optional)") },
                    placeholder = { Text("Original audio") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                val attachedMusic = selectedMusicTrack
                if (attachedMusic == null) {
                    OutlinedButton(
                        onClick = { showMusicPicker = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("dialog_reel_add_music")
                    ) {
                        Icon(Icons.Filled.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Song to Reel", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    StoryMusicStickerPill(
                        title = attachedMusic.title,
                        artist = buildString {
                            append(attachedMusic.artist)
                            if (muteOriginalAudio) append(" · Video muted")
                        },
                        artworkUrl = attachedMusic.artworkUrl,
                        isPlaying = false,
                        onClick = { showMusicPicker = true },
                        onRemove = {
                            selectedMusicTrack = null
                            selectedMusicStartSec = 0
                            muteOriginalAudio = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CampusConstants.POST_CATEGORIES.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                Button(
                    onClick = {
                        val readyUri = videoUri ?: return@Button
                        onPublishReel(
                            readyUri,
                            caption.trim(),
                            category,
                            audioTitle.trim().ifBlank { "Original audio" },
                            videoAspectRatio,
                            selectedMusicTrack,
                            selectedMusicStartSec,
                            muteOriginalAudio
                        ) {}
                    },
                    enabled = videoUri != null,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = InstaBlue, contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("dialog_publish_reel_button")
                ) {
                    Text("Publish Reel", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showMusicPicker) {
        StoryMusicPickerSheet(
            title = "Add Music to Reel",
            currentTrack = selectedMusicTrack,
            currentStartSec = selectedMusicStartSec,
            allowMuteVideoAudio = true,
            muteOriginalAudio = muteOriginalAudio,
            onApply = { track, startSec, muteOrig ->
                selectedMusicTrack = track
                selectedMusicStartSec = startSec
                muteOriginalAudio = muteOrig
            },
            onDismiss = { showMusicPicker = false }
        )
    }
}
