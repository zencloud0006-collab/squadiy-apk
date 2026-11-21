package com.bsdcampus.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bsdcampus.app.model.CampusTab
import com.bsdcampus.app.ui.theme.Blue600
import com.bsdcampus.app.ui.theme.Emerald600
import com.bsdcampus.app.ui.theme.InstaPink
import com.bsdcampus.app.ui.theme.Purple600
import com.bsdcampus.app.ui.theme.Rose500

private data class QuickLauncherItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tint: Color,
    val onClick: () -> Unit
)

@Composable
fun RadialActionMenu(
    visible: Boolean,
    onDismiss: () -> Unit,
    onNavigate: (CampusTab) -> Unit,
    onOpenStoryCreator: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.92f),
        exit = fadeOut() + scaleOut(targetScale = 0.92f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .clickable(onClick = onDismiss)
                .padding(24.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            val actions = listOf(
                QuickLauncherItem("Share Story", "24h photo or video for followers", Icons.Filled.AddCircleOutline, InstaPink) {
                    onDismiss()
                    onOpenStoryCreator()
                },
                QuickLauncherItem("Create Post", "Share campus updates or photos", Icons.Outlined.AddPhotoAlternate, Blue600) {
                    onDismiss()
                    onNavigate(CampusTab.CREATE)
                },
                QuickLauncherItem("Reels", "Watch short campus videos", Icons.Outlined.SmartDisplay, Purple600) {
                    onDismiss()
                    onNavigate(CampusTab.REELS)
                },
                QuickLauncherItem("Direct Chat", "Message students or faculty", Icons.Outlined.Forum, Emerald600) {
                    onDismiss()
                    onNavigate(CampusTab.CHAT)
                },
                QuickLauncherItem("Discover", "Explore posts & directory", Icons.Outlined.Explore, Purple600) {
                    onDismiss()
                    onNavigate(CampusTab.EXPLORE)
                },
                QuickLauncherItem("Activity", "View alerts & engagement", Icons.Outlined.Notifications, Rose500) {
                    onDismiss()
                    onNavigate(CampusTab.ACTIVITY)
                }
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 84.dp)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline),
                tonalElevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quick Campus Launcher",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable(onClick = onDismiss)
                                .testTag("radial_close"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Close quick menu", modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    actions.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(onClick = item.onClick)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(item.tint, item.tint.copy(alpha = 0.75f))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(item.icon, contentDescription = item.title, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
