package com.bsdcampus.app.ui.components

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.bsdcampus.app.data.CampusRepository
import com.bsdcampus.app.data.CampusMediaRepository
import com.bsdcampus.app.data.MediaUtils
import com.bsdcampus.app.data.MusicCacheManager
import com.bsdcampus.app.data.StoryMusicTrack
import com.bsdcampus.app.model.CampusComment
import com.bsdcampus.app.model.CampusConstants
import com.bsdcampus.app.model.CampusPost
import com.bsdcampus.app.model.CampusUser
import com.bsdcampus.app.model.PostImageRatio
import com.bsdcampus.app.ui.theme.Amber500
import com.bsdcampus.app.ui.theme.Gold300
import com.bsdcampus.app.ui.theme.Gold400
import com.bsdcampus.app.ui.theme.InstaBlue
import com.bsdcampus.app.ui.theme.InstaLikeRed
import com.bsdcampus.app.ui.theme.JetBrainsMonoFamily
import com.bsdcampus.app.ui.theme.Slate900
import com.bsdcampus.app.ui.theme.Zinc700
import com.bsdcampus.app.ui.theme.Zinc900
import com.google.firebase.Timestamp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatRelativeTime(ts: Timestamp?): String {
    val millis = ts?.toDate()?.time ?: return "just now"
    val diffSec = ((System.currentTimeMillis() - millis) / 1000L).coerceAtLeast(0L)
    return when {
        diffSec < 60 -> "just now"
        diffSec < 3600 -> "${diffSec / 60}m"
        diffSec < 86400 -> "${diffSec / 3600}h"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(millis))
    }
}

private object ActivePostAudioCoordinator {
    private val _activePostId = MutableStateFlow<String?>(null)
    val activePostId = _activePostId

    fun requestPlay(postId: String) {
        _activePostId.value = postId
    }

    fun clearIfMatching(postId: String) {
        if (_activePostId.value == postId) {
            _activePostId.value = null
        }
    }
}

fun CampusPost.hasPlayableAutoContent(): Boolean {
    return !musicPreviewUrl.isNullOrBlank() || images.any { MediaUtils.isVideoMediaUri(it) }
}

fun computeFocusedPostId(
    layoutInfo: LazyListLayoutInfo,
    candidatePostIds: Set<String>
): String? {
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty() || candidatePostIds.isEmpty()) return null
    val viewportStart = layoutInfo.viewportStartOffset
    val viewportEnd = layoutInfo.viewportEndOffset
    val viewportHeight = (viewportEnd - viewportStart).coerceAtLeast(1)
    val viewportCenter = (viewportStart + viewportEnd) / 2f

    var bestId: String? = null
    var bestScore = Float.NEGATIVE_INFINITY

    for (item in visibleItems) {
        val key = item.key as? String ?: continue
        if (!candidatePostIds.contains(key)) continue
        val itemStart = item.offset
        val itemEnd = item.offset + item.size
        val visiblePx = (minOf(itemEnd, viewportEnd) - maxOf(itemStart, viewportStart)).coerceAtLeast(0)
        if (visiblePx <= 0) continue

        val visibleFractionOfItem = visiblePx.toFloat() / item.size.coerceAtLeast(1)
        val visibleFractionOfViewport = visiblePx.toFloat() / viewportHeight
        if (visibleFractionOfItem < 0.35f && visibleFractionOfViewport < 0.40f) continue

        val itemCenter = (maxOf(itemStart, viewportStart) + minOf(itemEnd, viewportEnd)) / 2f
        val normalizedDist = kotlin.math.abs(itemCenter - viewportCenter) / (viewportHeight / 2f)
        val score = visibleFractionOfViewport * 2f + visibleFractionOfItem - normalizedDist * 0.6f
        if (score > bestScore) {
            bestScore = score
            bestId = key
        }
    }
    return bestId
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostCard(
    post: CampusPost,
    currentUser: CampusUser,
    isAdmin: Boolean,
    repository: CampusRepository,
    hideLikeCounts: Boolean,
    isBookmarked: Boolean,
    usersById: Map<String, CampusUser> = emptyMap(),
    onToggleLike: () -> Unit,
    onToggleBookmark: () -> Unit,
    onAddComment: (String, CampusComment?) -> Unit,
    onToggleCommentLike: (CampusComment) -> Unit = {},
    onDeleteComment: (String) -> Unit,
    onMarkViewed: () -> Unit = {},
    onTogglePin: () -> Unit,
    onDeletePost: () -> Unit,
    onOpenAuthorProfile: (String) -> Unit,
    onMessageAuthor: (String) -> Unit,
    onReportPost: () -> Unit,
    onUpdatePost: ((String, String, String, StoryMusicTrack?, Int) -> Unit)? = null,
    onAddToStory: (() -> Unit)? = null,
    compactDiscussion: Boolean = false,
    isAutoPlayActive: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showComments by remember { mutableStateOf(false) }
    var commentDraft by remember { mutableStateOf("") }
    var replyingToComment by remember(post.id) { mutableStateOf<CampusComment?>(null) }
    var expandedReplyParentIds by remember(post.id) { mutableStateOf(setOf<String>()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var editContent by remember(post.id, post.content) { mutableStateOf(post.content) }
    var editCategory by remember(post.id, post.category) { mutableStateOf(post.category) }
    var editAspectRatio by remember(post.id, post.imageAspectRatio) { mutableStateOf(PostImageRatio.fromKey(post.imageAspectRatio)) }
    val initialTrack = remember(post.id, post.musicPreviewUrl, post.musicTitle, post.musicArtist, post.musicArtworkUrl) {
        val preview = post.musicPreviewUrl?.takeIf { it.isNotBlank() }
        val title = post.musicTitle?.takeIf { it.isNotBlank() }
        if (preview != null && title != null) {
            StoryMusicTrack(
                title = title,
                artist = post.musicArtist.orEmpty().ifBlank { "Artist" },
                artworkUrl = post.musicArtworkUrl.orEmpty(),
                previewUrl = preview
            )
        } else null
    }
    var editMusicTrack by remember(post.id, initialTrack) { mutableStateOf(initialTrack) }
    var editMusicStartSec by remember(post.id, post.musicStartSec) { mutableIntStateOf(post.musicStartSec) }
    var showEditMusicPicker by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showHeartBurst by remember { mutableStateOf(false) }
    var zoomPageIndex by remember { mutableStateOf<Int?>(null) }

    val postMusicUrl = post.musicPreviewUrl?.takeIf { it.isNotBlank() }
    val activeAudioPostId by ActivePostAudioCoordinator.activePostId.collectAsStateWithLifecycle()
    var musicPlayer by remember(post.id, postMusicUrl) { mutableStateOf<ExoPlayer?>(null) }
    var isSongMuted by remember(post.id, postMusicUrl) { mutableStateOf(false) }

    LaunchedEffect(isAutoPlayActive, post.id, postMusicUrl) {
        val url = postMusicUrl ?: return@LaunchedEffect
        if (isAutoPlayActive) {
            ActivePostAudioCoordinator.requestPlay(post.id)
            val player = musicPlayer ?: ExoPlayer.Builder(context).build().apply {
                val resolvedUri = MusicCacheManager.resolvePlayableUri(context, url)
                setMediaItem(MediaItem.fromUri(resolvedUri))
                repeatMode = Player.REPEAT_MODE_ALL
                volume = if (isSongMuted) 0f else 1f
                if (post.musicStartSec > 0) {
                    seekTo(post.musicStartSec.coerceIn(0, 60) * 1000L)
                }
                prepare()
            }.also { musicPlayer = it }
            player.volume = if (isSongMuted) 0f else 1f
            player.playWhenReady = true
            player.play()
        } else {
            musicPlayer?.pause()
            ActivePostAudioCoordinator.clearIfMatching(post.id)
        }
    }

    LaunchedEffect(musicPlayer, isSongMuted) {
        musicPlayer?.volume = if (isSongMuted) 0f else 1f
    }

    LaunchedEffect(activeAudioPostId, post.id) {
        if (activeAudioPostId != post.id && musicPlayer?.isPlaying == true) {
            musicPlayer?.pause()
        }
    }

    DisposableEffect(lifecycleOwner, post.id, postMusicUrl) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                musicPlayer?.pause()
                ActivePostAudioCoordinator.clearIfMatching(post.id)
            } else if (event == Lifecycle.Event.ON_RESUME && isAutoPlayActive && postMusicUrl != null) {
                ActivePostAudioCoordinator.requestPlay(post.id)
                musicPlayer?.let {
                    it.volume = if (isSongMuted) 0f else 1f
                    it.play()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            ActivePostAudioCoordinator.clearIfMatching(post.id)
            musicPlayer?.release()
            musicPlayer = null
        }
    }

    LaunchedEffect(post.id, currentUser.uid) {
        if (!post.viewedBy.contains(currentUser.uid)) {
            onMarkViewed()
        }
    }

    val liveAuthor = usersById[post.authorId]
    val effectiveIsBlackSociety = liveAuthor?.isBlackSociety ?: post.authorIsBlackSociety
    val effectiveBlackSocietyNumber = liveAuthor?.blackSocietyNumber ?: post.authorBlackSocietyNumber
    val effectiveIsClassRep = liveAuthor?.isClassRepresentative ?: post.authorIsClassRepresentative
    val effectiveClassRepGrade = liveAuthor?.classRepresentativeGrade ?: post.authorClassRepresentativeGrade
    val effectiveIsVerified = (liveAuthor?.isVerified ?: post.authorIsVerified) ||
        effectiveIsClassRep ||
        (liveAuthor?.role == "admin" || post.authorRole == "admin")
    val effectiveAuthorRole = liveAuthor?.role ?: post.authorRole
    val effectiveAuthorName = if (effectiveIsBlackSociety) {
        "Member - ${effectiveBlackSocietyNumber ?: 1}"
    } else {
        liveAuthor?.name?.takeIf { it.isNotBlank() } ?: post.authorName
    }
    val effectiveAvatar = if (effectiveIsBlackSociety) null else (liveAuthor?.avatar ?: post.authorAvatar)
    val effectiveGrade = liveAuthor?.grade ?: post.authorGrade

    val isLiked = post.likedBy.contains(currentUser.uid)
    val canDelete = isAdmin || (post.authorId == currentUser.uid && !post.isProtectedAnnouncement)

    val likeScale by animateFloatAsState(
        targetValue = if (isLiked) 1.22f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "like_scale"
    )
    val bookmarkScale by animateFloatAsState(
        targetValue = if (isBookmarked) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "bookmark_scale"
    )

    LaunchedEffect(showHeartBurst) {
        if (showHeartBurst) {
            delay(650L)
            showHeartBurst = false
        }
    }

    val comments by remember(post.id, showComments) {
        if (showComments) repository.observeComments(post.id) else kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .animateContentSize(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow))
            .testTag("post_card_${post.id}")
    ) {
        // Optional subtle Pinned / Official ribbon above header
        if (post.isPinned || post.isOfficial) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (post.isPinned) {
                    Surface(shape = CircleShape, color = InstaBlue.copy(alpha = 0.14f)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.PushPin, contentDescription = null, tint = InstaBlue, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pinned", style = MaterialTheme.typography.labelSmall, color = InstaBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (post.isOfficial) {
                    Surface(shape = CircleShape, color = Amber500.copy(alpha = 0.14f)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Verified, contentDescription = null, tint = Amber500, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Official Notice", style = MaterialTheme.typography.labelSmall, color = Amber500, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Instagram Header Bar: Avatar + Name + Blue Tick + Grade/Category + Overflow Menu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenAuthorProfile(post.authorId) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(
                    src = effectiveAvatar,
                    name = effectiveAuthorName,
                    role = effectiveAuthorRole,
                    isVerified = effectiveIsVerified,
                    isBlackSociety = effectiveIsBlackSociety,
                    blackSocietyNumber = effectiveBlackSocietyNumber,
                    isClassRepresentative = effectiveIsClassRep,
                    showStoryRing = post.isOfficial || post.images.isNotEmpty(),
                    size = AvatarSize.MD
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = effectiveAuthorName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (effectiveIsVerified && !effectiveIsBlackSociety) {
                            VerifiedBadgeIcon(size = 15.dp)
                        }
                        if (effectiveIsClassRep) {
                            Surface(
                                shape = CircleShape,
                                color = Gold400,
                                border = BorderStroke(0.5.dp, Gold300)
                            ) {
                                Text(
                                    text = "C${effectiveClassRepGrade ?: 10} Rep",
                                    color = Slate900,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
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
                                    fontFamily = JetBrainsMonoFamily,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = buildString {
                            append(
                                effectiveGrade?.takeIf { it.isNotBlank() }
                                    ?: when (effectiveAuthorRole) {
                                        "admin" -> "Administration"
                                        "teacher" -> "Faculty"
                                        else -> "Student"
                                    }
                            )
                            append(" · ")
                            append(post.category)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Post options",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    if (onAddToStory != null) {
                        DropdownMenuItem(
                            text = { Text("Add to your story") },
                            leadingIcon = { Icon(Icons.Outlined.AddCircleOutline, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onAddToStory()
                            }
                        )
                    }
                    if (post.authorId != currentUser.uid) {
                        DropdownMenuItem(
                            text = { Text("Message Author") },
                            leadingIcon = { Icon(Icons.Outlined.Forum, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onMessageAuthor(post.authorId)
                            }
                        )
                    }
                    if (isAdmin) {
                        DropdownMenuItem(
                            text = { Text(if (post.isPinned) "Unpin Post" else "Pin to Top") },
                            leadingIcon = { Icon(Icons.Outlined.PushPin, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onTogglePin()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Report Post") },
                        leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onReportPost()
                        }
                    )
                    if (canDelete && onUpdatePost != null) {
                        DropdownMenuItem(
                            text = { Text("Edit Post") },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                editContent = post.content
                                editCategory = post.category
                                editAspectRatio = PostImageRatio.fromKey(post.imageAspectRatio)
                                editMusicTrack = initialTrack
                                editMusicStartSec = post.musicStartSec
                                showEditDialog = true
                            }
                        )
                    }
                    if (canDelete) {
                        DropdownMenuItem(
                            text = { Text("Delete Post", color = InstaLikeRed) },
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

        // If post has no media (text-only campus update or discussion), show crisp typographic card block
        if (post.images.isEmpty() || compactDiscussion) {
            if (post.content.isNotBlank()) {
                Text(
                    text = post.content,
                    style = if (compactDiscussion) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        // Full-bleed edge-to-edge Instagram media carousel (images + videos)
        if (post.images.isNotEmpty() && !compactDiscussion) {
            val pagerState = rememberPagerState(pageCount = { post.images.size })
            val parsedRatio = PostImageRatio.fromKey(post.imageAspectRatio)
            val isFreeRatio = parsedRatio == PostImageRatio.FREE
            var naturalAspectRatio by remember(post.id, post.images.firstOrNull()) {
                mutableStateOf<Float?>(null)
            }
            LaunchedEffect(post.id, isFreeRatio, post.images.firstOrNull()) {
                if (isFreeRatio) {
                    val resolved = CampusMediaRepository.resolveNaturalAspectRatio(context, post.images.firstOrNull())
                    if (resolved != null) {
                        naturalAspectRatio = resolved
                    }
                }
            }
            val mediaAspectRatio = (parsedRatio.ratio ?: naturalAspectRatio ?: 1f).coerceIn(0.5625f, 1.91f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(mediaAspectRatio)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val mediaItem = post.images[page]
                    if (MediaUtils.isVideoMediaUri(mediaItem)) {
                        CampusVideoPlayer(
                            videoUri = mediaItem,
                            modifier = Modifier.fillMaxSize(),
                            autoPlay = isAutoPlayActive && pagerState.currentPage == page,
                            fitVideoToBounds = isFreeRatio,
                            onVideoSizeChanged = { detectedRatio ->
                                if (isFreeRatio && page == 0) {
                                    naturalAspectRatio = detectedRatio
                                }
                            }
                        )
                    } else {
                        CampusImage(
                            urlOrDataUri = mediaItem,
                            contentDescription = "Post media ${page + 1}",
                            contentScale = if (isFreeRatio) ContentScale.Fit else ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(post.id, page, isLiked) {
                                    detectTapGestures(
                                        onTap = { zoomPageIndex = page },
                                        onDoubleTap = {
                                            showHeartBurst = true
                                            if (!isLiked) onToggleLike()
                                        }
                                    )
                                }
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = showHeartBurst,
                    enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(84.dp)
                    )
                }

                val currentPageIsVideo = post.images.getOrNull(pagerState.currentPage)
                    ?.let { MediaUtils.isVideoMediaUri(it) } == true
                if (postMusicUrl != null && !currentPageIsVideo) {
                    IconButton(
                        onClick = { isSongMuted = !isSongMuted },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .testTag("post_music_mute_${post.id}")
                    ) {
                        Icon(
                            imageVector = if (isSongMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isSongMuted) "Unmute song" else "Mute song",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                if (post.images.size > 1) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1}/${post.images.size}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (post.images.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dotCount = minOf(post.images.size, 8)
                    val activeDot = if (post.images.size <= 8) pagerState.currentPage else {
                        ((pagerState.currentPage.toFloat() / post.images.lastIndex) * 7).toInt()
                    }
                    repeat(dotCount) { idx ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.5.dp)
                                .size(if (activeDot == idx) 6.5.dp else 5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (activeDot == idx) InstaBlue
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                )
                        )
                    }
                }
            }
        }

        // Instagram Action Bar: Like, Comment, Share/DM left; Bookmark right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleLike,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("like_post_${post.id}")
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) InstaLikeRed else MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .size(25.dp)
                            .scale(likeScale)
                    )
                }

                IconButton(
                    onClick = { showComments = !showComments },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("comments_post_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comments",
                        tint = if (showComments) InstaBlue else MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(23.dp)
                    )
                }

                IconButton(
                    onClick = {
                        if (onAddToStory != null) {
                            showShareSheet = true
                        } else {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "$effectiveAuthorName on Squadify (${post.category}):\n${post.content}"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Squadify Post"))
                        }
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("share_post_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "Share or Add to Story",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (post.authorId != currentUser.uid) {
                    IconButton(
                        onClick = { onMessageAuthor(post.authorId) },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Forum,
                            contentDescription = "Direct Message Author",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (postMusicUrl != null && (post.images.isEmpty() || compactDiscussion)) {
                    IconButton(
                        onClick = { isSongMuted = !isSongMuted },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("post_music_mute_${post.id}")
                    ) {
                        Icon(
                            imageVector = if (isSongMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isSongMuted) "Unmute song" else "Mute song",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("bookmark_post_${post.id}")
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Save Post",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .size(24.dp)
                            .scale(bookmarkScale)
                    )
                }
            }
        }

        // Likes count, separated author header + caption body, comments teaser & timestamp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!hideLikeCounts && post.likedBy.isNotEmpty()) {
                Text(
                    text = if (post.likedBy.size == 1) "1 like" else "${post.likedBy.size} likes",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (post.images.isNotEmpty() && !compactDiscussion && post.content.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clickable { onOpenAuthorProfile(post.authorId) }
                    ) {
                        Text(
                            text = effectiveAuthorName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (effectiveIsVerified && !effectiveIsBlackSociety) {
                            VerifiedBadgeIcon(size = 13.dp)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = post.content,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                        )
                    }
                }
            }

            if (post.commentsCount > 0) {
                Text(
                    text = if (showComments) "Hide comments" else "View all ${post.commentsCount} comments",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clickable { showComments = !showComments }
                        .padding(vertical = 2.dp)
                )
            }

            val viewCount = post.viewedBy.size
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.testTag("post_views_${post.id}")
            ) {
                Text(
                    text = formatRelativeTime(post.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "·",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.Outlined.Visibility,
                    contentDescription = "Views",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = if (viewCount == 1) "1 view" else "$viewCount views",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        AnimatedVisibility(visible = showComments) {
            val commentTree = remember(comments) {
                val byId = comments.associateBy { it.id }
                fun rootIdFor(c: CampusComment): String {
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (topLevelComments.isEmpty()) {
                    Text(
                        text = "No comments yet. Start the conversation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    topLevelComments.forEach { parentComment ->
                        val childReplies = repliesByParent[parentComment.id].orEmpty()
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            CommentRow(
                                comment = parentComment,
                                currentUserId = currentUser.uid,
                                liveUser = usersById[parentComment.authorId],
                                isReply = false,
                                canDelete = isAdmin || parentComment.authorId == currentUser.uid,
                                onToggleLike = { onToggleCommentLike(parentComment) },
                                onReply = { replyingToComment = parentComment },
                                onDelete = { onDeleteComment(parentComment.id) },
                                onClickAuthor = { onOpenAuthorProfile(parentComment.authorId) }
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
                                            .testTag("view_replies_${parentComment.id}")
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
                                            CommentRow(
                                                comment = reply,
                                                currentUserId = currentUser.uid,
                                                liveUser = usersById[reply.authorId],
                                                isReply = true,
                                                canDelete = isAdmin || reply.authorId == currentUser.uid,
                                                onToggleLike = { onToggleCommentLike(reply) },
                                                onReply = { replyingToComment = reply },
                                                onDelete = { onDeleteComment(reply.id) },
                                                onClickAuthor = { onOpenAuthorProfile(reply.authorId) }
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
                                                .testTag("hide_replies_${parentComment.id}")
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

        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            title = { Text("Delete Post?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDeletePost()
                }) {
                    Text("Delete", color = InstaLikeRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }

    if (showEditDialog && onUpdatePost != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            title = { Text("Edit Post", fontWeight = FontWeight.Bold) },
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
                    if (post.images.isNotEmpty()) {
                        Text(
                            text = "Media Ratio",
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
                                    selected = editAspectRatio == ratio,
                                    onClick = { editAspectRatio = ratio },
                                    label = { Text(ratio.label) }
                                )
                            }
                        }
                    }
                    val currentEditTrack = editMusicTrack
                    if (currentEditTrack == null) {
                        OutlinedButton(
                            onClick = { showEditMusicPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Song")
                        }
                    } else {
                        StoryMusicStickerPill(
                            title = currentEditTrack.title,
                            artist = currentEditTrack.artist,
                            artworkUrl = currentEditTrack.artworkUrl,
                            isPlaying = false,
                            onClick = { showEditMusicPicker = true },
                            onRemove = {
                                editMusicTrack = null
                                editMusicStartSec = 0
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = editContent,
                        onValueChange = { editContent = it },
                        label = { Text("Caption") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEditDialog = false
                        onUpdatePost(editContent, editCategory, editAspectRatio.key, editMusicTrack, editMusicStartSec)
                    },
                    enabled = editContent.isNotBlank() || post.images.isNotEmpty()
                ) {
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
            title = "Edit Post Song",
            currentTrack = editMusicTrack,
            currentStartSec = editMusicStartSec,
            allowMuteVideoAudio = false,
            muteOriginalAudio = false,
            onApply = { track, startSec, _ ->
                editMusicTrack = track
                editMusicStartSec = startSec
            },
            onDismiss = { showEditMusicPicker = false }
        )
    }

    zoomPageIndex?.let { initialPage ->
        ZoomableImageLightbox(
            mediaUrls = post.images,
            initialPage = initialPage,
            authorName = effectiveAuthorName,
            onDismiss = { zoomPageIndex = null }
        )
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
                    text = "Share Post",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                if (onAddToStory != null) {
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
                            .testTag("share_sheet_add_to_story")
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
                                    text = "Add post to your story",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = InstaBlue
                                )
                                Text(
                                    text = "Share ${effectiveAuthorName.substringBefore(" ")}'s post with your followers for 24h",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                if (post.authorId != currentUser.uid) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                showShareSheet = false
                                onMessageAuthor(post.authorId)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Forum,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(20.dp)
                            )
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
                                    "$effectiveAuthorName on Squadify (${post.category}):\n${post.content}"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Squadify Post"))
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(20.dp)
                        )
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
}

@Composable
private fun CommentRow(
    comment: CampusComment,
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
                        .testTag("reply_comment_${comment.id}")
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
                    .testTag("like_comment_${comment.id}")
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
