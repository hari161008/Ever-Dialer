/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.coolappstore.evercallrecorder.by.svhp.system

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * BiometricAuthHelper.kt wraps [androidx.biometric.BiometricPrompt] so the App Lock feature can
 * trigger the system fingerprint/face unlock UI from a composable without boilerplate.
 */

/** The authenticator classes the App Lock feature accepts: any enrolled fingerprint or face unlock. */
private val APP_LOCK_BIOMETRIC_AUTHENTICATORS =
    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK

/** Why [Context.checkBiometricAvailability] thinks biometric unlock can or cannot be used right now. */
enum class BiometricAvailability {
    /** A fingerprint/face is enrolled and ready to use. */
    READY,
    /** This device has no biometric sensor at all. */
    NO_HARDWARE,
    /** The device has a sensor, but the user hasn't enrolled a fingerprint/face yet. */
    NONE_ENROLLED,
    /** Biometrics exist but are temporarily unusable (e.g. sensor busy, security update pending). */
    UNAVAILABLE
}

/** Checks whether this device can authenticate the user with biometrics right now. */
fun Context.checkBiometricAvailability(): BiometricAvailability {
    val canAuthenticate = BiometricManager.from(this).canAuthenticate(APP_LOCK_BIOMETRIC_AUTHENTICATORS)
    return when (canAuthenticate) {
        BiometricManager.BIOMETRIC_SUCCESS             -> BiometricAvailability.READY
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE-> BiometricAvailability.NO_HARDWARE
        else                                            -> BiometricAvailability.UNAVAILABLE
    }
}

/**
 * Remembers a function that shows the system biometric prompt and reports the outcome.
 *
 * Must be used from a composable hosted inside a [FragmentActivity] (true everywhere in this
 * app, since [com.coolappstore.evercallrecorder.by.svhp.MainActivity] is an `AppCompatActivity`).
 *
 * @param onSuccess Called on the main thread once the user is authenticated.
 * @param onError   Called with a human-readable message for real failures. User-initiated
 *                  cancellation (back press, tapping "Cancel") is intentionally NOT reported here.
 * @return A function you call with a prompt title/subtitle to show the biometric sheet.
 */
@Composable
fun rememberBiometricPrompt(
    onSuccess: () -> Unit,
    onError: (String) -> Unit = {}
): (title: String, subtitle: String?) -> Unit {
    val context = LocalContext.current
    val activity = remember(context) {
        var ctx: Context = context
        while (ctx is ContextWrapper && ctx !is FragmentActivity) {
            ctx = ctx.baseContext
        }
        ctx as? FragmentActivity
    }
    val currentOnSuccess by rememberUpdatedState(onSuccess)
    val currentOnError by rememberUpdatedState(onError)

    val prompt = remember(activity) {
        activity?.let { fragmentActivity ->
            BiometricPrompt(
                fragmentActivity,
                ContextCompat.getMainExecutor(fragmentActivity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        currentOnSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        val isUserDismissal = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                            errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                            errorCode == BiometricPrompt.ERROR_CANCELED
                        if (!isUserDismissal) currentOnError(errString.toString())
                    }

                    override fun onAuthenticationFailed() {
                        // A single failed scan attempt; the system prompt stays open for a retry.
                    }
                }
            )
        }
    }

    return { title, subtitle ->
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(APP_LOCK_BIOMETRIC_AUTHENTICATORS)
            .setConfirmationRequired(false)
        if (subtitle != null) builder.setSubtitle(subtitle)
        val info = builder.build()
        val activePrompt = prompt
        if (activePrompt != null) {
            activePrompt.authenticate(info)
        } else {
            currentOnError("Biometric authentication is not available right now.")
        }
    }
}
