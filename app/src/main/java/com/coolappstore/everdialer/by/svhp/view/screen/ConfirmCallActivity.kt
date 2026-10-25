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
                    onConfirm = { chosenHandle ->
                        makeCall(this, number, chosenHandle, skipConfirm = true)
                        finish()
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }

    private var touchDownX = 0f
    private var touchDownY = 0f
    private var pendingTouchHapticFired = false
    private val touchSlop by lazy { android.view.ViewConfiguration.get(this).scaledTouchSlop }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        if (ev != null) {
            val touchOnlyEnabled = prefs.getBoolean(PreferenceManager.KEY_HAPTIC_TOUCH_ONLY, false)
            val appHapticsEnabled = prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)

            if (appHapticsEnabled) {
                when (ev.actionMasked) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        touchDownX = ev.x
                        touchDownY = ev.y
                        pendingTouchHapticFired = false
                        if (touchOnlyEnabled) {
                            val strength = prefs.getString(PreferenceManager.KEY_APP_HAPTICS_STRENGTH, "light") ?: "light"
                            val customIntensity = prefs.getFloat(PreferenceManager.KEY_HAPTICS_CUSTOM_INTENSITY, 0.5f)
                            com.coolappstore.everdialer.by.svhp.view.components.performAppHaptic(this, strength, customIntensity, fromDirectTouch = true)
                            pendingTouchHapticFired = true
                        }
                    }
                    android.view.MotionEvent.ACTION_MOVE -> {
                        val dx = ev.x - touchDownX
                        val dy = ev.y - touchDownY
                        if (dx * dx + dy * dy > touchSlop * touchSlop) {
                            pendingTouchHapticFired = true
                        }
                    }
                    android.view.MotionEvent.ACTION_UP -> {
                        if (!pendingTouchHapticFired) {
                            val dx = ev.x - touchDownX
                            val dy = ev.y - touchDownY
                            if (dx * dx + dy * dy <= touchSlop * touchSlop) {
                                val strength = prefs.getString(PreferenceManager.KEY_APP_HAPTICS_STRENGTH, "light") ?: "light"
                                val customIntensity = prefs.getFloat(PreferenceManager.KEY_HAPTICS_CUSTOM_INTENSITY, 0.5f)
                                com.coolappstore.everdialer.by.svhp.view.components.performAppHaptic(this, strength, customIntensity, fromDirectTouch = true)
                            }
                        }
                    }
                    android.view.MotionEvent.ACTION_CANCEL -> {
                        pendingTouchHapticFired = true
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}

@Composable
private fun ConfirmCallDialogUi(
    number: String,
    accountHandle: PhoneAccountHandle?,
    prefs: PreferenceManager,
    onConfirm: (PhoneAccountHandle?) -> Unit,
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

    val telecomManager = remember { context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager }
    val accounts = remember {
        try { telecomManager?.callCapablePhoneAccounts ?: emptyList() } catch (_: Throwable) { emptyList() }
    }
    val defaultSim = remember { prefs.getInt(PreferenceManager.KEY_DEFAULT_SIM, 1) }
    var selectedSimSlot by remember(accountHandle, accounts) {
        val initialSlot = if (accountHandle != null) {
            val idx = accounts.indexOf(accountHandle)
            if (idx in 0..1) idx + 1 else defaultSim
        } else {
            defaultSim
        }
        mutableIntStateOf(initialSlot)
    }

    val sim1Color = remember { Color(prefs.getInt(PreferenceManager.KEY_SIM1_COLOR, PreferenceManager.DEFAULT_SIM1_COLOR)) }
    val sim2Color = remember { Color(prefs.getInt(PreferenceManager.KEY_SIM2_COLOR, PreferenceManager.DEFAULT_SIM2_COLOR)) }
    val hasTwoSims = accounts.size >= 2

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

                Spacer(Modifier.height(16.dp))

                if (hasTwoSims) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // SIM 1 Selector
                        Surface(
                            onClick = { selectedSimSlot = 1 },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selectedSimSlot == 1) sim1Color else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedSimSlot == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            border = if (selectedSimSlot == 1) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            tonalElevation = if (selectedSimSlot == 1) 4.dp else 0.dp,
                            modifier = Modifier.height(38.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SimCardIconWithNumber(
                                    simSlotNumber = "1",
                                    tint = if (selectedSimSlot == 1) Color.White else sim1Color,
                                    isLarge = false
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "SIM 1",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // SIM 2 Selector
                        Surface(
                            onClick = { selectedSimSlot = 2 },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selectedSimSlot == 2) sim2Color else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedSimSlot == 2) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            border = if (selectedSimSlot == 2) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            tonalElevation = if (selectedSimSlot == 2) 4.dp else 0.dp,
                            modifier = Modifier.height(38.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SimCardIconWithNumber(
                                    simSlotNumber = "2",
                                    tint = if (selectedSimSlot == 2) Color.White else sim2Color,
                                    isLarge = false
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "SIM 2",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    val activeColor = if (selectedSimSlot == 2) sim2Color else sim1Color
                    val slotNumber = if (selectedSimSlot == 2) "2" else "1"
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = activeColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.5.dp, activeColor.copy(alpha = 0.45f)),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SimCardIconWithNumber(
                                simSlotNumber = slotNumber,
                                tint = activeColor,
                                isLarge = false
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "SIM $slotNumber",
                                style = MaterialTheme.typography.labelMedium,
                                color = activeColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(26.dp))

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
                        onClick = {
                            val targetHandle = if (accounts.isNotEmpty()) {
                                com.coolappstore.everdialer.by.svhp.controller.util.getPhoneAccountForSimSlot(
                                    context, accounts, if (selectedSimSlot == 2) 1 else 0
                                )
                            } else accountHandle
                            onConfirm(targetHandle)
                        },
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
