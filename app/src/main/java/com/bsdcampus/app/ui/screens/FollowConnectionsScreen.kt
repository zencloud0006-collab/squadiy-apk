package com.bsdcampus.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsdcampus.app.model.CampusUser
import com.bsdcampus.app.ui.components.AvatarSize
import com.bsdcampus.app.ui.components.UserAvatar
import com.bsdcampus.app.ui.components.VerifiedBadgeIcon
import com.bsdcampus.app.ui.theme.Gold300
import com.bsdcampus.app.ui.theme.Gold400
import com.bsdcampus.app.ui.theme.InstaBlue
import com.bsdcampus.app.ui.theme.JetBrainsMonoFamily
import com.bsdcampus.app.ui.theme.Slate900
import com.bsdcampus.app.ui.theme.Zinc700
import com.bsdcampus.app.ui.theme.Zinc900
import kotlinx.coroutines.launch

@Composable
fun FollowConnectionsScreen(
    profileUser: CampusUser,
    currentUser: CampusUser,
    allUsers: List<CampusUser>,
    initialTabIndex: Int,
    onToggleFollow: (String) -> Unit,
    onOpenUserProfile: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = initialTabIndex.coerceIn(0, 1),
        pageCount = { 2 }
    )
    var searchQuery by remember { mutableStateOf("") }

    val usersById = remember(allUsers) { allUsers.associateBy { it.uid } }
    val followersList = remember(profileUser.followers, usersById) {
        profileUser.followers.mapNotNull { usersById[it] }
    }
    val followingList = remember(profileUser.following, usersById) {
        profileUser.following.mapNotNull { usersById[it] }
    }

    val displayProfileName = if (profileUser.isBlackSociety) {
        "Member - ${profileUser.blackSocietyNumber ?: 1}"
    } else {
        profileUser.name
    }
    val effectiveVerified = (profileUser.isVerified || profileUser.isClassRepresentative || profileUser.role == "admin") &&
        !profileUser.isBlackSociety

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
        ) {
            // Instagram-style Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("connections_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = displayProfileName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (effectiveVerified) {
                            VerifiedBadgeIcon(size = 15.dp)
                        }
                    }
                    Text(
                        text = profileUser.grade ?: profileUser.role.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Two-Tab Selector: Followers vs Following
            Row(modifier = Modifier.fillMaxWidth()) {
                val tabs = listOf(
                    "${followersList.size} Followers",
                    "${followingList.size} Following"
                )
                tabs.forEachIndexed { index, label ->
                    val selected = pagerState.currentPage == index
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            }
                            .padding(top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.75.dp)
                                .background(if (selected) MaterialTheme.colorScheme.onBackground else Color.Transparent)
                        )
                    }
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

            // Search Box
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search followers or following...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(InstaBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("connections_search_input")
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val rawList = if (page == 0) followersList else followingList
                val filtered = remember(rawList, searchQuery) {
                    if (searchQuery.isBlank()) rawList
                    else rawList.filter { user ->
                        user.name.contains(searchQuery, ignoreCase = true) ||
                            (user.grade?.contains(searchQuery, ignoreCase = true) == true) ||
                            user.role.contains(searchQuery, ignoreCase = true)
                    }
                }

                if (filtered.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Group,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No matching members" else if (page == 0) "No followers yet" else "Not following anyone yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) {
                                "Try another name or class."
                            } else if (page == 0) {
                                "Students and faculty who follow $displayProfileName will appear here."
                            } else {
                                "Members followed by $displayProfileName will appear here."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        items(filtered, key = { "${page}_${it.uid}" }) { member ->
                            val isFollowing = currentUser.following.contains(member.uid)
                            val memberVerified = (member.isVerified || member.isClassRepresentative || member.role == "admin") &&
                                !member.isBlackSociety
                            val memberName = if (member.isBlackSociety) {
                                "Member - ${member.blackSocietyNumber ?: 1}"
                            } else {
                                member.name
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenUserProfile(member.uid) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    UserAvatar(
                                        src = member.avatar,
                                        name = member.name,
                                        role = member.role,
                                        isVerified = memberVerified,
                                        isBlackSociety = member.isBlackSociety,
                                        blackSocietyNumber = member.blackSocietyNumber,
                                        isClassRepresentative = member.isClassRepresentative,
                                        size = AvatarSize.LG
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = memberName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (memberVerified) {
                                                VerifiedBadgeIcon(size = 14.dp)
                                            }
                                            if (member.isClassRepresentative) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Gold400,
                                                    border = BorderStroke(0.5.dp, Gold300)
                                                ) {
                                                    Text(
                                                        text = "CR",
                                                        color = Slate900,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Black,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            if (member.isBlackSociety) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Zinc900,
                                                    border = BorderStroke(0.5.dp, Zinc700)
                                                ) {
                                                    Text(
                                                        text = "♠",
                                                        color = Color.White,
                                                        fontSize = 8.sp,
                                                        fontFamily = JetBrainsMonoFamily,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = listOfNotNull(
                                                member.grade ?: member.role.replaceFirstChar { it.uppercase() },
                                                member.bio?.takeIf { it.isNotBlank() }?.take(36)
                                            ).joinToString(" · "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (member.uid != currentUser.uid) {
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Button(
                                        onClick = { onToggleFollow(member.uid) },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
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
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text(
                                            text = if (isFollowing) "Following" else "Follow",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold
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
}
