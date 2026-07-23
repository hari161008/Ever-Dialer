package com.coolappstore.everdialer.by.svhp.controller.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Describes one button shown on the ongoing call screen ("Feature Buttons") plus the
 * persistence layer for its visibility/ordering, shared between the
 * Settings → Appearance → Caller UI screen and the actual [CallActivity] UI so that any change
 * made in Settings is reflected immediately on the real call screen.
 *
 * [color] is only used for the Settings-side previews (icon chips, drag grid) so each button is
 * easy to tell apart at a glance — the live call screen keeps its own deliberate monochrome style.
 */
data class CallButtonSpec(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val color: Color
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
    const val ID_RECORD    = "record"
    const val ID_HANGUP    = "hangup"

    /** Buttons that can never be hidden by the user. */
    val ALWAYS_ENABLED = setOf(ID_HANGUP)

    val SPECS: List<CallButtonSpec> = listOf(
        CallButtonSpec(ID_HOLD,      "Hold",       Icons.Default.Pause,      Color(0xFFFFA000)),
        CallButtonSpec(ID_ADD,       "Add Person", Icons.Default.PersonAdd,  Color(0xFF009688)),
        CallButtonSpec(ID_DIALPAD,   "Dialpad",    Icons.Default.Dialpad,    Color(0xFF3F51B5)),
        CallButtonSpec(ID_NOTE,      "Note",       Icons.Default.EditNote,   Color(0xFF2196F3)),
        CallButtonSpec(ID_MUTE,      "Mute",       Icons.Default.Mic,        Color(0xFFE53935)),
        CallButtonSpec(ID_SPEAKER,   "Speaker",    Icons.Default.VolumeDown, Color(0xFF4CAF50)),
        CallButtonSpec(ID_BLUETOOTH, "Bluetooth",  Icons.Default.Bluetooth,  Color(0xFF1976D2)),
        CallButtonSpec(ID_MORE,      "More",       Icons.Default.MoreHoriz,  Color(0xFF757575)),
        CallButtonSpec(ID_RECORD,    "Record",     Icons.Default.FiberManualRecord, Color(0xFFE53935)),
        CallButtonSpec(ID_HANGUP,    "Hang Up",    Icons.Default.CallEnd,    Color(0xFFD32F2F))
    )

    val ALL_IDS: List<String> = SPECS.map { it.id }

    const val DEFAULT_ORDER = "hold,add,dialpad,note,mute,speaker,bluetooth,more,record,hangup"
    // "more" and "record" are new — both off by default (unticked in the 3-dot menu) so
    // existing users see the same buttons as before until they opt in.
    const val DEFAULT_DISABLED = "more,record"

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

    // ── Freeform layout ──────────────────────────────────────────────────────────────
    // When enabled, Feature Buttons are placed at custom (x, y) positions instead of the
    // fixed 3-per-row grid. Shared between the Settings preview (draggable) and the real
    // ongoing-call screen (renders the same saved positions) so both stay in sync.

    fun isFreeformEnabled(prefs: PreferenceManager): Boolean =
        prefs.getBoolean(PreferenceManager.KEY_CALL_BUTTONS_FREEFORM, false)

    fun setFreeformEnabled(prefs: PreferenceManager, enabled: Boolean) {
        prefs.setBoolean(PreferenceManager.KEY_CALL_BUTTONS_FREEFORM, enabled)
    }

    /** Loads saved freeform positions as fractions (0f..1f) of the draggable area. Format: "id:x:y|id:x:y". */
    fun getFreeformPositions(prefs: PreferenceManager): Map<String, Pair<Float, Float>> {
        val raw = prefs.getString(PreferenceManager.KEY_CALL_BUTTONS_FREEFORM_POSITIONS, null) ?: return emptyMap()
        val map = mutableMapOf<String, Pair<Float, Float>>()
        raw.split("|").forEach { entry ->
            val parts = entry.split(":")
            if (parts.size == 3) {
                val x = parts[1].toFloatOrNull()
                val y = parts[2].toFloatOrNull()
                if (x != null && y != null) map[parts[0]] = x.coerceIn(0f, 1f) to y.coerceIn(0f, 1f)
            }
        }
        return map
    }

    fun setFreeformPositions(prefs: PreferenceManager, positions: Map<String, Pair<Float, Float>>) {
        val raw = positions.entries.joinToString("|") { (id, xy) -> "$id:${xy.first}:${xy.second}" }
        prefs.setString(PreferenceManager.KEY_CALL_BUTTONS_FREEFORM_POSITIONS, raw)
    }

    /** Default freeform fraction for a button at [index] within [totalCount], matching where it
     *  would sit in the normal 3-per-row grid — so turning Freeform on doesn't jumble the layout. */
    fun defaultFreeformFraction(index: Int, totalCount: Int): Pair<Float, Float> {
        val rows = if (totalCount <= 0) 1 else ((totalCount + 2) / 3)
        val col = index % 3
        val row = index / 3
        val x = (col + 0.5f) / 3f
        val y = (row + 0.5f) / rows
        return x to y
    }
}
