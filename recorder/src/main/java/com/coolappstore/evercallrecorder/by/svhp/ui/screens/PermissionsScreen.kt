/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.coolappstore.evercallrecorder.by.svhp.ui.screens

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coolappstore.evercallrecorder.by.svhp.R
import com.coolappstore.evercallrecorder.by.svhp.data.AppPreferences
import com.coolappstore.evercallrecorder.by.svhp.integrations.shizuku.ShizukuConnectionManager
import com.coolappstore.evercallrecorder.by.svhp.onboarding.OnboardingStatus
import com.coolappstore.evercallrecorder.by.svhp.system.openAppSettings
import com.coolappstore.evercallrecorder.by.svhp.ui.common.StorageLocationDialog
import com.coolappstore.evercallrecorder.by.svhp.ui.theme.ShizucallrecorderTheme
import com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels.PermissionsViewModel
import kotlin.system.exitProcess

@Composable
fun PermissionsScreen(
    status: OnboardingStatus.Status,
    onPermissionGranted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PermissionsViewModel = viewModel()
) {
    val activityContext = LocalContext.current
    var showStorageChoiceDialog by remember { mutableStateOf(false) }

    val permissionRequestLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        if (!result) activityContext.openAppSettings()
        onPermissionGranted()
    }
    val folderPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            activityContext.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            AppPreferences(activityContext).apply {
                setRecordingFolderUri(uri)
                setStorageMode(AppPreferences.StorageMode.SAF_FOLDER)
            }
        }
        onPermissionGranted()
    }

    if (status.shizukuPermissionGranted && ShizukuConnectionManager.hasPermission(activityContext)) {
        if (ShizukuConnectionManager.isAvailable()) {
            val requiredPermissions = listOf(Manifest.permission.CAPTURE_AUDIO_OUTPUT)
            val missingPermissions = requiredPermissions.filter { !ShizukuConnectionManager.checkServerPermission(it) }
            if (missingPermissions.isNotEmpty()) {
                val cleanPermissionsString = missingPermissions.joinToString("\n") { it.substringAfterLast(".") }
                val dialogMessage = activityContext.getString(R.string.general_system_limitation_message, cleanPermissionsString)
                AlertDialog.Builder(activityContext)
                    .setTitle(R.string.general_system_limitation)
                    .setMessage(dialogMessage)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setCancelable(false)
                    .setPositiveButton("Exit") { _, _ -> exitProcess(0) }
                    .show()
            }
        }
    }

    val grantAccess = {
        viewModel.onGrantAccess(
            status = status,
            onPermissionGranted = onPermissionGranted,
            requestRuntimePermission = { permission -> permissionRequestLauncher.launch(permission) },
            showStorageChoice = { showStorageChoiceDialog = true },
        )
    }

    // Auto-pop next permission when Shizuku is ready and a runtime permission is still missing
    val autoGrantable = status.shizukuRunning && status.shizukuPermissionGranted &&
        (!status.notificationsGranted || !status.contactsGranted ||
         !status.phoneStateGranted   || !status.callLogGranted ||
         !status.batteryExempted     || !status.storageSelected)

    LaunchedEffect(status) {
        if (autoGrantable) {
            grantAccess()
        }
    }

    PermissionsContent(
        status = status,
        onGrantAccessButtonClick = grantAccess,
        modifier = modifier
    )

    if (showStorageChoiceDialog) {
        StorageLocationDialog(
            onChooseFolder = {
                showStorageChoiceDialog = false
                folderPickerLauncher.launch(null)
            },
            onChoosePrivate = {
                AppPreferences(activityContext).setStorageMode(AppPreferences.StorageMode.PRIVATE)
                showStorageChoiceDialog = false
                onPermissionGranted()
            },
            onDismiss = { showStorageChoiceDialog = false }
        )
    }
}

@Composable
fun PermissionsContent(
    status: OnboardingStatus.Status,
    onGrantAccessButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary       = MaterialTheme.colorScheme.primary
    val primaryCont   = MaterialTheme.colorScheme.primaryContainer
    val secondaryCont = MaterialTheme.colorScheme.secondaryContainer
    val tertiaryCont  = MaterialTheme.colorScheme.tertiaryContainer
    val onPrimaryCont = MaterialTheme.colorScheme.onPrimaryContainer

    val grantedCount = listOf(
        status.shizukuRunning && status.shizukuPermissionGranted,
        status.notificationsGranted, status.contactsGranted,
        status.phoneStateGranted,    status.callLogGranted,
        status.batteryExempted,      status.storageSelected
    ).count { it }
    val totalCount = 7

    val animatedProgress by animateFloatAsState(
        targetValue = grantedCount.toFloat() / totalCount,
        animationSpec = tween(600),
        label = "progress"
    )

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Hero header ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(
                        Brush.linearGradient(
                            colorStops = arrayOf(
                                0.0f to primaryCont,
                                0.5f to secondaryCont,
                                1.0f to tertiaryCont
                            )
                        )
                    )
            ) {
                // — Decorative circles (clean, no blur) —

                // Large ring top-right
                Box(
                    Modifier
                        .size(130.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 36.dp, y = (-36).dp)
                        .clip(CircleShape)
                        .background(onPrimaryCont.copy(alpha = 0.10f))
                )
                // Medium circle top-right inner
                Box(
                    Modifier
                        .size(70.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 10.dp, y = (-4).dp)
                        .clip(CircleShape)
                        .background(primary.copy(alpha = 0.16f))
                )
                // Large ring bottom-left
                Box(
                    Modifier
                        .size(110.dp)
                        .align(Alignment.BottomStart)
                        .offset(x = (-32).dp, y = 32.dp)
                        .clip(CircleShape)
                        .background(onPrimaryCont.copy(alpha = 0.09f))
                )
                // Small accent circle bottom-left inner
                Box(
                    Modifier
                        .size(50.dp)
                        .align(Alignment.BottomStart)
                        .offset(x = (-8).dp, y = 8.dp)
                        .clip(CircleShape)
                        .background(primary.copy(alpha = 0.14f))
                )
                // Tiny dot centre-right
                Box(
                    Modifier
                        .size(18.dp)
                        .align(Alignment.CenterEnd)
                        .offset(x = (-28).dp, y = (-20).dp)
                        .clip(CircleShape)
                        .background(onPrimaryCont.copy(alpha = 0.22f))
                )
                // Tiny dot top-left
                Box(
                    Modifier
                        .size(12.dp)
                        .offset(x = 28.dp, y = 32.dp)
                        .clip(CircleShape)
                        .background(onPrimaryCont.copy(alpha = 0.18f))
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // Circle icon badge
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(onPrimaryCont.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Shield, null, tint = onPrimaryCont, modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.permissions_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = onPrimaryCont,
                        letterSpacing = (-0.3).sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.permissions_intro),
                        style = MaterialTheme.typography.bodySmall,
                        color = onPrimaryCont.copy(alpha = 0.78f),
                        lineHeight = 16.sp
                    )
                }
            }

            // ── Progress strip ─────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Setup Progress", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                    Text("$grantedCount / $totalCount", style = MaterialTheme.typography.labelMedium, color = primary, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }

            // ── Permission rows ────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PermissionRow(
                    icon = Icons.Outlined.DeveloperMode,
                    label = stringResource(R.string.permission_shizuku_label),
                    description = stringResource(R.string.permission_shizuku_description),
                    granted = status.shizukuRunning && status.shizukuPermissionGranted,
                    statusOverride = when {
                        !status.shizukuRunning           -> stringResource(R.string.permission_shizuku_not_running)
                        !status.shizukuPermissionGranted -> stringResource(R.string.permissions_status_required)
                        else                             -> null
                    }
                )
                PermissionRow(Icons.Outlined.Notifications,       stringResource(R.string.permission_notifications_label), stringResource(R.string.permission_notifications_description), status.notificationsGranted)
                PermissionRow(Icons.Outlined.Contacts,            stringResource(R.string.permission_contacts_label),      stringResource(R.string.permission_contacts_description),      status.contactsGranted)
                PermissionRow(Icons.Outlined.Phone,               stringResource(R.string.permission_phone_state_label),   stringResource(R.string.permission_phone_state_description),   status.phoneStateGranted)
                PermissionRow(Icons.Outlined.History,             stringResource(R.string.permission_call_log_label),      stringResource(R.string.permission_call_log_description),      status.callLogGranted)
                PermissionRow(Icons.Outlined.BatteryChargingFull, stringResource(R.string.permission_battery_label),       stringResource(R.string.permission_battery_description),       status.batteryExempted)
                PermissionRow(Icons.Outlined.FolderOpen,          stringResource(R.string.settings_recording_folder_label), stringResource(R.string.permission_storage_description),     status.storageSelected)
            }

            // ── Action button ──────────────────────────────────────────────────
            Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp)) {
                    Button(
                        onClick = onGrantAccessButtonClick,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                    ) {
                        val icon = when {
                            status.isComplete()    -> Icons.Outlined.Check
                            !status.shizukuRunning -> Icons.Outlined.OpenInNew
                            else                   -> Icons.Outlined.LockOpen
                        }
                        Icon(icon, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = when {
                                status.isComplete()    -> stringResource(R.string.general_continue)
                                !status.shizukuRunning -> stringResource(R.string.permission_shizuku_open)
                                else                   -> stringResource(R.string.permissions_grant_access)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    label: String,
    description: String,
    granted: Boolean,
    statusOverride: String? = null
) {
    val primary      = MaterialTheme.colorScheme.primary
    val error        = MaterialTheme.colorScheme.error
    val onSurface    = MaterialTheme.colorScheme.onSurface
    val onSurfaceVar = MaterialTheme.colorScheme.onSurfaceVariant

    val bgColor by animateColorAsState(
        targetValue = if (granted) MaterialTheme.colorScheme.surfaceContainerHigh
                      else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
        animationSpec = tween(400), label = "bg"
    )
    val iconBgColor by animateColorAsState(
        targetValue = if (granted) MaterialTheme.colorScheme.primaryContainer
                      else MaterialTheme.colorScheme.errorContainer,
        animationSpec = tween(400), label = "iconBg"
    )
    val iconTint by animateColorAsState(
        targetValue = if (granted) primary else error,
        animationSpec = tween(400), label = "iconTint"
    )

    Surface(shape = RoundedCornerShape(18.dp), color = bgColor, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape).background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = onSurface)
                Text(description, style = MaterialTheme.typography.bodySmall, color = onSurfaceVar, lineHeight = 15.sp)
            }
            Surface(
                shape = CircleShape,
                color = if (granted) primary.copy(alpha = 0.12f) else error.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        if (granted) Icons.Filled.CheckCircle else Icons.Outlined.ErrorOutline,
                        null, tint = if (granted) primary else error, modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = statusOverride ?: if (granted) "Granted" else "Required",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (granted) primary else error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionsScreenPreview() {
    ShizucallrecorderTheme(darkTheme = false) {
        PermissionsContent(
            status = OnboardingStatus.Status(
                disclaimerAccepted = true, notificationsGranted = false, contactsGranted = true,
                phoneStateGranted = false, callLogGranted = false, batteryExempted = false,
                storageSelected = false, shizukuRunning = false, shizukuPermissionGranted = false
            ),
            onGrantAccessButtonClick = {}
        )
    }
}
