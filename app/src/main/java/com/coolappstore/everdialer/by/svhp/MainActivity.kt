package com.coolappstore.everdialer.by.svhp

import android.Manifest
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
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.coolappstore.everdialer.by.svhp.view.theme.Rivo4Theme
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.ContactDetailsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.DialPadScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ContactEditScreenDestination
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.GlobalContext.startKoin

class MainActivity : ComponentActivity() {

    // Step 2: after permissions result → ask for default dialer role
    private val requestRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ -> }

    // Step 1: ask all runtime permissions → on result → request default dialer
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
        // Also attempt immediately in case all permissions were already granted
        // (requestPermissionsLauncher callback won't fire if system skips the dialog)
        requestDefaultDialer()

        setContent {
            Rivo4Theme {
                val navController = rememberNavController()

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
