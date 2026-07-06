package com.coolappstore.everdialer.by.svhp.controller.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Describes one button shown on the ongoing call screen ("Feature Buttons") plus the
 * persistence layer for its visibility/ordering, shared between the
 * Settings → Appearance → Caller UI screen and the actual [CallActivity] UI so that any change
 * made in Settings is reflected immediately on the real call screen.
 */
data class CallButtonSpec(
    val id: String,
    val label: String,
    val icon: ImageVector
)

object CallButtonPrefs {

    const val ID_HOLD      = "hold"
    const val ID_ADD       = "add"
    const val ID_DIALPAD   = "dialpad"
    const val ID_NOTE      = "note"
    const val ID_MUTE      = "mute"
    const val ID_SPEAKER   = "speaker"
    const val ID_BLUETOOTH = "bluetooth"
    const val ID_MORE      = "more"
    const val ID_HANGUP    = "hangup"

    /** Buttons that can never be hidden by the user. */
    val ALWAYS_ENABLED = setOf(ID_HANGUP)

    val SPECS: List<CallButtonSpec> = listOf(
        CallButtonSpec(ID_HOLD,      "Hold",       Icons.Default.Pause),
        CallButtonSpec(ID_ADD,       "Add Person", Icons.Default.PersonAdd),
        CallButtonSpec(ID_DIALPAD,   "Dialpad",    Icons.Default.Dialpad),
        CallButtonSpec(ID_NOTE,      "Note",       Icons.Default.EditNote),
        CallButtonSpec(ID_MUTE,      "Mute",       Icons.Default.Mic),
        CallButtonSpec(ID_SPEAKER,   "Speaker",    Icons.Default.VolumeDown),
        CallButtonSpec(ID_BLUETOOTH, "Bluetooth",  Icons.Default.Bluetooth),
        CallButtonSpec(ID_MORE,      "More",       Icons.Default.MoreHoriz),
        CallButtonSpec(ID_HANGUP,    "Hang Up",    Icons.Default.CallEnd)
    )

    val ALL_IDS: List<String> = SPECS.map { it.id }

    const val DEFAULT_ORDER = "hold,add,dialpad,note,mute,speaker,bluetooth,more,hangup"
    // "more" is new — off by default so existing users see the same buttons as before.
    const val DEFAULT_DISABLED = "more"

    fun specFor(id: String): CallButtonSpec? = SPECS.find { it.id == id }

    /** Ordered list of button ids, guaranteed to contain every known id exactly once. */
    fun getOrder(prefs: PreferenceManager): List<String> {
        val raw = prefs.getString(PreferenceManager.KEY_CALL_BUTTONS_ORDER, DEFAULT_ORDER) ?: DEFAULT_ORDER
        val stored = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() && it in ALL_IDS }
        val missing = ALL_IDS.filter { it !in stored }
        val combined = (stored + missing).toMutableList()
        // Hangup always rendered as the final, dedicated action.
        combined.remove(ID_HANGUP)
        combined.add(ID_HANGUP)
        return combined
    }

    fun setOrder(prefs: PreferenceManager, order: List<String>) {
        prefs.setString(PreferenceManager.KEY_CALL_BUTTONS_ORDER, order.joinToString(","))
    }

    fun getDisabled(prefs: PreferenceManager): Set<String> {
        val raw = prefs.getString(PreferenceManager.KEY_CALL_BUTTONS_DISABLED, DEFAULT_DISABLED) ?: DEFAULT_DISABLED
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet() - ALWAYS_ENABLED
    }

    fun setDisabled(prefs: PreferenceManager, disabled: Set<String>) {
        prefs.setString(PreferenceManager.KEY_CALL_BUTTONS_DISABLED, (disabled - ALWAYS_ENABLED).joinToString(","))
    }

    fun isEnabled(prefs: PreferenceManager, id: String): Boolean =
        id in ALWAYS_ENABLED || id !in getDisabled(prefs)

    fun setEnabled(prefs: PreferenceManager, id: String, enabled: Boolean) {
        if (id in ALWAYS_ENABLED) return
        val current = getDisabled(prefs).toMutableSet()
        if (enabled) current.remove(id) else current.add(id)
        setDisabled(prefs, current)
    }

    /** Ordered, enabled ids excluding hangup (which is rendered separately as the end-call action). */
    fun getActiveActionIds(prefs: PreferenceManager): List<String> {
        val disabled = getDisabled(prefs)
        return getOrder(prefs).filter { it != ID_HANGUP && it !in disabled }
    }
}
