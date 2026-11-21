package com.bsdcampus.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = InstaBlue,
    onPrimary = Color.White,
    primaryContainer = Blue50,
    onPrimaryContainer = InstaBlueDark,
    secondary = InstaIndigo,
    onSecondary = Color.White,
    secondaryContainer = LightSecondaryBg,
    onSecondaryContainer = Color.Black,
    tertiary = InstaPurple,
    onTertiary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = LightSecondaryBg,
    onSurfaceVariant = LightSecondaryText,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Slate50,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Slate100,
    surfaceContainerHighest = Slate200,
    outline = LightBorder,
    outlineVariant = Color(0xFFEDEDED),
    error = InstaLikeRed,
    onError = Color.White,
    inverseSurface = Color(0xFF121212),
    inverseOnSurface = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = InstaBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0A2540),
    onPrimaryContainer = Color(0xFF66BDFF),
    secondary = InstaIndigo,
    onSecondary = Color.White,
    secondaryContainer = DarkBubble,
    onSecondaryContainer = Color.White,
    tertiary = InstaPurple,
    onTertiary = Color.White,
    background = PitchBlack,
    onBackground = Color.White,
    surface = PitchBlack,
    onSurface = Color.White,
    surfaceVariant = DarkElevated,
    onSurfaceVariant = DarkSecondaryText,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = PitchBlack,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkElevated,
    surfaceContainerHighest = DarkBubble,
    outline = DarkBorder,
    outlineVariant = DarkElevated,
    error = InstaLikeRed,
    onError = Color.White,
    inverseSurface = Color.White,
    inverseOnSurface = Color.Black
)

val BSDCampusShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun BSDCampusTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = BSDCampusTypography,
        shapes = BSDCampusShapes,
        content = content
    )
}
