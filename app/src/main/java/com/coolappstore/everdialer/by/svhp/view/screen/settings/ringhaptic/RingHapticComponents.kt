package com.coolappstore.everdialer.by.svhp.view.screen.settings.ringhaptic

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.coolappstore.everdialer.by.svhp.R
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.haptics.CustomHapticSequence
import com.coolappstore.everdialer.by.svhp.haptics.HapticPattern
import com.coolappstore.everdialer.by.svhp.haptics.RingHapticManager
import com.coolappstore.everdialer.by.svhp.view.components.performScrollHaptic
import com.coolappstore.everdialer.by.svhp.view.theme.expressive.ExpressiveShapeHolder
import com.coolappstore.everdialer.by.svhp.view.theme.expressive.PetalButton
import com.coolappstore.everdialer.by.svhp.view.theme.expressive.PetalIcons
import com.coolappstore.everdialer.by.svhp.view.theme.expressive.PetalMaterialShapes
import kotlin.math.roundToInt

const val SliderTickStepsDefault = 19

fun slider01ToTickIndex(value: Float, steps: Int = SliderTickStepsDefault): Int =
    (value.coerceIn(0f, 1f) * (steps + 1)).roundToInt().coerceIn(0, steps + 1)

object PetalContainmentDefaults {
    val shape = RoundedCornerShape(28.dp)
    val groupedMiddleShape = RoundedCornerShape(8.dp)
}

enum class PetalGroupPosition { SINGLE, TOP, MIDDLE, BOTTOM }

fun petalGroupShape(position: PetalGroupPosition): RoundedCornerShape = when (position) {
    PetalGroupPosition.SINGLE -> RoundedCornerShape(28.dp)
    PetalGroupPosition.TOP -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
    PetalGroupPosition.MIDDLE -> RoundedCornerShape(6.dp)
    PetalGroupPosition.BOTTOM -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
}

@Composable
fun petalGroupColor(): Color {
    val scheme = MaterialTheme.colorScheme
    return if (scheme.background.luminance() > 0.5f) scheme.surfaceContainerLow else scheme.surfaceContainerHigh
}

class PetalGroupScope {
    internal val rows = mutableListOf<@Composable (PetalGroupPosition) -> Unit>()
    fun row(content: @Composable (PetalGroupPosition) -> Unit) {
        rows.add(content)
    }
}

@Composable
fun PetalGroup(
    modifier: Modifier = Modifier,
    content: PetalGroupScope.() -> Unit,
) {
    val scope = remember(content) { PetalGroupScope().apply(content) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        scope.rows.forEachIndexed { index, rowContent ->
            val pos = when {
                scope.rows.size <= 1 -> PetalGroupPosition.SINGLE
                index == 0 -> PetalGroupPosition.TOP
                index == scope.rows.lastIndex -> PetalGroupPosition.BOTTOM
                else -> PetalGroupPosition.MIDDLE
            }
            rowContent(pos)
        }
    }
}

@Composable
fun PetalGroupToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    position: PetalGroupPosition = PetalGroupPosition.SINGLE,
    badgeShape: ExpressiveShapeHolder = PetalMaterialShapes.Cookie9Sided,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.98f else 1f, spring(stiffness = 400f), label = "rowScale")
    Surface(
        onClick = { if (enabled) onCheckedChange(!checked) },
        enabled = enabled,
        shape = petalGroupShape(position),
        color = petalGroupColor(),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.45f
            },
        interactionSource = source,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(badgeShape.toShape())
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
    }
}

@Composable
fun PetalContentRow(
    position: PetalGroupPosition = PetalGroupPosition.MIDDLE,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = petalGroupShape(position),
        color = petalGroupColor(),
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = if (enabled) 1f else 0.45f }) {
            content()
        }
    }
}

@Composable
fun PetalTestRow(
    label: String,
    onClick: () -> Unit,
    position: PetalGroupPosition = PetalGroupPosition.BOTTOM,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = petalGroupShape(position),
        color = petalGroupColor(),
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            PetalButton(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(label, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun PhonePermissionCard(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    fun isPhonePermGranted(): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED

    var isGranted by remember { mutableStateOf(isPhonePermGranted()) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        isGranted = granted
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) isGranted = isPhonePermGranted()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    if (isGranted) return

    PermissionCard(
        icon = Icons.Rounded.PhoneDisabled,
        title = "Phone permission required",
        body = "Grant phone state access so Ever Dialer can detect incoming calls accurately.",
        buttonLabel = "Grant",
        isError = false,
        onClick = { launcher.launch(Manifest.permission.READ_PHONE_STATE) },
        modifier = modifier,
    )
}

@Composable
fun PermissionCard(
    icon: ImageVector,
    title: String,
    body: String,
    buttonLabel: String,
    isError: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
    val fg = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
    val btnBg = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
    val btnFg = if (isError) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSecondary

    Surface(
        color = bg,
        shape = PetalContainmentDefaults.shape,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = fg, fontWeight = FontWeight.Bold)
                Text(body, style = MaterialTheme.typography.bodySmall, color = fg)
            }
            PetalButton(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = btnBg),
            ) {
                Text(buttonLabel, color = btnFg)
            }
        }
    }
}

@Composable
fun SystemHapticsNoticeCard(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val prefs = remember { PreferenceManager(ctx) }
    var forceTouch by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_RING_HAPTIC_FORCE_TOUCH_ROUTING, false))
    }
    var isSystemRingOn by remember {
        mutableStateOf(RingHapticManager.isSystemRingVibrationEnabled(ctx))
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isSystemRingOn = RingHapticManager.isSystemRingVibrationEnabled(ctx)
                forceTouch = prefs.getBoolean(PreferenceManager.KEY_RING_HAPTIC_FORCE_TOUCH_ROUTING, false)
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val isWorkaroundActive = forceTouch || !isSystemRingOn

    Surface(
        color = if (isWorkaroundActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
        shape = PetalContainmentDefaults.shape,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Icon(
                    if (isWorkaroundActive) PetalIcons.Check else PetalIcons.Info,
                    contentDescription = null,
                    tint = if (isWorkaroundActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(20.dp).padding(top = 2.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    Text(
                        if (isWorkaroundActive) "Touch Engine Routing Active" else "Turn off system vibration for calls",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isWorkaroundActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (isWorkaroundActive)
                            "System call vibration is disabled. Ever Dialer is actively routing ring haptics through your device's touch feedback engine, so custom patterns and beat sync vibrate smoothly."
                        else
                            "For best results, disable your system's default vibration for incoming calls. Otherwise system and Ever Dialer patterns may overlap.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isWorkaroundActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
            PetalButton(
                onClick = {
                    val intent = Intent(AndroidSettings.ACTION_SOUND_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                    try {
                        ctx.startActivity(intent)
                    } catch (_: Exception) {
                        ctx.startActivity(Intent(AndroidSettings.ACTION_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isWorkaroundActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                    contentColor = if (isWorkaroundActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(PetalIcons.Vibration, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(8.dp))
                Text(if (isWorkaroundActive) "System Sound Settings" else "Open Vibration Settings", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun PatternSelectorWithCustom(
    pattern: HapticPattern,
    intensity: Float,
    customSequence: CustomHapticSequence,
    isAutoActive: Boolean = false,
    onPatternSelected: (HapticPattern) -> Unit,
    onIntensityCommit: (Float) -> Unit,
    onOpenCustomEditor: () -> Unit,
    onOpenAutoSettings: () -> Unit = {},
    onClearCustom: () -> Unit = {},
    onDisableAuto: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isCustomActive = !customSequence.isEmpty
    val patterns = remember { HapticPattern.entries }
    Column(modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            patterns.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowItems.forEach { p ->
                        PatternCard(
                            pattern = p,
                            isSelected = !isCustomActive && !isAutoActive && p == pattern,
                            onClick = { onPatternSelected(p); onClearCustom(); onDisableAuto() },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                    if (rowItems.size == 1) Box(Modifier.weight(1f).fillMaxHeight())
                }
            }
            AutoPatternCard(
                isSelected = isAutoActive,
                onClick = { onOpenAutoSettings(); onClearCustom() },
                modifier = Modifier.fillMaxWidth(),
            )
            CustomPatternCard(
                isSelected = isCustomActive && !isAutoActive,
                beatCount = customSequence.beats.size,
                durationMs = customSequence.durationMs,
                onClick = { onOpenCustomEditor(); onDisableAuto() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(4.dp))
        IntensitySlider(intensity = intensity, onIntensityCommit = onIntensityCommit)
    }
}

@Composable
private fun PatternCard(
    pattern: HapticPattern,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        spring(stiffness = 300f),
        label = "pc"
    )
    val titleColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val descColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
    val stateDesc = stringResource(if (isSelected) R.string.pattern_selected else R.string.pattern_not_selected)
    val interactionSource = remember { MutableInteractionSource() }
    val corner by animateDpAsState(if (isSelected) 32.dp else 20.dp, spring(dampingRatio = 0.6f, stiffness = 380f), label = "pattern_corner")

    Surface(
        modifier = modifier
            .heightIn(min = 132.dp)
            .selectable(
                selected = isSelected,
                onClick = onClick,
                role = Role.RadioButton,
                interactionSource = interactionSource,
                indication = ripple(bounded = true)
            )
            .semantics { this.stateDescription = stateDesc },
        color = containerColor,
        shape = RoundedCornerShape(corner),
    ) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = PetalMaterialShapes.Cookie6Sided.toShape()),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        pattern.icon, null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
                SelectionDot(isSelected)
            }
            Spacer(Modifier.height(2.dp))
            Text(stringResource(pattern.labelRes), style = MaterialTheme.typography.titleMedium, color = titleColor)
            Text(stringResource(pattern.descriptionRes), style = MaterialTheme.typography.bodySmall, color = descColor)
        }
    }
}

@Composable
private fun AutoPatternCard(isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val containerColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        spring(stiffness = 300f),
        label = "ac"
    )
    val interactionSource = remember { MutableInteractionSource() }
    val corner by animateDpAsState(if (isSelected) 32.dp else 20.dp, spring(dampingRatio = 0.6f, stiffness = 380f), label = "pattern_corner")

    Surface(
        modifier = modifier.selectable(
            selected = isSelected,
            onClick = onClick,
            role = Role.RadioButton,
            interactionSource = interactionSource,
            indication = ripple(bounded = true)
        ),
        color = containerColor,
        shape = RoundedCornerShape(corner),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = PetalMaterialShapes.Cookie6Sided.toShape()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.GraphicEq, null,
                    tint = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Auto", style = MaterialTheme.typography.titleMedium, color = if (isSelected) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Text("Vibrate in sync with ringtone audio beats in real time", style = MaterialTheme.typography.bodySmall, color = if (isSelected) MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun CustomPatternCard(isSelected: Boolean, beatCount: Int, durationMs: Long, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val containerColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        spring(stiffness = 300f),
        label = "cc"
    )
    val interactionSource = remember { MutableInteractionSource() }
    val corner by animateDpAsState(if (isSelected) 32.dp else 20.dp, spring(dampingRatio = 0.6f, stiffness = 380f), label = "pattern_corner")

    Surface(
        modifier = modifier.selectable(
            selected = isSelected,
            onClick = onClick,
            role = Role.RadioButton,
            interactionSource = interactionSource,
            indication = ripple(bounded = true)
        ),
        color = containerColor,
        shape = RoundedCornerShape(corner),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = PetalMaterialShapes.Cookie6Sided.toShape()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Draw, null,
                    tint = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Custom", style = MaterialTheme.typography.titleMedium, color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface)
                Text(if (isSelected && beatCount > 0) "$beatCount beats · ${durationMs}ms" else "Record your own haptic rhythm", style = MaterialTheme.typography.bodySmall, color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SelectionDot(isSelected: Boolean) {
    val scale by animateFloatAsState(if (isSelected) 1f else 0f, spring(stiffness = 600f), label = "sd")
    Box(
        modifier = Modifier.size(22.dp).scale(scale).background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) Icon(PetalIcons.Check, stringResource(R.string.pattern_selected), tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun IntensitySlider(intensity: Float, onIntensityCommit: (Float) -> Unit) {
    val ctx = LocalContext.current
    var draft by remember(intensity) { mutableFloatStateOf(intensity) }
    var lastTick by remember(intensity) { mutableIntStateOf(slider01ToTickIndex(intensity)) }

    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Intensity", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                Text(
                    "${(draft * 100f).roundToInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
        PetalSlider(
            value = draft,
            onValueChange = {
                draft = it
                val t = slider01ToTickIndex(it)
                if (t != lastTick) {
                    lastTick = t
                    performScrollHaptic(ctx, 40)
                }
            },
            onValueChangeFinished = { onIntensityCommit(draft) },
            valueRange = 0f..1f,
            steps = SliderTickStepsDefault,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
        )
    }
}

private val TrackHeight = 46.dp
private val ThumbSize = 30.dp
private val ThumbInset = 8.dp

@Composable
fun PetalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors? = null,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        enabled = enabled,
        onValueChangeFinished = onValueChangeFinished,
        modifier = modifier.height(TrackHeight),
        thumb = { Spacer(Modifier.size(0.dp)) },
        track = { state ->
            val rangeSpan = state.valueRange.endInclusive - state.valueRange.start
            val target = if (rangeSpan > 0f) {
                ((state.value - state.valueRange.start) / rangeSpan).coerceIn(0f, 1f)
            } else 0f

            val fraction by animateFloatAsState(
                targetValue = target,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = 900f
                ),
                label = "sliderFraction"
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TrackHeight)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                val travel = maxWidth - ThumbSize - ThumbInset * 2
                val thumbStart = ThumbInset + travel * fraction
                val circumference = ThumbSize.value * Math.PI.toFloat()
                val rollDegrees = (travel.value * fraction / circumference) * 360f

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(thumbStart + ThumbSize + ThumbInset)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (enabled) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = thumbStart)
                        .size(ThumbSize)
                        .graphicsLayer { rotationZ = rollDegrees }
                        .clip(PetalMaterialShapes.Cookie12Sided.toShape())
                        .background(
                            if (enabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainer
                        )
                )
            }
        }
    )
}
