package com.coolappstore.everdialer.by.svhp.view.screen

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import coil.compose.AsyncImage
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.makeCall
import com.coolappstore.everdialer.by.svhp.view.theme.Rivo4Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

class ConfirmCallActivity : FragmentActivity() {

    companion object {
        const val EXTRA_NUMBER = "extra_confirm_call_number"
        const val EXTRA_ACCOUNT_HANDLE = "extra_confirm_call_account_handle"
    }

    private val prefs: PreferenceManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val number = intent?.getStringExtra(EXTRA_NUMBER)?.trim().orEmpty()
        if (number.isEmpty()) {
            finish()
            return
        }

        val accountHandle: PhoneAccountHandle? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_ACCOUNT_HANDLE, PhoneAccountHandle::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_ACCOUNT_HANDLE)
        }

        setContent {
            Rivo4Theme {
                ConfirmCallDialogUi(
                    number = number,
                    accountHandle = accountHandle,
                    prefs = prefs,
                    onConfirm = {
                        makeCall(this, number, accountHandle, skipConfirm = true)
                        finish()
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }
}

@Composable
private fun ConfirmCallDialogUi(
    number: String,
    accountHandle: PhoneAccountHandle?,
    prefs: PreferenceManager,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var contactName by remember { mutableStateOf<String?>(null) }
    var photoUri by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(number) {
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
                context.contentResolver.query(
                    uri,
                    arrayOf(
                        ContactsContract.PhoneLookup.DISPLAY_NAME,
                        ContactsContract.PhoneLookup.PHOTO_URI,
                        ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI
                    ),
                    null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                        val photoIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI)
                        val thumbIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI)
                        if (nameIdx >= 0) contactName = cursor.getString(nameIdx)
                        if (photoIdx >= 0) photoUri = cursor.getString(photoIdx)
                            ?: if (thumbIdx >= 0) cursor.getString(thumbIdx) else null
                    }
                }
            } catch (_: Throwable) {}
        }
    }

    val (simLabel, simColor) = remember(accountHandle) {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        val accounts = try { telecomManager?.callCapablePhoneAccounts } catch (_: Throwable) { null } ?: emptyList()
        val slotIndex = if (accountHandle != null) accounts.indexOf(accountHandle) else -1
        when (slotIndex) {
            0 -> Pair("SIM 1", Color(prefs.getInt(PreferenceManager.KEY_SIM1_COLOR, PreferenceManager.DEFAULT_SIM1_COLOR)))
            1 -> Pair("SIM 2", Color(prefs.getInt(PreferenceManager.KEY_SIM2_COLOR, PreferenceManager.DEFAULT_SIM2_COLOR)))
            else -> {
                if (accounts.size == 1) {
                    Pair("SIM 1", Color(prefs.getInt(PreferenceManager.KEY_SIM1_COLOR, PreferenceManager.DEFAULT_SIM1_COLOR)))
                } else {
                    Pair<String?, Color?>(null, null)
                }
            }
        }
    }

    BackHandler { onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .wrapContentHeight()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {},
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            tonalElevation = 6.dp,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header badge
                Surface(
                    shape = RoundedCornerShape(100),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Confirm placing call",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Contact Avatar with Material You expressive squircle shape
                Surface(
                    modifier = Modifier.size(84.dp),
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    tonalElevation = 2.dp,
                    shadowElevation = 4.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!photoUri.isNullOrEmpty()) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = contactName,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(26.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else if (!contactName.isNullOrEmpty()) {
                            Text(
                                text = contactName!!.trim().take(1).uppercase(),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(42.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    text = contactName ?: number,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (!contactName.isNullOrEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = number,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                if (simLabel != null) {
                    Spacer(Modifier.height(14.dp))
                    val badgeColor = simColor ?: MaterialTheme.colorScheme.tertiary
                    Surface(
                        shape = RoundedCornerShape(100),
                        color = badgeColor.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.SimCard, null, modifier = Modifier.size(14.dp), tint = badgeColor)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = simLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = badgeColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))

                // Action Buttons using Material You dynamic colors & pill shapes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Call",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
