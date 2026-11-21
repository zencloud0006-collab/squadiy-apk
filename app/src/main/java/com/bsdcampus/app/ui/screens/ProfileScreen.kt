package com.bsdcampus.app.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bsdcampus.app.data.CampusRepository
import com.bsdcampus.app.data.MediaUtils
import com.bsdcampus.app.data.MusicCacheManager
import com.bsdcampus.app.data.StoryMusicTrack
import com.bsdcampus.app.model.CampusPost
import com.bsdcampus.app.model.CampusReel
import com.bsdcampus.app.model.CampusStory
import com.bsdcampus.app.model.CampusTab
import com.bsdcampus.app.model.CampusUser
import com.bsdcampus.app.model.HighlightMediaItem
import com.bsdcampus.app.model.StoryHighlight
import com.bsdcampus.app.model.UserPrivateInfo
import com.bsdcampus.app.ui.auth.AuthHelper
import com.bsdcampus.app.ui.components.AvatarSize
import com.bsdcampus.app.ui.components.CampusImage
import com.bsdcampus.app.ui.components.CircularAvatarPreviewDialog
import com.bsdcampus.app.ui.components.HighlightCreatorDialog
import com.bsdcampus.app.ui.components.HighlightViewerDialog
import com.bsdcampus.app.ui.components.PostCard
import com.bsdcampus.app.ui.components.UserAvatar
import com.bsdcampus.app.ui.components.VerifiedBadgeIcon
import com.bsdcampus.app.ui.components.computeFocusedPostId
import com.bsdcampus.app.ui.components.hasPlayableAutoContent
import com.bsdcampus.app.ui.theme.Gold300
import com.bsdcampus.app.ui.theme.Gold400
import com.bsdcampus.app.ui.theme.Gold600
import com.bsdcampus.app.ui.theme.InstaBlue
import com.bsdcampus.app.ui.theme.JetBrainsMonoFamily
import com.bsdcampus.app.ui.theme.Slate900
import com.bsdcampus.app.ui.theme.Zinc700
import com.bsdcampus.app.ui.theme.Zinc900
import kotlinx.coroutines.delay

private enum class ProfileContentTab {
    GRID,
    REELS,
    FEED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    currentUser: CampusUser,
    isAdmin: Boolean,
    profileUser: CampusUser,
    allUsers: List<CampusUser>,
    posts: List<CampusPost>,
    reels: List<CampusReel> = emptyList(),
    highlights: List<StoryHighlight> = emptyList(),
    ownActiveStories: List<CampusStory> = emptyList(),
    profileActiveStories: List<CampusStory> = emptyList(),
    onOpenProfileStory: () -> Unit = {},
    privateInfo: UserPrivateInfo?,
    repository: CampusRepository,
    onNavigate: (CampusTab) -> Unit,
    onOpenUserProfile: (String) -> Unit,
    onOpenConnections: (userId: String, initialTabIndex: Int) -> Unit,
    onToggleFollow: (String) -> Unit,
    onOpenDirectChat: (String) -> Unit,
    onReportUser: (CampusUser) -> Unit,
    onToggleLike: (CampusPost) -> Unit,
    onToggleBookmark: (String) -> Unit,
    onAddComment: (CampusPost, String, com.bsdcampus.app.model.CampusComment?) -> Unit,
    onToggleCommentLike: (CampusPost, com.bsdcampus.app.model.CampusComment) -> Unit = { _, _ -> },
    onDeleteComment: (String, String) -> Unit,
    onMarkPostViewed: (CampusPost) -> Unit = {},
    onTogglePin: (CampusPost) -> Unit,
    onDeletePost: (CampusPost) -> Unit,
    onReportPost: (CampusPost) -> Unit,
    onUpdatePost: (CampusPost, String, String, String, StoryMusicTrack?, Int) -> Unit,
    onAddPostToStory: (CampusPost) -> Unit = {},
    onToggleLikeReel: (CampusReel) -> Unit = {},
    onToggleBookmarkReel: (String) -> Unit = {},
    onAddReelComment: (CampusReel, String, com.bsdcampus.app.model.ReelComment?) -> Unit = { _, _, _ -> },
    onToggleReelCommentLike: (CampusReel, com.bsdcampus.app.model.ReelComment) -> Unit = { _, _ -> },
    onDeleteReelComment: (String, String) -> Unit = { _, _ -> },
    onMarkReelViewed: (CampusReel) -> Unit = {},
    onUpdateReel: (CampusReel, String, String, String, String, StoryMusicTrack?, Int, Boolean) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onDeleteReel: (CampusReel) -> Unit = {},
    onReportReel: (CampusReel) -> Unit = {},
    onAddReelToStory: (CampusReel) -> Unit = {},
    onCreateHighlight: (title: String, coverAvatar: String?, coverEmoji: String?, items: List<HighlightMediaItem>, onDone: () -> Unit) -> Unit,
    onUpdateHighlight: (highlightId: String, title: String, coverAvatar: String?, coverEmoji: String?, items: List<HighlightMediaItem>, onDone: () -> Unit) -> Unit,
    onDeleteHighlight: (String) -> Unit,
    onShowToast: (String) -> Unit,
    isScreenActive: Boolean = true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isOwnProfile = profileUser.uid == currentUser.uid
    val isFollowing = currentUser.following.contains(profileUser.uid)

    val usersById = remember(allUsers) { allUsers.associateBy { it.uid } }

    var selectedContentTab by remember { mutableStateOf(ProfileContentTab.GRID) }
    var selectedLightboxPost by remember { mutableStateOf<CampusPost?>(null) }
    var selectedReelForDialog by remember { mutableStateOf<CampusReel?>(null) }
    var showHighlightCreator by remember { mutableStateOf(false) }
    var editingHighlight by remember { mutableStateOf<StoryHighlight?>(null) }
    var viewingHighlight by remember { mutableStateOf<StoryHighlight?>(null) }
    var showAvatarPreview by remember(profileUser.uid) { mutableStateOf(false) }
    var showAvatarActionSheet by remember(profileUser.uid) { mutableStateOf(false) }

    val userPosts = remember(posts, profileUser.uid) {
        posts.filter { it.authorId == profileUser.uid }
    }
    val userReels = remember(reels, profileUser.uid) {
        reels.filter { it.authorId == profileUser.uid }
    }
    val userHighlights = remember(highlights, profileUser.uid) {
        highlights.filter { it.ownerId == profileUser.uid }
    }

    val effectiveVerified = profileUser.isVerified || profileUser.isClassRepresentative || profileUser.role == "admin"

    val listState = rememberLazyListState()
    val playablePostIds = remember(userPosts) {
        userPosts.filter { it.hasPlayableAutoContent() }.map { it.id }.toSet()
    }
    val rawFocusedPostId by remember(playablePostIds, selectedContentTab) {
        derivedStateOf {
            if (selectedContentTab == ProfileContentTab.FEED) {
                computeFocusedPostId(listState.layoutInfo, playablePostIds)
            } else null
        }
    }
    LaunchedEffect(profileActiveStories) {
        val storySongs = profileActiveStories.mapNotNull { it.musicPreviewUrl?.takeIf { u -> u.isNotBlank() } }
        MusicCacheManager.prefetchUrls(context, storySongs)
    }

    LaunchedEffect(userPosts, selectedContentTab, listState.firstVisibleItemIndex) {
        if (selectedContentTab == ProfileContentTab.FEED) {
            val startIdx = (listState.firstVisibleItemIndex - 2).coerceAtLeast(0)
            val upcoming = userPosts
                .drop(startIdx)
                .take(6)
                .mapNotNull { it.musicPreviewUrl?.takeIf { u -> u.isNotBlank() } }
            MusicCacheManager.prefetchUrls(context, upcoming)
        }
    }

    var activeFeedPostId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(
        rawFocusedPostId,
        isScreenActive,
        selectedContentTab,
        selectedLightboxPost,
        selectedReelForDialog
    ) {
        val shouldPlay = isScreenActive &&
            selectedContentTab == ProfileContentTab.FEED &&
            selectedLightboxPost == null &&
            selectedReelForDialog == null &&
            rawFocusedPostId != null
        if (!shouldPlay) {
            activeFeedPostId = null
        } else if (activeFeedPostId != rawFocusedPostId) {
            activeFeedPostId = null
            delay(60L)
            activeFeedPostId = rawFocusedPostId
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 92.dp)
        ) {
            if (profileUser.isBlackSociety) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Zinc900,
                        border = BorderStroke(1.dp, Zinc700),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "♠ DARK SOCIETY ARCHIVE",
                                    color = Color.White,
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Confidential Member #${profileUser.blackSocietyNumber ?: 1}",
                                    color = Color.White.copy(alpha = 0.72f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.14f)) {
                                Text(
                                    text = "#${profileUser.blackSocietyNumber ?: 1}",
                                    color = Color.White,
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (profileUser.isClassRepresentative) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, Gold300),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Brush.horizontalGradient(listOf(Gold400, Gold600)))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "👑 Official Class ${profileUser.classRepresentativeGrade ?: 10} Representative • Verified Ambassador",
                                color = Slate900,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            // Classic Instagram Profile Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .animateItem(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val hasActiveProfileStories = profileActiveStories.isNotEmpty()
                        val hasUnseenProfileStories = profileActiveStories.any {
                            it.authorId != currentUser.uid && !it.viewedBy.contains(currentUser.uid)
                        }
                        UserAvatar(
                            src = profileUser.avatar,
                            name = profileUser.name,
                            role = profileUser.role,
                            isVerified = effectiveVerified,
                            isBlackSociety = profileUser.isBlackSociety,
                            blackSocietyNumber = profileUser.blackSocietyNumber,
                            isClassRepresentative = profileUser.isClassRepresentative,
                            showStoryRing = hasActiveProfileStories && (isOwnProfile || hasUnseenProfileStories),
                            showSeenStoryRing = hasActiveProfileStories && !isOwnProfile && !hasUnseenProfileStories,
                            onClick = {
                                if (hasActiveProfileStories) {
                                    showAvatarActionSheet = true
                                } else {
                                    showAvatarPreview = true
                                }
                            },
                            modifier = Modifier.testTag("profile_avatar_${profileUser.uid}"),
                            size = AvatarSize.XL
                        )
                        Spacer(modifier = Modifier.width(18.dp))
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatPill(value = "${userPosts.size}", label = "posts") {}
                            StatPill(value = "${profileUser.followers.size}", label = "followers") {
                                onOpenConnections(profileUser.uid, 0)
                            }
                            StatPill(value = "${profileUser.following.size}", label = "following") {
                                onOpenConnections(profileUser.uid, 1)
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (profileUser.isBlackSociety) "Member - ${profileUser.blackSocietyNumber ?: 1}" else profileUser.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (effectiveVerified && !profileUser.isBlackSociety) {
                                VerifiedBadgeIcon(size = 16.dp)
                            }
                        }
                        Text(
                            text = listOfNotNull(
                                profileUser.role.replaceFirstChar { it.uppercase() },
                                profileUser.grade?.takeIf { it.isNotBlank() }
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!profileUser.bio.isNullOrBlank()) {
                            Text(
                                text = profileUser.bio,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    // Instagram Action Buttons
                    if (isOwnProfile) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onNavigate(CampusTab.EDIT_PROFILE) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onBackground
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .testTag("profile_edit_button")
                            ) {
                                Text("Edit profile", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                            Button(
                                onClick = { onNavigate(CampusTab.SETTINGS) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onBackground
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .testTag("profile_settings_button")
                            ) {
                                Icon(Icons.Outlined.Settings, contentDescription = "Settings", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Settings", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                            if (isAdmin) {
                                OutlinedButton(
                                    onClick = { onNavigate(CampusTab.ADMIN) },
                                    contentPadding = PaddingValues(horizontal = 10.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(Icons.Outlined.Shield, contentDescription = "Admin", modifier = Modifier.size(16.dp))
                                }
                            }
                            OutlinedButton(
                                onClick = { AuthHelper.performSignOut(context, scope) },
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.Outlined.Logout, contentDescription = "Logout", modifier = Modifier.size(16.dp))
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onToggleFollow(profileUser.uid) },
                                colors = if (isFollowing) {
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onBackground
                                    )
                                } else {
                                    ButtonDefaults.buttonColors(
                                        containerColor = InstaBlue,
                                        contentColor = Color.White
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                            ) {
                                Text(if (isFollowing) "Following" else "Follow", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                            Button(
                                onClick = { onOpenDirectChat(profileUser.uid) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onBackground
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                            ) {
                                Text("Message", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                            IconButton(
                                onClick = { onReportUser(profileUser) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Outlined.Flag, contentDescription = "Report user", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // User-Created Story Highlights Strip (no fake preset highlights)
                    if (isOwnProfile || userHighlights.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(top = 4.dp)
                                .testTag("profile_highlights_strip"),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            userHighlights.forEach { hl ->
                                val coverImage = hl.coverAvatar ?: hl.items.firstOrNull()?.mediaUrl
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(66.dp)
                                        .clickable { viewingHighlight = hl }
                                        .testTag("profile_highlight_${hl.id}")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(58.dp)
                                            .clip(CircleShape)
                                            .border(1.25.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when {
                                            !coverImage.isNullOrBlank() -> CampusImage(
                                                urlOrDataUri = coverImage,
                                                contentDescription = hl.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            !hl.coverEmoji.isNullOrBlank() -> Text(hl.coverEmoji, fontSize = 22.sp)
                                            else -> Text(
                                                text = hl.title.take(1).uppercase(),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = hl.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (isOwnProfile) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(66.dp)
                                        .clickable {
                                            editingHighlight = null
                                            showHighlightCreator = true
                                        }
                                        .testTag("profile_add_highlight_button")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(58.dp)
                                            .clip(CircleShape)
                                            .border(1.25.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = "New highlight",
                                            tint = MaterialTheme.colorScheme.onBackground,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "New",
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Instagram Underline Tab Strip: Grid vs Reels vs Feed
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedContentTab = ProfileContentTab.GRID }
                                .padding(top = 10.dp)
                                .testTag("profile_tab_grid"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.GridOn,
                                contentDescription = "Grid view",
                                tint = if (selectedContentTab == ProfileContentTab.GRID) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.5.dp)
                                    .background(if (selectedContentTab == ProfileContentTab.GRID) MaterialTheme.colorScheme.onBackground else Color.Transparent)
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedContentTab = ProfileContentTab.REELS }
                                .padding(top = 10.dp)
                                .testTag("profile_tab_reels"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.SmartDisplay,
                                contentDescription = "Reels view",
                                tint = if (selectedContentTab == ProfileContentTab.REELS) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.5.dp)
                                    .background(if (selectedContentTab == ProfileContentTab.REELS) MaterialTheme.colorScheme.onBackground else Color.Transparent)
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedContentTab = ProfileContentTab.FEED }
                                .padding(top = 10.dp)
                                .testTag("profile_tab_feed"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.ViewAgenda,
                                contentDescription = "Feed view",
                                tint = if (selectedContentTab == ProfileContentTab.FEED) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.5.dp)
                                    .background(if (selectedContentTab == ProfileContentTab.FEED) MaterialTheme.colorScheme.onBackground else Color.Transparent)
                            )
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                }
            }

            if (selectedContentTab == ProfileContentTab.REELS) {
                if (userReels.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Filled.SmartDisplay,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("No reels shared yet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "Campus reels posted here will appear in this 9:16 gallery.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    val reelRows = userReels.chunked(3)
                    items(reelRows) { rowReels ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 1.5.dp)
                                .animateItem(),
                            horizontalArrangement = Arrangement.spacedBy(1.5.dp)
                        ) {
                            rowReels.forEach { reel ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(9f / 16f)
                                        .background(Color.Black)
                                        .clickable { selectedReelForDialog = reel }
                                        .testTag("profile_reel_tile_${reel.id}")
                                ) {
                                    CampusImage(
                                        urlOrDataUri = reel.videoUrl,
                                        contentDescription = reel.caption.ifBlank { "Reel video" },
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .align(Alignment.BottomCenter)
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                                                )
                                            )
                                    )
                                    Icon(
                                        Icons.Filled.SmartDisplay,
                                        contentDescription = "Reel",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(18.dp)
                                    )
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(horizontal = 6.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Favorite,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "${reel.likedBy.size}",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            repeat(3 - rowReels.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else if (userPosts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("No posts shared yet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "Campus moments shared here will appear in this gallery.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (selectedContentTab == ProfileContentTab.GRID) {
                val rows = userPosts.chunked(3)
                items(rows) { rowPosts ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 1.5.dp)
                            .animateItem(),
                        horizontalArrangement = Arrangement.spacedBy(1.5.dp)
                    ) {
                        rowPosts.forEach { post ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clickable { selectedLightboxPost = post }
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
                        repeat(3 - rowPosts.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                items(userPosts, key = { it.id }) { post ->
                    Box(modifier = Modifier.animateItem()) {
                        PostCard(
                            post = post,
                            currentUser = currentUser,
                            isAdmin = isAdmin,
                            repository = repository,
                            hideLikeCounts = privateInfo?.hideLikeCounts == true,
                            isBookmarked = privateInfo?.savedPostIds?.contains(post.id) == true,
                            usersById = usersById,
                            onToggleLike = { onToggleLike(post) },
                            onToggleBookmark = { onToggleBookmark(post.id) },
                            onAddComment = { text, replyTo -> onAddComment(post, text, replyTo) },
                            onToggleCommentLike = { comment -> onToggleCommentLike(post, comment) },
                            onDeleteComment = { cId -> onDeleteComment(post.id, cId) },
                            onMarkViewed = { onMarkPostViewed(post) },
                            onTogglePin = { onTogglePin(post) },
                            onDeletePost = { onDeletePost(post) },
                            onOpenAuthorProfile = onOpenUserProfile,
                            onMessageAuthor = onOpenDirectChat,
                            onReportPost = { onReportPost(post) },
                            onUpdatePost = { content, cat, ratio, track, startSec ->
                                onUpdatePost(post, content, cat, ratio, track, startSec)
                            },
                            onAddToStory = { onAddPostToStory(post) },
                            isAutoPlayActive = post.id == activeFeedPostId
                        )
                    }
                }
            }
        }
    }

    selectedLightboxPost?.let { post ->
        val fresh = posts.firstOrNull { it.id == post.id } ?: post
        Dialog(
            onDismissRequest = { selectedLightboxPost = null },
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
                    post = fresh,
                    currentUser = currentUser,
                    isAdmin = isAdmin,
                    repository = repository,
                    hideLikeCounts = privateInfo?.hideLikeCounts == true,
                    isBookmarked = privateInfo?.savedPostIds?.contains(fresh.id) == true,
                    usersById = usersById,
                    onToggleLike = { onToggleLike(fresh) },
                    onToggleBookmark = { onToggleBookmark(fresh.id) },
                    onAddComment = { text, replyTo -> onAddComment(fresh, text, replyTo) },
                    onToggleCommentLike = { comment -> onToggleCommentLike(fresh, comment) },
                    onDeleteComment = { cId -> onDeleteComment(fresh.id, cId) },
                    onMarkViewed = { onMarkPostViewed(fresh) },
                    onTogglePin = { onTogglePin(fresh) },
                    onDeletePost = {
                        onDeletePost(fresh)
                        selectedLightboxPost = null
                    },
                    onOpenAuthorProfile = { uid ->
                        selectedLightboxPost = null
                        onOpenUserProfile(uid)
                    },
                    onMessageAuthor = { uid ->
                        selectedLightboxPost = null
                        onOpenDirectChat(uid)
                    },
                    onReportPost = { onReportPost(fresh) },
                    onUpdatePost = { content, cat, ratio, track, startSec ->
                        onUpdatePost(fresh, content, cat, ratio, track, startSec)
                    },
                    onAddToStory = {
                        selectedLightboxPost = null
                        onAddPostToStory(fresh)
                    },
                    isAutoPlayActive = isScreenActive
                )
            }
        }
    }

    selectedReelForDialog?.let { targetReel ->
        val fresh = userReels.firstOrNull { it.id == targetReel.id } ?: targetReel
        ReelPlayerDialog(
            initialReel = fresh,
            allReels = userReels,
            currentUser = currentUser,
            isAdmin = isAdmin,
            privateInfo = privateInfo,
            usersById = usersById,
            repository = repository,
            onToggleLike = onToggleLikeReel,
            onToggleBookmark = onToggleBookmarkReel,
            onToggleFollow = onToggleFollow,
            onAddComment = onAddReelComment,
            onToggleCommentLike = onToggleReelCommentLike,
            onDeleteComment = onDeleteReelComment,
            onMarkViewed = onMarkReelViewed,
            onUpdateReel = onUpdateReel,
            onDeleteReel = { r ->
                onDeleteReel(r)
                selectedReelForDialog = null
            },
            onAddToStory = { r ->
                selectedReelForDialog = null
                onAddReelToStory(r)
            },
            onOpenAuthorProfile = { uid ->
                selectedReelForDialog = null
                onOpenUserProfile(uid)
            },
            onMessageAuthor = { uid ->
                selectedReelForDialog = null
                onOpenDirectChat(uid)
            },
            onReportReel = onReportReel,
            onDismiss = { selectedReelForDialog = null }
        )
    }

    if (showHighlightCreator) {
        HighlightCreatorDialog(
            currentUser = currentUser,
            isAdmin = isAdmin,
            ownActiveStories = ownActiveStories,
            existingHighlight = editingHighlight,
            onShowToast = onShowToast,
            onSave = { title, coverAvatar, coverEmoji, items ->
                val target = editingHighlight
                if (target == null) {
                    onCreateHighlight(title, coverAvatar, coverEmoji, items) {
                        showHighlightCreator = false
                    }
                } else {
                    onUpdateHighlight(target.id, title, coverAvatar, coverEmoji, items) {
                        showHighlightCreator = false
                        editingHighlight = null
                    }
                }
            },
            onDismiss = {
                showHighlightCreator = false
                editingHighlight = null
            }
        )
    }

    viewingHighlight?.let { hl ->
        val freshHl = highlights.firstOrNull { it.id == hl.id } ?: hl
        HighlightViewerDialog(
            highlight = freshHl,
            owner = profileUser,
            isOwnHighlight = isOwnProfile,
            isAdmin = isAdmin,
            onEdit = {
                editingHighlight = freshHl
                showHighlightCreator = true
            },
            onDelete = { onDeleteHighlight(freshHl.id) },
            onDismiss = { viewingHighlight = null }
        )
    }

    if (showAvatarActionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAvatarActionSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                ListItem(
                    headlineContent = {
                        Text("View Story", fontWeight = FontWeight.SemiBold)
                    },
                    leadingContent = {
                        Icon(Icons.Filled.AutoStories, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        showAvatarActionSheet = false
                        onOpenProfileStory()
                    }
                )
                ListItem(
                    headlineContent = {
                        Text("View Profile Photo", fontWeight = FontWeight.SemiBold)
                    },
                    leadingContent = {
                        Icon(Icons.Filled.AccountCircle, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        showAvatarActionSheet = false
                        showAvatarPreview = true
                    }
                )
            }
        }
    }

    if (showAvatarPreview) {
        val displayTitle = if (profileUser.isBlackSociety) {
            "Member - ${profileUser.blackSocietyNumber ?: 1}"
        } else {
            profileUser.name
        }
        val subtitleText = listOfNotNull(
            profileUser.role.replaceFirstChar { it.uppercase() },
            profileUser.grade?.takeIf { it.isNotBlank() }
        ).joinToString(" · ")
        CircularAvatarPreviewDialog(
            avatarSrc = profileUser.avatar,
            displayName = displayTitle,
            subtitle = subtitleText,
            isVerified = effectiveVerified,
            isBlackSociety = profileUser.isBlackSociety,
            blackSocietyNumber = profileUser.blackSocietyNumber,
            onDismiss = { showAvatarPreview = false }
        )
    }
}

@Composable
private fun StatPill(value: String, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
