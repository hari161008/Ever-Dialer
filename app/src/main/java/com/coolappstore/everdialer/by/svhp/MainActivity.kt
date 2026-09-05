package com.coolappstore.everdialer.by.svhp

import android.Manifest
import android.app.DownloadManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.core.content.ContextCompat
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
import com.coolappstore.everdialer.by.svhp.view.components.FullScreenIntentDialog
import com.coolappstore.everdialer.by.svhp.view.components.BottomBar
import com.coolappstore.everdialer.by.svhp.view.components.enterNotesTab
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
import androidx.compose.material.icons.outlined.FiberManualRecord
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.ramcosta.composedestinations.generated.destinations.ContactScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FavoritesScreenDestination
import com.ramcosta.composedestinations.generated.destinations.NotesScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RecentScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RecordingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.UpdatesScreenDestination
import org.koin.core.context.GlobalContext

class MainActivity : FragmentActivity() {

    private val requestRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ -> }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> /* permissions result; dialer popup now shown after welcome */ }

    // If a third-party direct-call shortcut hands us ACTION_CALL before CALL_PHONE happens to be
    // granted yet (e.g. very first run, right as the default-dialer role prompt from
    // requestDefaultDialer() is still pending an answer), don't silently fall back to just
    // opening the dialpad with the number filled in — that reads as "the shortcut did nothing."
    // Stash the number, ask for CALL_PHONE directly, and complete the call the moment it's
    // granted; only fall back to ACTION_DIAL if the user actually denies it.
    private var pendingExternalCallNumber: String? = null
    private val requestCallPhonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val number = pendingExternalCallNumber
        pendingExternalCallNumber = null
        if (number != null) {
            if (granted) {
                com.coolappstore.everdialer.by.svhp.controller.util.makeCall(this, number)
            } else {
                val intent = Intent(Intent.ACTION_DIAL, android.net.Uri.fromParts("tel", number, null))
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
        }
    }

    // Holds whatever intent should currently be processed by handleIntent(). Set from onCreate's
    // initial intent and re-set from onNewIntent() so Compose actually reacts to intents delivered
    // to an already-running instance (contact/dial shortcuts, "call back" from other apps like
    // Truecaller, widgets, etc.) — a plain onNewIntent() { setIntent(intent) } does NOT retrigger
    // the LaunchedEffect below, since Compose has no way to observe a mutation of the Activity's
    // own `intent` field.
    private var pendingIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge() triggers Adreno GPU driver SIGSEGV on first RenderThread draw.
        // Edge-to-edge is set via theme XML instead (windowDrawsSystemBarBackgrounds etc).
        WindowCompat.setDecorFitsSystemWindows(window, false)

        requestRequiredPermissions()
        // On first launch, show default dialer prompt first; welcome dialog appears after.
        requestDefaultDialer()

        // Auto refresh system wallpaper if enabled
        try {
            val appPrefs = org.koin.core.context.GlobalContext.get().get<PreferenceManager>()
            com.coolappstore.everdialer.by.svhp.controller.util.WallpaperExportHelper.refreshAutoWallpaperIfEnabled(this, appPrefs)
        } catch (_: Exception) {}

        pendingIntent = intent

        setContent {
            Rivo4Theme {
                val navController = rememberNavController()

                // Eagerly create CallLogViewModel here, at the top of the compose tree, instead
                // of letting it lazily spin up the first time the Calls/Recents screen (or any
                // other screen that happens to reference it) is composed. Its init{} block is
                // what registers the CallLog/Contacts ContentObservers and starts collecting
                // CallService.currentCallSession — so until it exists, a call placed or received
                // through EverDialer *or any other app* (any write to the system CallLog
                // provider) doesn't trigger a refresh at all; the list only ever catches up the
                // next time the user happens to open the app or navigate into the call log
                // section, since that's what was creating the ViewModel for the first time.
                // Creating it unconditionally here means the observers are live as soon as
                // MainActivity is, regardless of which tab is the configured start destination,
                // so call log updates from anywhere arrive immediately instead of on next visit.
                val callLogViewModel: com.coolappstore.everdialer.by.svhp.controller.CallLogViewModel =
                    org.koin.compose.viewmodel.koinActivityViewModel()

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
                        "favorites"  -> FavoritesScreenDestination
                        "contacts"   -> ContactScreenDestination
                        "recordings" -> RecordingsScreenDestination()
                        "notes"      -> NotesScreenDestination()
                        else         -> RecentScreenDestination
                    }
                }

                val isFirstLaunch = remember {
                    !prefs.getBoolean(PreferenceManager.KEY_FIRST_LAUNCH_DONE, false)
                }

                // ── First Launch Welcome Dialog ─────────────────────────────
                // Show AFTER the default dialer prompt (which fires in onCreate)
                var showWelcomeDialog by remember { mutableStateOf(false) }
                var showTelegramDialog by remember { mutableStateOf(false) }
                var showFullScreenIntentDialog by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    if (isFirstLaunch) {
                        // Small delay so the default dialer system dialog appears first
                        kotlinx.coroutines.delay(600)
                        showWelcomeDialog = true
                    } else if (!prefs.getBoolean(PreferenceManager.KEY_TELEGRAM_SHOWN, false)) {
                        // Welcome already done but Telegram dialog not yet shown — show it
                        kotlinx.coroutines.delay(800)
                        showTelegramDialog = true
                    } else if (needsFullScreenIntentPermission()) {
                        kotlinx.coroutines.delay(800)
                        showFullScreenIntentDialog = true
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
                            } else if (needsFullScreenIntentPermission()) {
                                showFullScreenIntentDialog = true
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
                            if (needsFullScreenIntentPermission()) {
                                showFullScreenIntentDialog = true
                            }
                        },
                        onSkip = {
                            prefs.setBoolean(PreferenceManager.KEY_TELEGRAM_SHOWN, true)
                            showTelegramDialog = false
                            if (needsFullScreenIntentPermission()) {
                                showFullScreenIntentDialog = true
                            }
                        }
                    )
                }

                // ── Full-Screen Intent Permission Dialog ─────────────────────
                if (showFullScreenIntentDialog) {
                    FullScreenIntentDialog(
                        onEnable = {
                            showFullScreenIntentDialog = false
                            val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                                data = Uri.fromParts("package", packageName, null)
                            }
                            try {
                                startActivity(intent)
                            } catch (_: Exception) {
                                val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", packageName, null)
                                }
                                startActivity(fallback)
                            }
                        },
                        onSkip = { showFullScreenIntentDialog = false }
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
                            navController.navigate(UpdatesScreenDestination.route)
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

                // ── Donate popup state ──────────────────────────────────────
                var showDonateDialog by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    val lastVersion = prefs.getString(PreferenceManager.KEY_LAST_APP_VERSION, null)
                    val openCount = prefs.getInt(PreferenceManager.KEY_APP_OPEN_COUNT, 0) + 1
                    prefs.setInt(PreferenceManager.KEY_APP_OPEN_COUNT, openCount)

                    if (lastVersion == null) {
                        // Fresh install
                        prefs.setString(PreferenceManager.KEY_LAST_APP_VERSION, APP_VERSION)
                        if (openCount == 4 && !prefs.getBoolean(PreferenceManager.KEY_DONATE_POPUP_SHOWN_INSTALL, false)) {
                            showDonateDialog = true
                        }
                    } else if (lastVersion != APP_VERSION) {
                        // App update!
                        prefs.setString(PreferenceManager.KEY_LAST_APP_VERSION, APP_VERSION)
                        showDonateDialog = true
                    } else {
                        // Same version
                        if (openCount == 4 && !prefs.getBoolean(PreferenceManager.KEY_DONATE_POPUP_SHOWN_INSTALL, false)) {
                            showDonateDialog = true
                        }
                    }
                }

                // ── Biometric blur + lock ─────────────────────────────────
                val blurRadius by animateDpAsState(
                    targetValue = if (!isUnlocked) 22.dp else 0.dp,
                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                    label = "biometricBlur"
                )

                // ── Ongoing Call Banner + Main nav host ───────────────────
                val callSession by CallService.currentCallSession.collectAsState()
                val isCallActive = callSession != null
                val hasOngoingCall = callSession != null && callSession?.state != android.telecom.Call.STATE_RINGING

                // ── Donate Popup Dialog (shows on update or 4th launch; if in call, waits until call ends) ──
                if (showDonateDialog && !isCallActive && !showWelcomeDialog && !showTelegramDialog && !showFullScreenIntentDialog) {
                    com.coolappstore.everdialer.by.svhp.view.components.DonateDialog(
                        onDonate = {
                            prefs.setBoolean(PreferenceManager.KEY_DONATE_POPUP_SHOWN_INSTALL, true)
                            showDonateDialog = false
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://hariprabhu.com/Ever-Dialer/#donate")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(intent)
                        },
                        onLater = {
                            prefs.setBoolean(PreferenceManager.KEY_DONATE_POPUP_SHOWN_INSTALL, true)
                            showDonateDialog = false
                        }
                    )
                }

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
                    // Keep the page-switching slide animation's tab order in sync with
                    // whatever order the user has configured in Settings > Appearance >
                    // Tab Sections — never hardcoded, so reordering tabs there never makes
                    // the slide direction feel wrong on the main screen.
                    remember(settingsVer) {
                        com.coolappstore.everdialer.by.svhp.view.theme.syncTabTransitionOrder(prefs)
                    }
                    val navBackStack by navController.currentBackStackEntryAsState()
                    val currentDest = navBackStack?.destination
                    val prefs2 = remember { GlobalContext.get().get<PreferenceManager>() }
                    val showNotesRail = prefs2.getBoolean(PreferenceManager.KEY_TAB_SHOW_NOTES, true)
                    val showRecordingsRail = prefs2.getBoolean(PreferenceManager.KEY_TAB_SHOW_RECORDINGS, true)

                    fun navTo(route: String) {
                        // Always open Notes fresh from the rail — enterNotesTab() guarantees a
                        // brand new instance with no leftover highlightQuery, so the search bar
                        // and nav rail can never come back hidden from a previous search visit.
                        if (route == NotesScreenDestination.route) {
                            navController.enterNotesTab()
                            return
                        }
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }

                    if (isLandscape) {
                        // The rail's own windowInsetsPadding(displayCutout) below already keeps
                        // the whole column clear of the camera cutout — no need for any per-item
                        // horizontal padding, which was both throwing off centering and eating
                        // into the width labels like "Favourites"/"Recordings" need to fit
                        // without getting clipped ("Favourit", "Recordin").
                        val railPaddingStart = 0.dp
                        val railPaddingEnd   = 0.dp

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
                                        .width(112.dp)
                                        .windowInsetsPadding(
                                            WindowInsets.displayCutout
                                                .union(WindowInsets.systemBars)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        // Evenly distribute every item (including the divider) across
                                        // the full rail height so the gaps are always uniform, instead
                                        // of clustering everything in the middle with big empty space
                                        // above/below.
                                        verticalArrangement = Arrangement.SpaceEvenly,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // Nav items — order and visibility driven by the same
                                        // Settings > Appearance > Tab Sections config as the
                                        // portrait bottom bar, never hardcoded.
                                        val showFavoritesRail = prefs2.getBoolean(PreferenceManager.KEY_TAB_SHOW_FAVORITES, true)
                                        val showCallsRail     = prefs2.getBoolean(PreferenceManager.KEY_TAB_SHOW_CALLS, true)
                                        val showContactsRail  = prefs2.getBoolean(PreferenceManager.KEY_TAB_SHOW_CONTACTS, true)
                                        val railTabOrder = remember(settingsVer) {
                                            PreferenceManager.parseTabOrder(prefs2.getString(PreferenceManager.KEY_TAB_ORDER, null))
                                        }

                                        railTabOrder.forEach { tabKey ->
                                            key(tabKey) {
                                                when (tabKey) {
                                                    "favorites" -> if (showFavoritesRail) RailItem(
                                                        selected = currentDest?.hierarchy?.any { it.route == FavoritesScreenDestination.route } == true,
                                                        icon = { sel -> Icon(if (sel) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, "Favourites", modifier = Modifier.size(24.dp)) },
                                                        label = "Favourites",
                                                        paddingStart = railPaddingStart,
                                                        paddingEnd = railPaddingEnd,
                                                        onClick = { navTo(FavoritesScreenDestination.route) }
                                                    )
                                                    "calls" -> if (showCallsRail) RailItem(
                                                        selected = currentDest?.hierarchy?.any { it.route == RecentScreenDestination.route } == true,
                                                        icon = { sel -> Icon(if (sel) Icons.Filled.History else Icons.Outlined.History, "Calls", modifier = Modifier.size(24.dp)) },
                                                        label = "Calls",
                                                        paddingStart = railPaddingStart,
                                                        paddingEnd = railPaddingEnd,
                                                        onClick = { navTo(RecentScreenDestination.route) }
                                                    )
                                                    "contacts" -> if (showContactsRail) RailItem(
                                                        selected = currentDest?.hierarchy?.any { it.route == ContactScreenDestination.route } == true,
                                                        icon = { sel -> Icon(if (sel) Icons.Filled.Person else Icons.Outlined.Person, "Contacts", modifier = Modifier.size(24.dp)) },
                                                        label = "Contacts",
                                                        paddingStart = railPaddingStart,
                                                        paddingEnd = railPaddingEnd,
                                                        onClick = { navTo(ContactScreenDestination.route) }
                                                    )
                                                    "recordings" -> if (showRecordingsRail) RailItem(
                                                        selected = currentDest?.hierarchy?.any { it.route == RecordingsScreenDestination.route } == true,
                                                        icon = { sel -> Icon(if (sel) Icons.Filled.FiberManualRecord else Icons.Outlined.FiberManualRecord, "Recordings", modifier = Modifier.size(24.dp)) },
                                                        label = "Recordings",
                                                        paddingStart = railPaddingStart,
                                                        paddingEnd = railPaddingEnd,
                                                        onClick = { navTo(RecordingsScreenDestination.route) }
                                                    )
                                                    "notes" -> if (showNotesRail) RailItem(
                                                        selected = currentDest?.hierarchy?.any { it.route == NotesScreenDestination.route } == true,
                                                        icon = { sel -> Icon(if (sel) Icons.Filled.Note else Icons.Outlined.Note, "Notes", modifier = Modifier.size(24.dp)) },
                                                        label = "Notes",
                                                        paddingStart = railPaddingStart,
                                                        paddingEnd = railPaddingEnd,
                                                        onClick = { navTo(NotesScreenDestination.route) }
                                                    )
                                                }
                                            }
                                        }

                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 8.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        )

                                        RailItem(
                                            // Compare only the path portion of the route (before any "?" query
                                            // args) — Recordings' own route params like "openedFromSettings"
                                            // contain the substring "settings" too, which used to falsely
                                            // highlight this icon while just sitting on the Recordings tab.
                                            selected = currentDest?.hierarchy?.any {
                                                it.route?.substringBefore("?")?.contains("settings", ignoreCase = true) == true
                                            } == true,
                                            icon = { _ -> Icon(Icons.Default.Tune, "Settings", modifier = Modifier.size(24.dp)) },
                                            label = "Settings",
                                            paddingStart = railPaddingStart,
                                            paddingEnd = railPaddingEnd,
                                            onClick = { navTo(com.ramcosta.composedestinations.generated.destinations.SettingsScreenDestination().route) }
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

                LaunchedEffect(pendingIntent) {
                    pendingIntent?.let {
                        handleIntent(it, navController)
                        pendingIntent = null
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingIntent = intent
    }

    private fun resolvePhoneNumberFromContactUri(context: Context, uri: Uri): String? {
        if (uri.scheme == "tel") return uri.schemeSpecificPart
        return try {
            // These content:// URIs come in several incompatible shapes depending on which app
            // built them:
            //  - content://com.android.contacts/data/<id>            → a Data row id (one
            //    specific phone number entry)
            //  - content://com.android.contacts/contacts/<id>         → an aggregate Contact id
            //  - content://com.android.contacts/contacts/lookup/<key>/<id> → lookup-key form,
            //    where lastPathSegment can be the numeric id BUT the segment before it is the
            //    non-numeric lookup key, and on some OEM builds the trailing numeric id is stale
            //    and needs re-resolving via the lookup key instead.
            // Blindly treating lastPathSegment as a CommonDataKinds.Phone.CONTACT_ID (the old
            // behavior) silently returns null - and therefore silently drops the call - for the
            // first and third shapes above. Try each interpretation in turn.
            val lastSegment = uri.lastPathSegment
            val isDataUri = uri.pathSegments.getOrNull(0) == "data"

            fun queryByContactId(id: Long): String? =
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(id.toString()),
                    "${ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY} DESC"
                )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }

            fun queryByDataId(id: Long): String? =
                context.contentResolver.query(
                    uri.buildUpon().authority(ContactsContract.AUTHORITY).path(null)
                        .appendPath("data").appendPath(id.toString()).build(),
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    null, null, null
                )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }

            fun queryByLookup(): String? {
                val lookupUri = try { ContactsContract.Contacts.lookupContact(context.contentResolver, uri) } catch (_: Exception) { null } ?: return null
                val id = lookupUri.lastPathSegment?.toLongOrNull() ?: return null
                return queryByContactId(id)
            }

            when {
                isDataUri -> lastSegment?.toLongOrNull()?.let(::queryByDataId) ?: queryByLookup()
                else -> {
                    val id = lastSegment?.toLongOrNull()
                    (id?.let(::queryByContactId)) ?: queryByLookup()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("EverDialerCall", "resolvePhoneNumberFromContactUri failed for $uri", e)
            null
        }
    }

    private fun handleIntent(intent: Intent?, navController: androidx.navigation.NavController) {
        intent ?: return
        android.util.Log.d("EverDialerCall", "handleIntent action=${intent.action} data=${intent.data}")
        val data = intent.data
        val action = intent.action

        if (intent.getBooleanExtra("NAV_TO_RECENTS", false) && action != "com.coolappstore.everdialer.OPEN_CALL_LOGS_DETAIL") {
            navController.navigate(RecentScreenDestination.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
            }
            return
        }

        when (action) {
            "com.coolappstore.everdialer.OPEN_RECENTS" -> {
                navController.navigate(RecentScreenDestination.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                }
            }
            "com.coolappstore.everdialer.OPEN_CALL_LOGS_DETAIL" -> {
                val contactId = intent.getStringExtra("contact_id")
                val phoneNumber = intent.getStringExtra("phone_number")
                if (!contactId.isNullOrBlank() || !phoneNumber.isNullOrBlank()) {
                    navController.navigate("call_log_detail_screen?contactId=${contactId ?: "null"}&phoneNumber=${phoneNumber ?: "null"}")
                } else {
                    navController.navigate(RecentScreenDestination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                    }
                }
            }
            Intent.ACTION_VIEW -> {
                val mimeType = intent.type
                if (mimeType == "vnd.android.cursor.dir/calls" ||
                    data?.toString()?.contains("call_log") == true ||
                    data?.toString()?.contains("calls") == true) {
                    navController.navigate(RecentScreenDestination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                    }
                } else if (data?.scheme == "tel") {
                    val number = data.schemeSpecificPart
                    navController.navigate(DialPadScreenDestination(initialNumber = number).route)
                } else if (data?.toString()?.contains("contacts") == true ||
                    data?.toString()?.contains("com.android.contacts") == true ||
                    intent.hasExtra("contact_id")) {
                    // Home-screen contact widgets and "call this contact" shortcuts (e.g. from
                    // Google Search/Assistant) hand us a contact content:// URI instead of a
                    // tel: URI. Resolve it to the contact's number so we can dial straight away
                    // instead of just opening the contact's detail page with nothing filled in.
                    val number = data?.let { resolvePhoneNumberFromContactUri(this, it) }
                    if (number != null) {
                        navController.navigate(DialPadScreenDestination(initialNumber = number).route)
                    } else {
                        val id = data?.lastPathSegment ?: intent.getStringExtra("contact_id")
                        if (id != null) {
                            navController.navigate(ContactDetailsScreenDestination(contactId = id).route)
                        }
                    }
                }
            }
            Intent.ACTION_DIAL -> {
                if (data?.scheme == "tel") {
                    val number = data.schemeSpecificPart
                    navController.navigate(DialPadScreenDestination(initialNumber = number).route)
                } else if (data != null) {
                    val number = resolvePhoneNumberFromContactUri(this, data)
                    if (number != null) {
                        navController.navigate(DialPadScreenDestination(initialNumber = number).route)
                    }
                }
            }
            Intent.ACTION_CALL, "android.intent.action.CALL_PRIVILEGED" -> {
                // Contact widgets/Assistant "call [contact]" shortcuts, and third-party caller-ID
                // apps like Truecaller "call back" on a missed call, send plain ACTION_CALL with
                // a tel: URI expecting the call to be placed immediately, not just shown on a
                // dialpad — that's the realistic third-party trigger. (ACTION_CALL_PRIVILEGED is
                // also matched here defensively, but note it's restricted by the system to
                // apps holding the signature|privileged CALL_PRIVILEGED permission — ordinary
                // third-party apps and launcher shortcuts cannot send it, so in practice this
                // branch is reached via ACTION_CALL.)
                val number = when {
                    data?.scheme == "tel" -> data.schemeSpecificPart?.let { android.net.Uri.decode(it) }
                    data?.scheme == "voicemail" -> null // not handled; let system voicemail flow own it
                    data != null -> resolvePhoneNumberFromContactUri(this, data)
                    else -> null
                }?.trim()
                android.util.Log.d("EverDialerCall", "external call intent action=$action data=$data resolvedNumber=$number")
                if (!number.isNullOrBlank()) {
                    // Do NOT navigate to the dialpad here — ACTION_CALL means "place the call
                    // now", and doing both at once (navigating this Activity's UI while also
                    // handing the call off to Telecom, which immediately brings up CallActivity
                    // in front of it) raced the two screens for foreground/composition and was
                    // why direct-call shortcuts got stuck showing "Connecting..." forever instead
                    // of ever reaching the live call screen.
                    //
                    // Deliberately does NOT run any of Ever Dialer's own SIM-selection logic
                    // (no per-contact preference, no app-wide default-SIM setting, no picker) —
                    // just hands the number straight to Telecom with no PhoneAccountHandle, the
                    // same as if the call were placed with plain ACTION_CALL and no dialer app
                    // installed at all. Telecom then falls back to the system's own configured
                    // default (Settings → Network & internet → SIMs → Calls), or its native SIM
                    // picker if that's set to "Ask every time".
                    //
                    // Check CALL_PHONE directly here rather than trusting makeCall()'s own
                    // internal check-and-fallback: onCreate's requestRequiredPermissions() is
                    // fire-and-forget, so on a very first run a shortcut tapped in the same
                    // moment the permission dialog is still pending could otherwise silently
                    // degrade to "just opens the dialpad" instead of actually calling — asking
                    // directly here and completing the call once granted avoids that gap.
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                        com.coolappstore.everdialer.by.svhp.controller.util.makeCall(this, number)
                    } else {
                        pendingExternalCallNumber = number
                        requestCallPhonePermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                    }
                } else {
                    android.util.Log.w("EverDialerCall", "external call intent had no resolvable number, ignoring")
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

    fun needsFullScreenIntentPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false
        val nm = getSystemService(NotificationManager::class.java)
        return !nm.canUseFullScreenIntent()
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

    override fun onResume() {
        super.onResume()
        com.coolappstore.everdialer.by.svhp.controller.util.MissedCallBadgeManager.markMissedCallsAsRead(this)
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
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = androidx.compose.ui.Modifier.fillMaxWidth()
        )
    }
}
