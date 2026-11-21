package com.bsdcampus.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.bsdcampus.app.data.CampusRepository
import com.bsdcampus.app.data.MediaUtils
import com.bsdcampus.app.data.MusicCacheManager
import com.bsdcampus.app.data.StoryMusicTrack
import com.bsdcampus.app.model.CampusConstants
import com.bsdcampus.app.model.CampusPost
import com.bsdcampus.app.model.CampusUser
import com.bsdcampus.app.model.FeedLayoutMode
import com.bsdcampus.app.model.StoryGroup
import com.bsdcampus.app.model.UiState
import com.bsdcampus.app.model.UserPrivateInfo
import com.bsdcampus.app.notifications.MediaUploadKind
import com.bsdcampus.app.notifications.MediaUploadState
import com.bsdcampus.app.notifications.MediaUploadTracker
import com.bsdcampus.app.notifications.StoryUploadTracker
import com.bsdcampus.app.ui.components.AvatarSize
import com.bsdcampus.app.ui.components.CampusImage
import com.bsdcampus.app.ui.components.PostCard
import com.bsdcampus.app.ui.components.UserAvatar
import com.bsdcampus.app.ui.components.computeFocusedPostId
import com.bsdcampus.app.ui.components.hasPlayableAutoContent
import com.bsdcampus.app.ui.theme.Amber500
import com.bsdcampus.app.ui.theme.Emerald500
import com.bsdcampus.app.ui.theme.InstaBlue
import com.bsdcampus.app.ui.theme.InstaLikeRed
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    currentUser: CampusUser,
    isAdmin: Boolean,
    privateInfo: UserPrivateInfo?,
    postsState: UiState<List<CampusPost>>,
    usersById: Map<String, CampusUser> = emptyMap(),
    repository: CampusRepository,
    onToggleLike: (CampusPost) -> Unit,
    onToggleBookmark: (String) -> Unit,
    onAddComment: (CampusPost, String, com.bsdcampus.app.model.CampusComment?) -> Unit,
    onToggleCommentLike: (CampusPost, com.bsdcampus.app.model.CampusComment) -> Unit = { _, _ -> },
    onDeleteComment: (String, String) -> Unit,
    onMarkPostViewed: (CampusPost) -> Unit = {},
    onTogglePin: (CampusPost) -> Unit,
    onDeletePost: (CampusPost) -> Unit,
    onOpenAuthorProfile: (String) -> Unit,
    onMessageAuthor: (String) -> Unit,
    onReportPost: (CampusPost) -> Unit,
    onUpdatePost: (CampusPost, String, String, String, StoryMusicTrack?, Int) -> Unit,
    onAddPostToStory: (CampusPost) -> Unit = {},
    storyGroups: List<StoryGroup> = emptyList(),
    onOpenAddStory: () -> Unit,
    onOpenStory: (String) -> Unit,
    onNavigateCreate: () -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    isScreenActive: Boolean = true
) {
    val storyUploadState by StoryUploadTracker.state.collectAsStateWithLifecycle()
    val mediaUploadState by MediaUploadTracker.state.collectAsStateWithLifecycle()
    val postUploadState = mediaUploadState?.takeIf { it.kind == MediaUploadKind.POST }
    LaunchedEffect(storyUploadState?.isCompleted, storyUploadState?.uploadId) {
        if (storyUploadState?.isCompleted == true) {
            delay(3200L)
            StoryUploadTracker.clear()
        }
    }
    LaunchedEffect(postUploadState?.isCompleted, postUploadState?.uploadId) {
        if (postUploadState?.isCompleted == true) {
            delay(3200L)
            MediaUploadTracker.clear()
        }
    }
    var selectedCategory by remember { mutableStateOf("All") }
    var officialOnly by remember { mutableStateOf(false) }
    var selectedGridPost by remember { mutableStateOf<CampusPost?>(null) }

    val layoutMode = FeedLayoutMode.fromKey(privateInfo?.feedLayoutMode)
    val savedIds = privateInfo?.savedPostIds.orEmpty()
    val archivedIds = privateInfo?.archivedPostIds.orEmpty()
    val hideLikeCounts = privateInfo?.hideLikeCounts == true

    val allCategories = remember { listOf("All") + CampusConstants.POST_CATEGORIES }

    val ownStoryGroup = remember(storyGroups) { storyGroups.firstOrNull { it.isOwn } }
    val followedStoryGroups = remember(storyGroups) { storyGroups.filter { !it.isOwn } }
    val context = LocalContext.current

    LaunchedEffect(storyGroups) {
        val storySongUrls = storyGroups.flatMap { grp ->
            grp.stories.mapNotNull { it.musicPreviewUrl?.takeIf { u -> u.isNotBlank() } }
        }
        MusicCacheManager.prefetchUrls(context, storySongUrls)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .testTag("feed_pull_to_refresh")
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                when (postsState) {
                    is UiState.Loading -> {
                        FeedStoriesAndFilterHeader(
                            currentUser = currentUser,
                            ownStoryGroup = ownStoryGroup,
                            followedStoryGroups = followedStoryGroups,
                            storyUploadState = storyUploadState,
                            postUploadState = postUploadState,
                            allCategories = allCategories,
                            selectedCategory = selectedCategory,
                            officialOnly = officialOnly,
                            onOpenAddStory = onOpenAddStory,
                            onOpenStory = onOpenStory,
                            onSelectCategory = { selectedCategory = it },
                            onToggleOfficialOnly = { officialOnly = !officialOnly }
                        )
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is UiState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            FeedStoriesAndFilterHeader(
                                currentUser = currentUser,
                                ownStoryGroup = ownStoryGroup,
                                followedStoryGroups = followedStoryGroups,
                                storyUploadState = storyUploadState,
                                postUploadState = postUploadState,
                                allCategories = allCategories,
                                selectedCategory = selectedCategory,
                                officialOnly = officialOnly,
                                onOpenAddStory = onOpenAddStory,
                                onOpenStory = onOpenStory,
                                onSelectCategory = { selectedCategory = it },
                                onToggleOfficialOnly = { officialOnly = !officialOnly }
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("Unable to load Squadify feed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(postsState.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                is UiState.Success -> {
                    val filtered = remember(postsState.data, selectedCategory, officialOnly, archivedIds) {
                        postsState.data.filter { post ->
                            !archivedIds.contains(post.id) &&
                                (selectedCategory == "All" || post.category == selectedCategory) &&
                                (!officialOnly || post.isOfficial)
                        }
                    }

                    if (filtered.isEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 92.dp)
                        ) {
                            item(key = "feed_stories_header") {
                                FeedStoriesAndFilterHeader(
                                    currentUser = currentUser,
                                    ownStoryGroup = ownStoryGroup,
                                    followedStoryGroups = followedStoryGroups,
                                    storyUploadState = storyUploadState,
                                    postUploadState = postUploadState,
                                    allCategories = allCategories,
                                    selectedCategory = selectedCategory,
                                    officialOnly = officialOnly,
                                    onOpenAddStory = onOpenAddStory,
                                    onOpenStory = onOpenStory,
                                    onSelectCategory = { selectedCategory = it },
                                    onToggleOfficialOnly = { officialOnly = !officialOnly }
                                )
                            }
                            item(key = "feed_empty") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 32.dp, vertical = 48.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Outlined.AddCircleOutline, contentDescription = null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("No posts in this view yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Share a Squadify moment, video, or announcement!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(onClick = onNavigateCreate) {
                                        Text("Create First Post")
                                    }
                                }
                            }
                        }
                    } else if (layoutMode == FeedLayoutMode.GRID) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 2.dp, bottom = 92.dp),
                            horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                            verticalArrangement = Arrangement.spacedBy(1.5.dp)
                        ) {
                            item(
                                key = "feed_stories_header",
                                span = { GridItemSpan(maxLineSpan) }
                            ) {
                                FeedStoriesAndFilterHeader(
                                    currentUser = currentUser,
                                    ownStoryGroup = ownStoryGroup,
                                    followedStoryGroups = followedStoryGroups,
                                    storyUploadState = storyUploadState,
                                    postUploadState = postUploadState,
                                    allCategories = allCategories,
                                    selectedCategory = selectedCategory,
                                    officialOnly = officialOnly,
                                    onOpenAddStory = onOpenAddStory,
                                    onOpenStory = onOpenStory,
                                    onSelectCategory = { selectedCategory = it },
                                    onToggleOfficialOnly = { officialOnly = !officialOnly }
                                )
                            }
                            items(filtered, key = { it.id }) { post ->
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .animateItem()
                                        .clickable { selectedGridPost = post }
                                ) {
                                    if (post.images.isNotEmpty()) {
                                        CampusImage(
                                            urlOrDataUri = post.images.first(),
                                            contentDescription = post.content,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        if (MediaUtils.isVideoMediaUri(post.images.first())) {
                                            Icon(
                                                Icons.Filled.PlayArrow,
                                                contentDescription = "Video",
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp)
                                                    .size(18.dp)
                                            )
                                        } else if (post.images.size > 1) {
                                            Icon(
                                                Icons.Filled.Collections,
                                                contentDescription = "Carousel",
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp)
                                                    .size(16.dp)
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .padding(8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = post.content,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 4,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val listState = rememberLazyListState()
                        LaunchedEffect(filtered, listState.firstVisibleItemIndex) {
                            val startIdx = (listState.firstVisibleItemIndex - 1).coerceAtLeast(0)
                            val upcomingSongs = filtered
                                .drop(startIdx)
                                .take(6)
                                .mapNotNull { it.musicPreviewUrl?.takeIf { u -> u.isNotBlank() } }
                            MusicCacheManager.prefetchUrls(context, upcomingSongs)
                        }
                        val playablePostIds = remember(filtered) {
                            filtered.filter { it.hasPlayableAutoContent() }.map { it.id }.toSet()
                        }
                        val rawFocusedPostId by remember(playablePostIds) {
                            derivedStateOf {
                                computeFocusedPostId(listState.layoutInfo, playablePostIds)
                            }
                        }
                        var activeFeedPostId by remember { mutableStateOf<String?>(null) }
                        LaunchedEffect(rawFocusedPostId, isScreenActive, selectedGridPost) {
                            if (!isScreenActive || selectedGridPost != null || rawFocusedPostId == null) {
                                activeFeedPostId = null
                            } else if (activeFeedPostId != rawFocusedPostId) {
                                activeFeedPostId = null
                                delay(60L)
                                activeFeedPostId = rawFocusedPostId
                            }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 92.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            item(key = "feed_stories_header") {
                                FeedStoriesAndFilterHeader(
                                    currentUser = currentUser,
                                    ownStoryGroup = ownStoryGroup,
                                    followedStoryGroups = followedStoryGroups,
                                    storyUploadState = storyUploadState,
                                    postUploadState = postUploadState,
                                    allCategories = allCategories,
                                    selectedCategory = selectedCategory,
                                    officialOnly = officialOnly,
                                    onOpenAddStory = onOpenAddStory,
                                    onOpenStory = onOpenStory,
                                    onSelectCategory = { selectedCategory = it },
                                    onToggleOfficialOnly = { officialOnly = !officialOnly }
                                )
                            }
                            items(filtered, key = { it.id }) { post ->
                                Box(modifier = Modifier.animateItem()) {
                                    PostCard(
                                        post = post,
                                        currentUser = currentUser,
                                        isAdmin = isAdmin,
                                        repository = repository,
                                        hideLikeCounts = hideLikeCounts,
                                        isBookmarked = savedIds.contains(post.id),
                                        usersById = usersById,
                                        onToggleLike = { onToggleLike(post) },
                                        onToggleBookmark = { onToggleBookmark(post.id) },
                                        onAddComment = { text, replyTo -> onAddComment(post, text, replyTo) },
                                        onToggleCommentLike = { comment -> onToggleCommentLike(post, comment) },
                                        onDeleteComment = { cId -> onDeleteComment(post.id, cId) },
                                        onMarkViewed = { onMarkPostViewed(post) },
                                        onTogglePin = { onTogglePin(post) },
                                        onDeletePost = { onDeletePost(post) },
                                        onOpenAuthorProfile = onOpenAuthorProfile,
                                        onMessageAuthor = onMessageAuthor,
                                        onReportPost = { onReportPost(post) },
                                        onUpdatePost = { content, cat, ratio, track, startSec ->
                                            onUpdatePost(post, content, cat, ratio, track, startSec)
                                        },
                                        onAddToStory = { onAddPostToStory(post) },
                                        compactDiscussion = layoutMode == FeedLayoutMode.DISCUSSION,
                                        isAutoPlayActive = post.id == activeFeedPostId
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }

    selectedGridPost?.let { post ->
        val freshPost = (postsState as? UiState.Success)?.data?.firstOrNull { it.id == post.id } ?: post
        Dialog(
            onDismissRequest = { selectedGridPost = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .widthIn(max = 580.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline)
            ) {
                PostCard(
                    post = freshPost,
                    currentUser = currentUser,
                    isAdmin = isAdmin,
                    repository = repository,
                    hideLikeCounts = hideLikeCounts,
                    isBookmarked = savedIds.contains(freshPost.id),
                    usersById = usersById,
                    onToggleLike = { onToggleLike(freshPost) },
                    onToggleBookmark = { onToggleBookmark(freshPost.id) },
                    onAddComment = { text, replyTo -> onAddComment(freshPost, text, replyTo) },
                    onToggleCommentLike = { comment -> onToggleCommentLike(freshPost, comment) },
                    onDeleteComment = { cId -> onDeleteComment(freshPost.id, cId) },
                    onMarkViewed = { onMarkPostViewed(freshPost) },
                    onTogglePin = { onTogglePin(freshPost) },
                    onDeletePost = {
                        onDeletePost(freshPost)
                        selectedGridPost = null
                    },
                    onOpenAuthorProfile = { uid ->
                        selectedGridPost = null
                        onOpenAuthorProfile(uid)
                    },
                    onMessageAuthor = { uid ->
                        selectedGridPost = null
                        onMessageAuthor(uid)
                    },
                    onReportPost = { onReportPost(freshPost) },
                    onUpdatePost = { content, cat, ratio, track, startSec ->
                        onUpdatePost(freshPost, content, cat, ratio, track, startSec)
                    },
                    onAddToStory = {
                        selectedGridPost = null
                        onAddPostToStory(freshPost)
                    },
                    isAutoPlayActive = isScreenActive
                )
            }
        }
    }
}

@Composable
private fun FeedStoriesAndFilterHeader(
    currentUser: CampusUser,
    ownStoryGroup: StoryGroup?,
    followedStoryGroups: List<StoryGroup>,
    storyUploadState: com.bsdcampus.app.notifications.StoryUploadState?,
    postUploadState: MediaUploadState?,
    allCategories: List<String>,
    selectedCategory: String,
    officialOnly: Boolean,
    onOpenAddStory: () -> Unit,
    onOpenStory: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onToggleOfficialOnly: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Instagram-style Stories Ribbon: Your Story + Followed Users Only
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("feed_stories_ribbon"),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(68.dp)
                    .clickable {
                        if (ownStoryGroup != null) onOpenStory(currentUser.uid)
                        else onOpenAddStory()
                    }
                    .testTag("feed_your_story_bubble")
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    UserAvatar(
                        src = if (currentUser.isBlackSociety) null else currentUser.avatar,
                        name = currentUser.name,
                        role = currentUser.role,
                        isVerified = currentUser.isVerified,
                        isBlackSociety = currentUser.isBlackSociety,
                        blackSocietyNumber = currentUser.blackSocietyNumber,
                        isClassRepresentative = currentUser.isClassRepresentative,
                        showStoryRing = ownStoryGroup != null,
                        size = AvatarSize.LG
                    )
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(InstaBlue)
                            .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                            .clickable(onClick = onOpenAddStory),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add story",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Your story",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            followedStoryGroups.forEach { group ->
                val member = group.author
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(68.dp)
                        .clickable { onOpenStory(member.uid) }
                        .testTag("feed_story_${member.uid}")
                ) {
                    UserAvatar(
                        src = if (member.isBlackSociety) null else member.avatar,
                        name = member.name,
                        role = member.role,
                        isVerified = member.isVerified,
                        isBlackSociety = member.isBlackSociety,
                        blackSocietyNumber = member.blackSocietyNumber,
                        isClassRepresentative = member.isClassRepresentative,
                        showStoryRing = group.hasUnseen,
                        showSeenStoryRing = !group.hasUnseen,
                        size = AvatarSize.LG
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (member.isBlackSociety) "#${member.blackSocietyNumber ?: 1}" else member.name.substringBefore(" "),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (group.hasUnseen) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        AnimatedVisibility(visible = storyUploadState != null) {
            storyUploadState?.let { uploadState ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .testTag("story_upload_banner"),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(
                        0.75.dp,
                        when {
                            uploadState.isFailed -> InstaLikeRed.copy(alpha = 0.65f)
                            uploadState.isCompleted -> Emerald500.copy(alpha = 0.65f)
                            else -> MaterialTheme.colorScheme.outline
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!uploadState.isVideo && !uploadState.previewPath.isNullOrBlank()) {
                                    AsyncImage(
                                        model = File(uploadState.previewPath),
                                        contentDescription = "Uploading story preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uploadState.statusLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!uploadState.errorMessage.isNullOrBlank()) {
                                    Text(
                                        text = uploadState.errorMessage,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = InstaLikeRed,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            when {
                                uploadState.isCompleted -> {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Uploaded",
                                        tint = Emerald500,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                uploadState.isFailed -> {
                                    Icon(
                                        imageVector = Icons.Outlined.ErrorOutline,
                                        contentDescription = "Failed",
                                        tint = InstaLikeRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    IconButton(
                                        onClick = { StoryUploadTracker.clear() },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Dismiss upload error",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                else -> {
                                    Text(
                                        text = "${(uploadState.progress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = InstaBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
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
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = postUploadState != null) {
            postUploadState?.let { uploadState ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .testTag("post_upload_banner"),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(
                        0.75.dp,
                        when {
                            uploadState.isFailed -> InstaLikeRed.copy(alpha = 0.65f)
                            uploadState.isCompleted -> Emerald500.copy(alpha = 0.65f)
                            else -> MaterialTheme.colorScheme.outline
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!uploadState.previewPath.isNullOrBlank()) {
                                    AsyncImage(
                                        model = File(uploadState.previewPath),
                                        contentDescription = "Uploading post preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Collections,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uploadState.statusLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!uploadState.errorMessage.isNullOrBlank()) {
                                    Text(
                                        text = uploadState.errorMessage,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = InstaLikeRed,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            when {
                                uploadState.isCompleted -> {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Post published",
                                        tint = Emerald500,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                uploadState.isFailed -> {
                                    Icon(
                                        imageVector = Icons.Outlined.ErrorOutline,
                                        contentDescription = "Failed",
                                        tint = InstaLikeRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    IconButton(
                                        onClick = { MediaUploadTracker.clear() },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Dismiss upload error",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                else -> {
                                    Text(
                                        text = "${(uploadState.progress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = InstaBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
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
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Category Pills + Official Notices Filter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = officialOnly,
                onClick = onToggleOfficialOnly,
                label = { Text("Official") },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Verified,
                        contentDescription = null,
                        tint = if (officialOnly) Amber500 else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.testTag("filter_official_chip")
            )
            allCategories.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onSelectCategory(category) },
                    label = { Text(category) },
                    modifier = Modifier.testTag("filter_cat_$category")
                )
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    }
}
