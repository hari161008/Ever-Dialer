/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.coolappstore.evercallrecorder.by.svhp.utils

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Small helper used by the App Lock feature to turn a PIN or password the user types into a
 * salted hash before it is persisted, so the plaintext secret never sits in SharedPreferences.
 *
 * This is intentionally lightweight (single-round SHA-256 with a random salt) - it is meant to
 * stop a casual reading of the prefs file from revealing the unlock code, not to resist offline
 * brute-forcing on a rooted device. The real gatekeeper is the Android app-lock UI itself.
 */
object AppLockCrypto {

    private const val SALT_BYTE_LENGTH = 16

    /** Generates a fresh random salt, encoded as a Base64 string so it can be stored as text. */
    fun generateSalt(): String {
        val bytes = ByteArray(SALT_BYTE_LENGTH)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /** Hashes [secret] together with [salt], returning a hex-encoded SHA-256 digest. */
    fun hash(secret: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt.toByteArray(Charsets.UTF_8))
        val bytes = digest.digest(secret.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** Re-hashes [secret] with the stored [salt] and compares it against [expectedHash]. */
    fun verify(secret: String, salt: String, expectedHash: String): Boolean {
        return hash(secret, salt) == expectedHash
    }
}
