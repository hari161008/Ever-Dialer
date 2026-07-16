package com.supernova.networkswitch.data.source

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.supernova.networkswitch.IShizukuController
import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.service.ShizukuControllerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume
import com.supernova.networkswitch.BuildConfig
class  ShizukuNetworkControlDataSource constructor(
    private val context: Context
) : NetworkControlDataSource {
    
    private var userService: IShizukuController? = null
    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    companion object {
        // Not private anymore: MainActivity/SettingsActivity register a
        // Shizuku.OnRequestPermissionResultListener and need to match this code
        // against the requestCode they receive.
        const val SHIZUKU_PERMISSION_REQUEST_ID = 8

        /** True once we've already auto-prompted for permission this process, so we
         *  don't re-trigger the system dialog on every single recomposition/resume —
         *  the explicit "Retry" button in the UI can still always re-request. */
        @Volatile private var hasAutoRequestedPermission = false

        /** Fires right after the user answers the Shizuku permission dialog (granted or
         *  denied), so the UI can immediately refresh instead of waiting for the user to
         *  background/foreground the app again. Set by MainActivity/SettingsActivity. */
        @Volatile var onPermissionResult: (() -> Unit)? = null

        private val permissionResultListener = object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                if (requestCode == SHIZUKU_PERMISSION_REQUEST_ID) {
                    onPermissionResult?.invoke()
                }
            }
        }

        @Volatile private var listenerRegistered = false

        /** Registers the permission-result listener exactly once for the process
         *  lifetime — matching the pattern used by Ever Call Recorder's
         *  ShizukuConnectionManager, which pairs every requestPermission() call with a
         *  registered Shizuku.OnRequestPermissionResultListener. */
        private fun ensureListenerRegistered() {
            if (listenerRegistered) return
            try {
                Shizuku.addRequestPermissionResultListener(permissionResultListener)
                listenerRegistered = true
            } catch (_: Exception) {}
        }
    }

    /**
     * Requests Shizuku permission from the user, mirroring how Ever Call Recorder's
     * ShizukuConnectionManager does it: trigger the system permission dialog directly
     * instead of just reporting "permission denied" and waiting for the user to find a
     * manual grant button.
     */
    fun requestPermission() {
        ensureListenerRegistered()
        try {
            if (!Shizuku.pingBinder()) return
        } catch (_: Exception) { return }
        val alreadyGranted = try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) { false }
        if (alreadyGranted) return
        try {
            Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_ID)
        } catch (_: Exception) {}
    }

    override suspend fun checkCompatibility(subId: Int): CompatibilityState {
        return try {
            // Shizuku's binder can take a brief moment to attach right after the app
            // or the Shizuku service itself starts (e.g. right after boot, or right
            // after the user just enabled Shizuku) — a single pingBinder() check can
            // falsely report "service not running" even though it becomes available
            // a fraction of a second later. Retry briefly instead of failing fast,
            // to avoid that false reading.
            var pinged = Shizuku.pingBinder()
            var attempts = 0
            while (!pinged && attempts < 4) {
                kotlinx.coroutines.delay(250)
                pinged = Shizuku.pingBinder()
                attempts++
            }
            if (!pinged) {
                return CompatibilityState.Incompatible("Shizuku service not running")
            }

            // Check if Shizuku permission is granted
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                // Auto-trigger the system permission dialog the first time we detect
                // it's missing, instead of silently reporting "denied" and relying on
                // the user to discover a manual grant action.
                if (!hasAutoRequestedPermission) {
                    hasAutoRequestedPermission = true
                    requestPermission()
                }
                return CompatibilityState.PermissionDenied(com.supernova.networkswitch.domain.model.ControlMethod.SHIZUKU)
            }
            
            // Both service and permission checks passed
            CompatibilityState.Compatible
        } catch (e: Exception) {
            CompatibilityState.Incompatible("Shizuku not available: ${e.message}")
        }
    }

    override suspend fun getCurrentNetworkMode(subId: Int): NetworkMode? {
        return if (ensureServiceBinding()) {
            try {
                val modeValue = userService?.getCurrentNetworkMode(subId) ?: -1
                if (modeValue == -1) null else NetworkMode.fromValue(modeValue)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    override suspend fun setNetworkMode(subId: Int, mode: NetworkMode) {
        if (ensureServiceBinding()) {
            try {
                userService?.setNetworkMode(subId, mode.value)
            } catch (e: Exception) {
                throw e
            }
        } else {
            throw SecurityException("Shizuku permission not granted or service binding failed")
        }
    }

    override fun isConnected(): Boolean = _isConnected.value
    
    override fun resetConnection() {
        userService = null
        _isConnected.value = false
    }

    /**
     * Simple permission and service check without delays or complex logic
     */
    private fun hasPermissionAndService(): Boolean {
        return try {
            // Only check if service is already connected, don't call Shizuku APIs that might block
            userService != null && _isConnected.value
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Ensure service binding for actual operations (not compatibility checks)
     */
    private suspend fun ensureServiceBinding(): Boolean {
        // Check permissions first
        if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        
        // If service is already connected, return true
        if (userService != null && _isConnected.value) {
            return true
        }
        
        // Bind service asynchronously with timeout
        return try {
            suspendCancellableCoroutine { continuation ->
                val args = Shizuku.UserServiceArgs(ComponentName(context, ShizukuControllerService::class.java))
                    .processNameSuffix("service")
                    .debuggable(BuildConfig.DEBUG)
                    .version(1)
                    .tag("NetworkSwitch")

                val serviceConnection = object : ServiceConnection {
                    override fun onServiceConnected(componentName: ComponentName?, binder: IBinder?) {
                        if (binder != null && binder.pingBinder()) {
                            userService = IShizukuController.Stub.asInterface(binder)
                            _isConnected.value = true
                            if (continuation.isActive) {
                                continuation.resume(true)
                            }
                        } else {
                            if (continuation.isActive) {
                                continuation.resume(false)
                            }
                        }
                    }

                    override fun onServiceDisconnected(componentName: ComponentName?) {
                        userService = null
                        _isConnected.value = false
                    }
                }

                continuation.invokeOnCancellation {
                    userService = null
                    _isConnected.value = false
                }

                try {
                    Shizuku.bindUserService(args, serviceConnection)
                } catch (e: Exception) {
                    if (continuation.isActive) {
                        continuation.resume(false)
                    }
                }
            }
        } catch (e: Exception) {
            _isConnected.value = false
            false
        }
    }
}
