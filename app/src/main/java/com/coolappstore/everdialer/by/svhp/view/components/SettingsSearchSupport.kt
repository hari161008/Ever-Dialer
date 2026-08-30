package com.coolappstore.everdialer.by.svhp.view.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Reviews
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.NetworkCell
import androidx.compose.material.icons.outlined.NoteAlt
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.PhoneCallback
import androidx.compose.material.icons.outlined.PhoneDisabled
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material.icons.outlined.ScreenLockPortrait
import androidx.compose.material.icons.outlined.SwipeVertical
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.coolappstore.everdialer.by.svhp.APP_VERSION
import com.coolappstore.everdialer.by.svhp.view.screen.settings.CardDivider
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.ViewStream
import androidx.compose.material.icons.outlined.PictureInPicture
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.CallReceived
import androidx.compose.material.icons.outlined.CallMade
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Notifications
import com.ramcosta.composedestinations.generated.destinations.AboutAppScreenDestination
import com.ramcosta.composedestinations.generated.destinations.AppSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.BiometricScreenDestination
import com.ramcosta.composedestinations.generated.destinations.CallSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.CallerUIScreenDestination
import com.ramcosta.composedestinations.generated.destinations.IncomingCallUIScreenDestination
import com.ramcosta.composedestinations.generated.destinations.InterfaceScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RaiseToAnswerScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RainModeScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RecordingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SoundVibrationScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay

/**
 * Applied to a settings row so that tapping a search result can reveal *where* that setting
 * lives (scrolling it into view and flashing it) instead of silently firing its action.
 *
 * Shared across SettingsScreen and every settings sub-screen so a search result that points
 * into a nested screen (e.g. "Auto Redial" inside Call Settings) can be scrolled to and
 * highlighted there too, after navigating with a `highlightKey` nav arg.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.settingsSearchHighlight(
    key: String,
    highlightedKey: String?,
    onConsumed: () -> Unit
): Modifier {
    val requester = remember(key) { BringIntoViewRequester() }
    val isHighlighted = highlightedKey == key
    val flash = remember(key) { Animatable(0f) }
    val highlightColor = MaterialTheme.colorScheme.primary
    LaunchedEffect(isHighlighted) {
        if (isHighlighted) {
            // The target row may not have completed its first layout pass yet — this fires
            // right when we switch from search results back to the full list, or right as a
            // freshly-navigated screen composes. Wait a few frames and retry so bringIntoView()
            // always has valid layout coordinates to scroll to.
            var attempt = 0
            var succeeded = false
            while (!succeeded && attempt < 6) {
                try {
                    requester.bringIntoView()
                    succeeded = true
                } catch (_: Exception) {
                    // Not laid out yet — wait and retry.
                    delay(if (attempt == 0) 120L else 60L)
                }
                attempt++
            }
            flash.snapTo(1f)
            flash.animateTo(0f, animationSpec = tween(1200))
            onConsumed()
        }
    }
    return this
        .bringIntoViewRequester(requester)
        .drawWithContent {
            drawContent()
            if (flash.value > 0f) {
                drawRoundRect(
                    color = highlightColor.copy(alpha = 0.30f * flash.value),
                    cornerRadius = CornerRadius(16.dp.toPx())
                )
            }
        }
}

/**
 * A search result for the cross-page settings search box (see [SettingsSearchEntryPoint]).
 * Unlike the local search on the main Settings screen (which can just flash a row in place),
 * every entry here always navigates somewhere — either into the sub-screen that owns the
 * setting, or back to the main Settings screen with `highlightKey` set, so it works no matter
 * which settings page the search was performed from.
 */
data class GlobalSettingsSearchEntry(
    val title: String,
    val subtitle: String,
    val key: String,
    val icon: ImageVector,
    val iconContainerColor: Color,
    val navigateTo: (DestinationsNavigator) -> Unit
)

private val GsColorPurple  = Color(0xFF9C27B0)
private val GsColorBlue    = Color(0xFF2196F3)
private val GsColorGreen   = Color(0xFF4CAF50)
private val GsColorTeal    = Color(0xFF009688)
private val GsColorIndigo  = Color(0xFF3F51B5)
private val GsColorBluGrey = Color(0xFF607D8B)
private val GsColorAmber   = Color(0xFFFFC107)
private val GsColorBrown   = Color(0xFF795548)
private val GsColorCyan    = Color(0xFF00BCD4)
private val GsColorRed     = Color(0xFFE53935)
private val GsColorPink    = Color(0xFFE91E63)

/** The full, flat list of every setting reachable from Settings, used by [SettingsSearchEntryPoint]
 *  so the same search box can be dropped onto any settings page. */
fun buildGlobalSettingsSearchEntries(): List<GlobalSettingsSearchEntry> = listOf(
    // ── Rows that live directly on the main Settings screen ─────────────────
    GlobalSettingsSearchEntry("Check For Updates", "Current version: v$APP_VERSION", "check_for_updates", Icons.Default.SystemUpdate, GsColorAmber) { it.navigate(SettingsScreenDestination(highlightKey = "check_for_updates")) },
    GlobalSettingsSearchEntry("Rate and Review", "Share your feedback about Ever Dialer", "rate_and_review", Icons.Default.Star, GsColorCyan) { it.navigate(SettingsScreenDestination(highlightKey = "rate_and_review")) },
    GlobalSettingsSearchEntry("Check Ratings and Reviews", "See what others are saying about Ever Dialer", "check_ratings", Icons.Default.Reviews, GsColorGreen) { it.navigate(SettingsScreenDestination(highlightKey = "check_ratings")) },
    GlobalSettingsSearchEntry("More Apps", "Check out other apps from the developer", "more_apps", Icons.Default.Apps, GsColorIndigo) { it.navigate(SettingsScreenDestination(highlightKey = "more_apps")) },
    GlobalSettingsSearchEntry("Interface", "Themes, colors, and layout", "interface", Icons.Outlined.Palette, GsColorPurple) { it.navigate(SettingsScreenDestination(highlightKey = "interface")) },
    GlobalSettingsSearchEntry("Tap Haptics", "Vibration on taps across the app", "tap_haptics", Icons.Outlined.Vibration, GsColorPurple) { it.navigate(SettingsScreenDestination(highlightKey = "tap_haptics")) },
    GlobalSettingsSearchEntry("Scroll Haptics", "Vibrate on scroll gestures across the app", "scroll_haptics", Icons.Outlined.SwipeVertical, GsColorIndigo) { it.navigate(SettingsScreenDestination(highlightKey = "scroll_haptics")) },
    GlobalSettingsSearchEntry("Authentication", "App lock, biometrics, and PIN/password", "authentication", Icons.Default.Fingerprint, Color(0xFF6750A4)) { it.navigate(SettingsScreenDestination(highlightKey = "authentication")) },
    GlobalSettingsSearchEntry("App Settings", "Call settings, network switcher, and notes", "app_settings", Icons.Outlined.Tune, GsColorTeal) { it.navigate(SettingsScreenDestination(highlightKey = "app_settings")) },
    GlobalSettingsSearchEntry("Contacts Hider", "Hide contacts behind a secret code", "contacts_hider", Icons.Outlined.Lock, Color(0xFF5E35B1)) { it.navigate(SettingsScreenDestination(highlightKey = "contacts_hider")) },
    GlobalSettingsSearchEntry("Fake Call", "Schedule fake incoming calls without calling the real person", "fake_call", Icons.Outlined.PhoneCallback, GsColorRed) { it.navigate(SettingsScreenDestination(highlightKey = "fake_call")) },
    GlobalSettingsSearchEntry("Call Recording", "Open Ever Call Recorder", "call_recording", Icons.Default.FiberManualRecord, Color(0xFFE53935)) { it.navigate(SettingsScreenDestination(highlightKey = "call_recording")) },
    GlobalSettingsSearchEntry("Silence Unknown Callers", "Automatically decline calls from unknown numbers", "silence_unknown", Icons.Outlined.PhoneDisabled, GsColorRed) { it.navigate(SettingsScreenDestination(highlightKey = "silence_unknown")) },
    GlobalSettingsSearchEntry("Blocked Numbers", "Numbers you've blocked from calling you", "blocked_numbers", Icons.Outlined.PersonOff, GsColorBluGrey) { it.navigate(SettingsScreenDestination(highlightKey = "blocked_numbers")) },
    GlobalSettingsSearchEntry("Auto Check For Updates", "Automatically check for updates when the app opens", "auto_check_updates", Icons.Default.Autorenew, GsColorAmber) { it.navigate(SettingsScreenDestination(highlightKey = "auto_check_updates")) },
    GlobalSettingsSearchEntry("Create Backup", "Save app configuration, settings and calling cards", "create_backup", Icons.Default.Backup, GsColorGreen) { it.navigate(SettingsScreenDestination(highlightKey = "create_backup")) },
    GlobalSettingsSearchEntry("Restore Backup", "Restore app configuration, settings and calling cards", "restore_backup", Icons.Default.Restore, GsColorBrown) { it.navigate(SettingsScreenDestination(highlightKey = "restore_backup")) },
    GlobalSettingsSearchEntry("About Ever Dialer", "Version $APP_VERSION · Developer info", "about_app", Icons.Outlined.Info, GsColorBluGrey) { it.navigate(SettingsScreenDestination(highlightKey = "about_app")) },

    // ── App Settings screen ──────────────────────────────────────────────────
    GlobalSettingsSearchEntry("Call Settings", "SIM, contacts to display, call behavior", "nav_call_settings", Icons.Outlined.Call, GsColorTeal) { it.navigate(AppSettingsScreenDestination(highlightKey = "nav_call_settings")) },
    GlobalSettingsSearchEntry("4G/5G Switcher", "Quickly switch network mode per app", "network_switcher", Icons.Outlined.NetworkCell, GsColorBlue) { it.navigate(AppSettingsScreenDestination(highlightKey = "network_switcher")) },
    GlobalSettingsSearchEntry("Integrate Notes Section", "Show notes alongside call recordings", "integrate_notes", Icons.Outlined.Notes, GsColorGreen) { it.navigate(AppSettingsScreenDestination(highlightKey = "integrate_notes")) },
    GlobalSettingsSearchEntry("Delete Notes With Recording", "Remove the note when its recording is deleted", "delete_notes_with_recording", Icons.Outlined.NoteAlt, GsColorRed) { it.navigate(AppSettingsScreenDestination(highlightKey = "delete_notes_with_recording")) },

    // ── Call Settings screen ─────────────────────────────────────────────────
    GlobalSettingsSearchEntry("Default SIM", "Which SIM is used to place calls", "default_sim", Icons.Outlined.SimCard, GsColorGreen) { it.navigate(CallSettingsScreenDestination(highlightKey = "default_sim")) },
    GlobalSettingsSearchEntry("Contacts to display", "Choose which accounts' contacts are shown", "contacts_to_display", Icons.Outlined.Contacts, GsColorBlue) { it.navigate(CallSettingsScreenDestination(highlightKey = "contacts_to_display")) },
    GlobalSettingsSearchEntry("Device Orientation with Proximity Sensor", "Combine orientation and proximity to prevent false screen-offs during a call", "proximity_orientation_bg", Icons.Outlined.ScreenLockPortrait, GsColorPink) { it.navigate(CallSettingsScreenDestination(highlightKey = "proximity_orientation_bg")) },
    GlobalSettingsSearchEntry("Proximity Sensor on in background", "Turn off screen when phone is near ear during a call", "proximity_sensor_bg", Icons.Outlined.Sensors, GsColorTeal) { it.navigate(CallSettingsScreenDestination(highlightKey = "proximity_sensor_bg")) },
    GlobalSettingsSearchEntry("Pocket Mode Prevention", "Block accidental answer/decline when phone is in pocket", "pocket_mode_prevention", Icons.Outlined.Sensors, GsColorAmber) { it.navigate(CallSettingsScreenDestination(highlightKey = "pocket_mode_prevention")) },
    GlobalSettingsSearchEntry("Floating Ongoing Call", "Draggable floating bubble during calls", "floating_ongoing_call", Icons.Outlined.Sensors, GsColorBlue) { it.navigate(CallSettingsScreenDestination(highlightKey = "floating_ongoing_call")) },
    GlobalSettingsSearchEntry("Direct Call on Tap", "Tap a call log entry to call directly", "direct_call_on_tap", Icons.Outlined.Call, GsColorGreen) { it.navigate(CallSettingsScreenDestination(highlightKey = "direct_call_on_tap")) },
    GlobalSettingsSearchEntry("Auto Speaker", "Switch to loudspeaker when phone is away from ear", "auto_speaker", Icons.Outlined.VolumeUp, GsColorRed) { it.navigate(CallSettingsScreenDestination(highlightKey = "auto_speaker")) },
    GlobalSettingsSearchEntry("Rain Mode", "Answer/decline calls with simultaneous 3-second volume button hold", "rain_mode_link", Icons.Outlined.WaterDrop, Color(0xFF0288D1)) { it.navigate(RainModeScreenDestination()) },
    GlobalSettingsSearchEntry("Auto Redial", "Automatically redial on rejected/unanswered/busy calls", "auto_redial", Icons.Default.Replay, GsColorBlue) { it.navigate(CallSettingsScreenDestination(highlightKey = "auto_redial")) },
    GlobalSettingsSearchEntry("Volume DND", "Toggle Do Not Disturb using volume button combination", "volume_dnd", Icons.Outlined.VolumeUp, GsColorPurple) { it.navigate(CallSettingsScreenDestination(highlightKey = "volume_dnd")) },

    // ── Rain Mode screen ─────────────────────────────────────────────────────
    GlobalSettingsSearchEntry("Enable Rain Mode", "Answer or decline calls using hardware volume buttons", "enable_rain_mode", Icons.Outlined.WaterDrop, Color(0xFF0288D1)) { it.navigate(RainModeScreenDestination(highlightKey = "enable_rain_mode")) },
    GlobalSettingsSearchEntry("Rain Mode Vibration Feedback", "Vibrate when call is answered or declined via Rain Mode", "rain_mode_vibrate", Icons.Outlined.Vibration, GsColorPurple) { it.navigate(RainModeScreenDestination(highlightKey = "rain_mode_vibrate")) },

    // ── Raise to Answer screen ───────────────────────────────────────────────
    GlobalSettingsSearchEntry("Enable Raise to Answer", "Answer calls by raising the phone to your ear", "enable_raise_to_answer", Icons.Outlined.Vibration, GsColorTeal) { it.navigate(RaiseToAnswerScreenDestination(highlightKey = "enable_raise_to_answer")) },
    GlobalSettingsSearchEntry("Answer at Any Angle", "Raise to Answer sensitivity", "answer_any_angle", Icons.Outlined.Vibration, GsColorTeal) { it.navigate(RaiseToAnswerScreenDestination(highlightKey = "answer_any_angle")) },
    GlobalSettingsSearchEntry("Decline by Flipping", "Flip the phone face down to decline a call", "decline_by_flipping", Icons.Outlined.Vibration, GsColorRed) { it.navigate(RaiseToAnswerScreenDestination(highlightKey = "decline_by_flipping")) },
    GlobalSettingsSearchEntry("Raise to Answer Beep Feedback", "Play a beep when raise/flip is detected", "raise_beep_feedback", Icons.Outlined.Vibration, GsColorAmber) { it.navigate(RaiseToAnswerScreenDestination(highlightKey = "raise_beep_feedback")) },
    GlobalSettingsSearchEntry("Raise to Answer Vibrate Feedback", "Vibrate when raise/flip is detected", "raise_vibrate_feedback", Icons.Outlined.Vibration, GsColorPurple) { it.navigate(RaiseToAnswerScreenDestination(highlightKey = "raise_vibrate_feedback")) },

    // ── Ever Call Recorder Settings ──────────────────────────────────────────
    GlobalSettingsSearchEntry("Call Recording Master Switch", "Enable or disable all background call recording", "call_recording_master", Icons.Default.FiberManualRecord, Color(0xFFE53935)) { it.navigate(RecordingsScreenDestination(openedFromSettings = true)) },
    GlobalSettingsSearchEntry("Auto Record Incoming Calls", "Automatically record incoming calls from all or specific contacts", "auto_record_incoming", Icons.Outlined.CallReceived, GsColorGreen) { it.navigate(RecordingsScreenDestination(openedFromSettings = true)) },
    GlobalSettingsSearchEntry("Auto Record Outgoing Calls", "Automatically record outgoing calls to all or specific contacts", "auto_record_outgoing", Icons.Outlined.CallMade, GsColorBlue) { it.navigate(RecordingsScreenDestination(openedFromSettings = true)) },
    GlobalSettingsSearchEntry("Recording Storage Location", "Choose custom folder or app-private storage for recordings", "recording_storage", Icons.Outlined.Folder, GsColorAmber) { it.navigate(RecordingsScreenDestination(openedFromSettings = true)) },
    GlobalSettingsSearchEntry("Recording Audio Source", "Microphone, media projection, or internal call stream", "recording_audio_source", Icons.Outlined.Mic, GsColorTeal) { it.navigate(RecordingsScreenDestination(openedFromSettings = true)) },
    GlobalSettingsSearchEntry("Recording Audio Codec", "Audio recording format (AAC, Opus, etc.)", "recording_audio_codec", Icons.Outlined.GraphicEq, GsColorPurple) { it.navigate(RecordingsScreenDestination(openedFromSettings = true)) },
    GlobalSettingsSearchEntry("Recording Sample Rate", "Audio quality sample rate (e.g. 48kHz, 44.1kHz)", "recording_sample_rate", Icons.Outlined.Equalizer, GsColorIndigo) { it.navigate(RecordingsScreenDestination(openedFromSettings = true)) },
    GlobalSettingsSearchEntry("Auto Delete Old Recordings", "Clean up call recordings older than 7/30/90 days", "auto_delete_recordings", Icons.Outlined.DeleteSweep, GsColorRed) { it.navigate(RecordingsScreenDestination(openedFromSettings = true)) },
    GlobalSettingsSearchEntry("Recording Notifications", "Show persistent notification while recording calls", "recording_notifications", Icons.Outlined.Notifications, GsColorAmber) { it.navigate(RecordingsScreenDestination(openedFromSettings = true)) },
    GlobalSettingsSearchEntry("Post-Recording File Actions", "Quick play, share, or delete notification after call ends", "post_recording_actions", Icons.Outlined.DoneAll, GsColorGreen) { it.navigate(RecordingsScreenDestination(openedFromSettings = true)) },
    GlobalSettingsSearchEntry("Call Recording App Lock", "Require PIN or biometric authentication for recordings", "recording_app_lock", Icons.Default.Fingerprint, Color(0xFF6750A4)) { it.navigate(RecordingsScreenDestination(openedFromSettings = true)) },
    GlobalSettingsSearchEntry("Separate Audio Channels", "Record caller and receiver on left and right channels", "separate_channels", Icons.Outlined.Headphones, GsColorCyan) { it.navigate(RecordingsScreenDestination(openedFromSettings = true)) },

    // ── 4G/5G Network Switch Settings ─────────────────────────────────────────
    GlobalSettingsSearchEntry("4G/5G Network Switcher", "Force LTE/NR network mode or per-app automation", "network_switcher_app", Icons.Outlined.NetworkCell, GsColorBlue) { it.navigate(AppSettingsScreenDestination(highlightKey = "network_switcher")) },
    GlobalSettingsSearchEntry("Per-App Network Mode Automation", "Automatically switch network mode per application", "network_automation", Icons.Outlined.AutoMode, GsColorIndigo) { it.navigate(AppSettingsScreenDestination(highlightKey = "network_switcher")) },
    GlobalSettingsSearchEntry("Preferred Network Mode", "Select 5G NR, 4G LTE, 3G, or 2G network modes", "preferred_network_mode", Icons.Outlined.SignalCellularAlt, GsColorTeal) { it.navigate(AppSettingsScreenDestination(highlightKey = "network_switcher")) },
    GlobalSettingsSearchEntry("Network Switcher Shizuku Mode", "Switch network modes without root using Shizuku", "network_shizuku", Icons.Outlined.Security, GsColorGreen) { it.navigate(AppSettingsScreenDestination(highlightKey = "network_switcher")) },
    GlobalSettingsSearchEntry("Network Switcher Root Mode", "Direct shell network mode execution with root access", "network_root", Icons.Outlined.AdminPanelSettings, GsColorRed) { it.navigate(AppSettingsScreenDestination(highlightKey = "network_switcher")) },
    GlobalSettingsSearchEntry("Network Switch Quick Settings Tile", "Toggle 4G/5G directly from Android notification shade", "network_tile", Icons.Outlined.ViewStream, GsColorAmber) { it.navigate(AppSettingsScreenDestination(highlightKey = "network_switcher")) },
    GlobalSettingsSearchEntry("Network Switch Floating Hint", "Show on-screen network mode floating indicator", "network_floating_hint", Icons.Outlined.PictureInPicture, GsColorCyan) { it.navigate(AppSettingsScreenDestination(highlightKey = "network_switcher")) },

    // ── Sound & Vibration screen ─────────────────────────────────────────────
    GlobalSettingsSearchEntry("DTMF Tone", "Play tones when dialing digits", "dtmf_tone", Icons.Outlined.VolumeUp, GsColorBlue) { it.navigate(SoundVibrationScreenDestination(highlightKey = "dtmf_tone")) },
    GlobalSettingsSearchEntry("Dial Pad Tone", "Choose the dialpad key tone", "dialpad_tone", Icons.Outlined.VolumeUp, GsColorTeal) { it.navigate(SoundVibrationScreenDestination(highlightKey = "dialpad_tone")) },
    GlobalSettingsSearchEntry("Ringtone Settings", "Choose your incoming call ringtone", "ringtone_settings", Icons.Outlined.VolumeUp, GsColorAmber) { it.navigate(SoundVibrationScreenDestination(highlightKey = "ringtone_settings")) },
    GlobalSettingsSearchEntry("Do Not Disturb", "Manage Do Not Disturb access", "dnd_settings", Icons.Outlined.VolumeUp, GsColorIndigo) { it.navigate(SoundVibrationScreenDestination(highlightKey = "dnd_settings")) },

    // ── Authentication (Biometric) screen ────────────────────────────────────
    GlobalSettingsSearchEntry("Authentication Method", "System biometrics, PIN, or password", "auth_method", Icons.Default.Fingerprint, Color(0xFF6750A4)) { it.navigate(BiometricScreenDestination(highlightKey = "auth_method")) },
    GlobalSettingsSearchEntry("Lock App on Open", "Require authentication whenever the app opens", "lock_app_open", Icons.Default.Fingerprint, GsColorRed) { it.navigate(BiometricScreenDestination(highlightKey = "lock_app_open")) },
    GlobalSettingsSearchEntry("Lock Call Actions", "Require authentication for sensitive call actions", "lock_call_actions", Icons.Default.Fingerprint, GsColorTeal) { it.navigate(BiometricScreenDestination(highlightKey = "lock_call_actions")) },

    // ── Interface screen ──────────────────────────────────────────────────────
    GlobalSettingsSearchEntry("Dynamic Colors", "Match app colors to your wallpaper (Material You)", "dynamic_colors", Icons.Outlined.Palette, GsColorPurple) { it.navigate(InterfaceScreenDestination(highlightKey = "dynamic_colors")) },
    GlobalSettingsSearchEntry("Saturated Colors", "Apply rich saturated colors behind containers", "saturated_colors", Icons.Outlined.Palette, GsColorAmber) { it.navigate(InterfaceScreenDestination(highlightKey = "saturated_colors")) },
    GlobalSettingsSearchEntry("Material Liquid You Glass", "Liquid glass visual effects", "liquid_glass_toggle", Icons.Outlined.Palette, GsColorBlue) { it.navigate(InterfaceScreenDestination(highlightKey = "liquid_glass_toggle")) },
    GlobalSettingsSearchEntry("Elements to have liquid glass effect", "Choose where liquid glass effects apply", "liquid_glass_elements_link", Icons.Outlined.Palette, GsColorBlue) { it.navigate(InterfaceScreenDestination(highlightKey = "liquid_glass_elements_link")) },
    GlobalSettingsSearchEntry("Material Blur Effects", "Blur effects across the interface", "blur_effects_toggle", Icons.Outlined.Palette, GsColorIndigo) { it.navigate(InterfaceScreenDestination(highlightKey = "blur_effects_toggle")) },
    GlobalSettingsSearchEntry("Elements to have blur effect", "Choose where blur effects apply", "blur_effects_elements_link", Icons.Outlined.Palette, GsColorIndigo) { it.navigate(InterfaceScreenDestination(highlightKey = "blur_effects_elements_link")) },
    GlobalSettingsSearchEntry("Hangup Animation", "Animate the screen when a call ends", "hangup_animation", Icons.Outlined.Palette, GsColorRed) { it.navigate(InterfaceScreenDestination(highlightKey = "hangup_animation")) },
    GlobalSettingsSearchEntry("Incoming Call UI", "Customize the incoming call screen", "incoming_call_ui_link", Icons.Outlined.Palette, GsColorGreen) { it.navigate(InterfaceScreenDestination(highlightKey = "incoming_call_ui_link")) },
    GlobalSettingsSearchEntry("Ongoing Call UI", "Customize the in-call screen layout", "caller_ui_link", Icons.Outlined.Palette, GsColorGreen) { it.navigate(InterfaceScreenDestination(highlightKey = "caller_ui_link")) },
    GlobalSettingsSearchEntry("Calls Section Elements", "Choose what shows in the Calls tab", "calls_section_elements", Icons.Outlined.Palette, GsColorTeal) { it.navigate(InterfaceScreenDestination(highlightKey = "calls_section_elements")) },
    GlobalSettingsSearchEntry("Context Menu Elements", "Choose what shows in long-press menus", "context_menu_elements", Icons.Outlined.Palette, GsColorTeal) { it.navigate(InterfaceScreenDestination(highlightKey = "context_menu_elements")) },
    GlobalSettingsSearchEntry("Tab Sections", "Choose which bottom tabs are visible", "tab_sections", Icons.Outlined.Palette, GsColorAmber) { it.navigate(InterfaceScreenDestination(highlightKey = "tab_sections")) },
    GlobalSettingsSearchEntry("Default Tab Section", "Which tab opens when you launch the app", "default_tab_section", Icons.Outlined.Palette, GsColorAmber) { it.navigate(InterfaceScreenDestination(highlightKey = "default_tab_section")) },
    GlobalSettingsSearchEntry("Scroll Animation", "Animate list scrolling", "scroll_animation", Icons.Outlined.Palette, GsColorBlue) { it.navigate(InterfaceScreenDestination(highlightKey = "scroll_animation")) },
    GlobalSettingsSearchEntry("Pill Style Navigation", "Pill-shaped bottom navigation bar", "pill_style_nav", Icons.Outlined.Palette, GsColorPurple) { it.navigate(InterfaceScreenDestination(highlightKey = "pill_style_nav")) },
    GlobalSettingsSearchEntry("Show Sims In Call Logs", "Show which SIM a call used in the call log", "show_sims_call_logs", Icons.Outlined.Palette, GsColorGreen) { it.navigate(InterfaceScreenDestination(highlightKey = "show_sims_call_logs")) },
    GlobalSettingsSearchEntry("Name non contacts as Unknown", "Display Unknown or phone number for unsaved callers", "name_non_contacts_as_unknown", Icons.Outlined.Palette, GsColorTeal) { it.navigate(InterfaceScreenDestination(highlightKey = "name_non_contacts_as_unknown")) },
    GlobalSettingsSearchEntry("Auto Delete Unknown No in call log", "Automatically clean up unknown-number entries", "auto_delete_unknown_calllog", Icons.Outlined.Palette, GsColorRed) { it.navigate(InterfaceScreenDestination(highlightKey = "auto_delete_unknown_calllog")) },

    GlobalSettingsSearchEntry("Call Time Format in call logs", "12-hour or 24-hour time format", "call_time_format", Icons.Outlined.Palette, GsColorTeal) { it.navigate(InterfaceScreenDestination(highlightKey = "call_time_format")) },
    GlobalSettingsSearchEntry("Icon-Only Bottom Bar", "Hide labels on the bottom navigation bar", "icon_only_bottom_bar", Icons.Outlined.Palette, GsColorIndigo) { it.navigate(InterfaceScreenDestination(highlightKey = "icon_only_bottom_bar")) },
    GlobalSettingsSearchEntry("Open Dialpad by Default", "Launch straight into the dialpad", "open_dialpad_default", Icons.Outlined.Palette, GsColorBlue) { it.navigate(InterfaceScreenDestination(highlightKey = "open_dialpad_default")) },
    GlobalSettingsSearchEntry("Show favourites in list", "Display favourites in a vertical list instead of grid", "favorites_in_list", Icons.Outlined.Palette, GsColorPink) { it.navigate(InterfaceScreenDestination(highlightKey = "favorites_in_list")) },
    GlobalSettingsSearchEntry("Show First Letter in Avatar", "Fallback avatar shows a contact's initial", "avatar_first_letter", Icons.Outlined.Palette, GsColorAmber) { it.navigate(InterfaceScreenDestination(highlightKey = "avatar_first_letter")) },
    GlobalSettingsSearchEntry("Solid Icons", "Use solid background behind icons without colors", "solid_icons", Icons.Outlined.Palette, GsColorBluGrey) { it.navigate(InterfaceScreenDestination(highlightKey = "solid_icons")) },
    GlobalSettingsSearchEntry("Use Colorful Avatars", "Give fallback avatars varied colors", "colorful_avatars", Icons.Outlined.Palette, GsColorPurple) { it.navigate(InterfaceScreenDestination(highlightKey = "colorful_avatars")) },
    GlobalSettingsSearchEntry("Show Picture in Avatar", "Show a contact's photo in their avatar", "avatar_picture", Icons.Outlined.Palette, GsColorGreen) { it.navigate(InterfaceScreenDestination(highlightKey = "avatar_picture")) },
    GlobalSettingsSearchEntry("App Icon", "Choose a custom launcher icon", "app_icon_link", Icons.Outlined.Palette, GsColorRed) { it.navigate(InterfaceScreenDestination(highlightKey = "app_icon_link")) },
    GlobalSettingsSearchEntry("App Name", "Change the name shown for the app", "app_name_link", Icons.Outlined.Badge, GsColorTeal) { it.navigate(InterfaceScreenDestination(highlightKey = "app_name_link")) },

    // ── Incoming Call UI screen ──────────────────────────────────────────────
    GlobalSettingsSearchEntry("Show Full screen call UI on any apps", "Open full screen incoming call UI over any app", "show_fullscreen_call_ui_on_any_apps", Icons.Outlined.Call, GsColorGreen) { it.navigate(IncomingCallUIScreenDestination(highlightKey = "show_fullscreen_call_ui_on_any_apps")) },
    GlobalSettingsSearchEntry("Show Mute button", "Show a button to silence ringtone during incoming calls", "incoming_show_mute_button", Icons.Outlined.VolumeUp, GsColorAmber) { it.navigate(IncomingCallUIScreenDestination(highlightKey = "incoming_show_mute_button")) },
    GlobalSettingsSearchEntry("Show Contact PFP", "Display caller's avatar photo over incoming call screen", "incoming_show_contact_pfp", Icons.Outlined.Contacts, GsColorCyan) { it.navigate(IncomingCallUIScreenDestination(highlightKey = "incoming_show_contact_pfp")) },
    GlobalSettingsSearchEntry("Show Phone Number", "Display caller's phone number on incoming call screen", "incoming_show_phone_number", Icons.Outlined.Call, GsColorGreen) { it.navigate(IncomingCallUIScreenDestination(highlightKey = "incoming_show_phone_number")) },
    GlobalSettingsSearchEntry("Default Message", "Quick-reply message shown for incoming calls", "default_message_link", Icons.Outlined.Message, GsColorBlue) { it.navigate(IncomingCallUIScreenDestination(highlightKey = "default_message_link")) },

    // ── Ongoing Call UI screen ───────────────────────────────────────────────
    GlobalSettingsSearchEntry("Show ongoing call UI when the call is answered", "Display full screen in-call screen after answering", "show_ongoing_call_ui_when_answered", Icons.Outlined.Call, GsColorBlue) { it.navigate(CallerUIScreenDestination()) },
    GlobalSettingsSearchEntry("Show Contact PFP in Ongoing Call", "Display contact avatar photo on ongoing call screen", "ongoing_show_contact_pfp", Icons.Outlined.Contacts, GsColorCyan) { it.navigate(CallerUIScreenDestination(highlightKey = "ongoing_show_contact_pfp")) },
    GlobalSettingsSearchEntry("Show Phone Number in Ongoing Call", "Display phone number on ongoing call screen", "ongoing_show_phone_number", Icons.Outlined.Call, GsColorGreen) { it.navigate(CallerUIScreenDestination(highlightKey = "ongoing_show_phone_number")) },

    // ── About screen ──────────────────────────────────────────────────────────
    GlobalSettingsSearchEntry("Made By Hari", "Developer info", "made_by_hari", Icons.Outlined.Info, GsColorBluGrey) { it.navigate(AboutAppScreenDestination(highlightKey = "made_by_hari")) },
    GlobalSettingsSearchEntry("Source Code", "View Ever Dialer's source on GitHub", "source_code", Icons.Outlined.Info, GsColorBluGrey) { it.navigate(AboutAppScreenDestination(highlightKey = "source_code")) },
    GlobalSettingsSearchEntry("Telegram App Support Group", "Get help and discuss the app", "telegram_support", Icons.Outlined.Info, GsColorBlue) { it.navigate(AboutAppScreenDestination(highlightKey = "telegram_support")) },
    GlobalSettingsSearchEntry("App Recommending Channel in Telegram", "Follow for app announcements", "telegram_channel", Icons.Outlined.Info, GsColorBlue) { it.navigate(AboutAppScreenDestination(highlightKey = "telegram_channel")) },
    GlobalSettingsSearchEntry("My Other App (Everlasting Android Tweak)", "Check out the developer's other app", "other_app_link", Icons.Outlined.Info, GsColorIndigo) { it.navigate(AboutAppScreenDestination(highlightKey = "other_app_link")) }
)

/**
 * A "Search settings" box that can be dropped as the first item on any settings page (main
 * Settings screen included, in principle, though it has its own richer local search). Typing
 * here searches every setting across every settings page; tapping a result navigates straight
 * to it (or back to the main Settings screen with that row highlighted, if it lives there).
 */
@Composable
fun SettingsSearchEntryPoint(navigator: DestinationsNavigator, modifier: Modifier = Modifier) {
    var query by rememberSaveable { mutableStateOf("") }
    val entries = remember { buildGlobalSettingsSearchEntries() }
    val filtered = remember(query) {
        if (query.isBlank()) emptyList()
        else entries.filter { it.title.contains(query, ignoreCase = true) || it.subtitle.contains(query, ignoreCase = true) }
    }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search settings") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    AnimatedVisibility(visible = query.isNotEmpty(), enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )
        }

        if (query.isNotBlank()) {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No settings found for \"$query\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                RivoExpressiveCard {
                    filtered.forEachIndexed { index, entry ->
                        RivoListItem(
                            headline = entry.title,
                            supporting = entry.subtitle,
                            leadingIcon = entry.icon,
                            iconContainerColor = entry.iconContainerColor,
                            trailingIcon = Icons.Default.ChevronRight,
                            onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus(force = true)
                                query = ""
                                entry.navigateTo(navigator)
                            }
                        )
                        if (index < filtered.size - 1) CardDivider()
                    }
                }
            }
        }
    }
}
