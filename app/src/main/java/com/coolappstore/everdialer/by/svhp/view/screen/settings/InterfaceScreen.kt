package com.coolappstore.everdialer.by.svhp.view.screen.settings

import com.ramcosta.composedestinations.generated.destinations.CustomFontScreenDestination
import com.ramcosta.composedestinations.generated.destinations.CallLogsSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.DialpadSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ContactInfoSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FavouritesSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.NavigationSettingsScreenDestination
import com.coolappstore.everdialer.by.svhp.view.theme.AppFontHelper
import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import com.coolappstore.everdialer.by.svhp.view.components.RivoCheckboxListItem
import com.coolappstore.everdialer.by.svhp.view.components.rivoHeadingColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import android.os.Build
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoListItem
import com.coolappstore.everdialer.by.svhp.view.components.RivoSwitchListItem
import com.coolappstore.everdialer.by.svhp.view.components.settingsSearchHighlight
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.io.File
import kotlin.math.roundToInt

private val ColorPurple = Color(0xFF9C27B0)
private val ColorTeal   = Color(0xFF009688)
private val ColorAmber  = Color(0xFFFFC107)
private val ColorBlue   = Color(0xFF2196F3)
private val ColorGreen  = Color(0xFF4CAF50)
private val ColorOrange = Color(0xFFFF9800)
private val ColorIndigo = Color(0xFF3F51B5)
private val ColorRed    = Color(0xFFE53935)

data class ThemeOption(val key: String, val label: String)

private val themeOptions = listOf(
    ThemeOption("auto",    "Auto"),
    ThemeOption("light",   "Light"),
    ThemeOption("dark",    "Dark"),
    ThemeOption("white",   "White"),
    ThemeOption("black",   "Black"),
    ThemeOption("auto_bw", "Auto B/W")
)

private fun triggerRestartPrompt(
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    context: android.content.Context
) {
    scope.launch {
        val result = snackbarHostState.showSnackbar(
            message = "Restart required to apply theme changes fully.",
            actionLabel = "Restart",
            duration = SnackbarDuration.Long
        )
        if (result == SnackbarResult.ActionPerformed) {
            val packageManager = context.packageManager
            val intent = packageManager.getLaunchIntentForPackage(context.packageName)
            val componentName = intent?.component
            val mainIntent = android.content.Intent.makeRestartActivityTask(componentName)
            context.startActivity(mainIntent)
            Runtime.getRuntime().exit(0)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun InterfaceScreen(navigator: DestinationsNavigator, highlightKey: String? = null) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    val isInteractive = lifecycleState.isAtLeast(Lifecycle.State.STARTED)

    val prefs = koinInject<PreferenceManager>()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var highlightedKey by remember { mutableStateOf(highlightKey) }

    var themeMode           by remember { mutableStateOf(prefs.getString(PreferenceManager.KEY_THEME_MODE, "auto") ?: "auto") }
    var dynamicColors       by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_DYNAMIC_COLORS, true)) }
    var showFloatingColorPicker by remember { mutableStateOf(false) }
    var saturatedColors     by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SATURATED_COLORS, false)) }
    var saturatedTarget     by remember { mutableStateOf(prefs.getSaturatedTarget()) }
    var saturatedModes      by remember { mutableStateOf(prefs.getSaturatedModesSet()) }
    var saturationLevelLight by remember { mutableFloatStateOf(prefs.getSaturationLevel(false)) }
    var saturationLevelDark  by remember { mutableFloatStateOf(prefs.getSaturationLevel(true)) }
    var cornerRadius        by remember { mutableFloatStateOf(prefs.getFloat(PreferenceManager.KEY_CORNER_RADIUS, 28f)) }
    var isRoundnessExpanded by remember { mutableStateOf(false) }
    var solidIcons          by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SOLID_ICONS, false)) }
    var solidIconsDynamic   by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SOLID_ICONS_DYNAMIC, false)) }
    var solidIconsLightStyle by remember { mutableStateOf(prefs.getString(PreferenceManager.KEY_SOLID_ICONS_LIGHT, PreferenceManager.SOLID_ICONS_STYLE_DIM) ?: PreferenceManager.SOLID_ICONS_STYLE_DIM) }
    var solidIconsDarkStyle  by remember { mutableStateOf(prefs.getString(PreferenceManager.KEY_SOLID_ICONS_DARK, PreferenceManager.SOLID_ICONS_STYLE_DIM) ?: PreferenceManager.SOLID_ICONS_STYLE_DIM) }
    var showSolidIconsLightDialog by remember { mutableStateOf(false) }
    var showSolidIconsDarkDialog  by remember { mutableStateOf(false) }
    var circleIcons         by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CIRCLE_ICONS, false)) }
    var showFirstLetter     by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_FIRST_LETTER, true)) }
    var colorfulAvatars     by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_COLORFUL_AVATARS, true)) }
    var showAvatarsInCalls    by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_AVATARS_CALLS, true)) }
    var showAvatarsInContacts by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_AVATARS_CONTACTS, true)) }
    var showAvatarSms         by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SMS_SHOW_AVATAR, true)) }
    var showPicture         by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_PICTURE, true)) }
    var iconOnlyNav         by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_ICON_ONLY_NAV, false)) }
    var scrollIndication    by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SCROLL_INDICATION, true)) }
    var pillNav             by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_PILL_NAV, true)) }
    var groupCallsByLatest  by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_GROUP_CALLS_BY_LATEST, false)) }
    var showSimsInCallLogs  by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_SIMS_IN_CALL_LOGS, prefs.getShowSimsInCallLogsDefault())) }
    var showTotalCallsMade  by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_TOTAL_CALLS_MADE, false)) }
    var hideDuplicateNumbersInContact by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_HIDE_DUPLICATE_NUMBERS_IN_CONTACT, true)) }
    var nameNonContactsAsUnknown by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_NAME_NON_CONTACTS_AS_UNKNOWN, true)) }
    var dialpadMemory  by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_DIALPAD_MEMORY, true)) }
    var showCallLogsInDialpadSearchList by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_CALL_LOGS_IN_DIALPAD_SEARCH_LIST, true)) }
    var showNumbersInCallLogs by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_NUMBERS_IN_CALL_LOGS, true)) }

    var autoDeleteUnknownEnabled by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_AUTO_DELETE_UNKNOWN_CALLS_ENABLED, false)) }
    var autoDeleteUnknownValue   by remember { mutableStateOf(prefs.getInt(PreferenceManager.KEY_AUTO_DELETE_UNKNOWN_CALLS_VALUE, 1).toString()) }
    var autoDeleteUnknownUnit    by remember { mutableStateOf(prefs.getString(PreferenceManager.KEY_AUTO_DELETE_UNKNOWN_CALLS_UNIT, "days") ?: "days") }
    var callTimeFormat24h   by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CALL_TIME_FORMAT_24H, false)) }
    var showTalkTimeInCallLogs by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_TALK_TIME_IN_CALL_LOGS, false)) }
    var customPrimaryColor  by remember { mutableStateOf(prefs.getInt("custom_primary_color", Color(0xFF6750A4).toArgb())) }
    var showIncomingCallUI  by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_INCOMING_CALL_UI, true)) }
    var showCallerUI        by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_CALLER_UI, true)) }
    var openDialpadDefault  by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_OPEN_DIALPAD_DEFAULT, false)) }
    var favoritesInList     by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_FAVORITES_IN_LIST, false)) }
    var hideRateAndReview   by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_HIDE_RATE_AND_REVIEW, false)) }
    val rateReviewToggleSettingsVersion by prefs.settingsChanged.collectAsState()
    val rateReviewSecretActive = remember(rateReviewToggleSettingsVersion) {
        prefs.getBoolean(PreferenceManager.KEY_RATE_REVIEW_HIDDEN_SECRET, false)
    }
    var scrollAnimation     by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SCROLL_ANIMATION, true)) }
    var animationStyle      by remember { mutableStateOf(prefs.getString(PreferenceManager.KEY_ANIMATION_STYLE, PreferenceManager.ANIMATION_STYLE_ZOOM) ?: PreferenceManager.ANIMATION_STYLE_ZOOM) }
    var showAnimationStyleDialog by remember { mutableStateOf(false) }
    var motionBlurAnimation by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_MOTION_BLUR_ANIMATION, false)) }
    var liquidGlass         by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_LIQUID_GLASS, false)) }
    var blurEffects         by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_BLUR_EFFECTS, false)) }
    var hangupAnimation     by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_HANGUP_ANIMATION, true)) }

    // App Name preset picker
    var showAppNameDialog by remember { mutableStateOf(false) }
    val appNamePresets = remember { buildAppNamePresets(context) }
    var selectedAppNameKey by remember {
        mutableStateOf(prefs.getString(PreferenceManager.KEY_APP_NAME_PRESET, "default") ?: "default")
    }



    // Default Tab dialog
    var showDefaultTabDialog by remember { mutableStateOf(false) }
    var defaultTab           by remember { mutableStateOf(prefs.getString(PreferenceManager.KEY_DEFAULT_TAB, "calls") ?: "calls") }

    var showTabSectionsDialog by remember { mutableStateOf(false) }
    var tabShowFavorites  by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_FAVORITES,  true)) }
    var tabShowCalls      by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_CALLS,      true)) }
    var tabShowContacts   by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_CONTACTS,   true)) }
    var tabShowSms        by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_SMS,        true)) }
    var tabShowGroups     by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_GROUPS,     false)) }
    var tabShowRecordings by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_RECORDINGS, true)) }
    var tabShowNotes      by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_NOTES,      true)) }
    var tabShowDialpad    by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_DIALPAD,    false)) }
    data class TabOption(val key: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
    val tabOptions = listOf(
        TabOption("favorites",  "Favourites", Icons.Outlined.FavoriteBorder),
        TabOption("calls",      "Calls",      Icons.Outlined.History),
        TabOption("contacts",   "Contacts",   Icons.Outlined.Person),
        TabOption("sms",        "SMS",        Icons.Outlined.Chat),
        TabOption("groups",     "Groups",     Icons.Outlined.Group),
        TabOption("recordings", "Recordings", Icons.Outlined.FiberManualRecord),
        TabOption("notes",      "Note",       Icons.Outlined.Note),
        TabOption("dialpad",    "Dialpad",    Icons.Outlined.Dialpad)
    )

    // Custom order of tab keys, persisted as a comma-separated string. Any tab keys
    // missing from a previously-saved (older) order are appended so new tabs always show.
    val tabOrder = remember {
        mutableStateListOf<String>().apply {
            val saved = prefs.getString(PreferenceManager.KEY_TAB_ORDER, null)
            val savedKeys = saved?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
            val validKeys = tabOptions.map { it.key }
            addAll(savedKeys.filter { it in validKeys })
            validKeys.forEach { key -> if (key !in this) add(key) }
        }
    }
    fun persistTabOrder() {
        prefs.setString(PreferenceManager.KEY_TAB_ORDER, tabOrder.joinToString(","))
    }
    fun resetTabSectionsToDefault() {
        val defaults = PreferenceManager.DEFAULT_TAB_ORDER.split(",").map { it.trim() }.filter { it.isNotBlank() }
        tabOrder.clear()
        tabOrder.addAll(defaults)
        persistTabOrder()
        tabShowFavorites  = true;  prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_FAVORITES,  true)
        tabShowCalls      = true;  prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_CALLS,      true)
        tabShowContacts   = true;  prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_CONTACTS,   true)
        tabShowSms        = true;  prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_SMS,        true)
        tabShowGroups     = false; prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_GROUPS,     false)
        tabShowRecordings = true;  prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_RECORDINGS, true)
        tabShowNotes      = true;  prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_NOTES,      true)
        tabShowDialpad    = false; prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_DIALPAD,    false)
    }



    // ── Context Menu Elements ──────────────────────────────────────────────
    // Top level: 3 fixed sections (Favourites, Call Logs, Contacts) — these are just
    // navigation rows (no checkbox/drag here). Tapping one opens a sub-dialog listing
    // that section's actual long-press context menu entries, each with a checkbox to
    // show/hide it and a drag handle to reorder it (same pattern as Tab Sections).
    var showContextMenuDialog by remember { mutableStateOf(false) }
    var activeContextMenuSection by remember { mutableStateOf<String?>(null) }

    data class ContextMenuSection(val key: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
    val contextMenuSections = listOf(
        ContextMenuSection("favorites", "Favourites", Icons.Outlined.FavoriteBorder),
        ContextMenuSection("call_logs", "Call Logs",  Icons.Outlined.History),
        ContextMenuSection("contacts",  "Contacts",   Icons.Outlined.Person)
    )

    data class ContextMenuItemOption(val key: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
    val contextMenuItemsBySection = mapOf(
        "favorites" to listOf(
            ContextMenuItemOption("select",          "Select",                    Icons.Default.CheckBox),
            ContextMenuItemOption("call",             "Call",                      Icons.Default.Call),
            ContextMenuItemOption("send_sms",         "Send SMS",                  Icons.Default.Message),
            ContextMenuItemOption("call_chat_via",    "Call/Chat Via",             Icons.AutoMirrored.Filled.Chat),
            ContextMenuItemOption("view_details",     "View Details",              Icons.Default.Info),
            ContextMenuItemOption("fake_call",        "Fake Call",                 Icons.Outlined.PhoneCallback),
            ContextMenuItemOption("remove_favorite",  "Remove from Favourites",    Icons.Default.Favorite)
        ),
        "call_logs" to listOf(
            ContextMenuItemOption("select",           "Select",                    Icons.Default.CheckBox),
            ContextMenuItemOption("call_back",         "Call back",                 Icons.Default.Call),
            ContextMenuItemOption("view_contact",      "View contact",              Icons.Default.Person),
            ContextMenuItemOption("edit_contact",      "Edit contact",              Icons.Default.Edit),
            ContextMenuItemOption("copy_number",       "Copy number",               Icons.Default.ContentCopy),
            ContextMenuItemOption("add_to_contacts",   "Add contact",               Icons.Default.PersonAdd),
            ContextMenuItemOption("share",             "Share contact",             Icons.Default.Share),
            ContextMenuItemOption("call_chat_via",     "Call/Chat Via",             Icons.AutoMirrored.Filled.Chat),
            ContextMenuItemOption("send_text",         "Send text",                 Icons.AutoMirrored.Filled.Message),
            ContextMenuItemOption("search_truecaller", "Search Truecaller",         Icons.Default.Search),
            ContextMenuItemOption("move_contact",      "Move contact",              Icons.Default.DriveFileMove),
            ContextMenuItemOption("toggle_favorite",   "Add/Remove Favourites",     Icons.Default.Favorite),
            ContextMenuItemOption("block_number",      "Block/Unblock number",      Icons.Default.Block),
            ContextMenuItemOption("fake_call",         "Fake Call",                 Icons.Outlined.PhoneCallback),
            ContextMenuItemOption("delete_call_log",   "Delete from call log",      Icons.Default.Delete)
        ),
        "contacts" to listOf(
            ContextMenuItemOption("select",           "Select",                    Icons.Default.CheckBox),
            ContextMenuItemOption("call",             "Call",                      Icons.Default.Call),
            ContextMenuItemOption("view_contact",      "View contact",              Icons.Default.Person),
            ContextMenuItemOption("edit_contact",      "Edit contact",              Icons.Default.Edit),
            ContextMenuItemOption("copy_number",       "Copy number",               Icons.Default.ContentCopy),
            ContextMenuItemOption("share_contact",     "Share contact",             Icons.Default.Share),
            ContextMenuItemOption("call_chat_via",     "Call/Chat Via",             Icons.AutoMirrored.Filled.Chat),
            ContextMenuItemOption("send_text",         "Send text",                 Icons.AutoMirrored.Filled.Message),
            ContextMenuItemOption("move_contact",      "Move contact",              Icons.Default.DriveFileMove),
            ContextMenuItemOption("toggle_favorite",   "Add/Remove Favourites",     Icons.Default.Favorite),
            ContextMenuItemOption("block_contact",     "Block/Unblock contact",     Icons.Default.Block),
            ContextMenuItemOption("fake_call",         "Fake Call",                 Icons.Outlined.PhoneCallback),
            ContextMenuItemOption("delete_contact",    "Delete contact",            Icons.Default.Delete)
        )
    )

    fun contextMenuShowKey(section: String, itemKey: String) = "context_menu_${section}_show_$itemKey"
    fun contextMenuOrderKey(section: String) = "context_menu_${section}_order"

    // Per-section ordered key list, persisted as a comma-separated string. Any item keys
    // missing from a previously-saved (older) order are appended so new entries always show.
    val contextMenuOrders = remember {
        mutableStateMapOf<String, MutableList<String>>().apply {
            contextMenuSections.forEach { section ->
                val validKeys = contextMenuItemsBySection[section.key].orEmpty().map { it.key }
                val saved = prefs.getString(contextMenuOrderKey(section.key), null)
                val savedKeys = saved?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
                val list = mutableStateListOf<String>()
                list.addAll(savedKeys.filter { it in validKeys })
                validKeys.forEach { key -> if (key !in list) list.add(key) }
                if (section.key == "call_logs") {
                    val copyIdx = list.indexOf("copy_number")
                    val addIdx = list.indexOf("add_to_contacts")
                    val deleteIdx = list.indexOf("delete_call_log")
                    if (copyIdx != -1 && addIdx != -1) {
                        if (deleteIdx != -1 && addIdx > deleteIdx) {
                            list.removeAt(addIdx)
                            val newCopyIdx = list.indexOf("copy_number")
                            list.add(newCopyIdx + 1, "add_to_contacts")
                            prefs.setString(contextMenuOrderKey(section.key), list.joinToString(","))
                        } else if (addIdx != copyIdx + 1 && (deleteIdx == -1 || copyIdx < deleteIdx)) {
                            list.removeAt(addIdx)
                            val newCopyIdx = list.indexOf("copy_number")
                            list.add(newCopyIdx + 1, "add_to_contacts")
                            prefs.setString(contextMenuOrderKey(section.key), list.joinToString(","))
                        }
                    }
                }
                if (section.key == "contacts" && "call" in list) {
                    val selectIdx = list.indexOf("select")
                    val callIdx = list.indexOf("call")
                    val deleteIdx = list.indexOf("delete_contact")
                    if (deleteIdx != -1 && callIdx > deleteIdx) {
                        list.removeAt(callIdx)
                        val targetIdx = if (selectIdx != -1) selectIdx + 1 else 0
                        list.add(targetIdx, "call")
                        prefs.setString(contextMenuOrderKey(section.key), list.joinToString(","))
                    }
                }
                this[section.key] = list
            }
        }
    }
    fun persistContextMenuOrder(section: String) {
        val order = contextMenuOrders[section] ?: return
        prefs.setString(contextMenuOrderKey(section), order.joinToString(","))
    }

    var hexInput by remember { mutableStateOf(String.format("%06X", 0xFFFFFF and customPrimaryColor)) }
    var hexError by remember { mutableStateOf(false) }

    var displayScale  by remember { mutableFloatStateOf(prefs.getFloat(PreferenceManager.KEY_DISPLAY_SCALE, 1.0f)) }
    var fontSizeScale by remember { mutableFloatStateOf(prefs.getFloat(PreferenceManager.KEY_CUSTOM_FONT_SIZE, 1.0f)) }

    val presetColors = listOf(
        Color(0xFF6750A4), Color(0xFF0061A4), Color(0xFF006A60), Color(0xFF436916),
        Color(0xFF984061), Color(0xFF006874), Color(0xFF705D00), Color(0xFFBF0031),
        Color(0xFFE91E63), Color(0xFFFF5722), Color(0xFF795548), Color(0xFF607D8B)
    )

    fun applyHexColor(hex: String) {
        val cleaned = hex.trimStart('#').uppercase()
        if (cleaned.length == 6) {
            try {
                val colorInt = android.graphics.Color.parseColor("#$cleaned")
                customPrimaryColor = colorInt
                prefs.setInt("custom_primary_color", colorInt)
                hexError = false
                triggerRestartPrompt(scope, snackbarHostState, context)
            } catch (_: Exception) { hexError = true }
        } else {
            hexError = true
        }
    }


    // ── Animation Style Dialog ───────────────────────────────────────────────
    if (showAnimationStyleDialog) {
        AlertDialog(
            onDismissRequest = { showAnimationStyleDialog = false },
            icon = { Icon(Icons.Outlined.Animation, null, tint = ColorTeal) },
            title = { Text("Animation Style") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Choose the page transition animation style used when navigating between screens.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        PreferenceManager.ANIMATION_STYLE_ZOOM to "Zoom (in/out)",
                        PreferenceManager.ANIMATION_STYLE_WINDOWS_PHONE to "Windows Phone"
                    ).forEach { (key, label) ->
                        val isSelected = animationStyle == key
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        animationStyle = key
                                        prefs.setString(PreferenceManager.KEY_ANIMATION_STYLE, key)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurface
                                )
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        animationStyle = key
                                        prefs.setString(PreferenceManager.KEY_ANIMATION_STYLE, key)
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary,
                                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAnimationStyleDialog = false }) { Text("Done") }
            }
        )
    }

    // ── Default Tab Dialog ─────────────────────────────────────────────────
    if (showDefaultTabDialog) {
        AlertDialog(
            onDismissRequest = { showDefaultTabDialog = false },
            icon = { Icon(Icons.Default.Tab, null, tint = ColorIndigo) },
            title = { Text("Default Tab Section") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Choose which tab opens when the app starts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    tabOptions.filter { it.key != "dialpad" }.forEach { option ->
                        val isSelected = defaultTab == option.key
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        defaultTab = option.key
                                        prefs.setString(PreferenceManager.KEY_DEFAULT_TAB, option.key)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    option.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurface
                                )
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        defaultTab = option.key
                                        prefs.setString(PreferenceManager.KEY_DEFAULT_TAB, option.key)
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary,
                                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDefaultTabDialog = false }) { Text("Done") }
            }
        )
    }

    // ── Tab Sections Dialog ──────────────────────────────────────────────────
    if (showTabSectionsDialog) {
        com.coolappstore.everdialer.by.svhp.view.components.TabSectionsDialog(
            onDismissRequest = { showTabSectionsDialog = false }
        )
    }



    // ── Context Menu Elements: top-level section list (no checkbox/drag here) ─
    if (showContextMenuDialog) {
        AlertDialog(
            onDismissRequest = { showContextMenuDialog = false },
            icon = { Icon(Icons.Default.ViewWeek, null, tint = ColorOrange) },
            title = { Text("Context Menu Elements") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Choose a section to view and customize its long-press context menu.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Column {
                        contextMenuSections.forEach { section ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clickable {
                                        showContextMenuDialog = false
                                        activeContextMenuSection = section.key
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = section.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(section.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showContextMenuDialog = false }) { Text("Done") }
            }
        )
    }

    // ── Context Menu Elements: per-section entries (checkbox + drag to reorder) ─
    activeContextMenuSection?.let { sectionKey ->
        val section = contextMenuSections.firstOrNull { it.key == sectionKey }
        val sectionItems = contextMenuItemsBySection[sectionKey].orEmpty()
        val sectionOrder = contextMenuOrders[sectionKey]

        if (section != null && sectionOrder != null) {
            val density = LocalDensity.current
            val haptic = LocalHapticFeedback.current
            val rowHeightDp = 52.dp
            val rowHeightPx = with(density) { rowHeightDp.toPx() }
            var draggedKey by remember(sectionKey) { mutableStateOf<String?>(null) }
            var dragOffsetY by remember(sectionKey) { mutableStateOf(0f) }

            fun itemChecked(itemKey: String) = prefs.getBoolean(contextMenuShowKey(sectionKey, itemKey), true)
            fun setItemChecked(itemKey: String, value: Boolean) {
                prefs.setBoolean(contextMenuShowKey(sectionKey, itemKey), value)
            }

            AlertDialog(
                onDismissRequest = { activeContextMenuSection = null },
                icon = { Icon(section.icon, null, tint = ColorOrange) },
                title = { Text("${section.label} Context Menu") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Choose which entries are visible, and drag the handle to reorder them in the context menu.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        val maxListHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.55f).coerceAtLeast(240.dp)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = maxListHeight)
                                .verticalScroll(rememberScrollState())
                        ) {
                            sectionOrder.forEach { itemKey ->
                                val option = sectionItems.firstOrNull { it.key == itemKey } ?: return@forEach
                                val isDragging = draggedKey == itemKey
                                key(itemKey) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    tonalElevation = if (isDragging) 6.dp else 0.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .zIndex(if (isDragging) 2f else 0f)
                                        .graphicsLayer {
                                            translationY = if (isDragging) dragOffsetY else 0f
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(rowHeightDp)
                                            .padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = option.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(option.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                        Checkbox(
                                            checked = itemChecked(itemKey),
                                            onCheckedChange = { setItemChecked(itemKey, it) },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = MaterialTheme.colorScheme.primary,
                                                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Filled.DragHandle,
                                            contentDescription = "Reorder ${option.label}",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .padding(start = 4.dp)
                                                .pointerInput(itemKey) {
                                                    detectDragGestures(
                                                        onDragStart = {
                                                            draggedKey = itemKey
                                                            dragOffsetY = 0f
                                                        },
                                                        onDragEnd = {
                                                            draggedKey = null
                                                            dragOffsetY = 0f
                                                            persistContextMenuOrder(sectionKey)
                                                        },
                                                        onDragCancel = {
                                                            draggedKey = null
                                                            dragOffsetY = 0f
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            dragOffsetY += dragAmount.y
                                                            val currentIdx = sectionOrder.indexOf(itemKey)
                                                            if (currentIdx != -1) {
                                                                val threshold = rowHeightPx * 0.6f
                                                                if (dragOffsetY > threshold && currentIdx < sectionOrder.lastIndex) {
                                                                    val item = sectionOrder.removeAt(currentIdx)
                                                                    sectionOrder.add(currentIdx + 1, item)
                                                                    dragOffsetY -= rowHeightPx
                                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                } else if (dragOffsetY < -threshold && currentIdx > 0) {
                                                                    val item = sectionOrder.removeAt(currentIdx)
                                                                    sectionOrder.add(currentIdx - 1, item)
                                                                    dragOffsetY += rowHeightPx
                                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                }
                                                            }
                                                        }
                                                    )
                                                }
                                        )
                                    }
                                }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { activeContextMenuSection = null }) { Text("Done") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        activeContextMenuSection = null
                        showContextMenuDialog = true
                    }) { Text("Back") }
                }
            )
        }
    }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            com.coolappstore.everdialer.by.svhp.view.components.SettingsPillTopAppBar(
                title = "User Interface",
                onBackClick = { navigator.navigateUp() }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val scrollState = rememberScrollState()
        Box(modifier = Modifier.padding(top = padding.calculateTopPadding()).fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + navBarBottom),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {


                com.coolappstore.everdialer.by.svhp.view.components.SettingsSearchEntryPoint(navigator = navigator)

                // ── App Theme ────────────────────────────────────────
                    RivoAnimatedSection(delayMs = 0L) {
                        Column {
                            Text("App Theme", style = MaterialTheme.typography.labelLarge,
                                color = rivoHeadingColor(),
                                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                            RivoExpressiveCard {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Color Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(Modifier.height(12.dp))
                                    themeOptions.chunked(3).forEach { rowItems ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                            rowItems.forEach { option ->
                                                val selected = themeMode == option.key
                                                Surface(
                                                    shape = RoundedCornerShape(50),
                                                    color = if (selected) MaterialTheme.colorScheme.primary
                                                            else MaterialTheme.colorScheme.surfaceContainerHighest,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(38.dp)
                                                        .then(
                                                            if (isInteractive) {
                                                                Modifier.clickable {
                                                                    themeMode = option.key
                                                                    prefs.setString(PreferenceManager.KEY_THEME_MODE, option.key)
                                                                    triggerRestartPrompt(scope, snackbarHostState, context)
                                                                }
                                                            } else Modifier
                                                        )
                                                ) {
                                                    val isPillBright = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.primary.toArgb()) > 0.40
                                                    val selectedTextColor = if (isPillBright) Color(0xFF1C1B1F) else Color.White
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(option.label, style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (selected) selectedTextColor
                                                                    else MaterialTheme.colorScheme.onSurface)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            RivoExpressiveCard {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "Display & Font Size",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        FilledTonalIconButton(
                                            onClick = {
                                                displayScale = 1.0f
                                                fontSizeScale = 1.0f
                                                prefs.setFloat(PreferenceManager.KEY_DISPLAY_SCALE, 1.0f)
                                                prefs.setFloat(PreferenceManager.KEY_CUSTOM_FONT_SIZE, 1.0f)
                                            },
                                            modifier = Modifier.size(28.dp),
                                            shape = CircleShape,
                                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        ) {
                                            Icon(
                                                Icons.Default.Refresh,
                                                contentDescription = "Reset scale",
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(14.dp))

                                    // Display Scaling Slider
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "Display Scaling",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "${(displayScale * 100).roundToInt()}%",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Slider(
                                        value = displayScale,
                                        onValueChange = { displayScale = it },
                                        onValueChangeFinished = {
                                            prefs.setFloat(PreferenceManager.KEY_DISPLAY_SCALE, displayScale)
                                        },
                                        valueRange = 0.70f..1.40f,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    // Font Size Slider
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "Font Size",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "${(fontSizeScale * 100).roundToInt()}%",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Slider(
                                        value = fontSizeScale,
                                        onValueChange = { fontSizeScale = it },
                                        onValueChangeFinished = {
                                            prefs.setFloat(PreferenceManager.KEY_CUSTOM_FONT_SIZE, fontSizeScale)
                                        },
                                        valueRange = 0.70f..1.40f,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                // ── Theme Colors ──────────────────────────────────────
                    RivoAnimatedSection(delayMs = 60L) {
                        Column {
                            Text("Theme Colors", style = MaterialTheme.typography.labelLarge,
                                color = rivoHeadingColor(),
                                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                            RivoExpressiveCard {
                                RivoSwitchListItem(
                                    headline = "Dynamic Colors",
                                    supporting = "Wallpaper based app color theming",
                                    leadingIcon = Icons.Outlined.AutoAwesome,
                                    iconContainerColor = Color(0xFFE91E63),
                                    checked = dynamicColors,
                                    modifier = Modifier.settingsSearchHighlight("dynamic_colors", highlightedKey) { highlightedKey = null },
                                    onCheckedChange = {
                                        dynamicColors = it
                                        prefs.setBoolean(PreferenceManager.KEY_DYNAMIC_COLORS, it)
                                        if (!it && prefs.getInt("custom_primary_color", 0) == 0) {
                                            prefs.setInt("custom_primary_color", customPrimaryColor)
                                        }
                                        triggerRestartPrompt(scope, snackbarHostState, context)
                                    }
                                )
                                AnimatedVisibility(
                                    visible = !dynamicColors,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column {
                                        HorizontalDivider(
                                            Modifier.padding(horizontal = 16.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text("Primary Color", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                            Spacer(Modifier.height(12.dp))
                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                items(presetColors) { color ->
                                                    Box(
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .clip(CircleShape)
                                                            .background(color)
                                                            .border(
                                                                width = if (customPrimaryColor == color.toArgb()) 3.dp else 0.dp,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                shape = CircleShape
                                                            )
                                                            .then(
                                                                if (isInteractive) {
                                                                    Modifier.clickable {
                                                                        customPrimaryColor = color.toArgb()
                                                                        prefs.setInt("custom_primary_color", color.toArgb())
                                                                        hexInput = String.format("%06X", 0xFFFFFF and color.toArgb())
                                                                        hexError = false
                                                                        triggerRestartPrompt(scope, snackbarHostState, context)
                                                                    }
                                                                } else Modifier
                                                            )
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.height(12.dp))
                                            OutlinedButton(
                                                onClick = { showFloatingColorPicker = true },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(50.dp)
                                            ) {
                                                Icon(
                                                    Icons.Outlined.Colorize,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text("Pick Custom Color (Interactive)")
                                                Spacer(Modifier.weight(1f))
                                                Box(
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(customPrimaryColor))
                                                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                                )
                                            }
                                            Spacer(Modifier.height(12.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Box(
                                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(
                                                        try { Color(android.graphics.Color.parseColor("#${hexInput.trimStart('#')}")) }
                                                        catch (_: Exception) { Color.Gray }
                                                    )
                                                )
                                                OutlinedTextField(
                                                    value = hexInput,
                                                    onValueChange = { v ->
                                                        hexInput = v.trimStart('#').uppercase().take(6)
                                                        hexError = false
                                                    },
                                                    label = { Text("Hex Color") },
                                                    prefix = { Text("#") },
                                                    isError = hexError,
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f),
                                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                                    keyboardActions = KeyboardActions(onDone = {
                                                        applyHexColor(hexInput)
                                                        keyboardController?.hide()
                                                    }),
                                                    shape = RoundedCornerShape(12.dp),
                                                    supportingText = if (hexError) {{ Text("Enter a valid 6-digit hex code") }} else null
                                                )
                                                Button(onClick = {
                                                    applyHexColor(hexInput)
                                                    keyboardController?.hide()
                                                }, shape = RoundedCornerShape(12.dp)) {
                                                    Text("Apply")
                                                }
                                            }
                                        }
                                    }
                                }
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                RivoSwitchListItem(
                                    headline = "Saturated Colors",
                                    supporting = "Apply rich saturated colors behind containers",
                                    leadingIcon = Icons.Outlined.InvertColors,
                                    iconContainerColor = Color(0xFFFF9800),
                                    checked = saturatedColors,
                                    modifier = Modifier.settingsSearchHighlight("saturated_colors", highlightedKey) { highlightedKey = null },
                                    onCheckedChange = {
                                        saturatedColors = it
                                        prefs.setBoolean(PreferenceManager.KEY_SATURATED_COLORS, it)
                                        val newDarkStyle = if (it) PreferenceManager.SOLID_ICONS_STYLE_BRIGHT else PreferenceManager.SOLID_ICONS_STYLE_DIM
                                        val newLightStyle = PreferenceManager.SOLID_ICONS_STYLE_DIM
                                        solidIconsDarkStyle = newDarkStyle
                                        solidIconsLightStyle = newLightStyle
                                        prefs.setString(PreferenceManager.KEY_SOLID_ICONS_DARK, newDarkStyle)
                                        prefs.setString(PreferenceManager.KEY_SOLID_ICONS_LIGHT, newLightStyle)
                                        triggerRestartPrompt(scope, snackbarHostState, context)
                                    }
                                )
                                AnimatedVisibility(
                                    visible = saturatedColors,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(50.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceEvenly,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val targetsList = listOf(
                                                    "Container" to PreferenceManager.SATURATED_TARGET_CONTAINER,
                                                    "Background" to PreferenceManager.SATURATED_TARGET_BACKGROUND
                                                )
                                                val isPrimaryBright = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.primary.toArgb()) > 0.45
                                                val selectedFg = if (isPrimaryBright) Color(0xFF1C1B1F) else Color.White
                                                targetsList.forEach { (label, key) ->
                                                    val isSelected = saturatedTarget == key
                                                    FilterChip(
                                                        selected = isSelected,
                                                        onClick = {
                                                            if (saturatedTarget != key) {
                                                                saturatedTarget = key
                                                                prefs.setSaturatedTarget(key)
                                                            }
                                                        },
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .padding(horizontal = 2.dp),
                                                        label = {
                                                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                                Text(
                                                                    label,
                                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                                )
                                                            }
                                                        },
                                                        shape = RoundedCornerShape(50.dp),
                                                        border = null,
                                                        colors = FilterChipDefaults.filterChipColors(
                                                            containerColor = Color.Transparent,
                                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            selectedLabelColor = selectedFg
                                                        )
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Surface(
                                            shape = RoundedCornerShape(50.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceEvenly,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val modesList = listOf("Light" to "light", "Dark" to "dark", "White" to "white", "Black" to "black")
                                                val isPrimaryBright = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.primary.toArgb()) > 0.45
                                                val selectedFg = if (isPrimaryBright) Color(0xFF1C1B1F) else Color.White
                                                modesList.forEach { (label, key) ->
                                                    val isSelected = saturatedModes.contains(key)
                                                    FilterChip(
                                                        selected = isSelected,
                                                        onClick = {
                                                            val nextModes = if (isSelected) {
                                                                saturatedModes - key
                                                            } else {
                                                                saturatedModes + key
                                                            }
                                                            saturatedModes = nextModes
                                                            prefs.setString(PreferenceManager.KEY_SATURATED_MODES, nextModes.joinToString(","))
                                                        },
                                                        label = {
                                                            Text(
                                                                label,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                            )
                                                        },
                                                        shape = RoundedCornerShape(50.dp),
                                                        border = null,
                                                        colors = FilterChipDefaults.filterChipColors(
                                                            containerColor = Color.Transparent,
                                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            selectedLabelColor = selectedFg
                                                        )
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.LightMode,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Text(
                                                        "Light Mode Intensity",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                                Text(
                                                    "${(saturationLevelLight * 100).toInt()}%",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Slider(
                                                value = saturationLevelLight,
                                                onValueChange = {
                                                    saturationLevelLight = it
                                                    prefs.setFloat(PreferenceManager.KEY_SATURATION_LEVEL_LIGHT, it)
                                                },
                                                valueRange = 0.2f..2.0f,
                                                steps = 17,
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                                thickness = 0.5.dp
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.DarkMode,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Text(
                                                        "Dark Mode Intensity",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                                Text(
                                                    "${(saturationLevelDark * 100).toInt()}%",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Slider(
                                                value = saturationLevelDark,
                                                onValueChange = {
                                                    saturationLevelDark = it
                                                    prefs.setFloat(PreferenceManager.KEY_SATURATION_LEVEL_DARK, it)
                                                },
                                                valueRange = 0.2f..2.0f,
                                                steps = 17,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                RivoSwitchListItem(
                                    headline = "Solid Icons",
                                    supporting = "Use solid colorful backgrounds behind icons",
                                    leadingIcon = Icons.Outlined.Category,
                                    iconContainerColor = Color(0xFF009688),
                                    checked = solidIcons,
                                    modifier = Modifier.settingsSearchHighlight("solid_icons", highlightedKey) { highlightedKey = null },
                                    onCheckedChange = { solidIcons = it; prefs.setBoolean(PreferenceManager.KEY_SOLID_ICONS, it) }
                                )
                                AnimatedVisibility(
                                    visible = solidIcons,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 6.dp)
                                    ) {
                                        RivoCheckboxListItem(
                                            headline = "Dynamic Colored Icons",
                                            supporting = "Tint all solid icon backgrounds with dynamic primary/theme colors",
                                            checked = solidIconsDynamic,
                                            onCheckedChange = { isChecked ->
                                                solidIconsDynamic = isChecked
                                                prefs.setBoolean(PreferenceManager.KEY_SOLID_ICONS_DYNAMIC, isChecked)
                                            }
                                        )
                                        AnimatedVisibility(
                                            visible = solidIconsDynamic,
                                            enter = expandVertically() + fadeIn(),
                                            exit = shrinkVertically() + fadeOut()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
                                                val isSaturatedActive = remember(rateReviewToggleSettingsVersion, isDark) { prefs.isSaturatedForTheme(isDark) }
                                                val solidChipColors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = if (isSaturatedActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                                                    selectedLabelColor = if (isSaturatedActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                                    selectedLeadingIconColor = if (isSaturatedActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                FilterChip(
                                                    selected = true,
                                                    onClick = { showSolidIconsLightDialog = true },
                                                    colors = solidChipColors,
                                                    label = {
                                                        Text("Light: ${if (solidIconsLightStyle == "bright") "Bright" else "Dim"}")
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                             Icons.Outlined.LightMode,
                                                             contentDescription = null,
                                                             modifier = Modifier.size(18.dp)
                                                        )
                                                    },
                                                    shape = RoundedCornerShape(50.dp),
                                                    border = null
                                                )
                                                FilterChip(
                                                    selected = true,
                                                    onClick = { showSolidIconsDarkDialog = true },
                                                    colors = solidChipColors,
                                                    label = {
                                                        Text("Dark: ${if (solidIconsDarkStyle == "bright") "Bright" else "Dim"}")
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                             Icons.Outlined.DarkMode,
                                                             contentDescription = null,
                                                             modifier = Modifier.size(18.dp)
                                                        )
                                                    },
                                                    shape = RoundedCornerShape(50.dp),
                                                    border = null
                                                )
                                            }
                                        }
                                    }
                                }
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                RivoSwitchListItem(
                                    headline = "Circle Icons",
                                    supporting = "Use circle shapes for icons across the app",
                                    leadingIcon = Icons.Outlined.Lens,
                                    iconContainerColor = Color(0xFF00BCD4),
                                    checked = circleIcons,
                                    modifier = Modifier.settingsSearchHighlight("circle_icons", highlightedKey) { highlightedKey = null },
                                    onCheckedChange = { circleIcons = it; prefs.setBoolean(PreferenceManager.KEY_CIRCLE_ICONS, it) }
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                RivoSwitchListItem(
                                    headline = "Use Colorful Avatars",
                                    supporting = "Random colors based on contact name",
                                    leadingIcon = Icons.Outlined.AccountCircle,
                                    iconContainerColor = ColorBlue,
                                    checked = colorfulAvatars,
                                    modifier = Modifier.settingsSearchHighlight("colorful_avatars", highlightedKey) { highlightedKey = null },
                                    onCheckedChange = { colorfulAvatars = it; prefs.setBoolean(PreferenceManager.KEY_COLORFUL_AVATARS, it) }
                                )
                            }
                        }
                    }

                // ── Fonts ───────────────────────────────────────────────
                    RivoAnimatedSection(delayMs = 70L) {
                        Column {
                            Text("Fonts", style = MaterialTheme.typography.labelLarge,
                                color = rivoHeadingColor(),
                                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                            RivoExpressiveCard {
                                val currentFontName = AppFontHelper.getFontDisplayName(prefs.getString(PreferenceManager.KEY_CUSTOM_FONT_PATH, null))
                                RivoListItem(
                                    headline = "Fonts",
                                    supporting = currentFontName,
                                    leadingIcon = Icons.Outlined.TextFormat,
                                    iconContainerColor = ColorPurple,
                                    trailingIcon = Icons.Default.ChevronRight,
                                    onClick = { navigator.navigate(CustomFontScreenDestination()) }
                                )
                            }
                        }
                    }

                // ── Roundness ───────────────────────────────────────────
                    RivoAnimatedSection(delayMs = 75L) {
                        Column {
                            Text("Roundness", style = MaterialTheme.typography.labelLarge,
                                color = rivoHeadingColor(),
                                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                            RivoExpressiveCard {
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                                    val radiusInt = cornerRadius.roundToInt()
                                    val cornerLabel = when {
                                        radiusInt == 0 -> "0 dp (Sharp)"
                                        radiusInt >= 28 -> "$radiusInt dp (Curved)"
                                        else -> "$radiusInt dp (Rounded)"
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .then(if (isInteractive) Modifier.clickable { isRoundnessExpanded = !isRoundnessExpanded } else Modifier)
                                    ) {
                                        com.coolappstore.everdialer.by.svhp.view.components.RivoIconBox(
                                            icon = Icons.Outlined.RoundedCorner,
                                            iconContainerColor = ColorTeal
                                        )
                                        Spacer(Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("App Roundness", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                            Text(
                                                cornerLabel,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        FilledTonalIconButton(
                                            onClick = {
                                                cornerRadius = 28f
                                                prefs.setFloat(PreferenceManager.KEY_CORNER_RADIUS, 28f)
                                            },
                                            modifier = Modifier.size(28.dp),
                                            shape = CircleShape,
                                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        ) {
                                            Icon(
                                                Icons.Default.Refresh,
                                                contentDescription = "Reset roundness",
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { isRoundnessExpanded = !isRoundnessExpanded },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isRoundnessExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = if (isRoundnessExpanded) "Collapse" else "Expand",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    AnimatedVisibility(
                                        visible = isRoundnessExpanded,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 16.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    "Corner Radius",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    "$radiusInt dp",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                "Adjust sharpness or roundness across the whole app",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Slider(
                                                value = cornerRadius,
                                                onValueChange = {
                                                    cornerRadius = it
                                                    prefs.setFloat(PreferenceManager.KEY_CORNER_RADIUS, it)
                                                },
                                                onValueChangeFinished = {
                                                    prefs.setFloat(PreferenceManager.KEY_CORNER_RADIUS, cornerRadius)
                                                },
                                                valueRange = 0f..36f,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                // ── Liquid Glass ─────────────────────────────────────
                    RivoAnimatedSection(delayMs = 80L) {
                        Column {
                            Text("Visual Effects", style = MaterialTheme.typography.labelLarge,
                                color = rivoHeadingColor(),
                                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                                RivoExpressiveCard {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Lens,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                "Not supported on this device",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                "Blur and Liquid Glass require Android 12 or higher",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            } else {
                            RivoExpressiveCard {
                                RivoSwitchListItem(
                                    headline = "Material Liquid You Glass",
                                    supporting = "Apply a liquid glass refraction effect to navigation and menus",
                                    leadingIcon = Icons.Outlined.Lens,
                                    iconContainerColor = Color(0xFF00BCD4),
                                    checked = liquidGlass,
                                    modifier = Modifier.settingsSearchHighlight("liquid_glass_toggle", highlightedKey) { highlightedKey = null },
                                    onCheckedChange = {
                                        liquidGlass = it
                                        prefs.setBoolean(PreferenceManager.KEY_LIQUID_GLASS, it)
                                    }
                                )
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                RivoListItem(
                                    headline = "Elements to have liquid glass effect",
                                    supporting = "Choose which UI elements use the liquid glass effect",
                                    leadingIcon = Icons.Outlined.Layers,
                                    iconContainerColor = Color(0xFF0097A7),
                                    trailingIcon = Icons.Default.ChevronRight,
                                    modifier = Modifier.settingsSearchHighlight("liquid_glass_elements_link", highlightedKey) { highlightedKey = null },
                                    onClick = {
                                        navigator.navigate(com.ramcosta.composedestinations.generated.destinations.LiquidGlassElementsScreenDestination)
                                    }
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            RivoExpressiveCard {
                                RivoSwitchListItem(
                                    headline = "Material Blur Effects",
                                    supporting = "Apply a background blur effect to navigation and menus",
                                    leadingIcon = Icons.Outlined.BlurOn,
                                    iconContainerColor = Color(0xFF5C6BC0),
                                    checked = blurEffects,
                                    modifier = Modifier.settingsSearchHighlight("blur_effects_toggle", highlightedKey) { highlightedKey = null },
                                    onCheckedChange = {
                                        blurEffects = it
                                        prefs.setBoolean(PreferenceManager.KEY_BLUR_EFFECTS, it)
                                    }
                                )
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                RivoListItem(
                                    headline = "Elements to have blur effect",
                                    supporting = "Choose which UI elements use the blur effect",
                                    leadingIcon = Icons.Outlined.Layers,
                                    iconContainerColor = Color(0xFF3949AB),
                                    trailingIcon = Icons.Default.ChevronRight,
                                    modifier = Modifier.settingsSearchHighlight("blur_effects_elements_link", highlightedKey) { highlightedKey = null },
                                    onClick = {
                                        navigator.navigate(com.ramcosta.composedestinations.generated.destinations.BlurEffectsElementsScreenDestination)
                                    }
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            RivoExpressiveCard {
                                RivoListItem(
                                    headline = "Animation Style",
                                    supporting = if (animationStyle == PreferenceManager.ANIMATION_STYLE_WINDOWS_PHONE) "Windows Phone" else "Zoom (in/out)",
                                    leadingIcon = Icons.Outlined.Animation,
                                    iconContainerColor = ColorTeal,
                                    trailingIcon = Icons.Default.ChevronRight,
                                    modifier = Modifier.settingsSearchHighlight("animation_style", highlightedKey) { highlightedKey = null },
                                    onClick = { showAnimationStyleDialog = true }
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            RivoExpressiveCard {
                                RivoSwitchListItem(
                                    headline = "Motion Blur in Animation",
                                    supporting = "Apply dynamic motion blur during zoom page transitions",
                                    leadingIcon = Icons.Outlined.BlurOn,
                                    iconContainerColor = ColorPurple,
                                    checked = motionBlurAnimation,
                                    modifier = Modifier.settingsSearchHighlight("motion_blur_animation", highlightedKey) { highlightedKey = null },
                                    onCheckedChange = {
                                        motionBlurAnimation = it
                                        prefs.setBoolean(PreferenceManager.KEY_MOTION_BLUR_ANIMATION, it)
                                    }
                                )
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                RivoSwitchListItem(
                                    headline = "Scroll Animation",
                                    supporting = "Fade-in animation for list items as you scroll",
                                    leadingIcon = Icons.Outlined.Animation,
                                    iconContainerColor = ColorBlue,
                                    checked = scrollAnimation,
                                    modifier = Modifier.settingsSearchHighlight("scroll_animation", highlightedKey) { highlightedKey = null },
                                    onCheckedChange = {
                                        scrollAnimation = it
                                        prefs.setBoolean(PreferenceManager.KEY_SCROLL_ANIMATION, it)
                                    }
                                )
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                RivoSwitchListItem(
                                    headline = "Hangup Animation",
                                    supporting = if (hangupAnimation)
                                        "The call screen smoothly slides away when a call ends"
                                    else
                                        "The call screen closes immediately when a call ends, with no slide animation",
                                    leadingIcon = Icons.Default.CallEnd,
                                    iconContainerColor = Color(0xFFE53935),
                                    checked = hangupAnimation,
                                    modifier = Modifier.settingsSearchHighlight("hangup_animation", highlightedKey) { highlightedKey = null },
                                    onCheckedChange = {
                                        hangupAnimation = it
                                        prefs.setBoolean(PreferenceManager.KEY_HANGUP_ANIMATION, it)
                                    }
                                )
                            }
                            }
                        }
                    }


                // ── Call UI ───────────────────────────────────────────
                    RivoAnimatedSection(delayMs = 100L) {
                        Column {
                            Text("Call UI", style = MaterialTheme.typography.labelLarge,
                                color = rivoHeadingColor(),
                                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                            RivoExpressiveCard {
                                RivoListItem(
                                    headline = "Incoming Call UI",
                                    supporting = "Customize the incoming call screen appearance",
                                    leadingIcon = Icons.Outlined.CallReceived,
                                    iconContainerColor = ColorGreen,
                                    trailingIcon = Icons.Default.ChevronRight,
                                    modifier = Modifier.settingsSearchHighlight("incoming_call_ui_link", highlightedKey) { highlightedKey = null },
                                    onClick = { navigator.navigate(com.ramcosta.composedestinations.generated.destinations.IncomingCallUIScreenDestination()) }
                                )
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                // ── Ongoing Call UI → separate page ─────────────────────────
                                RivoListItem(
                                    headline = "Ongoing Call UI",
                                    supporting = "Customize the in-call screen layout and controls",
                                    leadingIcon = Icons.Outlined.Person,
                                    iconContainerColor = ColorBlue,
                                    trailingIcon = Icons.Default.ChevronRight,
                                    modifier = Modifier.settingsSearchHighlight("caller_ui_link", highlightedKey) { highlightedKey = null },
                                    onClick = { navigator.navigate(com.ramcosta.composedestinations.generated.destinations.CallerUIScreenDestination()) }
                                )

                            }
                        }
                    }

                // ── UI Element Visibility ────────────────────────────
                    RivoAnimatedSection(delayMs = 130L) {
                        Column {
                            Text("UI Elements", style = MaterialTheme.typography.labelLarge,
                                color = rivoHeadingColor(),
                                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                            RivoExpressiveCard {
                                RivoListItem(
                                    headline = "Call Logs",
                                    supporting = "Customize call log grouping, SIM icons, call counts, formatting and auto-deletion",
                                    leadingIcon = Icons.Outlined.Call,
                                    iconContainerColor = ColorGreen,
                                    trailingIcon = Icons.Default.ChevronRight,
                                    modifier = Modifier.settingsSearchHighlight("call_logs_settings_link", highlightedKey) { highlightedKey = null },
                                    onClick = { navigator.navigate(CallLogsSettingsScreenDestination()) }
                                )
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                RivoListItem(
                                    headline = "Dialpad",
                                    supporting = "Customize dialpad memory, search list call logs, and startup behavior",
                                    leadingIcon = Icons.Outlined.Dialpad,
                                    iconContainerColor = ColorBlue,
                                    trailingIcon = Icons.Default.ChevronRight,
                                    modifier = Modifier.settingsSearchHighlight("dialpad_settings_link", highlightedKey) { highlightedKey = null },
                                    onClick = { navigator.navigate(DialpadSettingsScreenDestination()) }
                                )
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                RivoListItem(
                                    headline = "Contact info",
                                    supporting = "Customize contact info display and duplicate numbers",
                                    leadingIcon = Icons.Outlined.Person,
                                    iconContainerColor = ColorPurple,
                                    trailingIcon = Icons.Default.ChevronRight,
                                    modifier = Modifier.settingsSearchHighlight("contact_info_settings_link", highlightedKey) { highlightedKey = null },
                                    onClick = { navigator.navigate(ContactInfoSettingsScreenDestination()) }
                                )
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                RivoListItem(
                                    headline = "Favourites",
                                    supporting = "Customize favourites layout and view mode",
                                    leadingIcon = Icons.Outlined.Star,
                                    iconContainerColor = Color(0xFFE91E63),
                                    trailingIcon = Icons.Default.ChevronRight,
                                    modifier = Modifier.settingsSearchHighlight("favourites_settings_link", highlightedKey) { highlightedKey = null },
                                    onClick = { navigator.navigate(FavouritesSettingsScreenDestination()) }
                                )
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                RivoListItem(
                                    headline = "Navigation",
                                    supporting = "Customize tab sections, default tab, and bottom bar appearance",
                                    leadingIcon = Icons.Default.ViewWeek,
                                    iconContainerColor = ColorIndigo,
                                    trailingIcon = Icons.Default.ChevronRight,
                                    modifier = Modifier.settingsSearchHighlight("navigation_settings_link", highlightedKey) { highlightedKey = null },
                                    onClick = { navigator.navigate(NavigationSettingsScreenDestination()) }
                                )
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                RivoListItem(
                                    headline = "Context Menu Elements",
                                    supporting = "Customize Favourites, Call Logs, and Contacts context menus",
                                    leadingIcon = Icons.Default.MoreVert,
                                    iconContainerColor = ColorPurple,
                                    trailingIcon = Icons.Default.ChevronRight,
                                     modifier = Modifier.settingsSearchHighlight("context_menu_elements", highlightedKey) { highlightedKey = null },
                                    onClick = { showContextMenuDialog = true }
                                )
                                if (!rateReviewSecretActive) {
                                    HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    RivoSwitchListItem(
                                        headline = "Hide Rate And Review",
                                        supporting = "Completely hides the Rate and Review section from Settings",
                                        leadingIcon = Icons.Outlined.VisibilityOff,
                                        iconContainerColor = ColorBlue,
                                        checked = hideRateAndReview,
                                        modifier = Modifier.settingsSearchHighlight("hide_rate_and_review", highlightedKey) { highlightedKey = null },
                                        onCheckedChange = {
                                            hideRateAndReview = it
                                            prefs.setBoolean(PreferenceManager.KEY_HIDE_RATE_AND_REVIEW, it)
                                        }
                                    )
                                }
                            }
                        }
                    }

                // ── Avatars ──────────────────────────────────────────
                    RivoAnimatedSection(delayMs = 160L) {
                        Column {
                            Text("Avatars", style = MaterialTheme.typography.labelLarge,
                                color = rivoHeadingColor(),
                                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                            RivoExpressiveCard {
                                RivoSwitchListItem(
                                    headline = "Show Avatars in Calls",
                                    supporting = "Displays avatars in call history and logs",
                                    leadingIcon = Icons.Outlined.Call,
                                    iconContainerColor = ColorIndigo,
                                    checked = showAvatarsInCalls,
                                    modifier = Modifier.settingsSearchHighlight("avatar_calls", highlightedKey) { highlightedKey = null },
                                    onCheckedChange = { showAvatarsInCalls = it; prefs.setBoolean(PreferenceManager.KEY_SHOW_AVATARS_CALLS, it) }
                                )
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                RivoSwitchListItem(
                                    headline = "Show Avatars in Contacts",
                                    supporting = "Displays avatars in contacts list",
                                    leadingIcon = Icons.Outlined.Person,
                                    iconContainerColor = ColorTeal,
                                    checked = showAvatarsInContacts,
                                    modifier = Modifier.settingsSearchHighlight("avatar_contacts", highlightedKey) { highlightedKey = null },
                                    onCheckedChange = { showAvatarsInContacts = it; prefs.setBoolean(PreferenceManager.KEY_SHOW_AVATARS_CONTACTS, it) }
                                )
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                RivoSwitchListItem(
                                    headline = "Show Picture in Avatar",
                                    supporting = "Shows the contact picture if available",
                                    leadingIcon = Icons.Outlined.AccountCircle,
                                    iconContainerColor = ColorGreen,
                                    checked = showPicture,
                                    modifier = Modifier.settingsSearchHighlight("avatar_picture", highlightedKey) { highlightedKey = null },
                                    onCheckedChange = { showPicture = it; prefs.setBoolean(PreferenceManager.KEY_SHOW_PICTURE, it) }
                                )
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                RivoSwitchListItem(
                                    headline = "Show First Letter in Avatar",
                                    supporting = "Displays letter when picture is missing",
                                    leadingIcon = Icons.Outlined.TextFields,
                                    iconContainerColor = ColorAmber,
                                    checked = showFirstLetter,
                                    modifier = Modifier.settingsSearchHighlight("avatar_first_letter", highlightedKey) { highlightedKey = null },
                                    onCheckedChange = { showFirstLetter = it; prefs.setBoolean(PreferenceManager.KEY_SHOW_FIRST_LETTER, it) }
                                )
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                RivoSwitchListItem(
                                    headline = "Show avatar in SMS",
                                    supporting = "Display contact avatar in SMS messages list",
                                    leadingIcon = Icons.Outlined.AccountCircle,
                                    iconContainerColor = ColorPurple,
                                    checked = showAvatarSms,
                                    modifier = Modifier.settingsSearchHighlight("sms_show_avatar", highlightedKey) { highlightedKey = null },
                                    onCheckedChange = {
                                        showAvatarSms = it
                                        prefs.setBoolean(PreferenceManager.KEY_SMS_SHOW_AVATAR, it)
                                    }
                                )
                            }
                        }
                    }

                // ── App ─────────────────────────────────────────
                    Column {
                        Text(
                            "App",
                            style = MaterialTheme.typography.labelLarge,
                            color = rivoHeadingColor(),
                            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                        )
                        RivoExpressiveCard {
                            RivoListItem(
                                headline = "App Icon (Change)",
                                supporting = "Choose the app icon displayed on your home screen",
                                leadingIcon = Icons.Outlined.Apps,
                                iconContainerColor = ColorIndigo,
                                modifier = Modifier.settingsSearchHighlight("app_icon_link", highlightedKey) { highlightedKey = null },
                                onClick = {
                                    navigator.navigate(com.ramcosta.composedestinations.generated.destinations.AppIconScreenDestination)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        RivoExpressiveCard {
                            RivoListItem(
                                headline = "App Name (Change)",
                                supporting = "Currently: " + (appNamePresets.firstOrNull { it.key == selectedAppNameKey }?.label ?: "Ever Dialer (Default)"),
                                leadingIcon = Icons.Outlined.Badge,
                                iconContainerColor = ColorTeal,
                                modifier = Modifier.settingsSearchHighlight("app_name_link", highlightedKey) { highlightedKey = null },
                                onClick = { showAppNameDialog = true }
                            )
                        }
                    }

                Spacer(modifier = Modifier.height(100.dp))
            }

            if (showAppNameDialog) {
                AlertDialog(
                    onDismissRequest = { showAppNameDialog = false },
                    title = { Text("App Name") },
                    text = {
                        Column {
                            appNamePresets.forEach { entry ->
                                val isSelected = selectedAppNameKey == entry.key
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedAppNameKey = entry.key
                                            prefs.setString(PreferenceManager.KEY_APP_NAME_PRESET, entry.key)
                                            applyAppNamePreset(context, prefs, entry)
                                            showAppNameDialog = false
                                        }
                                ) {
                                    RadioButton(selected = isSelected, onClick = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(entry.label, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showAppNameDialog = false }) { Text("Done") }
                    }
                )
            }

            if (showSolidIconsLightDialog) {
                AlertDialog(
                    onDismissRequest = { showSolidIconsLightDialog = false },
                    title = { Text("Solid Icons in Light Mode") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                PreferenceManager.SOLID_ICONS_STYLE_DIM to ("Dim (Default)" to "Use dimmed container color for icon backgrounds"),
                                PreferenceManager.SOLID_ICONS_STYLE_BRIGHT to ("Bright" to "Use vibrant primary color for icon backgrounds")
                            ).forEach { (styleKey, pair) ->
                                val (title, sub) = pair
                                val isSelected = solidIconsLightStyle == styleKey
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            solidIconsLightStyle = styleKey
                                            prefs.setString(PreferenceManager.KEY_SOLID_ICONS_LIGHT, styleKey)
                                            showSolidIconsLightDialog = false
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp)
                                ) {
                                    RadioButton(selected = isSelected, onClick = null)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                        Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSolidIconsLightDialog = false }) { Text("Done") }
                    }
                )
            }

            if (showSolidIconsDarkDialog) {
                AlertDialog(
                    onDismissRequest = { showSolidIconsDarkDialog = false },
                    title = { Text("Solid Icons in Dark Mode") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                PreferenceManager.SOLID_ICONS_STYLE_DIM to ("Dim (Default)" to "Use dimmed container color for icon backgrounds"),
                                PreferenceManager.SOLID_ICONS_STYLE_BRIGHT to ("Bright" to "Use vibrant dynamic primary color for icon backgrounds")
                            ).forEach { (styleKey, pair) ->
                                val (title, sub) = pair
                                val isSelected = solidIconsDarkStyle == styleKey
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            solidIconsDarkStyle = styleKey
                                            prefs.setString(PreferenceManager.KEY_SOLID_ICONS_DARK, styleKey)
                                            showSolidIconsDarkDialog = false
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp)
                                ) {
                                    RadioButton(selected = isSelected, onClick = null)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                        Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSolidIconsDarkDialog = false }) { Text("Done") }
                    }
                )
            }

            if (showFloatingColorPicker) {
                FloatingColorPickerDialog(
                    initialColor = Color(customPrimaryColor),
                    onDismiss = { showFloatingColorPicker = false },
                    onColorSelected = { selectedColor ->
                        showFloatingColorPicker = false
                        customPrimaryColor = selectedColor.toArgb()
                        prefs.setInt("custom_primary_color", selectedColor.toArgb())
                        hexInput = String.format("%06X", 0xFFFFFF and selectedColor.toArgb())
                        hexError = false
                        triggerRestartPrompt(scope, snackbarHostState, context)
                    }
                )
            }

        }
    }
}

@Composable
private fun FloatingColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    val hsv = remember(initialColor) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initialColor.toArgb(), it) }
    }
    var hue by remember(initialColor) { mutableFloatStateOf(hsv[0]) }
    var sat by remember(initialColor) { mutableFloatStateOf(hsv[1].coerceIn(0f, 1f)) }
    var value by remember(initialColor) { mutableFloatStateOf(hsv[2].coerceIn(0f, 1f)) }

    val currentColor = remember(hue, sat, value) {
        val rgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))
        Color(rgb)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(28.dp)),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Color Picker",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // Live Color Preview Pill
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .width(80.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(currentColor)
                            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                    )
                }

                Spacer(Modifier.height(16.dp))

                // 2D Saturation-Value Panel with Pointer Reticle
                Text(
                    text = "Shade & Brightness",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                sat = (down.position.x / size.width).coerceIn(0f, 1f)
                                value = (1f - down.position.y / size.height).coerceIn(0f, 1f)
                                down.consume()
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    if (!change.pressed) break
                                    sat = (change.position.x / size.width).coerceIn(0f, 1f)
                                    value = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                                    change.consume()
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // 1. Horizontal gradient: White to pure Hue color
                        val pureHueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.White, pureHueColor)
                            )
                        )
                        // 2. Vertical gradient: Transparent to Black
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black)
                            )
                        )

                        // 3. Draw Pointer Reticle
                        val pointerX = (sat * size.width).coerceIn(0f, size.width)
                        val pointerY = ((1f - value) * size.height).coerceIn(0f, size.height)
                        val pointerCenter = Offset(pointerX, pointerY)

                        // Outer ring (black) for contrast
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.6f),
                            radius = 13.dp.toPx(),
                            center = pointerCenter,
                            style = Stroke(width = 3.dp.toPx())
                        )
                        // Inner ring (white)
                        drawCircle(
                            color = Color.White,
                            radius = 10.dp.toPx(),
                            center = pointerCenter,
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                        // Center dot (current color)
                        drawCircle(
                            color = currentColor,
                            radius = 6.dp.toPx(),
                            center = pointerCenter
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Hue Slider with pointer
                Text(
                    text = "Hue Spectrum",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                hue = (down.position.x / size.width * 360f).coerceIn(0f, 360f)
                                down.consume()
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    if (!change.pressed) break
                                    hue = (change.position.x / size.width * 360f).coerceIn(0f, 360f)
                                    change.consume()
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val rainbow = listOf(
                            Color.Red,
                            Color.Yellow,
                            Color.Green,
                            Color.Cyan,
                            Color.Blue,
                            Color.Magenta,
                            Color.Red
                        )
                        drawRect(brush = Brush.horizontalGradient(rainbow))

                        // Pointer Thumb on Hue Bar
                        val thumbX = ((hue / 360f) * size.width).coerceIn(12.dp.toPx(), size.width - 12.dp.toPx())
                        val thumbCenter = Offset(thumbX, size.height / 2)

                        drawCircle(
                            color = Color.Black.copy(alpha = 0.5f),
                            radius = 14.dp.toPx(),
                            center = thumbCenter,
                            style = Stroke(width = 3.dp.toPx())
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 12.dp.toPx(),
                            center = thumbCenter,
                            style = Stroke(width = 3.dp.toPx())
                        )
                        val currentHueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                        drawCircle(
                            color = currentHueColor,
                            radius = 8.dp.toPx(),
                            center = thumbCenter
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Hex Code Display
                val hexStr = remember(currentColor) {
                    String.format("#%06X", 0xFFFFFF and currentColor.toArgb())
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Hex Code",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = hexStr,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onColorSelected(currentColor) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Select Color")
                    }
                }
            }
        }
    }
}
