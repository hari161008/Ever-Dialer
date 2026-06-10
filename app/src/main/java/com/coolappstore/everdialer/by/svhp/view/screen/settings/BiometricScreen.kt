package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import androidx.compose.ui.window.Dialog
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoListItem
import com.coolappstore.everdialer.by.svhp.view.components.RivoSwitchListItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun BiometricScreen(navigator: DestinationsNavigator) {
    val prefs: PreferenceManager = koinInject()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var biometricsType by remember { mutableStateOf(prefs.getString(PreferenceManager.KEY_BIOMETRICS_TYPE, "") ?: "") }
    var appLockEnabled by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_BIOMETRICS_APP_LOCK, false)) }
    var callLockEnabled by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_BIOMETRICS_CALL_LOCK, false)) }

    var showTypeSheet by remember { mutableStateOf(false) }
    var showPinSetup by remember { mutableStateOf(false) }
    var showPasswordSetup by remember { mutableStateOf(false) }
    var isClosing by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }

    val alpha by animateFloatAsState(
        targetValue = if (visible && !isClosing) 1f else 0f,
        animationSpec = if (isClosing) tween(260) else tween(320),
        label = "alpha"
    )
    val offsetY by animateDpAsState(
        targetValue = if (visible && !isClosing) 0.dp else if (isClosing) 40.dp else 24.dp,
        animationSpec = if (isClosing) tween(270) else spring(stiffness = Spring.StiffnessMediumLow),
        label = "offsetY"
    )
    LaunchedEffect(Unit) { visible = true }

    fun navigateBack() {
        isClosing = true
        scope.launch { delay(260); navigator.navigateUp() }
    }

    val systemBiometricsAvailable = remember {
        val bm = BiometricManager.from(context)
        bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    val typeLabel = when (biometricsType) {
        "system" -> "System Biometrics"
        "pin"    -> "Custom PIN"
        "password" -> "Custom Password"
        else     -> "Not Set"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Biometrics", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = ::navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .offset(y = offsetY)
                .alpha(alpha)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Authentication Method card ─────────────────────────────────
            SectionLabel("Authentication Method")
            RivoExpressiveCard {
                RivoListItem(
                    headline = "Biometric Method",
                    supporting = typeLabel,
                    leadingIcon = Icons.Default.Fingerprint,
                    iconContainerColor = Color(0xFF6750A4),
                    trailingIcon = Icons.Default.ChevronRight,
                    onClick = { showTypeSheet = true }
                )
            }

            // ── Toggles — only visible when a method is configured ─────────
            AnimatedVisibility(
                visible = biometricsType.isNotEmpty(),
                enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                exit  = fadeOut(tween(200)) + shrinkVertically(tween(200))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SectionLabel("Biometrics")
                    RivoExpressiveCard {
                        RivoSwitchListItem(
                            headline = "Lock App on Open",
                            supporting = "Require authentication when opening Ever Dialer",
                            leadingIcon = Icons.Default.LockOpen,
                            iconContainerColor = Color(0xFF2196F3),
                            checked = appLockEnabled,
                            onCheckedChange = {
                                appLockEnabled = it
                                prefs.setBoolean(PreferenceManager.KEY_BIOMETRICS_APP_LOCK, it)
                                
                            }
                        )
                        CardDivider()
                        RivoSwitchListItem(
                            headline = "Lock Call Actions",
                            supporting = "Require authentication to answer or reject incoming calls",
                            leadingIcon = Icons.Default.PhonePaused,
                            iconContainerColor = Color(0xFF4CAF50),
                            checked = callLockEnabled,
                            onCheckedChange = {
                                callLockEnabled = it
                                prefs.setBoolean(PreferenceManager.KEY_BIOMETRICS_CALL_LOCK, it)
                                
                            }
                        )
                    }
                }
            }
        }
    }

    // ── Type Chooser Bottom Sheet ──────────────────────────────────────────
    if (showTypeSheet) {
        BiometricTypeSheet(
            systemAvailable = systemBiometricsAvailable,
            currentType = biometricsType,
            onSelect = { type ->
                showTypeSheet = false
                when (type) {
                    "system" -> {
                        biometricsType = "system"
                        prefs.setString(PreferenceManager.KEY_BIOMETRICS_TYPE, "system")
                        if (!appLockEnabled) { appLockEnabled = true; prefs.setBoolean(PreferenceManager.KEY_BIOMETRICS_APP_LOCK, true) }
                    }
                    "pin"      -> showPinSetup = true
                    "password" -> showPasswordSetup = true
                    ""         -> {
                        biometricsType = ""
                        prefs.setString(PreferenceManager.KEY_BIOMETRICS_TYPE, "")
                        prefs.setString(PreferenceManager.KEY_BIOMETRICS_PIN, "")
                        prefs.setString(PreferenceManager.KEY_BIOMETRICS_PASSWORD, "")
                        prefs.setBoolean(PreferenceManager.KEY_BIOMETRICS_APP_LOCK, false)
                        prefs.setBoolean(PreferenceManager.KEY_BIOMETRICS_CALL_LOCK, false)
                        appLockEnabled = false; callLockEnabled = false
                    }
                }
            },
            onDismiss = { showTypeSheet = false }
        )
    }

    if (showPinSetup) {
        PinSetupDialog(
            onConfirm = { pin ->
                biometricsType = "pin"
                prefs.setString(PreferenceManager.KEY_BIOMETRICS_TYPE, "pin")
                prefs.setString(PreferenceManager.KEY_BIOMETRICS_PIN, pin)
                if (!appLockEnabled) { appLockEnabled = true; prefs.setBoolean(PreferenceManager.KEY_BIOMETRICS_APP_LOCK, true) }
                showPinSetup = false
            },
            onDismiss = { showPinSetup = false }
        )
    }

    if (showPasswordSetup) {
        PasswordSetupDialog(
            onConfirm = { password ->
                biometricsType = "password"
                prefs.setString(PreferenceManager.KEY_BIOMETRICS_TYPE, "password")
                prefs.setString(PreferenceManager.KEY_BIOMETRICS_PASSWORD, password)
                if (!appLockEnabled) { appLockEnabled = true; prefs.setBoolean(PreferenceManager.KEY_BIOMETRICS_APP_LOCK, true) }
                showPasswordSetup = false
            },
            onDismiss = { showPasswordSetup = false }
        )
    }
}

// ─── Biometric Type Selector Sheet ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BiometricTypeSheet(
    systemAvailable: Boolean,
    currentType: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(width = 36.dp, height = 4.dp)
                ) {}
            }
        }
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Choose Method", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
            }

            // System Biometrics option
            BiometricOptionRow(
                icon = Icons.Default.Fingerprint,
                iconTint = Color(0xFF6750A4),
                title = "System Biometrics",
                subtitle = if (systemAvailable) "Fingerprint, face unlock, or device credentials"
                           else "Not available on this device",
                isSelected = currentType == "system",
                enabled = systemAvailable,
                onClick = { if (systemAvailable) onSelect("system") }
            )

            HorizontalDivider(
                Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
            Text(
                "Custom Biometrics",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
            )

            BiometricOptionRow(
                icon = Icons.Default.Pin,
                iconTint = Color(0xFF2196F3),
                title = "PIN",
                subtitle = "Set a numeric PIN of any length",
                isSelected = currentType == "pin",
                onClick = { onSelect("pin") }
            )

            BiometricOptionRow(
                icon = Icons.Default.Key,
                iconTint = Color(0xFF4CAF50),
                title = "Password",
                subtitle = "Set a custom alphanumeric password",
                isSelected = currentType == "password",
                onClick = { onSelect("password") }
            )

            if (currentType.isNotEmpty()) {
                HorizontalDivider(
                    Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
                Surface(
                    onClick = { onSelect("") },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.LockOpen, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Text("Remove Biometric Lock", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun BiometricOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        else Color.Transparent,
        spring(stiffness = Spring.StiffnessMediumLow), label = "optBg"
    )
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = bg,
        modifier = Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.4f)
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconTint.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn(spring(stiffness = Spring.StiffnessMedium)) + fadeIn(),
                exit  = scaleOut() + fadeOut()
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ─── PIN Setup Dialog ────────────────────────────────────────────────────────

@Composable
fun PinSetupDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Set PIN",
    isVerify: Boolean = false,
    expectedPin: String = "",
    showCloseButton: Boolean = true
) {
    var phase by remember { mutableIntStateOf(if (isVerify) 2 else 0) } // 0=enter, 1=confirm, 2=done
    var pin by remember { mutableStateOf("") }
    var firstPin by remember { mutableStateOf("") }
    var shakeState by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    val shake by animateDpAsState(
        targetValue = 0.dp,
        animationSpec = keyframes {
            durationMillis = 400
            0.dp at 0; (-12).dp at 60; 12.dp at 120; (-8).dp at 200; 8.dp at 280; 0.dp at 400
        },
        label = "shake",
        finishedListener = { shakeState = 0 }
    )

    fun vibError() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(80, 180))
            } else {
                @Suppress("DEPRECATION")
                (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
                    .vibrate(VibrationEffect.createOneShot(80, 180))
            }
        } catch (_: Exception) {}
    }

    fun vibSuccess() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(40, 120))
            } else {
                @Suppress("DEPRECATION")
                (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
                    .vibrate(VibrationEffect.createOneShot(40, 120))
            }
        } catch (_: Exception) {}
    }

    fun onDigit(d: String) {
        if (pin.length >= 12) return
        pin += d
    }

    fun onBackspace() {
        if (pin.isNotEmpty()) pin = pin.dropLast(1)
    }

    fun onSubmit() {
        when {
            pin.length < 4 -> { shakeState++; vibError() }
            isVerify -> {
                if (pin == expectedPin) { vibSuccess(); onConfirm(pin) }
                else { pin = ""; shakeState++; vibError() }
            }
            phase == 0 -> { firstPin = pin; pin = ""; phase = 1 }
            phase == 1 -> {
                if (pin == firstPin) { vibSuccess(); onConfirm(pin) }
                else { pin = ""; firstPin = ""; phase = 0; shakeState++; vibError() }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = showCloseButton,
            dismissOnClickOutside = showCloseButton
        )
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when {
                            isVerify -> title
                            phase == 0 -> "Enter PIN"
                            else -> "Confirm PIN"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (showCloseButton) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                }

                if (!isVerify) {
                    Text(
                        text = if (phase == 0) "Enter a PIN of at least 4 digits" else "Re-enter your PIN to confirm",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // PIN dots indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.offset(x = if (shakeState > 0) shake else 0.dp)
                ) {
                    val maxDots = pin.length.coerceAtLeast(4).coerceAtMost(12)
                    repeat(maxDots) { i ->
                        val filled = i < pin.length
                        val scale by animateFloatAsState(
                            targetValue = if (filled) 1f else 0.6f,
                            animationSpec = spring(stiffness = Spring.StiffnessMedium),
                            label = "dot$i"
                        )
                        Box(
                            Modifier
                                .size(14.dp)
                                .scale(scale)
                                .clip(CircleShape)
                                .background(
                                    if (filled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    }
                }

                // Numpad
                PinNumpad(
                    onDigit = ::onDigit,
                    onBackspace = ::onBackspace,
                    onSubmit = ::onSubmit
                )
            }
        }
    }
}

@Composable
private fun PinNumpad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "⌫")
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    val isPressed by interaction.collectIsPressedAsState()
                    val scale by animateFloatAsState(if (isPressed) 0.88f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "ks")
                    val radius by animateDpAsState(if (isPressed) 14.dp else 22.dp, spring(stiffness = Spring.StiffnessMedium), label = "kr")

                    Box(Modifier.weight(1f)) {
                        when (key) {
                            "" -> {} // empty cell
                            "⌫" -> Surface(
                                onClick = onBackspace,
                                shape = RoundedCornerShape(radius),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier.fillMaxWidth().height(56.dp).scale(scale),
                                interactionSource = interaction
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Backspace, null, modifier = Modifier.size(20.dp))
                                }
                            }
                            else -> Surface(
                                onClick = { onDigit(key) },
                                shape = RoundedCornerShape(radius),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier.fillMaxWidth().height(56.dp).scale(scale),
                                interactionSource = interaction
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(key, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        }
        // Confirm row
        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Confirm", fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─── Password Setup Dialog ───────────────────────────────────────────────────

@Composable
fun PasswordSetupDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Set Password",
    isVerify: Boolean = false,
    expectedPassword: String = "",
    showCloseButton: Boolean = true
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = showCloseButton,
            dismissOnClickOutside = showCloseButton
        )
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                Modifier.padding(24.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isVerify) title else "Set Password",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (showCloseButton) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                }

                if (!isVerify) {
                    Text(
                        "Enter any password. Supports letters, numbers and special characters.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorText = "" },
                    label = { Text(if (isVerify) "Password" else "Enter Password") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    isError = errorText.isNotEmpty()
                )

                if (!isVerify) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; errorText = "" },
                        label = { Text("Confirm Password") },
                        singleLine = true,
                        visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showConfirm = !showConfirm }) {
                                Icon(if (showConfirm) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        isError = errorText.isNotEmpty()
                    )
                }

                AnimatedVisibility(visible = errorText.isNotEmpty()) {
                    Text(errorText, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = {
                            when {
                                isVerify -> {
                                    if (password == expectedPassword) onConfirm(password)
                                    else errorText = "Incorrect password"
                                }
                                password.length < 4 -> errorText = "Password must be at least 4 characters"
                                password != confirmPassword -> errorText = "Passwords don't match"
                                else -> onConfirm(password)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Confirm") }
                }
            }
        }
    }
}

