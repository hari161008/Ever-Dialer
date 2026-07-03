package com.coolappstore.evercallrecorder.by.svhp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels.HomeViewModel
import com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels.PlaybackViewModel
import com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels.RecordingItem
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackScreen(
    recording: RecordingItem,
    onBack: () -> Unit,
    highlightQuery: String = "",
    modifier: Modifier = Modifier
) {
    val vm: PlaybackViewModel = viewModel()
    val homeVm: HomeViewModel = viewModel()
    val context = LocalContext.current
    val isPlaying by vm.isPlaying.collectAsState()
    val position by vm.currentPosition.collectAsState()
    val duration by vm.duration.collectAsState()
    val note by vm.note.collectAsState()

    LaunchedEffect(recording.uri) { vm.load(recording.uri) }
    DisposableEffect(Unit) { onDispose { vm.resetOnLeave() } }
    BackHandler { onBack() }

    val title = recording.contactName ?: recording.phoneNumber
    val subtitle = recording.contactName?.let { recording.phoneNumber } ?: ""
    val dateStr = recording.date?.let { SimpleDateFormat("MMMM d, yyyy • hh:mm a", Locale.getDefault()).format(it) } ?: ""
    val isIncoming  = recording.direction == "in"
    // accentColor is theme-driven and the same for both directions — direction is shown via badge
    val accentColor = MaterialTheme.colorScheme.primary

    // Load contact photo
    var photoBitmap by remember(recording.phoneNumber) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(recording.phoneNumber) {
        photoBitmap = homeVm.loadContactPhoto(context, recording.phoneNumber)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Recording", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Call info card ────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(modifier = Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        if (photoBitmap != null) {
                            Image(
                                bitmap = photoBitmap!!,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = title.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        if (subtitle.isNotBlank()) Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (dateStr.isNotBlank()) { Spacer(Modifier.height(2.dp)); Text(text = dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = accentColor.copy(alpha = 0.12f)) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(imageVector = if (isIncoming) Icons.Rounded.CallReceived else Icons.Rounded.CallMade, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                            Text(text = if (isIncoming) "Incoming" else "Outgoing", style = MaterialTheme.typography.labelSmall, color = accentColor, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // ── Player card ───────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    PlayerVisualizer(isPlaying = isPlaying, accentColor = accentColor)

                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        var isSeeking by remember { mutableStateOf(false) }
                        var seekValue by remember { mutableFloatStateOf(0f) }

                        val sliderPosition = if (isSeeking) seekValue
                            else if (duration > 0) position / duration.toFloat()
                            else 0f

                        Slider(
                            value = sliderPosition.coerceIn(0f, 1f),
                            onValueChange = { newVal ->
                                if (!isSeeking) seekValue = sliderPosition
                                isSeeking = true
                                seekValue = newVal
                            },
                            onValueChangeFinished = {
                                vm.seekTo((seekValue * duration).toLong())
                                isSeeking = false
                            },
                            colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor, inactiveTrackColor = accentColor.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = formatMs(if (isSeeking) (seekValue * duration).toLong() else position), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = formatMs(duration), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(onClick = { vm.seekBack() }, modifier = Modifier.size(52.dp), colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                            Icon(Icons.Rounded.Replay5, contentDescription = "Back 5s", modifier = Modifier.size(26.dp))
                        }
                        FilledIconButton(onClick = { vm.togglePlayPause() }, modifier = Modifier.size(68.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = accentColor)) {
                            Icon(imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = if (isPlaying) "Pause" else "Play", modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        FilledTonalIconButton(onClick = { vm.seekForward() }, modifier = Modifier.size(52.dp), colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                            Icon(Icons.Rounded.Forward5, contentDescription = "Forward 5s", modifier = Modifier.size(26.dp))
                        }
                    }
                }
            }

            // ── Notes card ────────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Rounded.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text(text = "Notes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        if (highlightQuery.isNotBlank() && note.lowercase().contains(highlightQuery.lowercase())) {
                            Spacer(Modifier.width(4.dp))
                            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) {
                                Text(
                                    text = "match",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = note,
                        onValueChange = { vm.updateNote(it) },
                        placeholder = {
                            Text("Add notes about this call…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        )
                    )
                    // Highlighted note view — shown when there's a search query match
                    if (highlightQuery.isNotBlank() && note.isNotBlank()) {
                        val primary = MaterialTheme.colorScheme.primary
                        val onSurface = MaterialTheme.colorScheme.onSurface
                        val highlighted = remember(note, highlightQuery) { buildHighlightedText(note, highlightQuery, primary, onSurface) }
                        if (highlighted != null) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            Text(text = "Preview with highlights:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = highlighted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun buildHighlightedText(
    text: String,
    query: String,
    highlightColor: Color,
    textColor: Color
): androidx.compose.ui.text.AnnotatedString? {
    if (query.isBlank()) return null
    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()
    if (!lowerText.contains(lowerQuery)) return null
    return buildAnnotatedString {
        var start = 0
        while (true) {
            val idx = lowerText.indexOf(lowerQuery, start)
            if (idx == -1) {
                withStyle(SpanStyle(color = textColor)) { append(text.substring(start)) }
                break
            }
            withStyle(SpanStyle(color = textColor)) { append(text.substring(start, idx)) }
            withStyle(SpanStyle(color = highlightColor, fontWeight = FontWeight.Bold, background = highlightColor.copy(alpha = 0.18f))) {
                append(text.substring(idx, idx + query.length))
            }
            start = idx + query.length
        }
    }
}

@Composable
private fun PlayerVisualizer(isPlaying: Boolean, accentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "viz")
    val barCount = 28

    // Each bar has its own target height that changes based on isPlaying
    // animateFloatAsState gives smooth transition when isPlaying toggles
    val idleHeights = remember { List(barCount) { index -> 0.08f + (index % 5) * 0.035f } }

    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
    ) {
        repeat(barCount) { index ->
            val groupOffset  = (index % 5) * 130
            val baseDuration = 700 + (index % 8) * 90

            val playingHeight by infiniteTransition.animateFloat(
                initialValue = 0.12f,
                targetValue  = 1f,
                animationSpec = infiniteRepeatable(
                    animation  = tween(durationMillis = baseDuration, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(groupOffset + index * 22)
                ),
                label = "bar$index"
            )

            val targetHeight = if (isPlaying) playingHeight else idleHeights[index]

            val smoothHeight by animateFloatAsState(
                targetValue  = targetHeight,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                label = "smooth$index"
            )

            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight(smoothHeight.coerceIn(0.05f, 1f))
                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp, bottomStart = 1.5.dp, bottomEnd = 1.5.dp))
                    .background(accentColor.copy(alpha = (0.5f + smoothHeight * 0.5f).coerceIn(0.5f, 1f)))
            )
        }
    }
}

private fun formatMs(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val mins = TimeUnit.MILLISECONDS.toMinutes(ms)
    val secs = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return "%d:%02d".format(mins, secs)
}
