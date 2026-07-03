/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.coolappstore.evercallrecorder.by.svhp.ui.common

import android.content.res.Configuration
import android.os.Environment
import android.os.StatFs
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

// ── Data model ────────────────────────────────────────────────────────────────

private data class StorageRoot(
    val label: String,
    val path: File,
    val icon: ImageVector,
    val subtitle: String
)

// ── Public composable ─────────────────────────────────────────────────────────

/**
 * Beautiful bottom-sheet file manager for picking a save destination.
 *
 * @param count     Number of recordings being saved (hidden if 0 or 1).
 * @param onDismiss Called when the user taps Close or the scrim.
 * @param onSave    Called with the chosen [File] directory when Save is tapped.
 */
@Composable
fun FileSavePickerDialog(
    count: Int = 1,
    onDismiss: () -> Unit,
    onSave: (File) -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // ── Sheet enter/exit animation state ─────────────────────────────────────
    var sheetVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(30) // one frame delay so the animation fires after the dialog attaches
        sheetVisible = true
    }

    fun dismissWithAnimation(block: () -> Unit = onDismiss) {
        scope.launch {
            sheetVisible = false
            delay(320)
            block()
        }
    }

    // ── Navigation state ──────────────────────────────────────────────────────
    var currentDir by remember { mutableStateOf<File?>(null) }
    val navStack   = remember { mutableStateListOf<File>() }
    val isAtRoot   = currentDir == null

    // ── Storage roots ─────────────────────────────────────────────────────────
    val storageRoots: List<StorageRoot> = remember {
        buildList {
            val primary = Environment.getExternalStorageDirectory()
            if (primary != null && primary.exists()) {
                add(StorageRoot("Internal Storage", primary, Icons.Rounded.PhoneAndroid, formatStorageSpace(primary)))
            }
            try {
                context.getExternalFilesDirs(null).drop(1).filterNotNull().forEach { appDir ->
                    var sdRoot = appDir
                    repeat(4) { sdRoot = sdRoot.parentFile ?: sdRoot }
                    if (sdRoot != primary && sdRoot.exists() && sdRoot.canRead()) {
                        add(StorageRoot("Memory Card", sdRoot, Icons.Rounded.SdCard, formatStorageSpace(sdRoot)))
                    }
                }
            } catch (_: Exception) { }
        }
    }

    // ── Sub-folders ───────────────────────────────────────────────────────────
    val folders: List<File> = remember(currentDir) {
        currentDir?.listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith(".") }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
    }

    // The directory used when Save is pressed — current dir or first storage root
    val saveTarget: File? = currentDir ?: storageRoots.firstOrNull()?.path

    // Friendly label for saveTarget (avoids showing raw "0" for /storage/emulated/0)
    val saveTargetLabel: String = remember(saveTarget) {
        storageRoots.firstOrNull { it.path == saveTarget }?.label
            ?: saveTarget?.name?.let { name ->
                if (name == "0") "Internal Storage" else name
            } ?: ""
    }

    // ── Sheet slide animation ─────────────────────────────────────────────────
    val sheetOffset by animateFloatAsState(
        targetValue   = if (sheetVisible) 0f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMediumLow
        ),
        label = "sheetOffset"
    )
    val sheetAlpha by animateFloatAsState(
        targetValue   = if (sheetVisible) 1f else 0f,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "sheetAlpha"
    )

    // ── Dialog ────────────────────────────────────────────────────────────────
    Dialog(
        onDismissRequest = { dismissWithAnimation() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress      = true,
            dismissOnClickOutside   = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = sheetAlpha }
                .background(Color.Black.copy(alpha = 0.48f))
                .pointerInput(Unit) {
                    detectTapGestures { dismissWithAnimation() }
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(if (isLandscape) 1f else 0.83f)
                    .graphicsLayer { translationY = size.height * sheetOffset }
                    .clickable(
                        indication        = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { /* consume */ },
                shape          = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color          = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Box(modifier = Modifier.fillMaxSize()) {

                    Column(modifier = Modifier.fillMaxSize()) {

                        // ── Drag handle ───────────────────────────────────────
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f))
                            )
                        }

                        // ── Header ────────────────────────────────────────────
                        AnimatedContent(
                            targetState  = isAtRoot,
                            transitionSpec = {
                                val enter = if (targetState)
                                    slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { -it / 2 } + fadeIn(tween(240))
                                else
                                    slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { it / 2 } + fadeIn(tween(240))
                                val exit = if (targetState)
                                    slideOutHorizontally(tween(200)) { it / 2 } + fadeOut(tween(180))
                                else
                                    slideOutHorizontally(tween(200)) { -it / 2 } + fadeOut(tween(180))
                                enter togetherWith exit
                            },
                            label = "header"
                        ) { atRoot ->
                            Row(
                                modifier          = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp, end = 20.dp, top = 10.dp, bottom = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (atRoot) {
                                    Box(
                                        modifier = Modifier
                                            .padding(start = 12.dp)
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.SaveAs,
                                            contentDescription = null,
                                            tint     = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Save Recordings",
                                            style      = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            "Choose a storage location",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = {
                                            if (navStack.isNotEmpty()) currentDir = navStack.removeLast()
                                            else currentDir = null
                                        }
                                    ) {
                                        Icon(
                                            Icons.Rounded.ArrowBackIosNew,
                                            contentDescription = "Back",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(4.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            currentDir?.name ?: "",
                                            style      = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines   = 1,
                                            overflow   = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            currentDir?.absolutePath ?: "",
                                            style    = MaterialTheme.typography.labelSmall,
                                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            thickness = 0.8.dp
                        )

                        // ── List content ──────────────────────────────────────
                        val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                        LazyColumn(
                            modifier       = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 140.dp + navBarHeight)
                        ) {
                            if (isAtRoot) {
                                itemsIndexed(storageRoots, key = { _, r -> r.path.absolutePath }) { index, root ->
                                    AnimatedListItem(index = index) {
                                        StorageRootCard(
                                            root    = root,
                                            onClick = {
                                                navStack.clear()
                                                currentDir = root.path
                                            }
                                        )
                                    }
                                }
                            } else {
                                if (folders.isEmpty()) {
                                    item { EmptyFolderHint() }
                                } else {
                                    itemsIndexed(folders, key = { _, f -> f.absolutePath }) { index, folder ->
                                        AnimatedListItem(index = index) {
                                            FolderRow(
                                                folder  = folder,
                                                onClick = {
                                                    navStack.add(currentDir!!)
                                                    currentDir = folder
                                                }
                                            )
                                        }
                                        if (index < folders.lastIndex) {
                                            HorizontalDivider(
                                                modifier  = Modifier.padding(start = 72.dp, end = 20.dp),
                                                thickness = 0.5.dp,
                                                color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Floating action buttons ───────────────────────────────
                    BottomFabBar(
                        modifier          = Modifier.align(Alignment.BottomCenter),
                        saveTarget        = saveTarget,
                        saveTargetLabel   = saveTargetLabel,
                        count             = count,
                        sheetVisible      = sheetVisible,
                        onClose           = { dismissWithAnimation() },
                        onSave            = { dir -> dismissWithAnimation { onSave(dir) } }
                    )
                }
            }
        }
    }
}

// ── Animated list item wrapper ────────────────────────────────────────────────

@Composable
private fun AnimatedListItem(index: Int, content: @Composable () -> Unit) {
    content()
}

// ── Bottom FAB bar ────────────────────────────────────────────────────────────

@Composable
private fun BottomFabBar(
    modifier:        Modifier,
    saveTarget:      File?,
    saveTargetLabel: String,
    count:           Int,
    sheetVisible:    Boolean,
    onClose:         () -> Unit,
    onSave:          (File) -> Unit
) {
    // FABs scale in with a spring bounce after the sheet settles
    var fabsVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(240)
        fabsVisible = true
    }

    val fabScale by animateFloatAsState(
        targetValue   = if (fabsVisible) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "fabScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .padding(horizontal = 28.dp, vertical = 20.dp)
            .navigationBarsPadding()
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {

            // ── Close FAB ─────────────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { scaleX = fabScale; scaleY = fabScale }
            ) {
                FloatingActionButton(
                    onClick        = onClose,
                    shape          = CircleShape,
                    modifier       = Modifier.size(64.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor   = MaterialTheme.colorScheme.onSurface,
                    elevation      = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Close",
                    style  = MaterialTheme.typography.labelSmall,
                    color  = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Path pill (hidden when no target) ─────────────────────────────
            AnimatedVisibility(
                visible      = saveTarget != null && saveTargetLabel.isNotBlank(),
                enter        = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit         = scaleOut() + fadeOut(),
                modifier     = Modifier.weight(1f).padding(horizontal = 12.dp)
            ) {
                Surface(
                    shape    = RoundedCornerShape(20.dp),
                    color    = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f)
                ) {
                    Row(
                        modifier              = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            modifier           = Modifier.size(15.dp),
                            tint               = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text       = saveTargetLabel,
                            style      = MaterialTheme.typography.labelMedium,
                            color      = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // ── Save FAB ──────────────────────────────────────────────────────
            val canSave = saveTarget != null
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { scaleX = fabScale; scaleY = fabScale }
            ) {
                FloatingActionButton(
                    onClick        = { saveTarget?.let { onSave(it) } },
                    shape          = CircleShape,
                    modifier       = Modifier.size(64.dp),
                    containerColor = if (canSave) MaterialTheme.colorScheme.primary
                                     else MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor   = if (canSave) MaterialTheme.colorScheme.onPrimary
                                     else MaterialTheme.colorScheme.onSurfaceVariant,
                    elevation      = FloatingActionButtonDefaults.elevation(
                        defaultElevation = if (canSave) 8.dp else 2.dp
                    )
                ) {
                    Icon(Icons.Rounded.SaveAlt, contentDescription = "Save here", modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.height(6.dp))
                // Only show "(N)" badge when N > 1
                Text(
                    text  = if (count > 1) "Save ($count)" else "Save",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (canSave) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ── Storage root card ─────────────────────────────────────────────────────────

@Composable
private fun StorageRootCard(root: StorageRoot, onClick: () -> Unit) {
    val primary   = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val accent    = if (root.icon == Icons.Rounded.SdCard) secondary else primary

    Surface(
        onClick   = onClick,
        shape     = RoundedCornerShape(20.dp),
        color     = accent.copy(alpha = 0.07f),
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier              = Modifier.padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier           = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment   = Alignment.Center
            ) {
                Icon(root.icon, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(root.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                if (root.subtitle.isNotBlank()) {
                    Text(root.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = accent.copy(alpha = 0.65f), modifier = Modifier.size(20.dp))
        }
    }
}

// ── Folder row ────────────────────────────────────────────────────────────────

@Composable
private fun FolderRow(folder: File, onClick: () -> Unit) {
    val subCount = remember(folder) {
        try { folder.listFiles()?.count { it.isDirectory && !it.name.startsWith(".") } ?: 0 }
        catch (_: Exception) { 0 }
    }
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier         = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(folder.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subCount > 0) {
                Text("$subCount folder${if (subCount != 1) "s" else ""}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f), modifier = Modifier.size(18.dp))
    }
}

// ── Empty hint ────────────────────────────────────────────────────────────────

@Composable
private fun EmptyFolderHint() {
    Box(
        modifier         = Modifier.fillMaxWidth().padding(vertical = 56.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier         = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
            }
            Text("No sub-folders here", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text("You can still save to this location.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatStorageSpace(root: File): String = try {
    val stat = StatFs(root.path)
    "${formatBytes(stat.availableBytes)} free of ${formatBytes(stat.totalBytes)}"
} catch (_: Exception) { "" }

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "${"%.1f".format(bytes / 1_073_741_824.0)} GB"
    bytes >= 1_048_576L     -> "${"%.0f".format(bytes / 1_048_576.0)} MB"
    else                    -> "${bytes / 1024} KB"
}
