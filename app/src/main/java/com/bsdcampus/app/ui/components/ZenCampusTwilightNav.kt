package com.bsdcampus.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsdcampus.app.model.CampusTab
import com.bsdcampus.app.model.CampusUser
import com.bsdcampus.app.ui.theme.InstaLikeRed

private data class NavDockItem(
    val tab: CampusTab,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val badge: Int = 0
)

@Composable
fun ZenCampusTwilightNav(
    activeTab: CampusTab,
    unreadNotificationsCount: Int,
    isAdmin: Boolean,
    currentUser: CampusUser? = null,
    onSelectTab: (CampusTab) -> Unit,
    onOpenRadialMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = buildList {
        add(NavDockItem(CampusTab.FEED, "Home", Icons.Outlined.Home, Icons.Filled.Home))
        add(NavDockItem(CampusTab.EXPLORE, "Discover", Icons.Outlined.Explore, Icons.Filled.Explore))
        add(NavDockItem(CampusTab.CREATE, "Create", Icons.Outlined.AddBox, Icons.Filled.AddBox))
        add(NavDockItem(CampusTab.REELS, "Reels", Icons.Outlined.SmartDisplay, Icons.Filled.SmartDisplay))
        add(NavDockItem(CampusTab.ACTIVITY, "Activity", Icons.Outlined.FavoriteBorder, Icons.Filled.Favorite, badge = unreadNotificationsCount))
        add(NavDockItem(CampusTab.PROFILE, "Profile", Icons.Outlined.Person, Icons.Filled.Person))
        if (isAdmin) {
            add(NavDockItem(CampusTab.ADMIN, "Admin", Icons.Outlined.Shield, Icons.Filled.Shield))
        }
    }

    val badgePulse = rememberInfiniteTransition(label = "dock_badge_pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "dock_badge_scale"
    ).value

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = item.tab == activeTab
                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.1f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "nav_icon_scale"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .clickable {
                                if (isSelected && item.tab == CampusTab.FEED) {
                                    onOpenRadialMenu()
                                } else {
                                    onSelectTab(item.tab)
                                }
                            }
                            .testTag("nav_${item.tab.route}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (item.tab == CampusTab.PROFILE && currentUser != null) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .scale(iconScale)
                                        .clip(CircleShape)
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(1.75.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                                            } else {
                                                Modifier.border(0.75.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                            }
                                        )
                                        .padding(1.5.dp)
                                ) {
                                    CampusImage(
                                        urlOrDataUri = currentUser.avatar,
                                        contentDescription = "Profile",
                                        fallbackText = currentUser.name,
                                        modifier = Modifier
                                            .size(25.dp)
                                            .clip(CircleShape)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.icon,
                                    contentDescription = item.label,
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.onBackground
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier
                                        .size(25.dp)
                                        .scale(iconScale)
                                )
                            }

                            if (item.badge > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 6.dp, y = (-3).dp)
                                        .scale(badgePulse)
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(InstaLikeRed),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (item.badge > 9) "9+" else "${item.badge}",
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
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
