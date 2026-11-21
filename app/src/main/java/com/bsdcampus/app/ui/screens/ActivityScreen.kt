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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bsdcampus.app.model.CampusNotification
import com.bsdcampus.app.model.CampusUser
import com.bsdcampus.app.ui.components.AvatarSize
import com.bsdcampus.app.ui.components.UserAvatar
import com.bsdcampus.app.ui.components.formatRelativeTime
import com.bsdcampus.app.ui.theme.Amber500
import com.bsdcampus.app.ui.theme.Blue600
import com.bsdcampus.app.ui.theme.Emerald500
import com.bsdcampus.app.ui.theme.Rose500

@Composable
fun ActivityScreen(
    notifications: List<CampusNotification>,
    usersById: Map<String, CampusUser> = emptyMap(),
    onMarkAllRead: () -> Unit,
    onOpenSenderProfile: (String) -> Unit,
    onBack: (() -> Unit)? = null
) {
    val unreadIds = notifications.filter { !it.read && it.id.isNotBlank() }.map { it.id }
    val unreadCount = unreadIds.size

    LaunchedEffect(unreadIds) {
        if (unreadIds.isNotEmpty()) {
            onMarkAllRead()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 700.dp)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                    Column(modifier = Modifier.padding(start = if (onBack == null) 4.dp else 0.dp)) {
                        Text("Notifications", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        Text(
                            text = if (unreadCount > 0) "$unreadCount new updates" else "All caught up",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (notifications.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Outlined.NotificationsNone, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No campus activity yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Likes, comments, new followers, and official school notices will show here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notifications, key = { it.id }) { notif ->
                        val (badgeIcon, badgeColor) = when (notif.type) {
                            "like" -> Icons.Filled.Favorite to Rose500
                            "comment" -> Icons.Outlined.ChatBubbleOutline to Blue600
                            "follow" -> Icons.Filled.PersonAdd to Emerald500
                            "announcement" -> Icons.Filled.Campaign to Amber500
                            else -> Icons.Outlined.Security to MaterialTheme.colorScheme.primary
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .clickable { onOpenSenderProfile(notif.senderId) },
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = 0.75.dp,
                                color = if (!notif.read) Blue600.copy(alpha = 0.45f)
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val liveSender = usersById[notif.senderId]
                                val resolvedAvatar = when {
                                    liveSender?.isBlackSociety == true -> null
                                    liveSender != null -> liveSender.avatar
                                    else -> notif.senderAvatar
                                }
                                val resolvedName = when {
                                    liveSender?.isBlackSociety == true -> "Member - ${liveSender.blackSocietyNumber ?: 1}"
                                    liveSender != null -> liveSender.name.ifBlank { notif.senderName }
                                    else -> notif.senderName
                                }
                                Box {
                                    UserAvatar(
                                        src = resolvedAvatar,
                                        name = resolvedName,
                                        role = liveSender?.role ?: "student",
                                        isVerified = liveSender?.let { it.isVerified || it.isClassRepresentative || it.role == "admin" } ?: false,
                                        isBlackSociety = liveSender?.isBlackSociety == true,
                                        blackSocietyNumber = liveSender?.blackSocietyNumber,
                                        isClassRepresentative = liveSender?.isClassRepresentative == true,
                                        size = AvatarSize.MD
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(badgeColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(badgeIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(notif.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(notif.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatRelativeTime(notif.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (!notif.read) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Blue600)
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
