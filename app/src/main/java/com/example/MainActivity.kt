package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.localization.AppLanguage
import com.example.localization.stringsFor
import com.example.ui.components.MiniAudioPlayerBar
import com.example.ui.navigation.AppRoute
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudyHubScreen
import com.example.ui.screens.SurahListScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.NuurQuranTheme
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.DataInitState
import com.example.ui.viewmodel.ToolsViewModel

class MainActivity : ComponentActivity() {

    private val appViewModel: AppViewModel by viewModels {
        AppViewModel.Factory(application as NuurQuranApplication)
    }
    private val toolsViewModel: ToolsViewModel by viewModels {
        ToolsViewModel.Factory(application as NuurQuranApplication)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition {
            appViewModel.initState.value is DataInitState.Loading
        }
        enableEdgeToEdge()

        setContent {
            val settings by appViewModel.settings.collectAsStateWithLifecycle()
            val initState by appViewModel.initState.collectAsStateWithLifecycle()
            val strings = remember(settings.language) { stringsFor(settings.language) }
            val layoutDirection = if (settings.language == AppLanguage.ARABIC) {
                LayoutDirection.Rtl
            } else {
                LayoutDirection.Ltr
            }

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                NuurQuranTheme(themeMode = settings.themeMode) {
                    when (val state = initState) {
                        DataInitState.Loading -> {
                            Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(padding),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(14.dp),
                                        modifier = Modifier.padding(24.dp)
                                    ) {
                                        CircularProgressIndicator()
                                        Text(
                                            text = strings.loadingData,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }
                        }
                        is DataInitState.Error -> {
                            Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(padding),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.padding(24.dp)
                                    ) {
                                        Text(state.message, color = MaterialTheme.colorScheme.error)
                                        Button(onClick = { appViewModel.initializeDataset() }) {
                                            Text(strings.retry)
                                        }
                                    }
                                }
                            }
                        }
                        is DataInitState.Ready -> {
                            NuurQuranAppShell(
                                appViewModel = appViewModel,
                                toolsViewModel = toolsViewModel,
                                manifest = state.manifest
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class TopNavItem(
    val route: AppRoute,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

@Composable
private fun NuurQuranAppShell(
    appViewModel: AppViewModel,
    toolsViewModel: ToolsViewModel,
    manifest: com.example.data.local.DataManifest
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    val settings by appViewModel.settings.collectAsStateWithLifecycle()
    val strings = remember(settings.language) { stringsFor(settings.language) }
    val surahs by appViewModel.surahs.collectAsStateWithLifecycle()
    val bookmarkedGlobals by appViewModel.bookmarkedGlobalAyahs.collectAsStateWithLifecycle()
    val bookmarks by appViewModel.bookmarks.collectAsStateWithLifecycle()
    val notes by appViewModel.notes.collectAsStateWithLifecycle()
    val readingPlans by appViewModel.readingPlans.collectAsStateWithLifecycle()
    val quranicDuas by appViewModel.quranicDuas.collectAsStateWithLifecycle()
    val downloadedCounts by appViewModel.downloadedCounts.collectAsStateWithLifecycle()
    val audioState by appViewModel.audioState.collectAsStateWithLifecycle()
    val searchQuery by appViewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by appViewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by appViewModel.isSearching.collectAsStateWithLifecycle()
    val onlineTafsirState by appViewModel.onlineTafsirState.collectAsStateWithLifecycle()

    val adhkarList by toolsViewModel.adhkarList.collectAsStateWithLifecycle()
    val adhkarCounters by toolsViewModel.adhkarCounters.collectAsStateWithLifecycle()
    val deviceAzimuth by toolsViewModel.deviceAzimuth.collectAsStateWithLifecycle()
    val hasCompassSensor by toolsViewModel.hasCompassSensor.collectAsStateWithLifecycle()
    val locationFeedback by toolsViewModel.locationFeedback.collectAsStateWithLifecycle()

    val navItems = listOf(
        TopNavItem(AppRoute.Home, strings.navHome, Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
        TopNavItem(AppRoute.Surahs, strings.navQuran, Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook, "nav_surahs"),
        TopNavItem(AppRoute.Search, strings.navSearch, Icons.Filled.Search, Icons.Outlined.Search, "nav_search"),
        TopNavItem(AppRoute.Study, strings.navStudy, Icons.Filled.Bookmarks, Icons.Outlined.Bookmarks, "nav_study"),
        TopNavItem(AppRoute.Tools(), strings.navTools, Icons.Filled.Explore, Icons.Outlined.Explore, "nav_tools"),
        TopNavItem(AppRoute.Settings, strings.navSettings, Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
    )

    val isReaderRoute = currentDest?.hasRoute<AppRoute.Reader>() == true

    fun navigateTop(route: AppRoute) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openReader(surahNum: Int, ayahNum: Int) {
        navController.navigate(AppRoute.Reader(surahNumber = surahNum, targetAyah = ayahNum))
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    var askedNotificationPermission by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    fun ensurePlaybackNotificationRequested() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !askedNotificationPermission) {
            askedNotificationPermission = true
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                Column {
                    MiniAudioPlayerBar(
                        audioState = audioState,
                        strings = strings,
                        onTogglePlayPause = { appViewModel.togglePlayPause() },
                        onPrevious = { appViewModel.previousAyah() },
                        onNext = { appViewModel.nextAyah() },
                        onSeek = { appViewModel.seekTo(it) },
                        onCycleRepeat = { appViewModel.cycleRepeatMode() },
                        onDismiss = { appViewModel.dismissAudio() },
                        onOpenReader = { s, a -> openReader(s, a) }
                    )
                    if (!useRail && !isReaderRoute) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            navItems.forEach { item ->
                                val selected = when (item.route) {
                                    is AppRoute.Tools -> currentDest?.hasRoute<AppRoute.Tools>() == true
                                    else -> currentDest?.hasRoute(item.route::class) == true
                                }
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTop(item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = item.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier.testTag(item.tag)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail && !isReaderRoute) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        Spacer(modifier = Modifier.height(12.dp))
                        navItems.forEach { item ->
                            val selected = when (item.route) {
                                is AppRoute.Tools -> currentDest?.hasRoute<AppRoute.Tools>() == true
                                else -> currentDest?.hasRoute(item.route::class) == true
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTop(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = { Text(item.label, maxLines = 1) },
                                modifier = Modifier.testTag(item.tag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = AppRoute.Home,
                    modifier = Modifier.weight(1f)
                ) {
                    composable<AppRoute.Home> {
                        HomeScreen(
                            settings = settings,
                            strings = strings,
                            surahs = surahs,
                            readingPlans = readingPlans,
                            onOpenReader = { s, a -> openReader(s, a) },
                            onBrowseSurahs = { navigateTop(AppRoute.Surahs) },
                            onOpenToolsTab = { tab ->
                                navController.navigate(AppRoute.Tools(initialTab = tab))
                            },
                            onOpenStudy = { navigateTop(AppRoute.Study) }
                        )
                    }

                    composable<AppRoute.Surahs> {
                        SurahListScreen(
                            surahs = surahs,
                            downloadedCounts = downloadedCounts,
                            audioState = audioState,
                            strings = strings,
                            onOpenSurah = { s -> openReader(s, 1) },
                            onDownloadSurah = { surah -> appViewModel.downloadSurahAudio(surah) },
                            onCancelDownload = { appViewModel.cancelSurahDownload() }
                        )
                    }

                    composable<AppRoute.Reader> { entry ->
                        val route: AppRoute.Reader = entry.toRoute()
                        val surah by appViewModel.observeSurah(route.surahNumber)
                            .collectAsStateWithLifecycle(initialValue = null)
                        val ayahs by appViewModel.observeAyahs(route.surahNumber)
                            .collectAsStateWithLifecycle(initialValue = emptyList())

                        ReaderScreen(
                            surah = surah,
                            ayahs = ayahs,
                            targetAyah = route.targetAyah,
                            settings = settings,
                            strings = strings,
                            bookmarkedGlobals = bookmarkedGlobals,
                            downloadedCount = downloadedCounts[route.surahNumber] ?: 0,
                            audioState = audioState,
                            onlineTafsirState = onlineTafsirState,
                            onBack = { navController.popBackStack() },
                            onPlaySurah = { s, list, idx ->
                                ensurePlaybackNotificationRequested()
                                appViewModel.playSurah(s, list, idx)
                            },
                            onDownloadSurah = { s -> appViewModel.downloadSurahAudio(s) },
                            onCancelDownload = { appViewModel.cancelSurahDownload() },
                            onDeleteSurahAudio = { sNum -> appViewModel.deleteSurahAudio(sNum) },
                            onToggleBookmark = { ayah -> appViewModel.toggleBookmark(ayah) },
                            onSaveNote = { ayah, title, body ->
                                appViewModel.saveNote(ayah, 0L, title, body)
                            },
                            onMarkLastRead = { ayah ->
                                appViewModel.markLastRead(ayah.surahNumber, ayah.numberInSurah, ayah.globalNumber)
                            },
                            onResetTafsir = { appViewModel.resetOnlineTafsir() },
                            onLoadOnlineTafsir = { s, a -> appViewModel.loadOnlineIbnKathir(s, a) },
                            onArabicSizeChange = { appViewModel.setArabicFontSize(it) },
                            onTranslationSizeChange = { appViewModel.setTranslationFontSize(it) },
                            onToggleSomali = { appViewModel.setShowSomali(it) },
                            onToggleEnglish = { appViewModel.setShowEnglish(it) },
                            onToggleTajweed = { appViewModel.setShowTajweed(it) }
                        )
                    }

                    composable<AppRoute.Search> {
                        SearchScreen(
                            query = searchQuery,
                            results = searchResults,
                            isSearching = isSearching,
                            strings = strings,
                            onQueryChange = { appViewModel.updateSearchQuery(it) },
                            onOpenReader = { s, a -> openReader(s, a) }
                        )
                    }

                    composable<AppRoute.Study> {
                        StudyHubScreen(
                            settings = settings,
                            strings = strings,
                            surahs = surahs,
                            bookmarks = bookmarks,
                            notes = notes,
                            readingPlans = readingPlans,
                            downloadedCounts = downloadedCounts,
                            onOpenReader = { s, a -> openReader(s, a) },
                            onRemoveBookmark = { g, _, _ ->
                                appViewModel.removeBookmark(g)
                            },
                            onSaveEditedNote = { item, title, body ->
                                appViewModel.updateNote(
                                    noteId = item.note.id,
                                    globalAyahNumber = item.note.globalAyahNumber,
                                    surahNumber = item.note.surahNumber,
                                    ayahNumber = item.note.ayahNumber,
                                    title = title,
                                    body = body
                                )
                            },
                            onDeleteNote = { appViewModel.deleteNote(it) },
                            onCreatePlan = { title, days -> appViewModel.createReadingPlan(title, days) },
                            onDeletePlan = { appViewModel.deletePlan(it) },
                            onResetKhatam = { appViewModel.resetKhatam() },
                            onDeleteOfflineSurah = { appViewModel.deleteSurahAudio(it) }
                        )
                    }

                    composable<AppRoute.Tools> { entry ->
                        val route: AppRoute.Tools = entry.toRoute()
                        ToolsScreen(
                            initialTab = route.initialTab,
                            settings = settings,
                            strings = strings,
                            adhkarList = adhkarList,
                            adhkarCounters = adhkarCounters,
                            quranicDuas = quranicDuas,
                            deviceAzimuth = deviceAzimuth,
                            hasCompassSensor = hasCompassSensor,
                            locationFeedback = locationFeedback,
                            onSelectCity = { toolsViewModel.selectManualCity(it) },
                            onSelectMethod = { toolsViewModel.setCalculationMethod(it) },
                            onSelectTimezone = { toolsViewModel.setTimezone(it) },
                            onRequestDeviceLocation = { toolsViewModel.applyDeviceCoarseLocation() },
                            onLocationDenied = { toolsViewModel.setLocationDeniedMessage(it) },
                            onStartCompass = { toolsViewModel.startCompass() },
                            onStopCompass = { toolsViewModel.stopCompass() },
                            onIncrementDhikr = { o, max -> toolsViewModel.incrementDhikrCounter(o, max) },
                            onOpenReader = { s, a -> openReader(s, a) }
                        )
                    }

                    composable<AppRoute.Settings> {
                        SettingsScreen(
                            settings = settings,
                            strings = strings,
                            manifest = manifest,
                            onLanguageChange = { appViewModel.setLanguage(it) },
                            onThemeChange = { appViewModel.setThemeMode(it) },
                            onArabicSizeChange = { appViewModel.setArabicFontSize(it) },
                            onTranslationSizeChange = { appViewModel.setTranslationFontSize(it) },
                            onToggleSomali = { appViewModel.setShowSomali(it) },
                            onToggleEnglish = { appViewModel.setShowEnglish(it) },
                            onToggleTajweed = { appViewModel.setShowTajweed(it) },
                            onReciterChange = { appViewModel.setReciter(it) }
                        )
                    }
                }
            }
        }
    }
}
