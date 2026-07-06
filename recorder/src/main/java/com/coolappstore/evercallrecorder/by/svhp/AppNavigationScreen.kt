package com.coolappstore.evercallrecorder.by.svhp

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coolappstore.evercallrecorder.by.svhp.data.AppPreferences
import com.coolappstore.evercallrecorder.by.svhp.onboarding.OnboardingStatus
import com.coolappstore.evercallrecorder.by.svhp.system.fetchLatestRelease
import com.coolappstore.evercallrecorder.by.svhp.system.isNewerVersion
import com.coolappstore.evercallrecorder.by.svhp.system.enqueueApkDownload
import com.coolappstore.evercallrecorder.by.svhp.system.installApkAndScheduleDelete
import com.coolappstore.evercallrecorder.by.svhp.system.getApkDestinationFile
import com.coolappstore.evercallrecorder.by.svhp.system.isApkReadyToInstall
import com.coolappstore.evercallrecorder.by.svhp.system.saveDownloadedVersion
import com.coolappstore.evercallrecorder.by.svhp.ui.screens.DisclaimerScreen
import com.coolappstore.evercallrecorder.by.svhp.ui.screens.HomeScreen
import com.coolappstore.evercallrecorder.by.svhp.ui.screens.AppLockScreen
import com.coolappstore.evercallrecorder.by.svhp.ui.screens.InAppWebViewScreen
import com.coolappstore.evercallrecorder.by.svhp.ui.screens.PermissionsScreen
import com.coolappstore.evercallrecorder.by.svhp.ui.screens.PlaybackScreen
import com.coolappstore.evercallrecorder.by.svhp.ui.screens.SettingsScreen
import com.coolappstore.evercallrecorder.by.svhp.ui.screens.WelcomeDialog
import com.coolappstore.evercallrecorder.by.svhp.ui.theme.ShizucallrecorderTheme
import com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels.AppLockViewModel
import com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels.AppNavigationViewModel
import com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels.RecordingItem
import com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels.SettingsViewModel
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay

private enum class AppScreen { Disclaimer, Permissions, Home }
private enum class SubScreen(val depth: Int) { None(0), Settings(1), Playback(1), WebView(2) }

private val NavEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
private const val DURATION_IN  = 460
private const val DURATION_OUT = 340

private fun enterTransition() =
    slideInHorizontally(tween(DURATION_IN, easing = NavEasing)) { (it * 0.20f).toInt() } +
    fadeIn(tween(DURATION_IN, easing = NavEasing))

private fun exitTransition() =
    slideOutHorizontally(tween(DURATION_OUT, easing = NavEasing)) { -(it * 0.08f).toInt() } +
    fadeOut(tween(DURATION_OUT - 40, easing = NavEasing))

private fun popEnterTransition() =
    slideInHorizontally(tween(DURATION_IN, easing = NavEasing)) { -(it * 0.20f).toInt() } +
    fadeIn(tween(DURATION_IN, easing = NavEasing))

private fun popExitTransition() =
    slideOutHorizontally(tween(DURATION_OUT, easing = NavEasing)) { (it * 0.08f).toInt() } +
    fadeOut(tween(DURATION_OUT - 40, easing = NavEasing))

@Composable
fun AppNavigationScreen(openSettingsDirectly: Boolean = false) {
    val activityContext = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val appNavViewModel: AppNavigationViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val appLockViewModel: AppLockViewModel = viewModel()
    val onboardingStatus by appNavViewModel.onboardingStatus.collectAsState()
    val settingsUpdateTrigger by settingsViewModel.updateTrigger.collectAsState()
    val isAppLockUnlocked by appLockViewModel.isUnlocked.collectAsState()
    val preferences = settingsViewModel.preferences

    var subScreen by rememberSaveable { mutableStateOf(if (openSettingsDirectly) SubScreen.Settings else SubScreen.None) }
    var selectedRecording by remember { mutableStateOf<RecordingItem?>(null) }
    var highlightQuery by remember { mutableStateOf("") }
    var webViewUrl by remember { mutableStateOf("") }
    var webViewEnableDownloads by remember { mutableStateOf(false) }
    var webViewBottomPadding by remember { mutableStateOf(24) } // dp value

    val goBack: () -> Unit = {
        // When this Activity was launched to jump straight to Settings (Ever Dialer's
        // Recordings tab → gear icon), there is no "Home" screen of this module to fall back
        // to — this whole Activity only exists for that Settings shortcut. Pressing back from
        // Settings in that case should finish the Activity and return to Ever Dialer's own
        // Recordings tab, not reveal this module's own (separate/duplicate) recordings list.
        if (subScreen == SubScreen.Settings && openSettingsDirectly) {
            (activityContext as? Activity)?.finish()
        } else {
            subScreen = SubScreen.None
        }
    }

    // ── Auto update check on startup ──────────────────────────────────────────
    var showAutoUpdateDialog    by remember { mutableStateOf(false) }
    var autoUpdateVersion       by remember { mutableStateOf("") }
    var autoUpdateApkUrl        by remember { mutableStateOf<String?>(null) }
    var autoDownloadId          by remember { mutableStateOf<Long?>(null) }
    var autoDownloadProgress    by remember { mutableFloatStateOf(0f) }
    var showAutoDownloadProgress by remember { mutableStateOf(false) }

    // ── First-launch welcome dialog ───────────────────────────────────────────
    var showWelcomeDialog by remember { mutableStateOf(!preferences.isWelcomeShown()) }

    LaunchedEffect(Unit) {
        if (preferences.isAutoUpdateCheckEnabled()) {
            val release = fetchLatestRelease(AppUrls.GITHUB_API_RELEASES)
            if (release != null) {
                val appVersion = try {
                    @Suppress("DEPRECATION")
                    activityContext.packageManager
                        .getPackageInfo(activityContext.packageName, 0).versionName ?: "0"
                } catch (_: Exception) { "0" }
                if (isNewerVersion(release.tagName, appVersion)) {
                    // If APK for this version is already downloaded, skip straight to install
                    if (isApkReadyToInstall(activityContext, release.tagName)) {
                        autoUpdateVersion = release.tagName
                        autoDownloadId = null
                        autoDownloadProgress = 1f
                        showAutoDownloadProgress = false
                        installApkAndScheduleDelete(activityContext, getApkDestinationFile())
                    } else {
                        autoUpdateVersion = release.tagName
                        autoUpdateApkUrl  = release.apkUrl
                        showAutoUpdateDialog = true
                    }
                }
            }
        }
    }

    if (showAutoDownloadProgress) {
        val dlId = autoDownloadId
        if (dlId != null) {
            LaunchedEffect(dlId) {
                val dm = activityContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                while (true) {
                    delay(300)
                    val query  = DownloadManager.Query().setFilterById(dlId)
                    val cursor = dm.query(query)
                    if (!cursor.moveToFirst()) { cursor.close(); break }
                    val dmStatus   = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total      = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    cursor.close()
                    when (dmStatus) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            showAutoDownloadProgress = false
                            autoDownloadId = null
                            saveDownloadedVersion(activityContext, autoUpdateVersion)
                            installApkAndScheduleDelete(activityContext, getApkDestinationFile())
                            break
                        }
                        DownloadManager.STATUS_FAILED -> {
                            showAutoDownloadProgress = false
                            autoDownloadId = null
                            break
                        }
                        else -> {
                            autoDownloadProgress = if (total > 0L)
                                (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
                        }
                    }
                }
            }
        }
    }

    if (showAutoUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showAutoUpdateDialog = false },
            icon  = { Icon(Icons.Outlined.SystemUpdate, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Update Available") },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Version v$autoUpdateVersion is available.")
                    Text("Would you like to download and install it now?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(onClick = {
                    showAutoUpdateDialog = false
                    val url = autoUpdateApkUrl
                    if (url != null) {
                        val id = enqueueApkDownload(activityContext, url)
                        if (id != null) {
                            autoDownloadId = id
                            autoDownloadProgress = 0f
                            showAutoDownloadProgress = true
                        }
                    }
                }) { Text("Download") }
            },
            dismissButton = {
                TextButton(onClick = { showAutoUpdateDialog = false }) { Text("Not Now") }
            }
        )
    }

    if (showAutoDownloadProgress) {
        Dialog(onDismissRequest = {}) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(Icons.Outlined.SystemUpdate, null, tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp))
                    Text("Downloading Update", style = MaterialTheme.typography.titleMedium)
                    Text("v$autoUpdateVersion", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(progress = { autoDownloadProgress },
                            modifier = Modifier.fillMaxWidth())
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${(autoDownloadProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Please wait…",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(settingsUpdateTrigger) {
        val newStatus = OnboardingStatus.getStatus(activityContext, preferences)
        if (newStatus != onboardingStatus) appNavViewModel.refresh()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    appNavViewModel.refresh()
                    settingsViewModel.refresh()
                }
                Lifecycle.Event.ON_STOP -> {
                    // isChangingConfigurations is true during rotation / other config changes —
                    // don't re-lock in that case, only when the app genuinely goes to background.
                    val isConfigChange = (activityContext as? Activity)?.isChangingConfigurations == true
                    if (!isConfigChange) appLockViewModel.lock()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val darkTheme = when (preferences.getThemeMode()) {
        AppPreferences.ThemeMode.LIGHT, AppPreferences.ThemeMode.WHITE -> false
        AppPreferences.ThemeMode.DARK,  AppPreferences.ThemeMode.BLACK -> true
        AppPreferences.ThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppPreferences.ThemeMode.AUTO_WB -> isSystemInDarkTheme()
    }

    // Respect the user's dynamic color preference; fall back to accent color when off
    val isDynamicColor = remember(settingsUpdateTrigger) { preferences.isDynamicColorEnabled() }
    val accentArgb     = remember(settingsUpdateTrigger) { preferences.getAccentColor() }

    val systemIsDark = isSystemInDarkTheme()
    // Pure white/black override: background is pure white/black, but accent/dynamic still applies
    val isPureWhite = preferences.getThemeMode() == AppPreferences.ThemeMode.WHITE ||
                      (preferences.getThemeMode() == AppPreferences.ThemeMode.AUTO_WB && !systemIsDark)
    val isPureBlack = preferences.getThemeMode() == AppPreferences.ThemeMode.BLACK ||
                      (preferences.getThemeMode() == AppPreferences.ThemeMode.AUTO_WB && systemIsDark)
    val resolvedAccentArgb: Int? = if (!isDynamicColor) accentArgb else null
    val resolvedDynamicColor = isDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    ShizucallrecorderTheme(
        darkTheme    = darkTheme,
        dynamicColor = resolvedDynamicColor,
        accentArgb   = resolvedAccentArgb,
        isPureWhite  = isPureWhite,
        isPureBlack  = isPureBlack
    ) {
        // ── Fix status bar icon colours to match in-app theme ─────────────
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as Activity).window
                WindowInsetsControllerCompat(window, view).apply {
                    isAppearanceLightStatusBars     = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }

        // Auto-update dialogs are inside the theme so they use the correct dynamic colours
        if (showAutoUpdateDialog) {
            AlertDialog(
                onDismissRequest = { showAutoUpdateDialog = false },
                icon  = { Icon(Icons.Outlined.SystemUpdate, null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("Update Available") },
                text  = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Version v$autoUpdateVersion is available.")
                        Text("Would you like to download and install it now?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        showAutoUpdateDialog = false
                        val url = autoUpdateApkUrl
                        if (url != null) {
                            val id = enqueueApkDownload(activityContext, url)
                            if (id != null) {
                                autoDownloadId = id
                                autoDownloadProgress = 0f
                                showAutoDownloadProgress = true
                            }
                        }
                    }) { Text("Download") }
                },
                dismissButton = {
                    TextButton(onClick = { showAutoUpdateDialog = false }) { Text("Not Now") }
                }
            )
        }

        if (showAutoDownloadProgress) {
            Dialog(onDismissRequest = {}) {
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(Icons.Outlined.SystemUpdate, null, tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp))
                        Text("Downloading Update", style = MaterialTheme.typography.titleMedium)
                        Text("v$autoUpdateVersion", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()) {
                            LinearProgressIndicator(progress = { autoDownloadProgress },
                                modifier = Modifier.fillMaxWidth())
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${(autoDownloadProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Please wait…",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // ── First-launch welcome dialog ───────────────────────────────────────

        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            val screen = resolveScreen(onboardingStatus)
            val isAppLocked = screen == AppScreen.Home && preferences.isAppLockEnabled() && !isAppLockUnlocked
            BackHandler(enabled = subScreen != SubScreen.None && !isAppLocked) { goBack() }

            when (screen) {
                AppScreen.Disclaimer -> DisclaimerScreen(
                    onContinue = {
                        preferences.setDisclaimerAccepted(true)
                        appNavViewModel.refresh()
                    }
                )
                AppScreen.Permissions -> PermissionsScreen(
                    status = onboardingStatus,
                    onPermissionGranted = { appNavViewModel.refresh() }
                )
                AppScreen.Home -> {
                    if (isAppLocked) {
                        AppLockScreen(
                            method = preferences.getAppLockMethod(),
                            onVerifySecret = { secret -> preferences.verifyAppLockSecret(secret) },
                            onUnlocked = { appLockViewModel.unlock() }
                        )
                    } else {
                    AnimatedContent(
                        targetState = subScreen,
                        transitionSpec = {
                            val pushing = targetState.depth >= initialState.depth
                            if (pushing) enterTransition() togetherWith exitTransition()
                            else popEnterTransition() togetherWith popExitTransition()
                        },
                        label = "SubScreen"
                    ) { target ->
                        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                            when (target) {
                                SubScreen.Settings -> SettingsScreen(
                            viewModel = settingsViewModel,
                            onBack = goBack,
                            onOpenWebView = { url, enableDownloads, extraBottomDp ->
                                webViewUrl = url
                                webViewEnableDownloads = enableDownloads
                                webViewBottomPadding = extraBottomDp
                                subScreen = SubScreen.WebView
                            }
                        )
                                SubScreen.WebView -> InAppWebViewScreen(
                                    url = webViewUrl,
                                    onBack = goBack,
                                    enableDownloads = webViewEnableDownloads,
                                    backButtonBottomPadding = webViewBottomPadding.dp
                                )
                                SubScreen.Playback -> {
                                    val rec = selectedRecording
                                    if (rec != null) PlaybackScreen(recording = rec, onBack = goBack, highlightQuery = highlightQuery)
                                }
                                SubScreen.None -> HomeScreen(
                                    appVersion = settingsViewModel.getAppVersion(),
                                    onSettingsClick = { subScreen = SubScreen.Settings },
                                    onRecordingClick = { recording, query ->
                                        selectedRecording = recording
                                        highlightQuery = query
                                        subScreen = SubScreen.Playback
                                    }
                                )
                            }
                        }
                    }

                    if (showWelcomeDialog) {
                        WelcomeDialog(
                            onDismiss = {
                                preferences.setWelcomeShown(true)
                                showWelcomeDialog = false
                            }
                        )
                    }
                    }
                }
            }
        }
    }
}

private fun resolveScreen(status: OnboardingStatus.Status): AppScreen = when {
    !status.disclaimerAccepted -> AppScreen.Disclaimer
    !status.isComplete()       -> AppScreen.Permissions
    else                       -> AppScreen.Home
}
