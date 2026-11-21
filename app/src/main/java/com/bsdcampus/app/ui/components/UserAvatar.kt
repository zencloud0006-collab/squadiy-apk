package com.bsdcampus.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsdcampus.app.model.CampusConstants
import com.bsdcampus.app.ui.theme.AdminBlack
import com.bsdcampus.app.ui.theme.AdminRed500
import com.bsdcampus.app.ui.theme.AdminRed600
import com.bsdcampus.app.ui.theme.AdminRed700
import com.bsdcampus.app.ui.theme.Gold300
import com.bsdcampus.app.ui.theme.Gold400
import com.bsdcampus.app.ui.theme.Gold500
import com.bsdcampus.app.ui.theme.Gold600
import com.bsdcampus.app.ui.theme.InstaBlue
import com.bsdcampus.app.ui.theme.InstaIndigo
import com.bsdcampus.app.ui.theme.InstaOrange
import com.bsdcampus.app.ui.theme.InstaPink
import com.bsdcampus.app.ui.theme.InstaPurple
import com.bsdcampus.app.ui.theme.JetBrainsMonoFamily
import com.bsdcampus.app.ui.theme.Stone900
import com.bsdcampus.app.ui.theme.Zinc700
import com.bsdcampus.app.ui.theme.Zinc800
import com.bsdcampus.app.ui.theme.Zinc900

enum class AvatarSize(val dp: Dp, val badgeDp: Dp) {
    XS(24.dp, 10.dp),
    SM(32.dp, 13.dp),
    MD(38.dp, 15.dp),
    LG(56.dp, 20.dp),
    XL(84.dp, 24.dp)
}

@Composable
fun VerifiedBadgeIcon(
    modifier: Modifier = Modifier,
    size: Dp = 15.dp
) {
    Icon(
        imageVector = Icons.Filled.Verified,
        contentDescription = "Verified Blue Tick",
        tint = InstaBlue,
        modifier = modifier.size(size)
    )
}

@Composable
fun UserAvatar(
    src: String?,
    name: String,
    role: String = "student",
    isVerified: Boolean = false,
    isBlackSociety: Boolean = false,
    blackSocietyNumber: Int? = null,
    isClassRepresentative: Boolean = false,
    showStoryRing: Boolean = false,
    showSeenStoryRing: Boolean = false,
    size: AvatarSize = AvatarSize.MD,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isAdmin = role == "admin"
    val transition = rememberInfiniteTransition(label = "avatar_ring")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rotation"
    )

    val ringColors = when {
        isAdmin -> listOf(AdminRed600, Color(0xFF050505), AdminRed500, Color.Black, AdminRed700, AdminRed600)
        isClassRepresentative -> listOf(Gold400, Color.White, Gold500, Gold300, Color.White, Gold600, Gold400)
        isBlackSociety -> listOf(Color.White, Zinc900, Color.Black, Zinc700, Color.White)
        showStoryRing -> listOf(InstaOrange, InstaPink, InstaPurple, InstaIndigo, InstaOrange)
        showSeenStoryRing -> listOf(Color(0xFF737373), Color(0xFF525252), Color(0xFF737373))
        else -> null
    }

    val containerModifier = modifier
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    Box(
        modifier = containerModifier,
        contentAlignment = Alignment.Center
    ) {
        val outerSize = if (ringColors != null) size.dp + 7.dp else size.dp

        Box(
            modifier = Modifier.size(outerSize),
            contentAlignment = Alignment.Center
        ) {
            if (ringColors != null) {
                Canvas(modifier = Modifier.size(outerSize)) {
                    rotate(degrees = if ((showStoryRing || showSeenStoryRing) && !isAdmin && !isClassRepresentative && !isBlackSociety) 0f else angle) {
                        drawCircle(
                            brush = Brush.sweepGradient(colors = ringColors)
                        )
                    }
                }
            }

            val innerBg = when {
                isAdmin -> AdminBlack
                isClassRepresentative -> Stone900
                isBlackSociety -> Color.Black
                else -> MaterialTheme.colorScheme.background
            }

            Box(
                modifier = Modifier
                    .size(if (ringColors != null) size.dp + 3.dp else size.dp)
                    .clip(CircleShape)
                    .background(innerBg)
                    .padding(if (ringColors != null) 2.dp else 0.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isBlackSociety) {
                    Box(
                        modifier = Modifier
                            .size(size.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Zinc800, Color.Black)
                                )
                            )
                            .border(1.dp, Zinc700, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (size == AvatarSize.XL || size == AvatarSize.LG) {
                                "#${blackSocietyNumber ?: 1}"
                            } else {
                                "♠"
                            },
                            color = Color.White,
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = when (size) {
                                AvatarSize.XS -> 10.sp
                                AvatarSize.SM -> 12.sp
                                AvatarSize.MD -> 14.sp
                                AvatarSize.LG -> 15.sp
                                AvatarSize.XL -> 22.sp
                            }
                        )
                    }
                } else {
                    CampusImage(
                        urlOrDataUri = src ?: CampusConstants.defaultAvatarFor(name),
                        contentDescription = "$name avatar",
                        fallbackText = name,
                        modifier = Modifier
                            .size(size.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (ringColors == null) 0.75.dp else 0.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                    )
                }
            }

            when {
                isAdmin -> {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 1.dp, y = 1.dp)
                            .size(size.badgeDp)
                            .clip(CircleShape)
                            .background(AdminRed600)
                            .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = "Admin",
                            tint = Color.White,
                            modifier = Modifier.size(size.badgeDp * 0.62f)
                        )
                    }
                }
                isClassRepresentative -> {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 1.dp, y = 1.dp)
                            .size(size.badgeDp)
                            .clip(CircleShape)
                            .background(InstaBlue)
                            .border(1.5.dp, Gold400, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Class Representative",
                            tint = Color.White,
                            modifier = Modifier.size(size.badgeDp * 0.65f)
                        )
                    }
                }
                isBlackSociety -> {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 1.dp, y = 1.dp)
                            .size(size.badgeDp)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "♠",
                            color = Color.White,
                            fontSize = (size.badgeDp.value * 0.55f).sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                isVerified -> {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 1.dp, y = 1.dp)
                            .size(size.badgeDp)
                            .clip(CircleShape)
                            .background(InstaBlue)
                            .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Verified",
                            tint = Color.White,
                            modifier = Modifier.size(size.badgeDp * 0.65f)
                        )
                    }
                }
            }
        }
    }
}
