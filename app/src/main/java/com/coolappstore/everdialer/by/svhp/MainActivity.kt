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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
import com.coolappstore.everdialer.by.svhp.view.components.BottomBar
import com.coolappstore.everdialer.by.svhp.view.theme.Rivo4Theme
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
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.GlobalContext.startKoin

class MainActivity : ComponentActivity() {

    private val requestRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ -> }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> /* permissions result; dialer popup now shown after welcome */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(this@MainActivity)
                modules(appModule)
            }
        }

        requestRequiredPermissions()
        // On first launch, show default dialer prompt first; welcome dialog appears after.
        requestDefaultDialer()

        setContent {
            Rivo4Theme {
                val navController = rememberNavController()

                val prefs = remember {
                    GlobalContext.get().get<PreferenceManager>()
                }

                val isFirstLaunch = remember {
                    !prefs.getBoolean(PreferenceManager.KEY_FIRST_LAUNCH_DONE, false)
                }

                // ── First Launch Welcome Dialog ─────────────────────────────
                // Show AFTER the default dialer prompt (which fires in onCreate)
                var showWelcomeDialog by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    if (isFirstLaunch) {
                        // Small delay so the default dialer system dialog appears first
                        kotlinx.coroutines.delay(600)
                        showWelcomeDialog = true
                    }
                }

                if (showWelcomeDialog) {
                    Dialog(onDismissRequest = {}) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            tonalElevation = 6.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "Welcome to Ever Dialer",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Since this app isn't installed from Playstore, In Android 14+ it is not possible to set as a default dialer without allowing \"Allow restricted settings\"\n\nSo to enable this settings, you have to long press the Ever Dialer App icon in your launcher, Click App info (which opens App info) > On top right corner > Click \"Allow restricted settings\"\n\nThen come back to the app and Enjoy :)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Start
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        onClick = {
                                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.fromParts("package", packageName, null)
                                            }
                                            startActivity(intent)
                                        },
                                        shape = RoundedCornerShape(50.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "App Info",
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Surface(
                                        onClick = {
                                            prefs.setBoolean(PreferenceManager.KEY_FIRST_LAUNCH_DONE, true)
                                            showWelcomeDialog = false
                                        },
                                        shape = RoundedCornerShape(50.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "Continue",
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // On subsequent launches, requestDefaultDialer is called in onCreate

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
                    AlertDialog(
                        onDismissRequest = { showAutoUpdateDialog = false },
                        icon = { Icon(Icons.Default.SystemUpdate, null, tint = Color(0xFF2196F3)) },
                        title = { Text("Update Available") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Version v${autoUpdateVersion} is available.")
                                Text(
                                    "Would you like to download and install it now?",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
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
                            }) { Text("Download") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAutoUpdateDialog = false }) { Text("Not Now") }
                        }
                    )
                }

                if (showAutoDownloadProgress) {
                    Dialog(onDismissRequest = {}) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(Icons.Default.SystemUpdate, null, tint = Color(0xFF2196F3), modifier = Modifier.size(36.dp))
                                Text("Downloading Update", style = MaterialTheme.typography.titleMedium)
                                Text("v${autoUpdateVersion ?: ""}", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                    LinearProgressIndicator(progress = { autoDownloadProgress }, modifier = Modifier.fillMaxWidth())
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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

                // ── Ongoing Call Banner + Main nav host ───────────────────────
                val callSession by CallService.currentCallSession.collectAsState()
                val hasOngoingCall = callSession != null && callSession?.state != android.telecom.Call.STATE_RINGING

                Column(modifier = Modifier.fillMaxSize()) {
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
                    val notesEnabled = prefs2.getBoolean(PreferenceManager.KEY_NOTES_ENABLED, true)

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
                                        if (notesEnabled) {
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
                            // ── Main content fills the rest ────────────────────────
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                DestinationsNavHost(navGraph = NavGraphs.root, navController = navController)
                            }
                        }
                    } else {
                        Scaffold(
                            bottomBar = { BottomBar(navController) },
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentWindowInsets = WindowInsets(0)
                        ) { scaffoldPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(scaffoldPadding)
                                    .then(
                                        if (hasOngoingCall)
                                            Modifier.consumeWindowInsets(WindowInsets.statusBars)
                                        else
                                            Modifier
                                    )
                            ) {
                                DestinationsNavHost(
                                    navGraph    = NavGraphs.root,
                                    navController = navController
                                )
                            }
                        }
                    }
                }

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
