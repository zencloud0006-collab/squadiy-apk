package com.bsdcampus.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bsdcampus.app.data.CampusRepository
import com.bsdcampus.app.data.MediaUtils
import com.bsdcampus.app.data.StoryMusicTrack
import com.bsdcampus.app.model.CampusPost
import com.bsdcampus.app.model.CampusUser
import com.bsdcampus.app.model.UiState
import com.bsdcampus.app.model.UserPrivateInfo
import com.bsdcampus.app.ui.components.AvatarSize
import com.bsdcampus.app.ui.components.CampusImage
import com.bsdcampus.app.ui.components.PostCard
import com.bsdcampus.app.ui.components.UserAvatar
import com.bsdcampus.app.ui.components.VerifiedBadgeIcon
import com.bsdcampus.app.ui.theme.Amber500
import com.bsdcampus.app.ui.theme.InstaBlue

@Composable
fun ExploreScreen(
    currentUser: CampusUser,
    isAdmin: Boolean,
    privateInfo: UserPrivateInfo?,
    allUsersState: UiState<List<CampusUser>>,
    postsState: UiState<List<CampusPost>>,
    repository: CampusRepository,
    onOpenUserProfile: (String) -> Unit,
    onToggleFollow: (String) -> Unit,
    onToggleLike: (CampusPost) -> Unit,
    onToggleBookmark: (String) -> Unit,
    onAddComment: (CampusPost, String, com.bsdcampus.app.model.CampusComment?) -> Unit,
    onToggleCommentLike: (CampusPost, com.bsdcampus.app.model.CampusComment) -> Unit = { _, _ -> },
    onDeleteComment: (String, String) -> Unit,
    onMarkPostViewed: (CampusPost) -> Unit = {},
    onTogglePin: (CampusPost) -> Unit,
    onDeletePost: (CampusPost) -> Unit,
    onMessageAuthor: (String) -> Unit,
    onReportPost: (CampusPost) -> Unit,
    onUpdatePost: (CampusPost, String, String, String, StoryMusicTrack?, Int) -> Unit,
    onAddPostToStory: (CampusPost) -> Unit = {},
    isScreenActive: Boolean = true
) {
    var query by remember { mutableStateOf("") }
    var selectedPost by remember { mutableStateOf<CampusPost?>(null) }

    val users = (allUsersState as? UiState.Success)?.data.orEmpty()
    val usersById = remember(users) { users.associateBy { it.uid } }
    val posts = (postsState as? UiState.Success)?.data.orEmpty()

    val matchingUsers = remember(users, query) {
        val visible = users.filter { it.isApproved && !it.isSuspended }
        if (query.isBlank()) visible.take(12)
        else visible.filter {
            it.name.contains(query, ignoreCase = true) ||
                (it.grade?.contains(query, ignoreCase = true) == true) ||
                it.role.contains(query, ignoreCase = true)
        }
    }

    val matchingPosts = remember(posts, query) {
        if (query.isBlank()) posts
        else posts.filter {
            it.content.contains(query, ignoreCase = true) ||
                it.authorName.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                placeholder = { Text("Search students, teachers, or posts") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("explore_search_input")
            )

            if (matchingUsers.isNotEmpty()) {
                Text(
                    text = if (query.isBlank()) "Suggested People" else "Matching People",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(matchingUsers, key = { it.uid }) { member ->
                        val isFollowing = currentUser.following.contains(member.uid)
                        Surface(
                            modifier = Modifier
                                .width(140.dp)
                                .animateItem()
                                .clickable { onOpenUserProfile(member.uid) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                UserAvatar(
                                    src = member.avatar,
                                    name = member.name,
                                    role = member.role,
                                    isVerified = member.isVerified,
                                    isBlackSociety = member.isBlackSociety,
                                    blackSocietyNumber = member.blackSocietyNumber,
                                    isClassRepresentative = member.isClassRepresentative,
                                    size = AvatarSize.LG
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = if (member.isBlackSociety) "Member - ${member.blackSocietyNumber ?: 1}" else member.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (member.isVerified && !member.isBlackSociety) {
                                        VerifiedBadgeIcon(size = 13.dp)
                                    }
                                }
                                Text(
                                    text = member.grade ?: member.role.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (member.uid != currentUser.uid) {
                                    Button(
                                        onClick = { onToggleFollow(member.uid) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = if (isFollowing) {
                                            ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.surface,
                                                contentColor = MaterialTheme.colorScheme.onSurface
                                            )
                                        } else {
                                            ButtonDefaults.buttonColors(containerColor = InstaBlue, contentColor = Color.White)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(32.dp)
                                    ) {
                                        Text(
                                            text = if (isFollowing) "Following" else "Follow",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 92.dp),
                horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                verticalArrangement = Arrangement.spacedBy(1.5.dp)
            ) {
                items(matchingPosts, key = { it.id }) { post ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .animateItem()
                            .clickable { selectedPost = post }
                    ) {
                        if (post.images.isNotEmpty()) {
                            CampusImage(
                                urlOrDataUri = post.images.first(),
                                contentDescription = post.content,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
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
                        if (post.images.firstOrNull()?.let { MediaUtils.isVideoMediaUri(it) } == true) {
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
                                contentDescription = "Multiple items",
                                tint = Color.White,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(16.dp)
                            )
                        } else if (post.isPinned) {
                            Icon(
                                Icons.Filled.PushPin,
                                contentDescription = null,
                                tint = InstaBlue,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(15.dp)
                            )
                        } else if (post.isOfficial) {
                            Icon(
                                Icons.Filled.Verified,
                                contentDescription = null,
                                tint = Amber500,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    selectedPost?.let { post ->
        val fresh = posts.firstOrNull { it.id == post.id } ?: post
        Dialog(
            onDismissRequest = { selectedPost = null },
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
                        selectedPost = null
                    },
                    onOpenAuthorProfile = { uid ->
                        selectedPost = null
                        onOpenUserProfile(uid)
                    },
                    onMessageAuthor = { uid ->
                        selectedPost = null
                        onMessageAuthor(uid)
                    },
                    onReportPost = { onReportPost(fresh) },
                    onUpdatePost = { content, cat, ratio, track, startSec ->
                        onUpdatePost(fresh, content, cat, ratio, track, startSec)
                    },
                    onAddToStory = {
                        selectedPost = null
                        onAddPostToStory(fresh)
                    },
                    isAutoPlayActive = isScreenActive
                )
            }
        }
    }
}
