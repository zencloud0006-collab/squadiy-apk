package com.bsdcampus.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsdcampus.app.model.CampusTab
import com.bsdcampus.app.model.CampusUser
import com.bsdcampus.app.ui.theme.InstaLikeRed

@Composable
fun AppleTopBar(
    currentUser: CampusUser,
    isAdmin: Boolean,
    activeTab: CampusTab,
    profileTitle: String = currentUser.name,
    unreadNotificationsCount: Int,
    unreadMessagesCount: Int = 0,
    onNavigate: (CampusTab) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenChat: () -> Unit = {}
) {
    val pulseTransition = rememberInfiniteTransition(label = "notif_pulse")
    val badgeScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badge_scale"
    )

    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onNavigate(CampusTab.FEED) }
                        .padding(horizontal = 2.dp, vertical = 2.dp)
                        .testTag("top_bar_brand"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        AnimatedContent(
                            targetState = activeTab to profileTitle,
                            transitionSpec = {
                                fadeIn(tween(180)).togetherWith(fadeOut(tween(120)))
                            },
                            label = "top_bar_title"
                        ) { (tab, resolvedProfileTitle) ->
                            Text(
                                text = when (tab) {
                                    CampusTab.FEED -> "Squadify"
                                    CampusTab.EXPLORE -> "Discover"
                                    CampusTab.CREATE -> "New Post"
                                    CampusTab.REELS -> "Reels"
                                    CampusTab.CHAT -> "Messages"
                                    CampusTab.ACTIVITY -> "Notifications"
                                    CampusTab.PROFILE -> resolvedProfileTitle.ifBlank { currentUser.name }
                                    CampusTab.CONNECTIONS -> "Connections"
                                    CampusTab.EDIT_PROFILE -> "Edit Profile"
                                    CampusTab.SETTINGS -> "Settings & Privacy"
                                    CampusTab.ADMIN -> "Admin Console"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.4).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "Unofficial • Not affiliated with BSD Public School",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box {
                        IconButton(
                            onClick = onOpenNotifications,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("top_bar_notifications")
                        ) {
                            Icon(
                                imageVector = if (activeTab == CampusTab.ACTIVITY) {
                                    Icons.Filled.Favorite
                                } else {
                                    Icons.Outlined.FavoriteBorder
                                },
                                contentDescription = "Notifications",
                                tint = if (activeTab == CampusTab.ACTIVITY) {
                                    InstaLikeRed
                                } else {
                                    MaterialTheme.colorScheme.onBackground
                                },
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        if (unreadNotificationsCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 8.dp, end = 8.dp)
                                    .scale(badgeScale)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(InstaLikeRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadNotificationsCount > 9) "9+" else "$unreadNotificationsCount",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Box {
                        IconButton(
                            onClick = onOpenChat,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("top_bar_chat")
                        ) {
                            Icon(
                                imageVector = if (activeTab == CampusTab.CHAT) {
                                    Icons.Filled.Forum
                                } else {
                                    Icons.Outlined.Forum
                                },
                                contentDescription = "Messages",
                                tint = if (activeTab == CampusTab.CHAT) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onBackground
                                },
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        if (unreadMessagesCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 8.dp, end = 8.dp)
                                    .scale(badgeScale)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(InstaLikeRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadMessagesCount > 9) "9+" else "$unreadMessagesCount",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (isAdmin) {
                        IconButton(
                            onClick = { onNavigate(CampusTab.ADMIN) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("top_bar_admin")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Shield,
                                contentDescription = "Admin",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                }
            }
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
