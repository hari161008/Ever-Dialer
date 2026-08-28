package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.app.DownloadManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coolappstore.everdialer.by.svhp.APP_VERSION
import com.coolappstore.everdialer.by.svhp.GITHUB_API_RELEASES
import com.coolappstore.everdialer.by.svhp.GITHUB_API_RELEASES_LIST
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.ReleaseInfo
import com.coolappstore.everdialer.by.svhp.controller.util.enqueueApkDownload
import com.coolappstore.everdialer.by.svhp.controller.util.fetchLatestRelease
import com.coolappstore.everdialer.by.svhp.controller.util.fetchReleaseForVersion
import com.coolappstore.everdialer.by.svhp.controller.util.getApkDestinationFile
import com.coolappstore.everdialer.by.svhp.controller.util.installApkAndScheduleDelete
import com.coolappstore.everdialer.by.svhp.controller.util.isNewerVersion
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoListItem
import com.coolappstore.everdialer.by.svhp.view.components.RivoSectionHeader
import com.coolappstore.everdialer.by.svhp.view.components.RivoSwitchListItem
import com.coolappstore.everdialer.by.svhp.view.components.performAppHaptic
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.roundToInt

// ─── State machines ────────────────────────────────────────────────────────

private sealed class CheckState {
    object Idle : CheckState()
    object Checking : CheckState()
    data class Done(val latest: ReleaseInfo?, val isNewer: Boolean) : CheckState()
    object Failed : CheckState()
}

private sealed class DownloadState {
    object Idle : DownloadState()
    data class Confirm(val release: ReleaseInfo, val readyToInstall: Boolean) : DownloadState()
    data class Downloading(val release: ReleaseInfo, val downloadId: Long, val progress: Float) : DownloadState()
    object Failed : DownloadState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun UpdatesScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val scope = rememberCoroutineScope()

    var checkState by remember { mutableStateOf<CheckState>(CheckState.Idle) }
    var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }
    var installedNotes by remember { mutableStateOf<String?>(null) }
    var installedNotesLoaded by remember { mutableStateOf(false) }
    var showCompareSheet by remember { mutableStateOf(false) }

    val settingsVersion by prefs.settingsChanged.collectAsState()
    var autoUpdateEnabled by remember(settingsVersion) {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_AUTO_UPDATE_CHECK, true))
    }

    fun triggerHaptic() {
        if (prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) {
            performAppHaptic(
                context,
                prefs.getString(PreferenceManager.KEY_APP_HAPTICS_STRENGTH, "light") ?: "light",
                prefs.getFloat(PreferenceManager.KEY_HAPTICS_CUSTOM_INTENSITY, 0.5f)
            )
        }
    }

    fun runCheck() {
        triggerHaptic()
        scope.launch {
            checkState = CheckState.Checking
            val release = fetchLatestRelease(GITHUB_API_RELEASES)
            checkState = when {
                release == null -> CheckState.Failed
                isNewerVersion(release.tagName, APP_VERSION) -> CheckState.Done(release, true)
                else -> CheckState.Done(release, false)
            }
        }
    }

    LaunchedEffect(Unit) {
        runCheck()
    }
    LaunchedEffect(Unit) {
        installedNotes = fetchReleaseForVersion(GITHUB_API_RELEASES_LIST, APP_VERSION)?.releaseNotes
        installedNotesLoaded = true
    }

    var screenVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { screenVisible = true }

    // ── Download confirmation + progress flow ──────────────────────────────
    when (val ds = downloadState) {
        is DownloadState.Confirm -> {
            PermissionToDownloadDialog(
                currentVersion = APP_VERSION,
                latestVersion = ds.release.tagName,
                readyToInstall = ds.readyToInstall,
                onConfirm = {
                    triggerHaptic()
                    if (ds.readyToInstall) {
                        downloadState = DownloadState.Idle
                        installApkAndScheduleDelete(context, getApkDestinationFile())
                    } else {
                        val url = ds.release.apkUrl
                        if (url != null) {
                            val id = enqueueApkDownload(context, url)
                            downloadState = if (id != null) DownloadState.Downloading(ds.release, id, 0f) else DownloadState.Failed
                        } else {
                            downloadState = DownloadState.Failed
                        }
                    }
                },
                onDismiss = { downloadState = DownloadState.Idle }
            )
        }
        is DownloadState.Downloading -> {
            LaunchedEffect(ds.downloadId) {
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                while (true) {
                    delay(300)
                    val query = DownloadManager.Query().setFilterById(ds.downloadId)
                    val cursor = dm.query(query)
                    if (!cursor.moveToFirst()) { cursor.close(); break }
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    cursor.close()
                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            prefs.setString(PreferenceManager.KEY_DOWNLOADED_UPDATE_VERSION, ds.release.tagName)
                            downloadState = DownloadState.Idle
                            installApkAndScheduleDelete(context, getApkDestinationFile())
                            break
                        }
                        DownloadManager.STATUS_FAILED -> {
                            downloadState = DownloadState.Failed
                            break
                        }
                        else -> {
                            val p = if (total > 0L) (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
                            downloadState = ds.copy(progress = p)
                        }
                    }
                }
            }
            DownloadingDialog(latestVersion = ds.release.tagName, progress = ds.progress)
        }
        is DownloadState.Failed -> {
            AlertDialog(
                onDismissRequest = { downloadState = DownloadState.Idle },
                shape = RoundedCornerShape(28.dp),
                icon = {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                title = { Text("Download Failed", fontWeight = FontWeight.Bold) },
                text = { Text("Something went wrong while downloading the update. Please check your internet connection and try again.") },
                confirmButton = {
                    Button(
                        onClick = { downloadState = DownloadState.Idle },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("OK")
                    }
                }
            )
        }
        else -> {}
    }

    if (showCompareSheet) {
        val latestForCompare = (checkState as? CheckState.Done)?.latest
        CompareReleaseNotesSheet(
            installedVersion = APP_VERSION,
            installedNotes = installedNotes,
            latestVersion = latestForCompare?.tagName,
            latestNotes = latestForCompare?.releaseNotes,
            onDismiss = { showCompareSheet = false }
        )
    }

    val infinite = rememberInfiniteTransition(label = "appBarSpin")
    val spinAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "spinAngle"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Updates", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { runCheck() },
                        enabled = checkState !is CheckState.Checking
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Check again",
                            modifier = if (checkState is CheckState.Checking) Modifier.rotate(spinAngle) else Modifier
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Hero Banner ──────────────────────────────────────────
            RivoAnimatedSection(delayMs = 0L) {
                ExpressiveUpdateHeroCard(checkState = checkState)
            }

            // ── Version Stat Cards ────────────────────────────────────
            RivoAnimatedSection(delayMs = 60L) {
                VersionStatsRow(checkState = checkState)
            }

            // ── Action Button Area ───────────────────────────────────
            RivoAnimatedSection(delayMs = 120L) {
                ExpressiveActionArea(
                    checkState = checkState,
                    onCheckAgain = { runCheck() },
                    onUpdateClick = { latest ->
                        triggerHaptic()
                        val apkFile = getApkDestinationFile()
                        val downloadedVersion = prefs.getString(PreferenceManager.KEY_DOWNLOADED_UPDATE_VERSION, null)
                        val readyToInstall = apkFile.exists() && apkFile.length() > 0L && downloadedVersion == latest.tagName
                        downloadState = DownloadState.Confirm(latest, readyToInstall)
                    }
                )
            }

            // ── Release Notes Section ────────────────────────────────
            RivoAnimatedSection(delayMs = 180L) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RivoSectionHeader(title = "What's New")
                    ExpressiveReleaseNotesCard(
                        checkState = checkState,
                        installedNotes = installedNotes,
                        onCompareClick = { showCompareSheet = true }
                    )
                }
            }

            // ── Compare Release Notes ─────────────────────────────────
            RivoAnimatedSection(delayMs = 210L) {
                RivoExpressiveCard {
                    RivoListItem(
                        headline = "Compare Release Notes",
                        supporting = "Compare installed v$APP_VERSION with the latest release",
                        leadingIcon = Icons.Outlined.Difference,
                        iconContainerColor = MaterialTheme.colorScheme.primary,
                        trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                        onClick = { showCompareSheet = true }
                    )
                }
            }

            // ── Update Preferences Card ──────────────────────────────
            RivoAnimatedSection(delayMs = 240L) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RivoSectionHeader(title = "Options")
                    RivoExpressiveCard {
                        RivoSwitchListItem(
                            headline = "Auto Check For Updates",
                            supporting = "Automatically check for new releases when app launches",
                            leadingIcon = Icons.Default.Autorenew,
                            iconContainerColor = MaterialTheme.colorScheme.primary,
                            checked = autoUpdateEnabled,
                            onCheckedChange = { enabled ->
                                autoUpdateEnabled = enabled
                                prefs.setBoolean(PreferenceManager.KEY_AUTO_UPDATE_CHECK, enabled)
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── Expressive Hero Status Card ──────────────────────────────────────────

@Composable
private fun ExpressiveUpdateHeroCard(checkState: CheckState) {
    val containerColor = when (checkState) {
        is CheckState.Done -> if (checkState.isNewer) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                              else MaterialTheme.colorScheme.surfaceContainerLow
        is CheckState.Failed -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }

    val animatedContainerColor by animateColorAsState(
        targetValue = containerColor,
        animationSpec = tween(400),
        label = "heroContainerColor"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = animatedContainerColor,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HeroStatusIcon(checkState = checkState)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Ever Dialer",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                AnimatedContent(
                    targetState = checkState,
                    transitionSpec = {
                        (fadeIn(tween(250)) + slideInVertically { it / 2 }) togetherWith
                            (fadeOut(tween(150)) + slideOutVertically { -it / 2 })
                    },
                    label = "heroTitle"
                ) { state ->
                    val (title, subtitle) = when (state) {
                        is CheckState.Checking -> "Checking for updates…" to "Connecting to update repository"
                        is CheckState.Done -> if (state.isNewer && state.latest != null) {
                            "Update Available!" to "v${state.latest.tagName} is ready to download"
                        } else {
                            "You're Up to Date" to "Ever Dialer v$APP_VERSION is the latest version"
                        }
                        is CheckState.Failed -> "Update Check Failed" to "Could not connect to update server"
                        else -> "Ever Dialer Updates" to "Current version v$APP_VERSION"
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = checkState,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "statusBadge"
            ) { state ->
                when (state) {
                    is CheckState.Checking -> ExpressiveStatusChip(
                        text = "Checking…",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        pulsing = true
                    )
                    is CheckState.Done -> if (state.isNewer && state.latest != null) {
                        ExpressiveStatusChip(
                            text = "New v${state.latest.tagName}",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        ExpressiveStatusChip(
                            text = "Latest Build",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    is CheckState.Failed -> ExpressiveStatusChip(
                        text = "Connection Error",
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                    else -> Spacer(Modifier.height(0.dp))
                }
            }
        }
    }
}

@Composable
private fun HeroStatusIcon(checkState: CheckState) {
    val infinite = rememberInfiniteTransition(label = "heroIconAnim")
    val spin by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "heroSpin"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "heroPulse"
    )

    val (icon, iconBgColor, iconTintColor) = when (checkState) {
        is CheckState.Checking -> Triple(
            Icons.Default.SystemUpdate,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        is CheckState.Done -> if (checkState.isNewer) {
            Triple(
                Icons.Default.NewReleases,
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.onPrimaryContainer
            )
        } else {
            Triple(
                Icons.Default.Verified,
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        is CheckState.Failed -> Triple(
            Icons.Default.CloudOff,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        else -> Triple(
            Icons.Default.SystemUpdate,
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.primary
        )
    }

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = iconBgColor,
        modifier = Modifier
            .size(76.dp)
            .scale(if (checkState is CheckState.Done && checkState.isNewer) pulse else 1f),
        shadowElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = checkState,
                transitionSpec = {
                    (fadeIn(tween(250)) + scaleIn(initialScale = 0.6f)) togetherWith
                        (fadeOut(tween(150)) + scaleOut(targetScale = 0.6f))
                },
                label = "heroIconGlyph"
            ) { state ->
                val modifier = if (state is CheckState.Checking) Modifier.rotate(spin) else Modifier
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTintColor,
                    modifier = modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
private fun ExpressiveStatusChip(
    text: String,
    containerColor: Color,
    contentColor: Color,
    pulsing: Boolean = false
) {
    val infinite = rememberInfiniteTransition(label = "statusChipPulse")
    val alphaAnim by infinite.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "chipAlpha"
    )

    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor,
        modifier = Modifier.alpha(if (pulsing) alphaAnim else 1f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

// ─── Version Stats Row ─────────────────────────────────────────────────────

@Composable
private fun VersionStatsRow(checkState: CheckState) {
    val latestVersionText = when (checkState) {
        is CheckState.Done -> checkState.latest?.tagName?.let { "v$it" } ?: "v$APP_VERSION"
        is CheckState.Checking -> "Checking…"
        is CheckState.Failed -> "Unavailable"
        else -> "v$APP_VERSION"
    }

    val isNewer = (checkState as? CheckState.Done)?.isNewer == true

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ExpressiveStatCard(
            label = "Installed",
            value = "v$APP_VERSION",
            icon = Icons.Outlined.PhoneAndroid,
            modifier = Modifier.weight(1f),
            iconTint = MaterialTheme.colorScheme.primary
        )

        ExpressiveStatCard(
            label = "Latest Release",
            value = latestVersionText,
            icon = if (isNewer) Icons.Outlined.CloudDownload else Icons.Outlined.CheckCircle,
            modifier = Modifier.weight(1f),
            iconTint = if (isNewer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            highlighted = isNewer
        )
    }
}

@Composable
private fun ExpressiveStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    highlighted: Boolean = false
) {
    val bgColor = if (highlighted) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconTint.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

// ─── Expressive Action Button Area ─────────────────────────────────────────

@Composable
private fun ExpressiveActionArea(
    checkState: CheckState,
    onCheckAgain: () -> Unit,
    onUpdateClick: (ReleaseInfo) -> Unit
) {
    AnimatedContent(
        targetState = checkState,
        transitionSpec = {
            (fadeIn(tween(250)) + slideInVertically { it / 3 }) togetherWith
                (fadeOut(tween(140)) + slideOutVertically { -it / 3 })
        },
        label = "actionArea"
    ) { state ->
        when {
            state is CheckState.Done && state.isNewer && state.latest != null -> {
                Button(
                    onClick = { onUpdateClick(state.latest) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Download & Install v${state.latest.tagName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            state is CheckState.Failed -> {
                FilledTonalButton(
                    onClick = onCheckAgain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Try Again", fontWeight = FontWeight.Bold)
                }
            }

            state is CheckState.Checking || state == CheckState.Idle -> {
                FilledTonalButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Checking for updates…", fontWeight = FontWeight.Medium)
                }
            }

            else -> {
                FilledTonalButton(
                    onClick = onCheckAgain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Check for Updates", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ─── Expressive Release Notes Card ─────────────────────────────────────────

@Composable
private fun ExpressiveReleaseNotesCard(
    checkState: CheckState,
    installedNotes: String?,
    onCompareClick: () -> Unit
) {
    RivoExpressiveCard {
        AnimatedContent(
            targetState = checkState,
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
            label = "notesAnimatedCard"
        ) { state ->
            when (state) {
                CheckState.Idle, is CheckState.Checking -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NotesShimmerPlaceholder()
                    }
                }

                is CheckState.Done -> {
                    if (state.isNewer && state.latest != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        "v${state.latest.tagName}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (state.latest.publishedAt != null) {
                                    Text(
                                        text = state.latest.publishedAt.substringBefore("T"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            SelectionContainer {
                                ReleaseNotesText(state.latest.releaseNotes)
                            }
                        }
                    } else {
                        // Up to date
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Text(
                                "You're on the latest build",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                "There are no pending updates. You're enjoying the most recent version of Ever Dialer.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            if (!installedNotes.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                TextButton(
                                    onClick = onCompareClick,
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("View Installed Version Notes")
                                }
                            }
                        }
                    }
                }

                is CheckState.Failed -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Text(
                            "Couldn't check for updates",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Unable to reach the GitHub releases repository. Please verify your connection.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesShimmerPlaceholder() {
    val infinite = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by infinite.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "shimmerX"
    )
    val base = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val highlight = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(0.7f, 1f, 0.88f, 0.65f).forEach { widthFraction ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .height(14.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(base, highlight, base),
                            start = Offset(shimmerX * 300f, 0f),
                            end = Offset(shimmerX * 300f + 300f, 0f)
                        )
                    )
            )
        }
    }
}

/** Rich GitHub Markdown renderer supporting code blocks, headers, quotes, lists, bold, italic, code, links. */
@Composable
private fun ReleaseNotesText(rawNotes: String?) {
    if (rawNotes.isNullOrBlank()) {
        Text(
            "No release notes were provided for this version.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val codeBgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    val lines = rawNotes.lines()
    var inCodeBlock = false
    val codeBlockLines = mutableListOf<String>()

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (rawLine in lines) {
            val trimmed = rawLine.trim()

            // Handle code block fences
            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    val codeContent = codeBlockLines.joinToString("\n")
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = codeContent,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = onSurfaceColor,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    codeBlockLines.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                }
                continue
            }

            if (inCodeBlock) {
                codeBlockLines.add(rawLine)
                continue
            }

            when {
                trimmed.isBlank() -> Spacer(Modifier.height(4.dp))

                trimmed.startsWith("#### ") -> {
                    Text(
                        text = parseMarkdownInline(trimmed.removePrefix("#### ").trim(), primaryColor, onSurfaceColor, codeBgColor),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = onSurfaceVariantColor,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                trimmed.startsWith("### ") -> {
                    Text(
                        text = parseMarkdownInline(trimmed.removePrefix("### ").trim(), primaryColor, onSurfaceColor, codeBgColor),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = onSurfaceColor,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                trimmed.startsWith("## ") -> {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            text = parseMarkdownInline(trimmed.removePrefix("## ").trim(), primaryColor, onSurfaceColor, codeBgColor),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                    }
                }
                trimmed.startsWith("# ") -> {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            text = parseMarkdownInline(trimmed.removePrefix("# ").trim(), primaryColor, onSurfaceColor, codeBgColor),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = primaryColor
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        )
                    }
                }

                trimmed.startsWith("> ") || trimmed.startsWith(">") -> {
                    val quoteText = trimmed.removePrefix(">").trim()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = primaryColor.copy(alpha = 0.7f),
                            modifier = Modifier
                                .width(3.5.dp)
                                .height(22.dp)
                        ) {}
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = parseMarkdownInline(quoteText, primaryColor, onSurfaceVariantColor, codeBgColor),
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = onSurfaceVariantColor
                        )
                    }
                }

                trimmed == "---" || trimmed == "***" || trimmed == "___" -> {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }

                trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ") -> {
                    val content = trimmed.substring(2).trim()
                    val leadingSpaces = rawLine.takeWhile { it == ' ' }.length
                    val indent = (leadingSpaces / 2 * 12).dp
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = indent),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = primaryColor,
                            modifier = Modifier
                                .padding(top = 7.dp)
                                .size(5.dp)
                        ) {}
                        Text(
                            text = parseMarkdownInline(content, primaryColor, onSurfaceColor, codeBgColor),
                            style = MaterialTheme.typography.bodyMedium,
                            color = onSurfaceColor
                        )
                    }
                }

                trimmed.matches(Regex("""^\d+\.\s+.*""")) -> {
                    val numPrefix = trimmed.substringBefore(".") + "."
                    val content = trimmed.substringAfter(".").trim()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = numPrefix,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        Text(
                            text = parseMarkdownInline(content, primaryColor, onSurfaceColor, codeBgColor),
                            style = MaterialTheme.typography.bodyMedium,
                            color = onSurfaceColor
                        )
                    }
                }

                else -> {
                    Text(
                        text = parseMarkdownInline(trimmed, primaryColor, onSurfaceColor, codeBgColor),
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurfaceColor
                    )
                }
            }
        }

        if (inCodeBlock && codeBlockLines.isNotEmpty()) {
            val codeContent = codeBlockLines.joinToString("\n")
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = codeContent,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = onSurfaceColor,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

/** Parses GitHub inline markdown into an AnnotatedString with formatting spans. */
private fun parseMarkdownInline(
    text: String,
    primaryColor: Color,
    onSurfaceColor: Color,
    codeBgColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        val pattern = Regex("""(\[(.*?)\]\((.*?)\)|`([^`]+)`|\*\*\*([^*]+)\*\*\*|\*\*([^*]+)\*\*|__([^_]+)__|(?<!\*)\*([^*]+)\*(?!\*)|(?<!_)_([^_]+)_(?!_)|~~([^~]+)~~|(#[0-9]+|@[a-zA-Z0-9_/-]+))""")
        var currentIndex = 0
        val matches = pattern.findAll(text)

        for (match in matches) {
            val start = match.range.first
            val end = match.range.last + 1

            if (start > currentIndex) {
                append(text.substring(currentIndex, start))
            }

            val fullMatch = match.value
            when {
                fullMatch.startsWith("[") && fullMatch.contains("](") -> {
                    val label = match.groupValues[2]
                    pushStyle(SpanStyle(color = primaryColor, textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Medium))
                    append(label)
                    pop()
                }
                fullMatch.startsWith("`") && fullMatch.endsWith("`") -> {
                    val codeText = match.groupValues[4]
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = codeBgColor,
                            color = primaryColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    append(" $codeText ")
                    pop()
                }
                fullMatch.startsWith("***") && fullMatch.endsWith("***") -> {
                    val content = match.groupValues[5]
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic))
                    append(content)
                    pop()
                }
                (fullMatch.startsWith("**") && fullMatch.endsWith("**")) || (fullMatch.startsWith("__") && fullMatch.endsWith("__")) -> {
                    val content = if (match.groupValues[6].isNotEmpty()) match.groupValues[6] else match.groupValues[7]
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(content)
                    pop()
                }
                (fullMatch.startsWith("*") && fullMatch.endsWith("*")) || (fullMatch.startsWith("_") && fullMatch.endsWith("_")) -> {
                    val content = if (match.groupValues[8].isNotEmpty()) match.groupValues[8] else match.groupValues[9]
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(content)
                    pop()
                }
                fullMatch.startsWith("~~") && fullMatch.endsWith("~~") -> {
                    val content = match.groupValues[10]
                    pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                    append(content)
                    pop()
                }
                fullMatch.startsWith("#") || fullMatch.startsWith("@") -> {
                    pushStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.SemiBold))
                    append(fullMatch)
                    pop()
                }
                else -> {
                    append(fullMatch)
                }
            }
            currentIndex = end
        }

        if (currentIndex < text.length) {
            append(text.substring(currentIndex))
        }
    }
}

// ─── Permission-to-download confirmation dialog ────────────────────────────

@Composable
private fun PermissionToDownloadDialog(
    currentVersion: String,
    latestVersion: String,
    readyToInstall: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        icon = {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (readyToInstall) Icons.Default.InstallMobile else Icons.Default.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
        title = {
            Text(
                if (readyToInstall) "Install Update?" else "Download Update?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                if (readyToInstall)
                    "Ever Dialer v$latestVersion has already been downloaded. Would you like to install it now?"
                else
                    "Ever Dialer v$latestVersion is available (you have v$currentVersion). The APK will be downloaded to your Downloads folder."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(if (readyToInstall) "Install Now" else "Download")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Not Now")
            }
        }
    )
}

@Composable
private fun DownloadingDialog(latestVersion: String, progress: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(250),
        label = "dlProgress"
    )

    AlertDialog(
        onDismissRequest = {},
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        shape = RoundedCornerShape(28.dp),
        icon = {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Downloading,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
        title = { Text("Downloading v$latestVersion", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50)),
                    strokeCap = StrokeCap.Round,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${(animatedProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Installing when ready…",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {}
    )
}

// ─── Compare release notes bottom sheet ─────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompareReleaseNotesSheet(
    installedVersion: String,
    installedNotes: String?,
    latestVersion: String?,
    latestNotes: String?,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Installed, 1 = Latest

    fun closeWithAnimation() {
        scope.launch {
            sheetState.hide()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Compare Release Notes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = { closeWithAnimation() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val hasLatest = latestVersion != null
            if (hasLatest) {
                ExpressiveSegmentedTab(
                    options = listOf("Installed · v$installedVersion", "Latest · v$latestVersion"),
                    selectedIndex = selectedTab,
                    onSelect = { selectedTab = it }
                )
            } else {
                Text(
                    "Showing release notes for your installed version.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedContent(
                targetState = if (hasLatest) selectedTab else 0,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInVertically { it / 4 } + fadeIn(tween(200))) togetherWith
                            (slideOutVertically { -it / 4 } + fadeOut(tween(150)))
                    } else {
                        (slideInVertically { -it / 4 } + fadeIn(tween(200))) togetherWith
                            (slideOutVertically { it / 4 } + fadeOut(tween(150)))
                    }
                },
                label = "compareContent"
            ) { tab ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .heightIn(min = 160.dp, max = 380.dp)
                            .padding(18.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        SelectionContainer {
                            ReleaseNotesText(if (tab == 0) installedNotes else latestNotes)
                        }
                    }
                }
            }

            Button(
                onClick = { closeWithAnimation() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ExpressiveSegmentedTab(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val bg by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                animationSpec = tween(220),
                label = "tabBg"
            )
            val fg by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(220),
                label = "tabFg"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = fg,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}
