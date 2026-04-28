package com.coolappstore.everdialer.by.svhp

import android.Manifest
import android.app.DownloadManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.enqueueApkDownload
import com.coolappstore.everdialer.by.svhp.controller.util.fetchLatestRelease
import com.coolappstore.everdialer.by.svhp.controller.util.getApkDestinationFile
import com.coolappstore.everdialer.by.svhp.controller.util.installApkAndScheduleDelete
import com.coolappstore.everdialer.by.svhp.controller.util.isNewerVersion
import com.coolappstore.everdialer.by.svhp.view.theme.Rivo4Theme
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.ContactDetailsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.DialPadScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ContactEditScreenDestination
import kotlinx.coroutines.delay
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.GlobalContext.startKoin

class MainActivity : ComponentActivity() {

    private val requestRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ -> }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        requestDefaultDialer()
    }

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
        requestDefaultDialer()

        setContent {
            Rivo4Theme {
                val navController = rememberNavController()

                // ── Auto update check on launch ───────────────────────────────
                val prefs = remember {
                    GlobalContext.get().get<PreferenceManager>()
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

                // Poll download progress for auto-update
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

                // ── Auto update confirmation popup ────────────────────────────
                if (showAutoUpdateDialog) {
                    AlertDialog(
                        onDismissRequest = { showAutoUpdateDialog = false },
                        icon = { Icon(Icons.Default.SystemUpdate, null, tint = Color(0xFF2196F3)) },
                        title = { Text("Update Available") },
                        text = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Version v${autoUpdateVersion} is available.")
                                Text(
                                    "Would you like to download and install it now?",
                                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
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

                // ── Auto-update download progress dialog ──────────────────────
                if (showAutoDownloadProgress) {
                    Dialog(onDismissRequest = {}) {
                        androidx.compose.material3.Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(Icons.Default.SystemUpdate, null, tint = Color(0xFF2196F3),
                                    modifier = Modifier.size(36.dp))
                                Text("Downloading Update", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                                Text("v${autoUpdateVersion ?: ""}", style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    androidx.compose.material3.LinearProgressIndicator(
                                        progress = { autoDownloadProgress },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${(autoDownloadProgress * 100).toInt()}%",
                                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Please wait…",
                                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                DestinationsNavHost(
                    navGraph = NavGraphs.root,
                    navController = navController
                )

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
        val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
        if (!roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
            requestRoleLauncher.launch(intent)
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
