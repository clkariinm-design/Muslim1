package com.nuur.muslim.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute {
    @Serializable
    data object Home : AppRoute

    @Serializable
    data object Surahs : AppRoute

    @Serializable
    data class Reader(val surahNumber: Int, val targetAyah: Int = 1) : AppRoute

    @Serializable
    data object Search : AppRoute

    @Serializable
    data object Study : AppRoute

    @Serializable
    data class Tools(val initialTab: Int = 0) : AppRoute

    @Serializable
    data object Settings : AppRoute
}
