package com.nuur.muslim.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object NuurSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

object NuurElevation {
    val flat = 0.dp
    val card = 2.dp
    val floating = 8.dp
}

object NuurMotion {
    const val SHORT_DURATION_MS = 200
    const val MEDIUM_DURATION_MS = 280
    val gentleSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
}

enum class ReadingTheme(val id: String) {
    DEFAULT("default"),
    EMERALD_NIGHT("emerald_night"),
    WARM_SEPIA("warm_sepia"),
    AMOLED_DARK("amoled_dark"),
    CLASSIC_PARCHMENT("classic_parchment");

    companion object {
        fun fromId(id: String): ReadingTheme = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

data class ReaderThemePalette(
    val background: Color,
    val cardSurface: Color,
    val cardBorder: Color,
    val arabicText: Color,
    val translationText: Color,
    val secondaryText: Color,
    val accent: Color
)

fun readerPaletteFor(
    theme: ReadingTheme,
    fallbackBg: Color,
    fallbackSurface: Color,
    fallbackBorder: Color,
    fallbackOnSurface: Color,
    fallbackMuted: Color,
    fallbackAccent: Color
): ReaderThemePalette = when (theme) {
    ReadingTheme.DEFAULT -> ReaderThemePalette(
        background = fallbackBg,
        cardSurface = fallbackSurface,
        cardBorder = fallbackBorder,
        arabicText = fallbackOnSurface,
        translationText = fallbackOnSurface,
        secondaryText = fallbackMuted,
        accent = fallbackAccent
    )
    ReadingTheme.EMERALD_NIGHT -> ReaderThemePalette(
        background = EmeraldDeepest,
        cardSurface = EmeraldSurface,
        cardBorder = EmeraldBorder,
        arabicText = IvoryText,
        translationText = IvoryText,
        secondaryText = SageMuted,
        accent = GoldPrimary
    )
    ReadingTheme.WARM_SEPIA -> ReaderThemePalette(
        background = SepiaBackground,
        cardSurface = SepiaSurface,
        cardBorder = SepiaBorder,
        arabicText = SepiaInk,
        translationText = SepiaInk,
        secondaryText = SepiaMuted,
        accent = AntiqueGold
    )
    ReadingTheme.AMOLED_DARK -> ReaderThemePalette(
        background = AmoledBlack,
        cardSurface = AmoledSurface,
        cardBorder = AmoledBorder,
        arabicText = IvoryText,
        translationText = IvoryText,
        secondaryText = SageMuted,
        accent = GoldLight
    )
    ReadingTheme.CLASSIC_PARCHMENT -> ReaderThemePalette(
        background = ParchmentBackground,
        cardSurface = ParchmentSurface,
        cardBorder = ParchmentBorder,
        arabicText = EmeraldInk,
        translationText = EmeraldInk,
        secondaryText = EmeraldInkMuted,
        accent = AntiqueGold
    )
}
