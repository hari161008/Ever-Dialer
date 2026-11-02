package com.coolappstore.everdialer.by.svhp.view.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.tr
import org.koin.compose.koinInject

/**
 * Unified search bar component used across both the resting main screen (pill mode)
 * and the active search screen (editable mode).
 *
 * Sharing this single component guarantees identical pill shape, height (52.dp),
 * typography, theme colors, and seamless icon transitions without hardcoded discrepancies.
 */
@Composable
fun AppSearchBar(
    modifier: Modifier = Modifier,
    isEditable: Boolean = false,
    query: TextFieldValue = TextFieldValue(""),
    onQueryChange: (TextFieldValue) -> Unit = {},
    placeholderText: String = tr("Search in Ever Dialer"),
    onSearchBarClick: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    onClearClick: (() -> Unit)? = null,
    onVoiceClick: (() -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    onSearchAction: (() -> Unit)? = null,
    animProgress: Float = 1f,
    interactionSource: MutableInteractionSource? = null,
    onGloballyPositioned: ((LayoutCoordinates) -> Unit)? = null
) {
    val prefs = koinInject<PreferenceManager>()
    val settingsVer by prefs.settingsChanged.collectAsState()
    val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
    val isSaturatedActive = remember(settingsVer, isDark) { prefs.isSaturatedForTheme(isDark) }
    val isSaturatedContainer = remember(settingsVer) { prefs.getSaturatedTarget() == PreferenceManager.SATURATED_TARGET_CONTAINER }
    val isBackgroundTarget = remember(settingsVer) { prefs.getSaturatedTarget() == PreferenceManager.SATURATED_TARGET_BACKGROUND }

    val searchBarBg = if (isSaturatedActive && isSaturatedContainer) {
        MaterialTheme.colorScheme.primary
    } else if (isSaturatedActive && isBackgroundTarget) {
        if (isDark) Color.Black else Color.White
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val searchBarFg = if (isSaturatedActive && isSaturatedContainer) {
        if (isDark) Color.Black else MaterialTheme.colorScheme.onPrimary
    } else if (isSaturatedActive && isBackgroundTarget) {
        if (isDark) Color.White else Color(0xFF111827)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val searchBarPlaceholder = if (isSaturatedActive && isSaturatedContainer) {
        (if (isDark) Color.Black else MaterialTheme.colorScheme.onPrimary).copy(alpha = 0.7f)
    } else if (isSaturatedActive && isBackgroundTarget) {
        (if (isDark) Color.White else Color(0xFF111827)).copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val defaultSource = remember { MutableInteractionSource() }
    val activeSource = interactionSource ?: defaultSource
    val searchPressed by activeSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (!isEditable && searchPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "appSearchBarPressScale"
    )

    Surface(
        onClick = { onSearchBarClick?.invoke() },
        enabled = (!isEditable && onSearchBarClick != null),
        modifier = modifier
            .height(52.dp)
            .scale(pressScale)
            .then(
                if (onGloballyPositioned != null) Modifier.onGloballyPositioned(onGloballyPositioned)
                else Modifier
            ),
        shape = CircleShape,
        color = searchBarBg,
        shadowElevation = 0.dp,
        interactionSource = activeSource
    ) {
        if (!isEditable) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = searchBarFg
                )
                Text(
                    text = placeholderText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = searchBarFg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (onVoiceClick != null) {
                    IconButton(onClick = onVoiceClick) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Search",
                            tint = searchBarFg
                        )
                    }
                }
            }
        } else {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = searchBarFg,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                cursorBrush = SolidColor(searchBarFg),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { onSearchAction?.invoke() }
                ),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 4.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onBackClick?.invoke() }) {
                            val iconRotation = lerp(-90f, 0f, animProgress)
                            Icon(
                                imageVector = if (animProgress < 0.35f) Icons.Default.Search else Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = searchBarFg,
                                modifier = Modifier.graphicsLayer { rotationZ = iconRotation }
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (query.text.isEmpty()) {
                                Text(
                                    text = if (animProgress < 0.35f) placeholderText else tr("Search in Ever Dialer"),
                                    color = searchBarPlaceholder,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                        if (query.text.isNotEmpty() && onClearClick != null) {
                            IconButton(onClick = onClearClick) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = searchBarFg)
                            }
                        }
                        if (onVoiceClick != null) {
                            IconButton(onClick = onVoiceClick) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice Search", tint = searchBarFg)
                            }
                        }
                    }
                }
            )
        }
    }
}
