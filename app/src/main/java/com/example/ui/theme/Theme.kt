package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = EmeraldDeepest,
    primaryContainer = GoldContainerDark,
    onPrimaryContainer = GoldLight,
    secondary = EmeraldSurfaceVariant,
    onSecondary = IvoryText,
    secondaryContainer = EmeraldSurface,
    onSecondaryContainer = GoldLight,
    tertiary = GoldLight,
    onTertiary = EmeraldDeepest,
    background = EmeraldBackground,
    onBackground = IvoryText,
    surface = EmeraldSurface,
    onSurface = IvoryText,
    surfaceVariant = EmeraldSurfaceVariant,
    onSurfaceVariant = SageMuted,
    outline = EmeraldBorder,
    outlineVariant = EmeraldBorder.copy(alpha = 0.55f),
    error = ErrorCoralDark,
    onError = EmeraldDeepest
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldInk,
    onPrimary = ParchmentSurface,
    primaryContainer = AntiqueGoldContainer,
    onPrimaryContainer = EmeraldInk,
    secondary = AntiqueGold,
    onSecondary = ParchmentSurface,
    secondaryContainer = ParchmentVariant,
    onSecondaryContainer = EmeraldInk,
    tertiary = AntiqueGold,
    onTertiary = ParchmentSurface,
    background = ParchmentBackground,
    onBackground = EmeraldInk,
    surface = ParchmentSurface,
    onSurface = EmeraldInk,
    surfaceVariant = ParchmentVariant,
    onSurfaceVariant = EmeraldInkMuted,
    outline = ParchmentBorder,
    outlineVariant = ParchmentBorder.copy(alpha = 0.6f),
    error = ErrorCoralLight,
    onError = ParchmentSurface
)

val NuurShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

enum class AppThemeMode(val id: String) {
    DARK("dark"),
    LIGHT("light"),
    SYSTEM("system");

    companion object {
        fun fromId(id: String): AppThemeMode = entries.firstOrNull { it.id == id } ?: DARK
    }
}

@Composable
fun NuurQuranTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (useDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NuurTypography,
        shapes = NuurShapes,
        content = content
    )
}
