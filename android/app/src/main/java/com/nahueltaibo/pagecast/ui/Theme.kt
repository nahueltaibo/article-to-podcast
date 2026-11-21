package com.nahueltaibo.pagecast.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nahueltaibo.pagecast.data.ThemeMode

// Neutral surfaces with near-white or near-black actions. Tertiary is the one accent: it marks figures.
private val DarkColors = darkColorScheme(
    primary = Color(0xFFEEF0F2),
    onPrimary = Color(0xFF0E1012),
    primaryContainer = Color(0xFF2A2F36),
    onPrimaryContainer = Color(0xFFEEF0F2),
    inversePrimary = Color(0xFF14171A),
    secondary = Color(0xFFA2A9B2),
    onSecondary = Color(0xFF0E1012),
    secondaryContainer = Color(0xFF20242A),
    onSecondaryContainer = Color(0xFFEEF0F2),
    tertiary = Color(0xFFF0B24A),
    onTertiary = Color(0xFF1A1203),
    tertiaryContainer = Color(0xFF3A2A08),
    onTertiaryContainer = Color(0xFFF6CF8A),
    background = Color(0xFF0E1012),
    onBackground = Color(0xFFEEF0F2),
    surface = Color(0xFF0E1012),
    onSurface = Color(0xFFEEF0F2),
    surfaceVariant = Color(0xFF20242A),
    onSurfaceVariant = Color(0xFFA2A9B2),
    surfaceTint = Color(0xFFEEF0F2),
    inverseSurface = Color(0xFFEEF0F2),
    inverseOnSurface = Color(0xFF14171A),
    error = Color(0xFFF5A08A),
    onError = Color(0xFF3A1F1A),
    errorContainer = Color(0xFF3A1F1A),
    onErrorContainer = Color(0xFFF5A08A),
    outline = Color(0xFF4A515B),
    outlineVariant = Color(0xFF20242A),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF2A2F36),
    surfaceDim = Color(0xFF0E1012),
    surfaceContainerLowest = Color(0xFF0A0B0D),
    surfaceContainerLow = Color(0xFF171A1E),
    surfaceContainer = Color(0xFF171A1E),
    surfaceContainerHigh = Color(0xFF20242A),
    surfaceContainerHighest = Color(0xFF2A2F36),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF14171A),
    onPrimary = Color(0xFFF4F5F6),
    primaryContainer = Color(0xFFD5D9DE),
    onPrimaryContainer = Color(0xFF14171A),
    inversePrimary = Color(0xFFEEF0F2),
    secondary = Color(0xFF5A626C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE4E7EA),
    onSecondaryContainer = Color(0xFF14171A),
    tertiary = Color(0xFF9A5B00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFBE6C2),
    onTertiaryContainer = Color(0xFF5C3600),
    background = Color(0xFFF4F5F6),
    onBackground = Color(0xFF14171A),
    surface = Color(0xFFF4F5F6),
    onSurface = Color(0xFF14171A),
    surfaceVariant = Color(0xFFE4E7EA),
    onSurfaceVariant = Color(0xFF5A626C),
    surfaceTint = Color(0xFF14171A),
    inverseSurface = Color(0xFF14171A),
    inverseOnSurface = Color(0xFFF4F5F6),
    error = Color(0xFFB03A1E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFADDD5),
    onErrorContainer = Color(0xFF6E1F0C),
    outline = Color(0xFF9AA1AB),
    outlineVariant = Color(0xFFE4E7EA),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFE4E7EA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFE9ECEF),
    surfaceContainerHighest = Color(0xFFD5D9DE),
)

private val Base = Typography()

private val PagecastTypography = Typography(
    headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp),
    headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
    titleSmall = Base.titleSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
    bodyLarge = Base.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = 0.sp),
    bodyMedium = Base.bodyMedium.copy(letterSpacing = 0.sp),
    bodySmall = Base.bodySmall.copy(letterSpacing = 0.sp),
    labelLarge = Base.labelLarge.copy(letterSpacing = 0.sp),
    labelMedium = Base.labelMedium.copy(letterSpacing = 0.sp),
    labelSmall = Base.labelSmall.copy(fontSize = 12.sp, letterSpacing = 0.sp),
)

private val PagecastShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun isDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun PagecastTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = PagecastTypography,
        shapes = PagecastShapes,
        content = content,
    )
}
