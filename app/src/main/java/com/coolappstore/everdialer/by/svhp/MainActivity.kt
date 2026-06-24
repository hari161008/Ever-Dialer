package com.coolappstore.everdialer.by.svhp

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.telecom.TelecomManager
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.coolappstore.everdialer.by.svhp.controller.CallService
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.enqueueApkDownload
import com.coolappstore.everdialer.by.svhp.controller.util.fetchLatestRelease
import com.coolappstore.everdialer.by.svhp.controller.util.getApkDestinationFile
import com.coolappstore.everdialer.by.svhp.controller.util.installApkAndScheduleDelete
import com.coolappstore.everdialer.by.svhp.controller.util.isNewerVersion
import com.coolappstore.everdialer.by.svhp.view.screen.CallActivity
import com.coolappstore.everdialer.by.svhp.view.components.Android14WelcomeDialog
import com.coolappstore.everdialer.by.svhp.view.components.TelegramJoinDialog
import com.coolappstore.everdialer.by.svhp.view.components.BottomBar
import com.coolappstore.everdialer.by.svhp.liquidglass.LocalLiquidGlassBackdrop
import com.coolappstore.everdialer.by.svhp.liquidglass.backdrops.rememberLayerBackdrop
import com.coolappstore.everdialer.by.svhp.liquidglass.backdrops.layerBackdrop
import com.coolappstore.everdialer.by.svhp.view.theme.Rivo4Theme
import com.coolappstore.everdialer.by.svhp.view.theme.TabTransitionStyle
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.ContactDetailsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.DialPadScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ContactEditScreenDestination
import kotlinx.coroutines.delay
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import android.view.Surface
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Note
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Person
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.ramcosta.composedestinations.generated.destinations.ContactScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FavoritesScreenDestination
import com.ramcosta.composedestinations.generated.destinations.NotesScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RecentScreenDestination
import org.koin.core.context.GlobalContext

class MainActivity : FragmentActivity() {

    private val requestRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ -> }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> /* permissions result; dialer popup now shown after welcome */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge() triggers Adreno GPU driver SIGSEGV on first RenderThread draw.
        // Edge-to-edge is set via theme XML instead (windowDrawsSystemBarBackgrounds etc).
        WindowCompat.setDecorFitsSystemWindows(window, false)

        requestRequiredPermissions()
        // On first launch, show default dialer prompt first; welcome dialog appears after.
        requestDefaultDialer()

        setContent {
            Rivo4Theme {
                val navController = rememberNavController()

                val prefs = remember {
                    GlobalContext.get().get<PreferenceManager>()
                }

                // ── Biometric app-lock ──────────────────────────────────────
                val settingsVer by prefs.settingsChanged.collectAsState()
                val biometricType = remember(settingsVer) {
                    prefs.getString(PreferenceManager.KEY_BIOMETRICS_TYPE, "") ?: ""
                }
                val appLockEnabled = remember(settingsVer) {
                    prefs.getBoolean(PreferenceManager.KEY_BIOMETRICS_APP_LOCK, false)
                }
                var isUnlocked by remember {
                    mutableStateOf(!(biometricType.isNotEmpty() && appLockEnabled))
                }

                // Compute start destination from prefs — done once so no flash
                val startDestination = remember {
                    when (prefs.getString(PreferenceManager.KEY_DEFAULT_TAB, "calls") ?: "calls") {
                        "favorites" -> FavoritesScreenDestination
                        "contacts"  -> ContactScreenDestination
                        "notes"     -> NotesScreenDestination
                        else        -> RecentScreenDestination
                    }
                }

                val isFirstLaunch = remember {
                    !prefs.getBoolean(PreferenceManager.KEY_FIRST_LAUNCH_DONE, false)
                }

                // ── First Launch Welcome Dialog ─────────────────────────────
                // Show AFTER the default dialer prompt (which fires in onCreate)
                var showWelcomeDialog by remember { mutableStateOf(false) }
                var showTelegramDialog by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    if (isFirstLaunch) {
                        // Small delay so the default dialer system dialog appears first
                        kotlinx.coroutines.delay(600)
                        showWelcomeDialog = true
                    } else if (!prefs.getBoolean(PreferenceManager.KEY_TELEGRAM_SHOWN, false)) {
                        // Welcome already done but Telegram dialog not yet shown — show it
                        kotlinx.coroutines.delay(800)
                        showTelegramDialog = true
                    }
                }

                if (showWelcomeDialog) {
                    Android14WelcomeDialog(
                        onAppInfo = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", packageName, null)
                            }
                            startActivity(intent)
                        },
                        onContinue = {
                            prefs.setBoolean(PreferenceManager.KEY_FIRST_LAUNCH_DONE, true)
                            showWelcomeDialog = false
                            requestDefaultDialer()
                            if (!prefs.getBoolean(PreferenceManager.KEY_TELEGRAM_SHOWN, false)) {
                                showTelegramDialog = true
                            }
                        }
                    )
                }

                // On subsequent launches, requestDefaultDialer is called in onCreate

                // ── Telegram Support Dialog ─────────────────────────────────
                if (showTelegramDialog) {
                    TelegramJoinDialog(
                        onJoin = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/EverlastingAndroidTweak"))
                            startActivity(intent)
                            prefs.setBoolean(PreferenceManager.KEY_TELEGRAM_SHOWN, true)
                            showTelegramDialog = false
                        },
                        onSkip = {
                            prefs.setBoolean(PreferenceManager.KEY_TELEGRAM_SHOWN, true)
                            showTelegramDialog = false
                        }
                    )
                }

                var autoUpdateVersion by remember { mutableStateOf<String?>(null) }
                var autoUpdateApkUrl by remember { mutableStateOf<String?>(null) }
                var showAutoUpdateDialog by remember { mutableStateOf(false) }
                var autoDownloadId by remember { mutableStateOf<Long?>(null) }
                var autoDownloadProgress by remember { mutableFloatStateOf(0f) }
                var showAutoDownloadProgress by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    val autoCheck = prefs.getBoolean(PreferenceManager.KEY_AUTO_UPDATE_CHECK, true)
                    if (autoCheck) {
                        val release = fetchLatestRelease(GITHUB_API_RELEASES)
                        if (release != null && isNewerVersion(release.tagName, APP_VERSION)) {
                            autoUpdateVersion = release.tagName
                            autoUpdateApkUrl = release.apkUrl
                            showAutoUpdateDialog = true
                        }
                    }
                }

                if (showAutoDownloadProgress) {
                    val dlId = autoDownloadId
                    if (dlId != null) {
                        LaunchedEffect(dlId) {
                            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            while (true) {
                                delay(300)
                                val query = DownloadManager.Query().setFilterById(dlId)
                                val cursor = dm.query(query)
                                if (!cursor.moveToFirst()) { cursor.close(); break }
                                val dmStatus = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                                val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                                val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                                cursor.close()
                                when (dmStatus) {
                                    DownloadManager.STATUS_SUCCESSFUL -> {
                                        showAutoDownloadProgress = false
                                        autoDownloadId = null
                                        val file = getApkDestinationFile()
                                        installApkAndScheduleDelete(this@MainActivity, file)
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
                    com.coolappstore.everdialer.by.svhp.view.components.UpdateAvailableDialog(
                        currentVersion = com.coolappstore.everdialer.by.svhp.APP_VERSION,
                        latestVersion = autoUpdateVersion ?: "",
                        readyToInstall = false,
                        onAction = {
                            showAutoUpdateDialog = false
                            val url = autoUpdateApkUrl
                            if (url != null) {
                                val id = enqueueApkDownload(this@MainActivity, url)
                                if (id != null) {
                                    autoDownloadId = id
                                    autoDownloadProgress = 0f
                                    showAutoDownloadProgress = true
                                }
                            }
                        },
                        onDismiss = { showAutoUpdateDialog = false }
                    )
                }

                if (showAutoDownloadProgress) {
                    com.coolappstore.everdialer.by.svhp.view.components.UpdateDownloadingDialog(
                        latestVersion = autoUpdateVersion ?: "",
                        progress = autoDownloadProgress
                    )
                }

                // ── Biometric blur + lock ─────────────────────────────────
                val blurRadius by animateDpAsState(
                    targetValue = if (!isUnlocked) 22.dp else 0.dp,
                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                    label = "biometricBlur"
                )

                // ── Ongoing Call Banner + Main nav host ───────────────────
                val callSession by CallService.currentCallSession.collectAsState()
                val hasOngoingCall = callSession != null && callSession?.state != android.telecom.Call.STATE_RINGING

                Box(modifier = Modifier.fillMaxSize()) {
                    // Main content — blurred when locked
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (blurRadius > 0.dp)
                                    Modifier.blur(blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                                else
                                    Modifier
                            )
                    ) {
                    // ── Ongoing Call Banner (above all content) ────────────
                    AnimatedVisibility(
                        visible = hasOngoingCall,
                        enter = slideInVertically { -it } + fadeIn(),
                        exit = slideOutVertically { -it } + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1B5E20))
                                .statusBarsPadding()
                                .clickable {
                                    startActivity(
                                        Intent(this@MainActivity, CallActivity::class.java).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                                        }
                                    )
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.Call,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Call is Ongoing — Tap to return",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // ── Main nav host + adaptive nav (bottom bar / rail) ───
                    val configuration = LocalConfiguration.current
                    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                    com.coolappstore.everdialer.by.svhp.view.theme.isLandscapeMode = isLandscape
                    val navBackStack by navController.currentBackStackEntryAsState()
                    val currentDest = navBackStack?.destination
                    val prefs2 = remember { GlobalContext.get().get<PreferenceManager>() }
                    val showNotesRail = prefs2.getBoolean(PreferenceManager.KEY_TAB_SHOW_NOTES, true)

                    fun navTo(route: String) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }

                    if (isLandscape) {
                        val ctx = LocalContext.current
                        @Suppress("DEPRECATION")
                        val rotation = (ctx.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager).defaultDisplay.rotation
                        val isRotation90  = rotation == Surface.ROTATION_90
                        val isRotation270 = rotation == Surface.ROTATION_270
                        val railPaddingStart = if (isRotation270) 10.dp else 0.dp
                        val railPaddingEnd   = if (isRotation90)  10.dp else 0.dp

                        val liquidGlassBackdropLandscape = rememberLayerBackdrop()
                        CompositionLocalProvider(LocalLiquidGlassBackdrop provides liquidGlassBackdropLandscape) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(96.dp)
                                        .windowInsetsPadding(
                                            WindowInsets.displayCutout
                                                .union(WindowInsets.systemBars)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // Nav items — perfectly centered
                                        RailItem(
                                            selected = currentDest?.hierarchy?.any { it.route == FavoritesScreenDestination.route } == true,
                                            icon = { sel -> Icon(if (sel) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, "Favourites", modifier = Modifier.size(24.dp)) },
                                            label = "Favourites",
                                            paddingStart = railPaddingStart,
                                            paddingEnd = railPaddingEnd,
                                            onClick = { navTo(FavoritesScreenDestination.route) }
                                        )
                                        RailItem(
                                            selected = currentDest?.hierarchy?.any { it.route == RecentScreenDestination.route } == true,
                                            icon = { sel -> Icon(if (sel) Icons.Filled.History else Icons.Outlined.History, "Calls", modifier = Modifier.size(24.dp)) },
                                            label = "Calls",
                                            paddingStart = railPaddingStart,
                                            paddingEnd = railPaddingEnd,
                                            onClick = { navTo(RecentScreenDestination.route) }
                                        )
                                        RailItem(
                                            selected = currentDest?.hierarchy?.any { it.route == ContactScreenDestination.route } == true,
                                            icon = { sel -> Icon(if (sel) Icons.Filled.Person else Icons.Outlined.Person, "Contacts", modifier = Modifier.size(24.dp)) },
                                            label = "Contacts",
                                            paddingStart = railPaddingStart,
                                            paddingEnd = railPaddingEnd,
                                            onClick = { navTo(ContactScreenDestination.route) }
                                        )
                                        if (showNotesRail) {
                                            RailItem(
                                                selected = currentDest?.hierarchy?.any { it.route == NotesScreenDestination.route } == true,
                                                icon = { sel -> Icon(if (sel) Icons.Filled.Note else Icons.Outlined.Note, "Notes", modifier = Modifier.size(24.dp)) },
                                                label = "Notes",
                                                paddingStart = railPaddingStart,
                                                paddingEnd = railPaddingEnd,
                                                onClick = { navTo(NotesScreenDestination.route) }
                                            )
                                        }

                                        Spacer(Modifier.height(16.dp))
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 8.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        )
                                        Spacer(Modifier.height(4.dp))

                                        RailItem(
                                            selected = currentDest?.hierarchy?.any { it.route?.contains("search", ignoreCase = true) == true } == true,
                                            icon = { _ -> Icon(Icons.Default.Search, "Search", modifier = Modifier.size(24.dp)) },
                                            label = "Search",
                                            paddingStart = railPaddingStart,
                                            paddingEnd = railPaddingEnd,
                                            onClick = { navTo(com.ramcosta.composedestinations.generated.destinations.SearchScreenDestination.route) }
                                        )
                                        RailItem(
                                            selected = currentDest?.hierarchy?.any { it.route?.contains("settings", ignoreCase = true) == true } == true,
                                            icon = { _ -> Icon(Icons.Default.Tune, "Settings", modifier = Modifier.size(24.dp)) },
                                            label = "Settings",
                                            paddingStart = railPaddingStart,
                                            paddingEnd = railPaddingEnd,
                                            onClick = { navTo(com.ramcosta.composedestinations.generated.destinations.SettingsScreenDestination.route) }
                                        )
                                    }
                                }
                            }
                            // ── Main content fills the rest, edge-to-edge ──────────────────────
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                DestinationsNavHost(navGraph = NavGraphs.root, navController = navController, start = startDestination, defaultTransitions = TabTransitionStyle)
                            }
                        }
                        } // end CompositionLocalProvider landscape
                    } else {
                        val liquidGlassBackdrop = rememberLayerBackdrop()
                        CompositionLocalProvider(LocalLiquidGlassBackdrop provides liquidGlassBackdrop) {
                            Scaffold(
                                bottomBar = { BottomBar(navController) },
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentWindowInsets = WindowInsets(0)
                            ) { scaffoldPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(scaffoldPadding)
                                        .layerBackdrop(liquidGlassBackdrop)
                                        .then(
                                            if (hasOngoingCall)
                                                Modifier.consumeWindowInsets(WindowInsets.statusBars)
                                            else
                                                Modifier
                                        )
                                ) {
                                    DestinationsNavHost(
                                        navGraph      = NavGraphs.root,
                                        navController = navController,
                                        start         = startDestination,
                                        defaultTransitions = TabTransitionStyle
                                    )
                                }
                            }
                        }
                    }
                } // end blurred Column

                    // ── Biometric overlay (above blur, inside Box) ─────────
                    if (!isUnlocked) {
                        val activity = this@MainActivity
                        LaunchedEffect(biometricType) {
                            if (biometricType.isEmpty() || !appLockEnabled) {
                                isUnlocked = true; return@LaunchedEffect
                            }
                            if (biometricType == "system") {
                                val executor = androidx.core.content.ContextCompat.getMainExecutor(activity)
                                val prompt = androidx.biometric.BiometricPrompt(
                                    activity, executor,
                                    object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                                        override fun onAuthenticationSucceeded(r: androidx.biometric.BiometricPrompt.AuthenticationResult) { isUnlocked = true }
                                        override fun onAuthenticationError(code: Int, msg: CharSequence) { finish() }
                                        override fun onAuthenticationFailed() { finish() }
                                    }
                                )
                                prompt.authenticate(
                                    androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                                        .setTitle("Ever Dialer")
                                        .setSubtitle("Verify your identity to continue")
                                        .setNegativeButtonText("Cancel")
                                        .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK)
                                        .build()
                                )
                            }
                        }
                        if (biometricType == "pin") {
                            com.coolappstore.everdialer.by.svhp.view.screen.settings.PinSetupDialog(
                                title = "Enter PIN", isVerify = true,
                                expectedPin = prefs.getString(PreferenceManager.KEY_BIOMETRICS_PIN, "") ?: "",
                                onConfirm = { isUnlocked = true }, onDismiss = { finish() }
                            )
                        } else if (biometricType == "password") {
                            com.coolappstore.everdialer.by.svhp.view.screen.settings.PasswordSetupDialog(
                                title = "Enter Password", isVerify = true,
                                expectedPassword = prefs.getString(PreferenceManager.KEY_BIOMETRICS_PASSWORD, "") ?: "",
                                onConfirm = { isUnlocked = true }, onDismiss = { finish() }
                            )
                        }
                    }
                } // end outer Box

                LaunchedEffect(intent) {
                    handleIntent(intent, navController)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun handleIntent(intent: Intent?, navController: androidx.navigation.NavController) {
        intent ?: return
        val data = intent.data
        val action = intent.action

        when (action) {
            "com.coolappstore.everdialer.OPEN_RECENTS" -> {
                navController.navigate(RecentScreenDestination.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                }
            }
            Intent.ACTION_DIAL, Intent.ACTION_VIEW -> {
                if (data?.scheme == "tel") {
                    val number = data.schemeSpecificPart
                    navController.navigate(DialPadScreenDestination(initialNumber = number).route)
                } else if (data?.toString()?.contains("contacts") == true ||
                    data?.toString()?.contains("com.android.contacts") == true ||
                    intent.hasExtra("contact_id")) {
                    val id = data?.lastPathSegment ?: intent.getStringExtra("contact_id")
                    if (id != null) {
                        navController.navigate(ContactDetailsScreenDestination(contactId = id).route)
                    }
                }
            }
            Intent.ACTION_INSERT -> {
                val name = intent.getStringExtra(ContactsContract.Intents.Insert.NAME)
                val phone = intent.getStringExtra(ContactsContract.Intents.Insert.PHONE)
                navController.navigate(ContactEditScreenDestination(initialName = name, initialPhone = phone).route)
            }
            Intent.ACTION_EDIT -> {
                val id = data?.lastPathSegment
                if (id != null) {
                    navController.navigate(ContactEditScreenDestination(contactId = id).route)
                }
            }
        }
    }

    fun requestDefaultDialer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(android.app.role.RoleManager::class.java)
            if (!roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_DIALER)) {
                val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_DIALER)
                requestRoleLauncher.launch(intent)
            }
        } else {
            // API 26-28: use TelecomManager ACTION_CHANGE_DEFAULT_DIALER
            val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            @Suppress("DEPRECATION")
            if (telecomManager.defaultDialerPackage != packageName) {
                val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                    .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
                requestRoleLauncher.launch(intent)
            }
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ANSWER_PHONE_CALLS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissionsLauncher.launch(permissions.toTypedArray())
    }
}

@androidx.compose.runtime.Composable
private fun RailItem(
    selected: Boolean,
    icon: @androidx.compose.runtime.Composable (selected: Boolean) -> Unit,
    label: String,
    paddingStart: androidx.compose.ui.unit.Dp = 0.dp,
    paddingEnd: androidx.compose.ui.unit.Dp = 0.dp,
    onClick: () -> Unit
) {
    val bgColor = if (selected)
        androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
    else
        androidx.compose.ui.graphics.Color.Transparent
    val contentColor = if (selected)
        androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
    else
        androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant

    androidx.compose.foundation.layout.Column(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(start = paddingStart, end = paddingEnd, top = 4.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = androidx.compose.ui.Modifier
                .size(width = 56.dp, height = 32.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides contentColor
            ) {
                icon(selected)
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1
        )
    }
}
