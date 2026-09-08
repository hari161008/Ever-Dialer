package com.coolappstore.everdialer.by.svhp

import android.Manifest
import android.app.DownloadManager
import android.app.NotificationManager
import android.content.ContentUris
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
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.outlined.Dialpad
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
import com.coolappstore.everdialer.by.svhp.controller.util.DefaultDialerManager
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.placeCallHonoringContactSim
import com.coolappstore.everdialer.by.svhp.controller.util.makeCall
import com.coolappstore.everdialer.by.svhp.controller.util.numbersLikelyMatch
import com.coolappstore.everdialer.by.svhp.modal.`interface`.IContactsRepository
import com.coolappstore.everdialer.by.svhp.view.components.SimPickerDialog
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
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Note
import androidx.compose.material.icons.outlined.FiberManualRecord
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.ramcosta.composedestinations.generated.destinations.ContactScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FavoritesScreenDestination
import com.ramcosta.composedestinations.generated.destinations.GroupsScreenDestination
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
    ) { _ ->
        // When permission prompt dismisses, prompt for default dialer if not already held
        if (!DefaultDialerManager.isDefaultDialer(this)) {
            requestDefaultDialer()
        }
    }

    // If a third-party direct-call shortcut hands us ACTION_CALL before CALL_PHONE happens to be
    // granted yet (e.g. very first run, right as the default-dialer role prompt from
    // requestDefaultDialer() is still pending an answer), don't silently fall back to just
    // opening the dialpad with the number filled in — that reads as "the shortcut did nothing."
    // Stash the number, ask for CALL_PHONE directly, and complete the call the moment it's
    // granted; only fall back to ACTION_DIAL if the user actually denies it.
    private var pendingExternalCallNumber: String? = null
    private var pendingExternalCallContactKey: String? = null
    private var showSimPicker by mutableStateOf(false)
    private var pendingSimPickerNumber by mutableStateOf<String?>(null)

    private val requestCallPhonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val number = pendingExternalCallNumber
        val contactKey = pendingExternalCallContactKey ?: number
        pendingExternalCallNumber = null
        pendingExternalCallContactKey = null
        if (number != null) {
            if (granted) {
                placeDirectCall(number, contactKey)
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

        val hasBasicPermissions = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

        if (!hasBasicPermissions) {
            requestRequiredPermissions()
        } else if (!DefaultDialerManager.isDefaultDialer(this)) {
            requestDefaultDialer()
        }

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
                        "groups"     -> GroupsScreenDestination
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
                                        val showGroupsRail    = prefs2.getBoolean(PreferenceManager.KEY_TAB_SHOW_GROUPS, false)
                                        val showDialpadRail   = prefs2.getBoolean(PreferenceManager.KEY_TAB_SHOW_DIALPAD, false)
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
                                                    "groups" -> if (showGroupsRail) RailItem(
                                                        selected = currentDest?.hierarchy?.any { it.route == GroupsScreenDestination.route } == true,
                                                        icon = { sel -> Icon(if (sel) Icons.Filled.Group else Icons.Outlined.Group, "Groups", modifier = Modifier.size(24.dp)) },
                                                        label = "Groups",
                                                        paddingStart = railPaddingStart,
                                                        paddingEnd = railPaddingEnd,
                                                        onClick = { navTo(GroupsScreenDestination.route) }
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
                                                    "dialpad" -> if (showDialpadRail) RailItem(
                                                        selected = currentDest?.hierarchy?.any { it.route == DialPadScreenDestination.route } == true,
                                                        icon = { sel -> Icon(if (sel) Icons.Filled.Dialpad else Icons.Outlined.Dialpad, "Dialpad", modifier = Modifier.size(24.dp)) },
                                                        label = "Dialpad",
                                                        paddingStart = railPaddingStart,
                                                        paddingEnd = railPaddingEnd,
                                                        onClick = {
                                                            if (currentDest?.hierarchy?.any { it.route == DialPadScreenDestination.route } == true) return@RailItem
                                                            navController.navigate(DialPadScreenDestination().route) {
                                                                launchSingleTop = true
                                                            }
                                                        }
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

                if (showSimPicker && pendingSimPickerNumber != null) {
                    SimPickerDialog(
                        onDismissRequest = { showSimPicker = false },
                        onSimSelected = { handle ->
                            makeCall(this@MainActivity, pendingSimPickerNumber!!, handle)
                            showSimPicker = false
                        }
                    )
                }

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

    private data class ResolvedCallTarget(
        val number: String,
        val contactId: String? = null,
        val displayName: String? = null
    )

    private fun resolveCallTargetByContactId(context: Context, contactId: String): ResolvedCallTarget? {
        val prefs = GlobalContext.get().get<PreferenceManager>()
        val defaultNumber = prefs.getContactDefaultNumber(contactId)

        return try {
            val cr = context.contentResolver
            cr.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                    ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY,
                    ContactsContract.CommonDataKinds.Phone.IS_PRIMARY
                ),
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(contactId),
                "${ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY} DESC, ${ContactsContract.CommonDataKinds.Phone.IS_PRIMARY} DESC"
            )?.use { cursor ->
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
                var firstNum: String? = null
                var contactName: String? = null
                while (cursor.moveToNext()) {
                    val num = if (numIdx >= 0) cursor.getString(numIdx) else null
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                    if (contactName == null && !name.isNullOrBlank()) contactName = name
                    if (!num.isNullOrBlank()) {
                        if (firstNum == null) firstNum = num
                        if (defaultNumber != null && numbersLikelyMatch(num, defaultNumber)) {
                            return@use ResolvedCallTarget(number = num, contactId = contactId, displayName = contactName)
                        }
                    }
                }
                if (firstNum != null) {
                    ResolvedCallTarget(number = firstNum, contactId = contactId, displayName = contactName)
                } else null
            }
        } catch (_: Exception) { null }
    }

    private fun resolveCallTargetFromUri(context: Context, uri: Uri): ResolvedCallTarget? {
        if (uri.scheme == "tel" || uri.scheme == "sip") {
            val num = uri.schemeSpecificPart?.let { Uri.decode(it) }?.trim()
            if (!num.isNullOrBlank()) return ResolvedCallTarget(number = num)
        }

        // Try querying the URI directly first
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val numColIdx = cursor.columnNames.indexOfFirst {
                        it.equals(ContactsContract.CommonDataKinds.Phone.NUMBER, ignoreCase = true) ||
                        it.equals(ContactsContract.Data.DATA1, ignoreCase = true) ||
                        it.equals("number", ignoreCase = true) ||
                        it.equals("data1", ignoreCase = true)
                    }
                    val cidColIdx = cursor.columnNames.indexOfFirst {
                        it.equals(ContactsContract.CommonDataKinds.Phone.CONTACT_ID, ignoreCase = true) ||
                        it.equals(ContactsContract.Contacts._ID, ignoreCase = true) ||
                        it.equals("contact_id", ignoreCase = true) ||
                        it.equals("_id", ignoreCase = true)
                    }
                    val nameColIdx = cursor.columnNames.indexOfFirst {
                        it.equals(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY, ignoreCase = true) ||
                        it.equals(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY, ignoreCase = true) ||
                        it.equals("display_name", ignoreCase = true)
                    }

                    val number = if (numColIdx >= 0) cursor.getString(numColIdx) else null
                    val cid = if (cidColIdx >= 0) cursor.getString(cidColIdx) else null
                    val name = if (nameColIdx >= 0) cursor.getString(nameColIdx) else null

                    if (!number.isNullOrBlank()) {
                        return ResolvedCallTarget(number = number.trim(), contactId = cid, displayName = name)
                    }

                    if (!cid.isNullOrBlank()) {
                        val byId = resolveCallTargetByContactId(context, cid)
                        if (byId != null) return byId
                    }
                }
            }
        } catch (_: Exception) {}

        // If direct query didn't return a phone number, try lookup / contact ID extraction
        try {
            val lastSegment = uri.lastPathSegment
            val id = lastSegment?.toLongOrNull()
            if (id != null) {
                // If it's a data ID
                val isData = uri.pathSegments.contains("data")
                if (isData) {
                    val dataUri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, id)
                    context.contentResolver.query(
                        dataUri,
                        arrayOf(ContactsContract.Data.DATA1, ContactsContract.Data.CONTACT_ID, ContactsContract.Data.DISPLAY_NAME_PRIMARY),
                        null, null, null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val num = cursor.getString(0)
                            val cid = cursor.getString(1)
                            val name = cursor.getString(2)
                            if (!num.isNullOrBlank()) {
                                return ResolvedCallTarget(number = num.trim(), contactId = cid, displayName = name)
                            }
                        }
                    }
                }

                // If it's a contact ID
                val byId = resolveCallTargetByContactId(context, id.toString())
                if (byId != null) return byId
            }

            // Try lookupContact
            val lookupUri = try { ContactsContract.Contacts.lookupContact(context.contentResolver, uri) } catch (_: Exception) { null }
            if (lookupUri != null) {
                val lookupId = lookupUri.lastPathSegment
                if (lookupId != null) {
                    val byId = resolveCallTargetByContactId(context, lookupId)
                    if (byId != null) return byId
                }
            }
        } catch (_: Exception) {}

        return null
    }

    private fun resolveCallTargetFromIntent(context: Context, intent: Intent): ResolvedCallTarget? {
        val data = intent.data

        // 1. Direct extras
        val extraPhone = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
            ?: intent.getStringExtra("android.intent.extra.PHONE_NUMBER")
            ?: intent.getStringExtra("phone")
            ?: intent.getStringExtra("phone_number")
            ?: intent.getStringExtra("number")
        val extraContactId = intent.getStringExtra("contact_id")

        if (!extraPhone.isNullOrBlank()) {
            return ResolvedCallTarget(extraPhone.trim(), extraContactId)
        }

        // 2. Data with tel: / sip: scheme
        if (data?.scheme == "tel" || data?.scheme == "sip") {
            val num = data.schemeSpecificPart?.let { Uri.decode(it) }?.trim()
            if (!num.isNullOrBlank()) {
                return ResolvedCallTarget(num, extraContactId)
            }
        }

        // 3. Data with content: scheme or other URI
        if (data != null) {
            val fromUri = resolveCallTargetFromUri(context, data)
            if (fromUri != null) {
                return ResolvedCallTarget(
                    number = fromUri.number,
                    contactId = extraContactId ?: fromUri.contactId,
                    displayName = fromUri.displayName
                )
            }
        }

        // 4. Fallback by contact_id extra if number wasn't provided yet
        if (!extraContactId.isNullOrBlank()) {
            val byId = resolveCallTargetByContactId(context, extraContactId)
            if (byId != null) return byId
        }

        return null
    }

    private fun placeDirectCall(targetNumber: String, contactKey: String? = null) {
        val cleanNumber = targetNumber.trim()
        if (cleanNumber.isBlank()) return

        val prefs = GlobalContext.get().get<PreferenceManager>()
        // If contactKey wasn't supplied, try finding the matching contact by phone number
        val resolvedKey = contactKey?.takeIf { it.isNotBlank() } ?: run {
            try {
                val contactsRepo = GlobalContext.get().get<IContactsRepository>()
                contactsRepo.getContactByNumber(cleanNumber)?.id
            } catch (_: Exception) { null }
        } ?: cleanNumber

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            placeCallHonoringContactSim(this, prefs, resolvedKey, cleanNumber) {
                pendingSimPickerNumber = cleanNumber
                showSimPicker = true
            }
        } else {
            pendingExternalCallNumber = cleanNumber
            pendingExternalCallContactKey = resolvedKey
            requestCallPhonePermissionLauncher.launch(Manifest.permission.CALL_PHONE)
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
                } else if (mimeType == "vnd.android.cursor.item/phone" ||
                    mimeType == "vnd.android.cursor.item/phone_v2" ||
                    intent.getBooleanExtra("android.intent.extra.CALL_NOW", false) ||
                    intent.getBooleanExtra("direct_call", false)) {
                    val target = resolveCallTargetFromIntent(this, intent)
                    if (target != null && target.number.isNotBlank()) {
                        placeDirectCall(target.number, target.contactId)
                    } else if (data?.scheme == "tel") {
                        val number = data.schemeSpecificPart
                        navController.navigate(DialPadScreenDestination(initialNumber = number).route)
                    }
                } else if (data?.scheme == "tel") {
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
            Intent.ACTION_DIAL -> {
                if (intent.getBooleanExtra("android.intent.extra.CALL_NOW", false) ||
                    intent.getBooleanExtra("direct_call", false)) {
                    val target = resolveCallTargetFromIntent(this, intent)
                    if (target != null && target.number.isNotBlank()) {
                        placeDirectCall(target.number, target.contactId)
                        return
                    }
                }
                if (data?.scheme == "tel") {
                    val number = data.schemeSpecificPart
                    navController.navigate(DialPadScreenDestination(initialNumber = number).route)
                } else if (data != null) {
                    val target = resolveCallTargetFromUri(this, data)
                    if (target != null && target.number.isNotBlank()) {
                        navController.navigate(DialPadScreenDestination(initialNumber = target.number).route)
                    }
                }
            }
            Intent.ACTION_CALL, "android.intent.action.CALL_PRIVILEGED" -> {
                val target = resolveCallTargetFromIntent(this, intent)
                android.util.Log.d("EverDialerCall", "external call intent action=$action data=$data target=$target")
                if (target != null && target.number.isNotBlank()) {
                    placeDirectCall(target.number, target.contactId)
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
        if (!DefaultDialerManager.isDefaultDialer(this)) {
            DefaultDialerManager.requestDefaultDialer(requestRoleLauncher, this)
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
            com.coolappstore.everdialer.by.svhp.controller.util.MissedCallBadgeManager.markMissedCallsAsRead(this)
        }
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
