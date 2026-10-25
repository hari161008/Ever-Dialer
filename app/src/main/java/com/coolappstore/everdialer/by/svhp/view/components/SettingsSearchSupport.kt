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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.compose.material.icons.filled.Favorite
import com.ramcosta.composedestinations.generated.destinations.AboutAppScreenDestination
import com.ramcosta.composedestinations.generated.destinations.DebugSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.LanguageSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.AnswerStyleScreenDestination
import com.ramcosta.composedestinations.generated.destinations.AppSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.BiometricScreenDestination
import com.ramcosta.composedestinations.generated.destinations.CallLogsSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.CallerUIScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ContactInfoSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.CustomFontScreenDestination
import com.ramcosta.composedestinations.generated.destinations.DialpadSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FavouritesSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.NavigationSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.DefaultMessageAppScreenDestination
import com.ramcosta.composedestinations.generated.destinations.IncomingCallUIScreenDestination
import com.ramcosta.composedestinations.generated.destinations.InterfaceScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RaiseToAnswerScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RainModeScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RecordingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SimAndCallPlacementScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SoundVibrationScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SwipeActionsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.UpdatesScreenDestination
import com.ramcosta.composedestinations.generated.destinations.VolumeDndScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PcConnectScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PcSyncSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SmsSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.CreateCallCategoryScreenDestination
import com.ramcosta.composedestinations.generated.destinations.CreateSmsCategoryScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay
import androidx.compose.runtime.collectAsState
import org.koin.compose.koinInject
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.SearchHistoryManager

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
    val titleLower: String = title.lowercase(),
    val subtitleLower: String = subtitle.lowercase(),
    val navigateTo: ((DestinationsNavigator) -> Unit)? = null
)

typealias SettingsSearchEntry = GlobalSettingsSearchEntry

private val GsColorPurple  = Color(0xFF9C27B0)
private val GsColorBlue    = Color(0xFF2196F3)
private val GsColorGreen   = Color(0xFF4CAF50)
private val GsColorTeal    = Color(0xFF009688)
private val GsColorIndigo  = Color(0xFF3F51B5)
private val GsColorBluGrey = Color(0xFF607D8B)
private val GsColorAmber   = Color(0xFFFFC107)
private val GsColorOrange  = Color(0xFFFF9800)
private val GsColorBrown   = Color(0xFF795548)
private val GsColorCyan    = Color(0xFF00BCD4)
private val GsColorRed     = Color(0xFFE53935)
private val GsColorPink    = Color(0xFFE91E63)

val globalSettingsSearchEntries: List<GlobalSettingsSearchEntry> by lazy {
    listOf(
        // ── Rows that live directly on the main Settings screen ─────────────────
        GlobalSettingsSearchEntry("Check For Updates", "Current version: v$APP_VERSION", "check_for_updates", Icons.Default.SystemUpdate, GsColorAmber),
        GlobalSettingsSearchEntry("Rate and Review", "Share your feedback about Ever Dialer", "rate_and_review", Icons.Default.Star, GsColorCyan),
        GlobalSettingsSearchEntry("Check Ratings and Reviews", "See what others are saying about Ever Dialer", "check_ratings", Icons.Default.Reviews, GsColorGreen),
        GlobalSettingsSearchEntry("More Apps", "Check out other apps from the developer", "more_apps", Icons.Default.Apps, GsColorIndigo),
        GlobalSettingsSearchEntry("Donate", "Support this open source project", "donate", Icons.Default.Favorite, GsColorRed),
        GlobalSettingsSearchEntry("Interface", "Themes, colors, and layout", "interface", Icons.Outlined.Palette, GsColorPurple),
        GlobalSettingsSearchEntry("Tap Haptics", "Vibration on taps across the app", "tap_haptics", Icons.Outlined.Vibration, GsColorPurple),
        GlobalSettingsSearchEntry("Scroll Haptics", "Vibrate on scroll gestures across the app", "scroll_haptics", Icons.Outlined.SwipeVertical, GsColorIndigo),
        GlobalSettingsSearchEntry("Authentication", "App lock, biometrics, and PIN/password", "authentication", Icons.Default.Fingerprint, Color(0xFF6750A4)),
        GlobalSettingsSearchEntry("Contacts Hider", "Hide contacts behind a secret code", "contacts_hider", Icons.Outlined.Lock, Color(0xFF5E35B1)),
        GlobalSettingsSearchEntry("Fake Call", "Schedule fake incoming calls without calling the real person", "fake_call", Icons.Outlined.PhoneCallback, GsColorRed),
        GlobalSettingsSearchEntry("Call Recording", "Open Ever Call Recorder", "call_recording", Icons.Default.FiberManualRecord, Color(0xFFE53935)),
        GlobalSettingsSearchEntry("Silence Unknown Callers", "Automatically decline calls from unknown numbers", "silence_unknown", Icons.Outlined.PhoneDisabled, GsColorRed),
        GlobalSettingsSearchEntry("Blocked Numbers", "Numbers you've blocked from calling you", "blocked_numbers", Icons.Outlined.PersonOff, GsColorBluGrey),
        GlobalSettingsSearchEntry("Show blocked numbers in call logs", "Display calls from blocked numbers in your call history", "show_blocked_calls_in_call_logs", Icons.Outlined.PersonOff, GsColorBluGrey),
        GlobalSettingsSearchEntry("Auto Check For Updates", "Automatically check for updates when the app opens", "auto_check_updates", Icons.Default.Autorenew, GsColorAmber) { it.navigate(UpdatesScreenDestination) },
        GlobalSettingsSearchEntry("Create Backup", "Save app configuration, settings and calling cards", "create_backup", Icons.Default.Backup, GsColorGreen),
        GlobalSettingsSearchEntry("Restore Backup", "Restore app configuration, settings and calling cards", "restore_backup", Icons.Default.Restore, GsColorBrown),
        GlobalSettingsSearchEntry("About Ever Dialer", "Version $APP_VERSION · Developer info", "about_app", Icons.Outlined.Info, GsColorBluGrey),
        GlobalSettingsSearchEntry("Debug", "Developer & experimental options", "debug_settings", Icons.Outlined.BugReport, GsColorAmber) { it.navigate(DebugSettingsScreenDestination()) },

        // ── App Settings screen ──────────────────────────────────────────────────
        GlobalSettingsSearchEntry("Interesting Settings !", "Call features, Volume DND, network switcher, notes", "app_settings", Icons.Outlined.Tune, GsColorTeal) { it.navigate(AppSettingsScreenDestination()) },
        GlobalSettingsSearchEntry("Swipe Actions", "Configure left and right swipe gestures for Call Logs, Contacts, Call Recordings, Notes, and Messages", "swipe_actions", Icons.Outlined.SwipeVertical, Color(0xFF673AB7)) { it.navigate(SwipeActionsScreenDestination()) },
        GlobalSettingsSearchEntry("Favourites Swipe Actions", "Configure swipe actions for favourite contacts", "favourites_swipe_actions", Icons.Default.Favorite, Color(0xFFE91E63)) { it.navigate(SwipeActionsScreenDestination()) },
        GlobalSettingsSearchEntry("Call Logs Swipe Actions", "Configure swipe actions for call log items", "call_logs_swipe_actions", Icons.Outlined.History, GsColorGreen) { it.navigate(SwipeActionsScreenDestination()) },
        GlobalSettingsSearchEntry("Contacts Swipe Actions", "Configure swipe actions for contact items", "contacts_swipe_actions", Icons.Outlined.Person, GsColorBlue) { it.navigate(SwipeActionsScreenDestination()) },
        GlobalSettingsSearchEntry("Call Recordings Swipe Actions", "Configure swipe actions for call recording items", "recordings_swipe_actions", Icons.Outlined.Mic, GsColorAmber) { it.navigate(SwipeActionsScreenDestination()) },
        GlobalSettingsSearchEntry("Notes Swipe Actions", "Configure swipe actions for notes items", "notes_swipe_actions", Icons.Outlined.Notes, GsColorTeal) { it.navigate(SwipeActionsScreenDestination()) },
        GlobalSettingsSearchEntry("SMS / Messages Swipe Actions", "Configure swipe actions for messages items", "sms_swipe_actions", Icons.Outlined.Message, GsColorPurple) { it.navigate(SwipeActionsScreenDestination()) },
        GlobalSettingsSearchEntry("4G/5G Switcher", "Quickly switch network mode per app", "network_switcher", Icons.Outlined.NetworkCell, GsColorBlue) { it.navigate(AppSettingsScreenDestination(highlightKey = "network_switcher")) },
        GlobalSettingsSearchEntry("Integrate Notes Section", "Show notes alongside call recordings", "integrate_notes", Icons.Outlined.Notes, GsColorGreen) { it.navigate(AppSettingsScreenDestination(highlightKey = "integrate_notes")) },
        GlobalSettingsSearchEntry("Delete Notes With Recording", "Remove the note when its recording is deleted", "delete_notes_with_recording", Icons.Outlined.NoteAlt, GsColorRed) { it.navigate(AppSettingsScreenDestination(highlightKey = "delete_notes_with_recording")) },

        // ── Sim And Call Placement screen ──────────────────────────────────────
        GlobalSettingsSearchEntry("Sim And Call Placement", "Default SIM, SIM colors, confirm calls, contacts", "sim_and_call_placement", Icons.Outlined.SimCard, GsColorGreen) { it.navigate(SimAndCallPlacementScreenDestination(highlightKey = "sim_and_call_placement")) },
        GlobalSettingsSearchEntry("Sound & Vibration", "Ringtones and dialpad tones", "sound_vibration_link", Icons.Outlined.VolumeUp, GsColorBlue) { it.navigate(SimAndCallPlacementScreenDestination(highlightKey = "sound_vibration_link")) },
        GlobalSettingsSearchEntry("Default SIM", "Which SIM is used to place calls", "default_sim", Icons.Outlined.SimCard, GsColorGreen) { it.navigate(SimAndCallPlacementScreenDestination(highlightKey = "default_sim")) },
        GlobalSettingsSearchEntry("Confirm placing a call", "Ask for confirmation before placing any outgoing call", "confirm_placing_call", Icons.Outlined.CheckCircle, Color(0xFF3F51B5)) { it.navigate(SimAndCallPlacementScreenDestination(highlightKey = "confirm_placing_call")) },
        GlobalSettingsSearchEntry("Show SIM buttons", "Show SIM 1 and SIM 2 buttons instead of dial button in dialpad", "show_sim_buttons_in_dialpad", Icons.Outlined.Dialpad, GsColorTeal) { it.navigate(SimAndCallPlacementScreenDestination(highlightKey = "show_sim_buttons_in_dialpad")) },
        GlobalSettingsSearchEntry("Use SIM based on call logs history on any call", "Automatically select the same SIM from call log history", "use_sim_from_call_log", Icons.Outlined.History, Color(0xFF3F51B5)) { it.navigate(SimAndCallPlacementScreenDestination(highlightKey = "use_sim_from_call_log")) },
        GlobalSettingsSearchEntry("Customize SIM Colors", "Choose custom colors for SIM 1 and SIM 2", "customize_sim_colors", Icons.Outlined.Palette, GsColorAmber) { it.navigate(SimAndCallPlacementScreenDestination(highlightKey = "customize_sim_colors")) },
        GlobalSettingsSearchEntry("Contacts to display", "Choose which accounts' contacts are shown", "contacts_to_display", Icons.Outlined.Contacts, GsColorBlue) { it.navigate(SimAndCallPlacementScreenDestination(highlightKey = "contacts_to_display")) },
        GlobalSettingsSearchEntry("Missed Call Notification", "Show missed call notifications through Ever Dialer", "missed_call_notification", Icons.AutoMirrored.Filled.CallMissed, GsColorRed) { it.navigate(SimAndCallPlacementScreenDestination(highlightKey = "missed_call_notification")) },

        // ── Call Features & Behavior ─────────────────────────────────────────────
        GlobalSettingsSearchEntry("Device Orientation with Proximity Sensor", "Combine orientation and proximity to prevent false screen-offs during a call", "proximity_orientation_bg", Icons.Outlined.ScreenLockPortrait, GsColorPink) { it.navigate(AppSettingsScreenDestination(highlightKey = "proximity_orientation_bg")) },
        GlobalSettingsSearchEntry("Proximity Sensor on in background", "Turn off screen when phone is near ear during a call", "proximity_sensor_bg", Icons.Outlined.Sensors, GsColorTeal) { it.navigate(AppSettingsScreenDestination(highlightKey = "proximity_sensor_bg")) },
        GlobalSettingsSearchEntry("Pocket Mode Prevention", "Block accidental answer/decline when phone is in pocket", "pocket_mode_prevention", Icons.Outlined.Sensors, GsColorAmber) { it.navigate(AppSettingsScreenDestination(highlightKey = "pocket_mode_prevention")) },
        GlobalSettingsSearchEntry("Floating Ongoing Call", "Draggable floating bubble during calls", "floating_ongoing_call", Icons.Outlined.Sensors, GsColorBlue) { it.navigate(AppSettingsScreenDestination(highlightKey = "floating_ongoing_call")) },
        GlobalSettingsSearchEntry("Direct Call on Tap", "Tap a call log entry to call directly", "direct_call_on_tap", Icons.Outlined.Call, GsColorGreen) { it.navigate(AppSettingsScreenDestination(highlightKey = "direct_call_on_tap")) },
        GlobalSettingsSearchEntry("Auto Speaker", "Switch to loudspeaker when phone is away from ear", "auto_speaker", Icons.Outlined.VolumeUp, GsColorRed) { it.navigate(AppSettingsScreenDestination(highlightKey = "auto_speaker")) },
        GlobalSettingsSearchEntry("Rain Mode", "Answer/decline calls by shaking your device", "rain_mode_link", Icons.Outlined.WaterDrop, Color(0xFF0288D1)) { it.navigate(RainModeScreenDestination()) },
        GlobalSettingsSearchEntry("Auto Redial", "Automatically redial on rejected/unanswered/busy calls", "auto_redial", Icons.Default.Replay, GsColorBlue) { it.navigate(AppSettingsScreenDestination(highlightKey = "auto_redial")) },
        GlobalSettingsSearchEntry("Missed Call Popup", "Show interactive popup over other apps on missed calls", "missed_call_popup", Icons.AutoMirrored.Filled.CallMissed, GsColorAmber) { it.navigate(AppSettingsScreenDestination(highlightKey = "missed_call_popup")) },
        GlobalSettingsSearchEntry("Always Show Popup After Every Call Ends", "Show popup after every call ends with only custom response", "always_show_after_call_ends", Icons.AutoMirrored.Filled.CallMissed, GsColorAmber) { it.navigate(AppSettingsScreenDestination(highlightKey = "missed_call_popup")) },
        GlobalSettingsSearchEntry("Volume DND", "Toggle Do Not Disturb using volume button combination", "volume_dnd", Icons.Outlined.VolumeUp, GsColorPurple) { it.navigate(VolumeDndScreenDestination(highlightKey = "volume_dnd")) },

        // ── Rain Mode screen ─────────────────────────────────────────────────────
        GlobalSettingsSearchEntry("Enable Rain Mode", "Answer or decline calls by shaking device", "enable_rain_mode", Icons.Outlined.WaterDrop, Color(0xFF0288D1)) { it.navigate(RainModeScreenDestination(highlightKey = "enable_rain_mode")) },
        GlobalSettingsSearchEntry("Rain Mode Shake Sensitivity", "Adjust shake intensity threshold for Rain Mode", "rain_mode_intensity", Icons.Outlined.Tune, Color(0xFF0288D1)) { it.navigate(RainModeScreenDestination()) },
        GlobalSettingsSearchEntry("Rain Mode Vibration Feedback", "Vibrate when call is answered or declined via shake gesture", "rain_mode_vibrate", Icons.Outlined.Vibration, GsColorPurple) { it.navigate(RainModeScreenDestination(highlightKey = "rain_mode_vibrate")) },

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

        // ── Call UI ──────────────────────────────────────────────────────────────
        GlobalSettingsSearchEntry("Call UI", "Incoming Call UI and Ongoing Call UI", "call_ui_section", Icons.Outlined.Call, GsColorGreen) { it.navigate(InterfaceScreenDestination(highlightKey = "incoming_call_ui_link")) },
        GlobalSettingsSearchEntry("Incoming Call UI", "Call UI: Customize the incoming call screen appearance", "incoming_call_ui_link", Icons.Outlined.CallReceived, GsColorGreen) { it.navigate(IncomingCallUIScreenDestination()) },
        GlobalSettingsSearchEntry("Ongoing Call UI", "Call UI: Customize the in-call screen layout and controls", "caller_ui_link", Icons.Outlined.Person, GsColorBlue) { it.navigate(CallerUIScreenDestination()) },
        GlobalSettingsSearchEntry("Calls Section Elements", "Call Logs: Toggle and drag to reorder Today, Missed, Outgoing, Call Time, Contacts, Groups cards", "calls_section_elements", Icons.Default.Dashboard, GsColorOrange) { it.navigate(CallLogsSettingsScreenDestination(highlightKey = "calls_section_elements")) },

        // ── UI Elements ──────────────────────────────────────────────────────────
        GlobalSettingsSearchEntry("UI Elements", "Call Logs, Dialpad, Contact info, Favourites, Navigation, Context Menu and Contact Info Elements", "ui_elements_section", Icons.Outlined.Widgets, GsColorPurple) { it.navigate(InterfaceScreenDestination(highlightKey = "call_logs_settings_link")) },
        GlobalSettingsSearchEntry("Call Logs Settings", "UI Elements: Customize call log grouping, SIM icons, call counts, formatting and auto-deletion", "call_logs_settings_link", Icons.Outlined.Call, GsColorGreen) { it.navigate(CallLogsSettingsScreenDestination()) },
        GlobalSettingsSearchEntry("Dialpad Settings", "UI Elements: Customize dialpad memory, search list call logs, and startup behavior", "dialpad_settings_link", Icons.Outlined.Dialpad, GsColorBlue) { it.navigate(DialpadSettingsScreenDestination()) },
        GlobalSettingsSearchEntry("Contact info Settings", "UI Elements: Customize contact info display and duplicate numbers", "contact_info_settings_link", Icons.Outlined.Person, GsColorPurple) { it.navigate(ContactInfoSettingsScreenDestination()) },
        GlobalSettingsSearchEntry("Favourites Settings", "UI Elements: Customize favourites layout and view mode", "favourites_settings_link", Icons.Outlined.Star, Color(0xFFE91E63)) { it.navigate(FavouritesSettingsScreenDestination()) },
        GlobalSettingsSearchEntry("Navigation Settings", "UI Elements: Customize tab sections, default tab, and bottom bar appearance", "navigation_settings_link", Icons.Default.ViewWeek, GsColorIndigo) { it.navigate(NavigationSettingsScreenDestination()) },
        GlobalSettingsSearchEntry("Context Menu Elements", "UI Elements: Customize Favourites, Call Logs, and Contacts context menus", "context_menu_elements", Icons.Default.MoreVert, GsColorTeal) { it.navigate(InterfaceScreenDestination(highlightKey = "context_menu_elements")) },
        GlobalSettingsSearchEntry("Contact Info Elements", "UI Elements: Toggle and drag to reorder elements in contact details", "contact_info_elements", Icons.Default.Contacts, GsColorAmber) { it.navigate(ContactInfoSettingsScreenDestination(highlightKey = "contact_info_elements")) },
        GlobalSettingsSearchEntry("Tab Sections", "UI Elements: Choose which bottom tabs are visible", "tab_sections", Icons.Outlined.Palette, GsColorAmber) { it.navigate(NavigationSettingsScreenDestination(highlightKey = "tab_sections")) },
        GlobalSettingsSearchEntry("Default Tab Section", "UI Elements: Which tab opens when you launch the app", "default_tab_section", Icons.Outlined.Palette, GsColorAmber) { it.navigate(NavigationSettingsScreenDestination(highlightKey = "default_tab_section")) },
        GlobalSettingsSearchEntry("Animation Style", "Zoom (in/out) or Windows Phone transition", "animation_style", Icons.Outlined.Animation, GsColorTeal) { it.navigate(InterfaceScreenDestination(highlightKey = "animation_style")) },
        GlobalSettingsSearchEntry("Motion Blur in Animation", "Apply dynamic motion blur during page transitions", "motion_blur_animation", Icons.Outlined.BlurOn, GsColorPurple) { it.navigate(InterfaceScreenDestination(highlightKey = "motion_blur_animation")) },
        GlobalSettingsSearchEntry("Scroll Animation", "Animate list scrolling", "scroll_animation", Icons.Outlined.Palette, GsColorBlue) { it.navigate(InterfaceScreenDestination(highlightKey = "scroll_animation")) },
        GlobalSettingsSearchEntry("Scroll Indication", "UI Elements: Show floating line indicator when tabs are scrollable", "scroll_indication", Icons.Outlined.Palette, GsColorPurple) { it.navigate(NavigationSettingsScreenDestination(highlightKey = "scroll_indication")) },
        GlobalSettingsSearchEntry("Pill Style Navigation", "UI Elements: Pill-shaped bottom navigation bar", "pill_style_nav", Icons.Outlined.Palette, GsColorPurple) { it.navigate(NavigationSettingsScreenDestination(highlightKey = "pill_style_nav")) },
        GlobalSettingsSearchEntry("Group numbers in call logs based on latest calls", "UI Elements: Group call logs by number and sort by latest calls", "group_calls_by_latest", Icons.Outlined.Palette, GsColorBlue) { it.navigate(CallLogsSettingsScreenDestination(highlightKey = "group_calls_by_latest")) },
        GlobalSettingsSearchEntry("Show Sims In Call Logs", "UI Elements: Show which SIM a call used in the call log", "show_sims_call_logs", Icons.Outlined.Palette, GsColorGreen) { it.navigate(CallLogsSettingsScreenDestination(highlightKey = "show_sims_call_logs")) },
        GlobalSettingsSearchEntry("Show total number of calls made", "UI Elements: Show total call count in call logs", "show_total_calls_made", Icons.Outlined.Palette, GsColorPurple) { it.navigate(CallLogsSettingsScreenDestination(highlightKey = "show_total_calls_made")) },
        GlobalSettingsSearchEntry("Hide Duplicate Numbers In A Contact", "UI Elements: Show only one of multiple identical phone numbers", "hide_duplicate_numbers_in_contact", Icons.Outlined.Palette, GsColorPurple) { it.navigate(ContactInfoSettingsScreenDestination(highlightKey = "hide_duplicate_numbers_in_contact")) },
        GlobalSettingsSearchEntry("Name non contacts as Unknown", "UI Elements: Display Unknown or phone number for unsaved callers", "name_non_contacts_as_unknown", Icons.Outlined.Palette, GsColorTeal) { it.navigate(CallLogsSettingsScreenDestination(highlightKey = "name_non_contacts_as_unknown")) },
        GlobalSettingsSearchEntry("Dialpad Memory", "UI Elements: Remember typed numbers when switching tabs or closing dialpad", "dialpad_memory", Icons.Outlined.Palette, GsColorTeal) { it.navigate(DialpadSettingsScreenDestination(highlightKey = "dialpad_memory")) },
        GlobalSettingsSearchEntry("Auto Delete Unknown No in call log", "UI Elements: Automatically clean up unknown-number entries", "auto_delete_unknown_calllog", Icons.Outlined.Palette, GsColorRed) { it.navigate(CallLogsSettingsScreenDestination(highlightKey = "auto_delete_unknown_calllog")) },

        GlobalSettingsSearchEntry("Call Time Format in call logs", "UI Elements: 12-hour or 24-hour time format", "call_time_format", Icons.Outlined.Palette, GsColorTeal) { it.navigate(CallLogsSettingsScreenDestination(highlightKey = "call_time_format")) },
        GlobalSettingsSearchEntry("Talk time in call logs", "UI Elements: Show call duration for non-missed calls", "talk_time_in_call_logs", Icons.Outlined.Timer, GsColorGreen) { it.navigate(CallLogsSettingsScreenDestination(highlightKey = "talk_time_in_call_logs")) },
        GlobalSettingsSearchEntry("Missed Call Duration", "UI Elements: Show duration for missed calls in call logs", "missed_call_duration", Icons.Outlined.Timer, GsColorRed) { it.navigate(CallLogsSettingsScreenDestination(highlightKey = "missed_call_duration")) },
        GlobalSettingsSearchEntry("Icon-Only Bottom Bar", "UI Elements: Hide labels on the bottom navigation bar", "icon_only_bottom_bar", Icons.Outlined.Palette, GsColorIndigo) { it.navigate(NavigationSettingsScreenDestination(highlightKey = "icon_only_bottom_bar")) },
        GlobalSettingsSearchEntry("Open Dialpad by Default", "UI Elements: Launch straight into the dialpad", "open_dialpad_default", Icons.Outlined.Palette, GsColorBlue) { it.navigate(DialpadSettingsScreenDestination(highlightKey = "open_dialpad_default")) },
        GlobalSettingsSearchEntry("Show favourites in list", "UI Elements: Display favourites in a vertical list instead of grid", "favorites_in_list", Icons.Outlined.Palette, GsColorPink) { it.navigate(FavouritesSettingsScreenDestination(highlightKey = "favorites_in_list")) },
        GlobalSettingsSearchEntry("Hide Rate and Review", "Hide Rate and Review tile in settings", "hide_rate_and_review", Icons.Outlined.Palette, GsColorTeal) { it.navigate(InterfaceScreenDestination(highlightKey = "hide_rate_and_review")) },
        GlobalSettingsSearchEntry("Show First Letter in Avatar", "Fallback avatar shows a contact's initial", "avatar_first_letter", Icons.Outlined.Palette, GsColorAmber) { it.navigate(InterfaceScreenDestination(highlightKey = "avatar_first_letter")) },
        GlobalSettingsSearchEntry("Solid Icons", "Use solid background behind icons without colors", "solid_icons", Icons.Outlined.Palette, GsColorBluGrey) { it.navigate(InterfaceScreenDestination(highlightKey = "solid_icons")) },
        GlobalSettingsSearchEntry("Circle Icons", "Use circle shapes for icons across the app", "circle_icons", Icons.Outlined.Palette, GsColorCyan) { it.navigate(InterfaceScreenDestination(highlightKey = "circle_icons")) },
        GlobalSettingsSearchEntry("Use Colorful Avatars", "Give fallback avatars varied colors", "colorful_avatars", Icons.Outlined.Palette, GsColorPurple) { it.navigate(InterfaceScreenDestination(highlightKey = "colorful_avatars")) },
        GlobalSettingsSearchEntry("Show Avatars in Calls", "Display avatars in call history and logs", "avatar_calls", Icons.Outlined.Call, GsColorIndigo) { it.navigate(InterfaceScreenDestination(highlightKey = "avatar_calls")) },
        GlobalSettingsSearchEntry("Show Avatars in Contacts", "Display avatars in contacts list", "avatar_contacts", Icons.Outlined.Person, GsColorTeal) { it.navigate(InterfaceScreenDestination(highlightKey = "avatar_contacts")) },
        GlobalSettingsSearchEntry("Show Picture in Avatar", "Show a contact's photo in their avatar", "avatar_picture", Icons.Outlined.Palette, GsColorGreen) { it.navigate(InterfaceScreenDestination(highlightKey = "avatar_picture")) },
        GlobalSettingsSearchEntry("App Icon (Change)", "Choose a custom launcher icon", "app_icon_link", Icons.Outlined.Palette, GsColorRed) { it.navigate(InterfaceScreenDestination(highlightKey = "app_icon_link")) },
        GlobalSettingsSearchEntry("App Name (Change)", "Change the name shown for the app", "app_name_link", Icons.Outlined.Badge, GsColorTeal) { it.navigate(InterfaceScreenDestination(highlightKey = "app_name_link")) },
        GlobalSettingsSearchEntry("Show call logs in the dialpad search list", "UI Elements: Show recent call logs in the search list when nothing is typed or searched", "show_call_logs_in_dialpad_search_list", Icons.Outlined.History, GsColorTeal) { it.navigate(DialpadSettingsScreenDestination(highlightKey = "show_call_logs_in_dialpad_search_list")) },
        GlobalSettingsSearchEntry("Show numbers in call logs", "UI Elements: Show phone numbers below contact names in call logs", "show_numbers_in_call_logs", Icons.Outlined.Numbers, GsColorBlue) { it.navigate(CallLogsSettingsScreenDestination(highlightKey = "show_numbers_in_call_logs")) },
        GlobalSettingsSearchEntry("Fonts", "Typography styles, Playwrite USA Modern, Comfortaa, Patrick Hand, Gochi Hand, and font scaling", "fonts", Icons.Outlined.TextFormat, GsColorPurple) { it.navigate(CustomFontScreenDestination()) },

        // ── Incoming Call UI screen ──────────────────────────────────────────────
        GlobalSettingsSearchEntry("Show Full screen call UI on any apps", "Open full screen incoming call UI over any app", "show_fullscreen_call_ui_on_any_apps", Icons.Outlined.Call, GsColorGreen) { it.navigate(IncomingCallUIScreenDestination(highlightKey = "show_fullscreen_call_ui_on_any_apps")) },
        GlobalSettingsSearchEntry("Show Mute button", "Show a button to silence ringtone during incoming calls", "incoming_show_mute_button", Icons.Outlined.VolumeUp, GsColorAmber) { it.navigate(IncomingCallUIScreenDestination(highlightKey = "incoming_show_mute_button")) },
        GlobalSettingsSearchEntry("Answer Style", "Choose between modern and classic incoming call answer style", "answer_style", Icons.Outlined.SwipeVertical, GsColorGreen) { it.navigate(AnswerStyleScreenDestination) },
        GlobalSettingsSearchEntry("Background And Contact PFP Customisation (Incoming)", "Customize background and avatar photo for incoming calls", "incoming_custom_background", Icons.Outlined.Wallpaper, GsColorPurple) { it.navigate(IncomingCallUIScreenDestination(highlightKey = "incoming_custom_background")) },
        GlobalSettingsSearchEntry("Show Contact PFP", "Display caller's avatar photo over incoming call screen", "incoming_show_contact_pfp", Icons.Outlined.Contacts, GsColorCyan) { it.navigate(IncomingCallUIScreenDestination(highlightKey = "incoming_show_contact_pfp")) },
        GlobalSettingsSearchEntry("Show PFP for Non-Contacts (Incoming)", "Display avatar for incoming callers without photo", "incoming_custom_pfp_show_for_no_pfp", Icons.Outlined.Contacts, GsColorCyan) { it.navigate(IncomingCallUIScreenDestination(highlightKey = "incoming_custom_pfp_show_for_no_pfp")) },
        GlobalSettingsSearchEntry("Show Phone Number", "Display caller's phone number on incoming call screen", "incoming_show_phone_number", Icons.Outlined.Call, GsColorGreen) { it.navigate(IncomingCallUIScreenDestination(highlightKey = "incoming_show_phone_number")) },
        GlobalSettingsSearchEntry("Default Message", "Quick-reply message shown for incoming calls", "default_message_link", Icons.Outlined.Message, GsColorBlue) { it.navigate(IncomingCallUIScreenDestination(highlightKey = "default_message_link")) },
        GlobalSettingsSearchEntry("Default Message App", "Choose default messaging app for quick replies", "default_message_app", Icons.Outlined.Message, GsColorBlue) { it.navigate(DefaultMessageAppScreenDestination) },

        // ── Ongoing Call UI screen ───────────────────────────────────────────────
        GlobalSettingsSearchEntry("Show ongoing call UI when the call is answered when phone is unlocked", "Display full screen in-call screen after answering", "show_ongoing_call_ui_when_answered", Icons.Outlined.Call, GsColorBlue) { it.navigate(CallerUIScreenDestination(highlightKey = "show_ongoing_call_ui_when_answered")) },
        GlobalSettingsSearchEntry("Show ongoing call UI screen in lockscreen when the call is answered", "Display in-call screen on lockscreen after answering", "show_ongoing_call_ui_on_lockscreen_when_answered", Icons.Outlined.Lock, GsColorIndigo) { it.navigate(CallerUIScreenDestination(highlightKey = "show_ongoing_call_ui_on_lockscreen_when_answered")) },
        GlobalSettingsSearchEntry("Background And Contact PFP Customisation (Ongoing)", "Customize background and avatar photo for ongoing in-call screen", "ongoing_custom_background", Icons.Outlined.Wallpaper, GsColorPurple) { it.navigate(CallerUIScreenDestination(highlightKey = "ongoing_custom_background")) },
        GlobalSettingsSearchEntry("Show Contact PFP in Ongoing Call", "Display contact avatar photo on ongoing call screen", "ongoing_show_contact_pfp", Icons.Outlined.Contacts, GsColorCyan) { it.navigate(CallerUIScreenDestination(highlightKey = "ongoing_show_contact_pfp")) },
        GlobalSettingsSearchEntry("Show PFP for Non-Contacts (Ongoing)", "Display avatar on ongoing call screen for callers without photo", "ongoing_custom_pfp_show_for_no_pfp", Icons.Outlined.Contacts, GsColorCyan) { it.navigate(CallerUIScreenDestination(highlightKey = "ongoing_custom_pfp_show_for_no_pfp")) },
        GlobalSettingsSearchEntry("Show Phone Number in Ongoing Call", "Display phone number on ongoing call screen", "ongoing_show_phone_number", Icons.Outlined.Call, GsColorGreen) { it.navigate(CallerUIScreenDestination(highlightKey = "ongoing_show_phone_number")) },

        // ── Languages ────────────────────────────────────────────────────────
        GlobalSettingsSearchEntry("Languages", "Change app language (System, English, Arabic, Hindi, Tamil)", "app_language", Icons.Outlined.Translate, GsColorTeal) { it.navigate(LanguageSettingsScreenDestination()) },

        // ── About screen ──────────────────────────────────────────────────────────
        GlobalSettingsSearchEntry("Made By Hari", "Developer info", "made_by_hari", Icons.Outlined.Info, GsColorBluGrey) { it.navigate(AboutAppScreenDestination(highlightKey = "made_by_hari")) },
        GlobalSettingsSearchEntry("Source Code", "View Ever Dialer's source on GitHub", "source_code", Icons.Outlined.Info, GsColorBluGrey) { it.navigate(AboutAppScreenDestination(highlightKey = "source_code")) },
        GlobalSettingsSearchEntry("Telegram App Support Group", "Get help and discuss the app", "telegram_support", Icons.Outlined.Info, GsColorBlue) { it.navigate(AboutAppScreenDestination(highlightKey = "telegram_support")) },
        GlobalSettingsSearchEntry("App Recommending Channel in Telegram", "Follow for app announcements", "telegram_channel", Icons.Outlined.Info, GsColorBlue) { it.navigate(AboutAppScreenDestination(highlightKey = "telegram_channel")) },
        GlobalSettingsSearchEntry("My Other App (Everlasting Android Tweak)", "Check out the developer's other app", "other_app_link", Icons.Outlined.Info, GsColorIndigo) { it.navigate(AboutAppScreenDestination(highlightKey = "other_app_link")) },

        // ── PC Connect & PC Sync ─────────────────────────────────────────────────
        GlobalSettingsSearchEntry("PC Connect", "Connect and sync with Ever Dialer PC over Bluetooth", "pc_connect", Icons.Outlined.Devices, GsColorBlue) { it.navigate(PcConnectScreenDestination()) },
        GlobalSettingsSearchEntry("Bluetooth PC Sync", "PC Connect: Synchronize data and enable desktop landscape view in Ever Dialer PC", "pc_bluetooth_sync", Icons.Outlined.BluetoothSearching, GsColorBlue) { it.navigate(PcSyncSettingsScreenDestination(highlightKey = "pc_bluetooth_sync")) },
        GlobalSettingsSearchEntry("Sync Favourites", "PC Connect: Sync starred & favorite contacts to PC", "pc_sync_favourites", Icons.Default.Favorite, Color(0xFFE91E63)) { it.navigate(PcConnectScreenDestination(highlightKey = "pc_sync_favourites")) },
        GlobalSettingsSearchEntry("Sync Call Logs", "PC Connect: Sync incoming, outgoing, and missed call history to PC", "pc_sync_call_logs", Icons.Default.History, GsColorGreen) { it.navigate(PcConnectScreenDestination(highlightKey = "pc_sync_call_logs")) },
        GlobalSettingsSearchEntry("Sync Contacts", "PC Connect: Sync full contact list and details to PC", "pc_sync_contacts", Icons.Default.Person, GsColorBlue) { it.navigate(PcConnectScreenDestination(highlightKey = "pc_sync_contacts")) },
        GlobalSettingsSearchEntry("Sync SMS", "PC Connect: Sync text message conversations and chats to PC", "pc_sync_sms", Icons.Default.Chat, GsColorPurple) { it.navigate(PcConnectScreenDestination(highlightKey = "pc_sync_sms")) },
        GlobalSettingsSearchEntry("Sync Notes", "PC Connect: Sync call notes and custom notes to PC", "pc_sync_notes", Icons.Default.Note, GsColorOrange) { it.navigate(PcConnectScreenDestination(highlightKey = "pc_sync_notes")) },
        GlobalSettingsSearchEntry("Sync Contact PFP", "PC Connect: Sync contact profile pictures to PC", "pc_sync_contact_pfp", Icons.Default.AccountCircle, GsColorTeal) { it.navigate(PcConnectScreenDestination(highlightKey = "pc_sync_contact_pfp")) },
        GlobalSettingsSearchEntry("Dynamic colors", "PC Connect: Sync phone's theme color to desktop version", "pc_sync_dynamic_colors", Icons.Default.Palette, GsColorPurple) { it.navigate(PcConnectScreenDestination(highlightKey = "pc_sync_dynamic_colors")) },
        GlobalSettingsSearchEntry("PC Sync Settings", "PC Connect: Bluetooth connection status, device pairing, and manual sync", "pc_sync_settings", Icons.Outlined.BluetoothConnected, GsColorBlue) { it.navigate(PcSyncSettingsScreenDestination()) },

        // ── Categories & Default Category (Call Logs & SMS) ──────────────────────
        GlobalSettingsSearchEntry("Category (Call Logs)", "Call Logs: Customize pills, reorder, visibility & custom categories", "call_category", Icons.Default.Category, GsColorPurple) { it.navigate(CallLogsSettingsScreenDestination(highlightKey = "call_category")) },
        GlobalSettingsSearchEntry("Default Category (Call Logs)", "Call Logs: Choose which category opens when call logs screen starts", "call_default_category", Icons.Default.Tab, GsColorPurple) { it.navigate(CallLogsSettingsScreenDestination(highlightKey = "call_default_category")) },
        GlobalSettingsSearchEntry("Create Call Category", "Call Logs: Create custom category and choose contacts to display in call logs", "create_call_category", Icons.Default.Category, GsColorPurple) { it.navigate(CreateCallCategoryScreenDestination()) },
        GlobalSettingsSearchEntry("SMS Settings", "Quik SMS settings, appearance, sending, and gestures", "sms_settings_link", Icons.Outlined.Sms, GsColorPurple) { it.navigate(SmsSettingsScreenDestination()) },
        GlobalSettingsSearchEntry("Category (SMS)", "SMS: Customize pills, reorder, visibility & custom categories", "sms_category", Icons.Default.Category, GsColorPurple) { it.navigate(SmsSettingsScreenDestination(highlightKey = "sms_category")) },
        GlobalSettingsSearchEntry("Default Category (SMS)", "SMS: Choose which category opens when SMS screen starts", "sms_default_category", Icons.Default.Tab, GsColorPurple) { it.navigate(SmsSettingsScreenDestination(highlightKey = "sms_default_category")) },
        GlobalSettingsSearchEntry("Create SMS Category", "SMS: Create custom category and choose contacts to display in SMS logs", "create_sms_category", Icons.Default.Category, GsColorPurple) { it.navigate(CreateSmsCategoryScreenDestination()) },

        // ── SMS Nested Settings ──────────────────────────────────────────────────
        GlobalSettingsSearchEntry("Message Text Size", "SMS: Customize message font size in chats", "sms_text_size", Icons.Outlined.FormatSize, GsColorPurple) { it.navigate(SmsSettingsScreenDestination(highlightKey = "sms_text_size")) },
        GlobalSettingsSearchEntry("Automatic Avatar Colors", "SMS: Generate distinct avatar colors for contacts", "sms_auto_color_avatars", Icons.Outlined.Palette, GsColorGreen) { it.navigate(SmsSettingsScreenDestination(highlightKey = "sms_auto_color_avatars")) },
        GlobalSettingsSearchEntry("Show Avatar in SMS", "SMS: Show contact avatars in message logs", "sms_show_avatar", Icons.Outlined.AccountCircle, GsColorPurple) { it.navigate(SmsSettingsScreenDestination(highlightKey = "sms_show_avatar")) },
        GlobalSettingsSearchEntry("Show Speech-to-Text Button", "SMS: Display microphone button in message composer", "sms_show_stt", Icons.Outlined.Mic, GsColorAmber) { it.navigate(SmsSettingsScreenDestination(highlightKey = "sms_show_stt")) },
        GlobalSettingsSearchEntry("Prevent Screenshots in SMS", "SMS: Block screen captures and screen recording in SMS", "sms_prevent_screenshots", Icons.Outlined.Security, GsColorRed) { it.navigate(SmsSettingsScreenDestination(highlightKey = "sms_prevent_screenshots")) },
        GlobalSettingsSearchEntry("Floating SMS", "SMS: Float bubble on incoming messages to chat without leaving apps", "sms_floating_bubble", Icons.Outlined.SpeakerNotes, GsColorBlue) { it.navigate(SmsSettingsScreenDestination(highlightKey = "sms_floating_bubble")) },
        GlobalSettingsSearchEntry("SMS App Icon in App Drawer", "SMS: Show dedicated icon in app drawer to launch SMS directly", "sms_app_icon_drawer", Icons.Outlined.Apps, GsColorIndigo) { it.navigate(SmsSettingsScreenDestination(highlightKey = "sms_app_icon_drawer")) },
        GlobalSettingsSearchEntry("SMS App Name", "SMS: Customize launcher name for the dedicated SMS shortcut", "sms_app_name", Icons.Outlined.Badge, GsColorTeal) { it.navigate(SmsSettingsScreenDestination(highlightKey = "sms_app_name")) },
        GlobalSettingsSearchEntry("Delay Sending SMS", "SMS: Delay outgoing messages for a few seconds to allow cancelling", "sms_send_delay", Icons.Outlined.Timer, GsColorAmber) { it.navigate(SmsSettingsScreenDestination(highlightKey = "sms_send_delay")) },
        GlobalSettingsSearchEntry("SMS Delivery Reports", "SMS: Request delivery confirmation for sent text messages", "sms_delivery_reports", Icons.Outlined.DoneAll, GsColorGreen) { it.navigate(SmsSettingsScreenDestination(highlightKey = "sms_delivery_reports")) },
        GlobalSettingsSearchEntry("SMS Signature", "SMS: Automatically append custom text signature to messages", "sms_signature", Icons.Outlined.Notes, GsColorPurple) { it.navigate(SmsSettingsScreenDestination(highlightKey = "sms_signature")) },
        GlobalSettingsSearchEntry("Strip Unicode Characters", "SMS: Replace accented characters with standard ones to save characters", "sms_strip_unicode", Icons.Outlined.TextFormat, GsColorBlue) { it.navigate(SmsSettingsScreenDestination(highlightKey = "sms_strip_unicode")) },
        GlobalSettingsSearchEntry("Send to Mobile-Only Numbers", "SMS: Prevent sending SMS to landlines or non-mobile numbers", "sms_mobile_only", Icons.Outlined.PhoneAndroid, GsColorIndigo) { it.navigate(SmsSettingsScreenDestination(highlightKey = "sms_mobile_only")) },
        GlobalSettingsSearchEntry("Send Long Messages as MMS", "SMS: Automatically convert multi-part long text messages to MMS", "sms_long_as_mms", Icons.Outlined.Attachment, GsColorOrange) { it.navigate(SmsSettingsScreenDestination(highlightKey = "sms_long_as_mms")) },
        GlobalSettingsSearchEntry("Maximum MMS Size", "SMS: Choose file size limit for outgoing multimedia messages", "sms_max_mms_size", Icons.Outlined.PhotoSizeSelectActual, GsColorTeal) { it.navigate(SmsSettingsScreenDestination(highlightKey = "sms_max_mms_size")) }
    )
}

/** The full, flat list of every setting reachable from Settings, used by [SettingsSearchEntryPoint]
 *  so the same search box can be dropped onto any settings page. */
fun buildGlobalSettingsSearchEntries(): List<GlobalSettingsSearchEntry> = globalSettingsSearchEntries

/**
 * A "Search settings" box that can be dropped as the first item on any settings page (main
 * Settings screen included, in principle, though it has its own richer local search). Typing
 * here searches every setting across every settings page; tapping a result navigates straight
 * to it (or back to the main Settings screen with that row highlighted, if it lives there).
 */
@Composable
fun SettingsSearchEntryPoint(navigator: DestinationsNavigator, modifier: Modifier = Modifier) {
    var query by rememberSaveable { mutableStateOf("") }
    val entries = globalSettingsSearchEntries
    val filtered = remember(query) {
        val q = query.trim()
        if (q.isBlank()) emptyList()
        else entries.filter {
            com.coolappstore.everdialer.by.svhp.controller.util.matchesFuzzySearch(it.title, q) ||
                    com.coolappstore.everdialer.by.svhp.controller.util.matchesFuzzySearch(it.subtitle, q)
        }.take(8)
    }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = androidx.compose.ui.platform.LocalContext.current

    var isFocused by remember { mutableStateOf(false) }
    val prefs: PreferenceManager = koinInject()
    val settingsState by prefs.settingsChanged.collectAsState()
    val settingsSearchHistory = remember(settingsState) {
        SearchHistoryManager.getHistory(prefs, SearchHistoryManager.Type.SETTINGS)
    }
    fun saveQuery(q: String = query) {
        if (q.isNotBlank()) {
            SearchHistoryManager.addHistory(prefs, SearchHistoryManager.Type.SETTINGS, q)
        }
    }

    val voiceSearchLauncher = com.coolappstore.everdialer.by.svhp.controller.util.rememberVoiceSearchLauncher { spokenText ->
        query = spokenText
        saveQuery(spokenText)
    }

    val appRoundness = LocalCardCornerRadius.current.value.coerceAtLeast(0f).dp
    val searchShape = RoundedCornerShape(appRoundness)

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            shape = searchShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().clip(searchShape)
        ) {
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused },
                placeholder = { Text("Search settings") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedVisibility(visible = query.isNotEmpty(), enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                        IconButton(onClick = {
                            com.coolappstore.everdialer.by.svhp.controller.util.VoiceSearchHelper.launchVoiceSearch(context, voiceSearchLauncher)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = {
                        saveQuery()
                        keyboardController?.hide()
                    }
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )
        }

        AnimatedVisibility(
            visible = isFocused && query.isBlank() && settingsSearchHistory.isNotEmpty(),
            enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(250)) +
                    androidx.compose.animation.expandVertically(androidx.compose.animation.core.tween(300)),
            exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200)) +
                    androidx.compose.animation.shrinkVertically(androidx.compose.animation.core.tween(250))
        ) {
            Column {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
                SearchHistorySection(
                    history = settingsSearchHistory,
                    onItemClick = { item ->
                        query = item
                        saveQuery(item)
                    },
                    onRemoveItem = { item ->
                        com.coolappstore.everdialer.by.svhp.controller.util.SearchHistoryManager.removeHistoryItem(
                            prefs,
                            com.coolappstore.everdialer.by.svhp.controller.util.SearchHistoryManager.Type.SETTINGS,
                            item
                        )
                    },
                    onClearAll = {
                        com.coolappstore.everdialer.by.svhp.controller.util.SearchHistoryManager.clearHistory(
                            prefs,
                            com.coolappstore.everdialer.by.svhp.controller.util.SearchHistoryManager.Type.SETTINGS
                        )
                    }
                )
            }
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
                                saveQuery()
                                keyboardController?.hide()
                                focusManager.clearFocus(force = true)
                                if (entry.navigateTo != null) {
                                    entry.navigateTo.invoke(navigator)
                                } else {
                                    navigator.navigate(SettingsScreenDestination(highlightKey = entry.key))
                                }
                            }
                        )
                        if (index < filtered.size - 1) CardDivider()
                    }
                }
            }
        }
    }
}
