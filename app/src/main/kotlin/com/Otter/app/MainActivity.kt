package com.Otter.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.animation.doOnEnd
import android.animation.ObjectAnimator
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.Otter.app.player.PlayerConnectionManager
import com.Otter.app.ui.components.AudioMiniPlayer
import com.Otter.app.ui.components.SyncProgressSnackbar
import com.Otter.app.ui.download.DownloadViewModel
import com.Otter.app.ui.download.configure.DownloadDialogViewModel
import com.Otter.app.ui.download.configure.DownloadErrorSnackbar
import com.Otter.app.ui.download.configure.ConfigureFormatsSheet
import com.Otter.app.ui.download.configure.PlaylistSelectionSheet
import com.Otter.app.ui.navigation.BottomNavItem
import com.Otter.app.ui.navigation.OtterBottomNavigation
import com.Otter.app.ui.screens.CrashScreen
import com.Otter.app.ui.screens.DownloadScreen
import com.Otter.app.ui.screens.PlayerScreen
import com.Otter.app.ui.screens.PlaylistDetailScreen
import com.Otter.app.ui.screens.PlaylistScreen
import com.Otter.app.ui.screens.WebViewLoginScreen
import com.Otter.app.ui.screens.settings.AboutSettings
import com.Otter.app.ui.screens.settings.AppearanceSettingsScreen
import com.Otter.app.ui.screens.settings.BackupAndRestore
import com.Otter.app.ui.screens.settings.ChangelogScreen
import com.Otter.app.ui.screens.settings.ContentSettings
import com.Otter.app.ui.screens.settings.ContributorsScreen
import com.Otter.app.ui.screens.settings.CookieTargetsScreen
import com.Otter.app.ui.screens.settings.DiagnosticsSettings
import com.Otter.app.ui.screens.settings.DownloadSettings
import com.Otter.app.ui.screens.settings.NotificationSettings
import com.Otter.app.ui.screens.settings.PowerSaverSettings
import com.Otter.app.ui.screens.settings.PrivacySettings
import com.Otter.app.ui.screens.settings.ProfilesSettings
import com.Otter.app.ui.screens.settings.SettingsScreen
import com.Otter.app.ui.screens.settings.StorageSettings
import com.Otter.app.ui.screens.settings.UpdatesSettings
import com.Otter.app.ui.session.AppSessionStore
import com.Otter.app.ui.theme.OtterTheme
import com.Otter.app.ui.viewmodels.SettingsViewModel
import com.Otter.app.ui.viewmodels.SyncViewModel
import com.Otter.app.util.CrashReportManager
import com.Otter.app.util.YtdlpUpdater
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        const val ACTION_RESTART_APP = "com.Otter.app.RESTART_APP"

        fun restartApp(context: Context) {
            val launchIntent =
                context.packageManager.getLaunchIntentForPackage(context.packageName)
                    ?.apply {
                        action = ACTION_RESTART_APP
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    ?: Intent(context, MainActivity::class.java).apply {
                        action = ACTION_RESTART_APP
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }

            context.startActivity(launchIntent)
            Process.killProcess(Process.myPid())
            exitProcess(0)
        }
    }

    @UnstableApi
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            val fadeOut = ObjectAnimator.ofFloat(
                splashScreenViewProvider.view,
                View.ALPHA,
                1f,
                0f
            )
            fadeOut.duration = 120L
            fadeOut.doOnEnd { splashScreenViewProvider.remove() }
            fadeOut.start()
        }
        
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.isNavigationBarContrastEnforced = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        setContent {
            Otter(onRestartApp = { restartApp(this) })
        }
    }
}

@Composable
@UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
fun Otter(onRestartApp: () -> Unit = {}) {
    val context = LocalContext.current
    val entryPoint =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            DiagnosticsEntryPoint::class.java,
        )
    val crashReportManager = entryPoint.crashReportManager()
    val playerConnectionManager = entryPoint.playerConnectionManager()
    val appSessionStore = entryPoint.appSessionStore()
    var showCrashScreen by remember { mutableStateOf<Boolean>(crashReportManager.hasCrash()) }

    DisposableEffect(Unit) {
        playerConnectionManager.connect()
        onDispose { playerConnectionManager.disconnect() }
    }

    if (showCrashScreen) {
        CrashScreen(
            onRestart = {
                crashReportManager.clearCrash()
                showCrashScreen = false
                onRestartApp()
            },
            onDismiss = {
                crashReportManager.clearCrash()
                showCrashScreen = false
            },
        )
        return
    }

    val navController = rememberNavController()
    val startDestination = BottomNavItem.Downloads.route
    var selectedRoute by remember { mutableStateOf(BottomNavItem.Downloads.route) }

    var postFirstFrame by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (postFirstFrame) return@LaunchedEffect
        withFrameNanos { }
        postFirstFrame = true
    }

    val scope = rememberCoroutineScope()
    var didRestoreTopLevelRoute by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (didRestoreTopLevelRoute) return@LaunchedEffect
        val restored = appSessionStore.lastTopLevelRoute.firstOrNull()
        val route =
            restored?.let {
                when (it) {
                    "home" -> BottomNavItem.Playlists.route
                    BottomNavItem.Playlists.route,
                    BottomNavItem.Downloads.route,
                    BottomNavItem.Settings.route,
                    -> it
                    else -> null
                }
            }
        if (route != null && route != BottomNavItem.Downloads.route) {
            selectedRoute = route
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
        didRestoreTopLevelRoute = true
    }

    val navBackStackEntry by navController.currentBackStackEntryFlow.collectAsState(null)

    LaunchedEffect(navBackStackEntry) {
        val parentRoute =
            navBackStackEntry?.destination?.route?.let { currentRoute ->
                when (currentRoute) {
                    BottomNavItem.Playlists.route,
                    BottomNavItem.Downloads.route,
                    BottomNavItem.Settings.route,
                    -> currentRoute
                    else -> {
                        selectedRoute ?: BottomNavItem.Downloads.route
                    }
                }
            } ?: BottomNavItem.Downloads.route

        selectedRoute = parentRoute
        if (didRestoreTopLevelRoute) {
            scope.launch { appSessionStore.setLastTopLevelRoute(parentRoute) }
        }
    }

    val currentRoute = navBackStackEntry?.destination?.route
    val bottomBarVisible by remember(currentRoute) {
        derivedStateOf {
            currentRoute == BottomNavItem.Playlists.route ||
                currentRoute == BottomNavItem.Downloads.route ||
                currentRoute == BottomNavItem.Settings.route
        }
    }

    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settings by settingsViewModel.settings.collectAsState()

    ApplyHighRefreshRate(enabled = settings.highRefreshRate && !settings.powerSaverEnabled)
    ApplyKeepScreenOn(enabled = settings.keepScreenOn)

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val isDarkTheme =
        when (settings.themeMode) {
            com.Otter.app.data.models.ThemeMode.DARK -> true
            com.Otter.app.data.models.ThemeMode.LIGHT -> false
            com.Otter.app.data.models.ThemeMode.SYSTEM -> isSystemInDarkTheme
        }

    val themeColor = Color(settings.seedColor.toInt())

    val dialogViewModel: DownloadDialogViewModel = hiltViewModel()
    val downloadViewModel: DownloadViewModel = hiltViewModel()
    val selectionState by dialogViewModel.selectionStateFlow.collectAsState()
    val sheetState by dialogViewModel.sheetStateFlow.collectAsState()

    var pendingPlaylistUrl by remember { mutableStateOf<String?>(null) }
    val playlistSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val formatSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showFormatSheet by remember { mutableStateOf(false) }

    LaunchedEffect(selectionState) {
        showFormatSheet =
            when (selectionState) {
                is DownloadDialogViewModel.SelectionState.Loading,
                is DownloadDialogViewModel.SelectionState.FormatSelection,
                -> true
                else -> false
            }
        if (!showFormatSheet) {
            formatSheetState.hide()
        }

        if (selectionState is DownloadDialogViewModel.SelectionState.Loading ||
            selectionState is DownloadDialogViewModel.SelectionState.FormatSelection
        ) {
            pendingPlaylistUrl = null
            playlistSheetState.hide()
        }
    }

    OtterTheme(
        darkTheme = isDarkTheme,
        useDynamicColor = settings.useDynamicColor,
        themeColor = themeColor,
        pureBlack = settings.pureBlack,
        monochromeTheme = settings.monochromeTheme,
        expressive = settings.expressive,
    ) {
        YtdlpUpdater()
        val isPlayerScreen = currentRoute?.startsWith("player/") == true
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (isPlayerScreen) Color.Black else MaterialTheme.colorScheme.background,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                androidx.compose.material3.Scaffold(
                    containerColor = if (isPlayerScreen) Color.Black else MaterialTheme.colorScheme.background,
                    contentColor = if (isPlayerScreen) Color.Black else MaterialTheme.colorScheme.onBackground,
                    bottomBar = {
                        OtterBottomNavigation(
                            selectedRoute = selectedRoute,
                            visible = bottomBarVisible,
                            onItemSelected = { route ->
                                selectedRoute = route
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    },
                ) { padding ->
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = if (isPlayerScreen) Modifier.fillMaxSize() else Modifier.padding(padding),
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec =
                                    tween(
                                        durationMillis = 350,
                                        easing = FastOutSlowInEasing,
                                    ),
                            ) +
                                fadeIn(
                                    animationSpec =
                                        tween(
                                            durationMillis = 300,
                                            easing = LinearOutSlowInEasing,
                                        ),
                                )
                        },
                        exitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { -it },
                                animationSpec =
                                    tween(
                                        durationMillis = 350,
                                        easing = FastOutSlowInEasing,
                                    ),
                            ) +
                                fadeOut(
                                    animationSpec =
                                        tween(
                                            durationMillis = 300,
                                            easing = LinearEasing,
                                        ),
                                )
                        },
                        popEnterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { -it },
                                animationSpec =
                                    tween(
                                        durationMillis = 350,
                                        easing = FastOutSlowInEasing,
                                    ),
                            ) +
                                fadeIn(
                                    animationSpec =
                                        tween(
                                            durationMillis = 300,
                                            easing = LinearOutSlowInEasing,
                                        ),
                                )
                        },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec =
                                    tween(
                                        durationMillis = 350,
                                        easing = FastOutSlowInEasing,
                                    ),
                            ) +
                                fadeOut(
                                    animationSpec =
                                        tween(
                                            durationMillis = 300,
                                            easing = LinearEasing,
                                        ),
                                )
                        },
                    ) {
                        composable(BottomNavItem.Playlists.route) {
                            PlaylistScreen(
                                navController = navController,
                                onShowPlaylistSheet = { url ->
                                    pendingPlaylistUrl = url
                                },
                            )
                        }
                        composable(BottomNavItem.Downloads.route) {
                            DownloadScreen(navController = navController)
                        }
                        composable(BottomNavItem.Settings.route) {
                            SettingsScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onNavigate = { route -> navController.navigate(route) },
                            )
                        }

                        composable("appearanceSettings") {
                            AppearanceSettingsScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable("contentSettings") {
                            ContentSettings(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable("profilesSettings") {
                            ProfilesSettings(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable(
                            route = "cookieTargets?profileId={profileId}",
                            arguments =
                                listOf(
                                    navArgument("profileId") {
                                        type = NavType.StringType
                                        nullable = false
                                    },
                                ),
                        ) {
                            val profileId = it.arguments?.getString("profileId") ?: return@composable
                            CookieTargetsScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                profileId = profileId,
                            )
                        }

                        composable("notificationSettings") {
                            NotificationSettings(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable("powerSaverSettings") {
                            PowerSaverSettings(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable("privacySettings") {
                            PrivacySettings(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable("aboutSettings") {
                            AboutSettings(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable("contributors") {
                            ContributorsScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable("diagnosticsSettings") {
                            DiagnosticsSettings(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable("storageSettings") {
                            StorageSettings(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable("downloadSettings") {
                            DownloadSettings(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable("backupAndRestore") {
                            BackupAndRestore(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onRestartApp = onRestartApp,
                            )
                        }

                        composable("updatesSettings") {
                            UpdatesSettings(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                            )
                        }

                        

                        composable("changelog") {
                            ChangelogScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                initialTag = null,
                            )
                        }

                        composable(
                            route = "changelog/{tag}",
                            arguments =
                                listOf(
                                    navArgument("tag") {
                                        type = NavType.StringType
                                        nullable = false
                                    },
                                ),
                        ) {
                            val tag = it.arguments?.getString("tag")
                            ChangelogScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                initialTag = tag,
                            )
                        }

                        composable(
                            route = "webview_login?profileId={profileId}&targetId={targetId}",
                            arguments =
                                listOf(
                                    navArgument("profileId") {
                                        type = NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    },
                                    navArgument("targetId") {
                                        type = NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    },
                                ),
                        ) {
                            val profileId = it.arguments?.getString("profileId")
                            val targetId = it.arguments?.getString("targetId")
                            WebViewLoginScreen(
                                onBack = { navController.popBackStack() },
                                onLoginComplete = { navController.popBackStack() },
                                profileId = profileId,
                                targetId = targetId,
                            )
                        }

                        composable("playlist/{playlistId}") {
                            val playlistId = it.arguments?.getString("playlistId")
                            if (playlistId == null) return@composable
                            PlaylistDetailScreen(
                                playlistId = playlistId,
                                navController = navController,
                                onShowPlaylistSheet = { url ->
                                    pendingPlaylistUrl = url
                                },
                            )
                        }

                        composable("player/{videoId}") {
                            val videoId = it.arguments?.getString("videoId")
                            if (videoId == null) return@composable
                            PlayerScreen(
                                videoId = videoId,
                                navController = navController,
                                playerConnectionManager = playerConnectionManager,
                            )
                        }
                    }
                }

                // Bottom overlay stack (sync progress + audio mini player)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    val bottomOverlayPadding = if (bottomBarVisible) 71.dp else 10.dp
                    val isAudioOnlySession by playerConnectionManager.isAudioOnlySession.collectAsState()

                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = bottomOverlayPadding),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (sheetState is DownloadDialogViewModel.SheetState.Error) {
                            val err = sheetState as DownloadDialogViewModel.SheetState.Error
                            DownloadErrorSnackbar(
                                throwable = err.throwable,
                                onDismiss = { dialogViewModel.postAction(DownloadDialogViewModel.Action.Reset) },
                                onRetry = { dialogViewModel.postAction(err.action) },
                                modifier = Modifier.padding(bottom = 12.dp),
                            )
                        }

                        if (postFirstFrame) {
                            val syncViewModel: SyncViewModel = hiltViewModel()
                            val syncState by syncViewModel.syncState.collectAsState()
                            SyncProgressSnackbar(
                                syncState = syncState,
                                onDismiss = { syncViewModel.dismissSyncSnackbar() },
                            )
                        }

                        if (settings.showMiniPlayerInAudioMode && isAudioOnlySession) {
                            AudioMiniPlayer(
                                playerConnectionManager = playerConnectionManager,
                            )
                        }
                    }
                }

                pendingPlaylistUrl?.let { url ->
                    PlaylistSelectionSheet(
                        url = url,
                        sheetState = playlistSheetState,
                        onDismissRequest = {
                            pendingPlaylistUrl = null
                            dialogViewModel.postAction(DownloadDialogViewModel.Action.Reset)
                        },
                    )
                }

                when (val st = selectionState) {
                    is DownloadDialogViewModel.SelectionState.Loading ->
                        if (showFormatSheet) {
                            ConfigureFormatsSheet(
                                sheetState = formatSheetState,
                                info = com.Otter.app.data.download.VideoInfo(),
                                basePreferences = st.preferences,
                                downloader = downloadViewModel.downloader,
                                onDismissRequest = {
                                    dialogViewModel.postAction(DownloadDialogViewModel.Action.Reset)
                                },
                                isLoading = true,
                            )
                        }

                    is DownloadDialogViewModel.SelectionState.FormatSelection ->
                        if (showFormatSheet) {
                            ConfigureFormatsSheet(
                                sheetState = formatSheetState,
                                info = st.info,
                                basePreferences = st.preferences,
                                downloader = downloadViewModel.downloader,
                                onDismissRequest = {
                                    dialogViewModel.postAction(DownloadDialogViewModel.Action.Reset)
                                },
                                isLoading = false,
                                playlistResult = st.playlistResult,
                                selectedIndices = st.selectedIndices,
                            )
                        }

                    else -> Unit
                }
            }
        }
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface DiagnosticsEntryPoint {
    fun crashReportManager(): CrashReportManager

    fun playerConnectionManager(): PlayerConnectionManager

    fun appSessionStore(): AppSessionStore
}

@Composable
private fun ApplyHighRefreshRate(enabled: Boolean) {
    val view = LocalView.current
    val activity = view.context as? Activity ?: return

    LaunchedEffect(enabled) {
        val window = activity.window
        val attributes = window.attributes

        if (!enabled) {
            attributes.preferredDisplayModeId = 0
            window.attributes = attributes
            return@LaunchedEffect
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return@LaunchedEffect

        val display =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.display
            } else {
                @Suppress("DEPRECATION")
                activity.windowManager.defaultDisplay
            }

        val bestMode = display?.supportedModes?.maxByOrNull { it.refreshRate }
        if (bestMode != null) {
            attributes.preferredDisplayModeId = bestMode.modeId
            window.attributes = attributes
        }
    }
}

@Composable
private fun ApplyKeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    val activity = view.context as? Activity ?: return

    LaunchedEffect(enabled) {
        if (enabled) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
