package com.coolappstore.everdialer.by.svhp.controller

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.coolappstore.everdialer.by.svhp.controller.util.NoteManager
import com.coolappstore.everdialer.by.svhp.view.theme.Rivo4Theme

class FloatingNotesService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private val lifecycleOwner = ServiceLifecycleOwner()

    companion object {
        const val EXTRA_CONTACT_NAME = "contact_name"
        const val EXTRA_PHONE_NUMBER = "phone_number"

        fun start(context: Context, contactName: String, phoneNumber: String) {
            context.startService(
                Intent(context, FloatingNotesService::class.java).apply {
                    putExtra(EXTRA_CONTACT_NAME, contactName)
                    putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
                }
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        lifecycleOwner.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        val name   = intent?.getStringExtra(EXTRA_CONTACT_NAME) ?: "Unknown"
        val number = intent?.getStringExtra(EXTRA_PHONE_NUMBER) ?: ""
        removeOverlay()
        showOverlay(name, number)
        return START_NOT_STICKY
    }

    private fun showOverlay(contactName: String, phoneNumber: String) {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }

        val cv = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                Rivo4Theme {
                    FloatingNoteOverlay(
                        contactName = contactName,
                        phoneNumber = phoneNumber,
                        onDismiss   = { removeOverlay(); stopSelf() }
                    )
                }
            }
        }
        overlayView = cv
        try { windowManager.addView(cv, params) } catch (_: Exception) { stopSelf() }
    }

    private fun removeOverlay() {
        try { overlayView?.let { windowManager.removeViewImmediate(it) } } catch (_: Exception) {}
        overlayView = null
    }

    override fun onDestroy() {
        lifecycleOwner.onDestroy()
        removeOverlay()
        super.onDestroy()
    }
}

@Composable
private fun FloatingNoteOverlay(
    contactName: String,
    phoneNumber: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf(NoteManager.readNote(context, contactName, phoneNumber)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = {
                NoteManager.writeNote(context, contactName, phoneNumber, text)
                onDismiss()
            }),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clickable(onClick = { /* consume – don't dismiss via scrim */ })
                .clip(RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            shadowElevation = 16.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = contactName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (phoneNumber.isNotEmpty()) {
                            Text(
                                text = phoneNumber,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = {
                            NoteManager.writeNote(context, contactName, phoneNumber, text)
                            onDismiss()
                        }) {
                            Icon(Icons.Default.Save, "Save", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = {
                            NoteManager.writeNote(context, contactName, phoneNumber, text)
                            onDismiss()
                        }) {
                            Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 130.dp, max = 280.dp),
                    placeholder = { Text("Type your note here…") },
                    shape = RoundedCornerShape(14.dp),
                    minLines = 5
                )

                Button(
                    onClick = {
                        NoteManager.writeNote(context, contactName, phoneNumber, text)
                        onDismiss()
                    },
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Save & Close") }
            }
        }
    }
}
