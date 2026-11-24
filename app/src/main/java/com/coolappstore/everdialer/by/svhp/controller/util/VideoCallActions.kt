package com.coolappstore.everdialer.by.svhp.controller.util

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.telecom.PhoneAccount
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.content.ContextCompat
import com.coolappstore.everdialer.by.svhp.controller.CallService

// ── Video calling ────────────────────────────────────────────────────────────
// One place that answers "what can I video call this person with?" and then actually
// starts that call. Ported from Rivo's VideoCallHelper: it adds the carrier (ViLTE)
// video call plus Signal, Viber, Skype, WeChat, LINE, imo and Zoom, which Ever Dialer
// previously had no way to reach. WhatsApp / WhatsApp Business / Telegram / Google Meet
// reuse the existing SocialAppActions implementations so both entry points behave the same.

const val SIGNAL_PACKAGE = "org.thoughtcrime.securesms"
const val VIBER_PACKAGE = "com.viber.voip"
const val SKYPE_PACKAGE = "com.skype.raider"
const val WECHAT_PACKAGE = "com.tencent.mm"
const val LINE_PACKAGE = "jp.naver.line.android"
const val IMO_PACKAGE = "com.imo.android.imoim"
const val ZOOM_PACKAGE = "us.zoom.videomeetings"

/** Older package name Google Meet (then Duo) shipped under; still present on some devices. */
const val GOOGLE_MEET_LEGACY_PACKAGE = "com.google.android.apps.meetings"

/** One way to place a video call: the carrier network, or a specific installed app. */
data class VideoAppOption(
    val id: String,
    val name: String,
    val packageName: String?,
    val icon: ImageBitmap?,
    val description: String
)

private fun appIcon(context: Context, pkg: String?): ImageBitmap? {
    if (pkg == null) return null
    return try {
        drawableToImageBitmap(context.packageManager.getApplicationIcon(pkg))
    } catch (_: Exception) { null }
}

private fun appLabel(context: Context, pkg: String, fallback: String): String =
    try {
        context.packageManager.getApplicationLabel(
            context.packageManager.getApplicationInfo(pkg, 0)
        ).toString()
    } catch (_: Exception) { fallback }

private fun hasReadContacts(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
        PackageManager.PERMISSION_GRANTED

private fun hasCallPhone(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
        PackageManager.PERMISSION_GRANTED

/** Full international digits for [phoneNumber] (no "+"), so apps whose deep links require E.164
 *  still resolve a number typed locally. Falls back to the plain digits. */
private fun e164Digits(context: Context, phoneNumber: String): String {
    val trimmed = phoneNumber.trim()
    if (trimmed.isEmpty()) return ""
    val digits = trimmed.filter { it.isDigit() }
    if (digits.isEmpty()) return ""
    if (trimmed.startsWith("+")) return digits
    val countryIso = try {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        (tm?.networkCountryIso?.takeIf { it.isNotBlank() }
            ?: tm?.simCountryIso?.takeIf { it.isNotBlank() }
            ?: context.resources.configuration.locales[0]?.country?.takeIf { it.isNotBlank() })?.uppercase()
    } catch (_: Exception) { null }
    if (countryIso != null) {
        val e164 = try { PhoneNumberUtils.formatNumberToE164(digits, countryIso) } catch (_: Exception) { null }
        if (!e164.isNullOrBlank()) return e164.filter { it.isDigit() }
    }
    return digits
}

/** Resolves the saved contact id for [phoneNumber], so app-specific call rows can be looked up. */
private fun resolveContactId(context: Context, phoneNumber: String): String? {
    if (!hasReadContacts(context)) return null
    val trimmed = phoneNumber.trim()
    if (trimmed.isEmpty()) return null
    return try {
        val lookupUri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(trimmed)
        )
        context.contentResolver.query(
            lookupUri,
            arrayOf(ContactsContract.PhoneLookup.CONTACT_ID),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst() && cursor.getColumnIndex(ContactsContract.PhoneLookup.CONTACT_ID) >= 0) {
                cursor.getLong(cursor.getColumnIndex(ContactsContract.PhoneLookup.CONTACT_ID)).toString()
            } else null
        }
    } catch (_: Exception) { null }
}

/**
 * Finds the ContactsContract.Data row an app registered for this contact as a direct call
 * shortcut — the same rows the stock Contacts app turns into "Voice call" / "Video call" quick
 * actions. Firing ACTION_VIEW on one starts a real call inside that app, instead of only opening
 * its home screen. Prefers a row whose MIME type mentions "video". Returns null when the contact
 * isn't synced with that app, in which case callers fall back to a deep link or opening the app.
 */
private fun findCallDataRow(
    context: Context,
    phoneNumber: String,
    mimeTypes: List<String>
): Pair<Uri, String>? {
    if (mimeTypes.isEmpty() || !hasReadContacts(context)) return null
    val contactId = resolveContactId(context, phoneNumber) ?: return null
    val placeholders = mimeTypes.joinToString(", ") { "?" }
    val selection = "${ContactsContract.Data.CONTACT_ID} = ? AND " +
        "${ContactsContract.Data.MIMETYPE} IN ($placeholders)"
    val args = arrayOf(contactId) + mimeTypes.toTypedArray()
    return try {
        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data._ID, ContactsContract.Data.MIMETYPE),
            selection, args, null
        )?.use { cursor ->
            var fallbackId: Long? = null
            var fallbackMime: String? = null
            while (cursor.moveToNext()) {
                val dataId = cursor.getLong(0)
                val mime = cursor.getString(1) ?: continue
                if (mime.contains("video", ignoreCase = true)) {
                    return ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, dataId) to mime
                }
                if (fallbackId == null) {
                    fallbackId = dataId
                    fallbackMime = mime
                }
            }
            val id = fallbackId
            val mime = fallbackMime
            if (id != null && mime != null) {
                ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, id) to mime
            } else null
        }
    } catch (_: Exception) { null }
}

private fun startDataRowIntent(context: Context, row: Pair<Uri, String>, pkg: String): Boolean = try {
    context.startActivity(
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(row.first, row.second)
            setPackage(pkg)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
    true
} catch (_: Exception) { false }

private fun openApp(context: Context, pkg: String): Boolean = try {
    val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
    true
} catch (_: Exception) { false }

/** Every video call route available on this device, carrier first (always present), then each
 *  installed app that can place a call to a phone number. */
fun getAvailableVideoApps(context: Context): List<VideoAppOption> {
    val list = mutableListOf<VideoAppOption>()

    // 1. Carrier / system video call (ViLTE) — always available, no app required.
    list.add(
        VideoAppOption(
            id = "system",
            name = "Phone video call",
            packageName = null,
            icon = null,
            description = "Carrier video call over the mobile network"
        )
    )

    // 2. WhatsApp or WhatsApp Business
    val waPkg = when {
        isPackageInstalled(context, WHATSAPP_PACKAGE) -> WHATSAPP_PACKAGE
        isPackageInstalled(context, WHATSAPP_BUSINESS_PACKAGE) -> WHATSAPP_BUSINESS_PACKAGE
        else -> null
    }
    if (waPkg != null) {
        list.add(
            VideoAppOption(
                id = "whatsapp",
                name = appLabel(context, waPkg, "WhatsApp"),
                packageName = waPkg,
                icon = appIcon(context, waPkg),
                description = "Video call through WhatsApp"
            )
        )
    }

    // 3. WhatsApp Business, listed separately when both are installed
    if (isWhatsAppBusinessInstalled(context) && waPkg != WHATSAPP_BUSINESS_PACKAGE) {
        list.add(
            VideoAppOption(
                id = "whatsapp_business",
                name = appLabel(context, WHATSAPP_BUSINESS_PACKAGE, "WhatsApp Business"),
                packageName = WHATSAPP_BUSINESS_PACKAGE,
                icon = appIcon(context, WHATSAPP_BUSINESS_PACKAGE),
                description = "Video call through WhatsApp Business"
            )
        )
    }

    // 4. Google Meet (formerly Duo)
    val meetPkg = when {
        isPackageInstalled(context, GOOGLE_MEET_PACKAGE) -> GOOGLE_MEET_PACKAGE
        isPackageInstalled(context, GOOGLE_MEET_LEGACY_PACKAGE) -> GOOGLE_MEET_LEGACY_PACKAGE
        else -> null
    }
    if (meetPkg != null) {
        list.add(
            VideoAppOption(
                id = "meet",
                name = appLabel(context, meetPkg, "Google Meet"),
                packageName = meetPkg,
                icon = appIcon(context, meetPkg),
                description = "Video call through Google Meet"
            )
        )
    }

    // 5. Telegram (any installed client, official preferred)
    if (isTelegramInstalled(context)) {
        val tgPkg = if (isPackageInstalled(context, OFFICIAL_TELEGRAM_PACKAGE)) OFFICIAL_TELEGRAM_PACKAGE else null
        list.add(
            VideoAppOption(
                id = "telegram",
                name = tgPkg?.let { appLabel(context, it, "Telegram") } ?: "Telegram",
                packageName = tgPkg,
                icon = tgPkg?.let { appIcon(context, it) } ?: getTelegramIcon(context),
                description = "Video call through Telegram"
            )
        )
    }

    // 6. Signal
    if (isPackageInstalled(context, SIGNAL_PACKAGE)) {
        list.add(
            VideoAppOption(
                id = "signal",
                name = appLabel(context, SIGNAL_PACKAGE, "Signal"),
                packageName = SIGNAL_PACKAGE,
                icon = appIcon(context, SIGNAL_PACKAGE),
                description = "Video call through Signal"
            )
        )
    }

    // 7. WeChat
    if (isPackageInstalled(context, WECHAT_PACKAGE)) {
        list.add(
            VideoAppOption(
                id = "wechat",
                name = appLabel(context, WECHAT_PACKAGE, "WeChat"),
                packageName = WECHAT_PACKAGE,
                icon = appIcon(context, WECHAT_PACKAGE),
                description = "Video call through WeChat"
            )
        )
    }

    // 8. LINE
    if (isPackageInstalled(context, LINE_PACKAGE)) {
        list.add(
            VideoAppOption(
                id = "line",
                name = appLabel(context, LINE_PACKAGE, "LINE"),
                packageName = LINE_PACKAGE,
                icon = appIcon(context, LINE_PACKAGE),
                description = "Video call through LINE"
            )
        )
    }

    // 9. Viber
    if (isPackageInstalled(context, VIBER_PACKAGE)) {
        list.add(
            VideoAppOption(
                id = "viber",
                name = appLabel(context, VIBER_PACKAGE, "Viber"),
                packageName = VIBER_PACKAGE,
                icon = appIcon(context, VIBER_PACKAGE),
                description = "Video call through Viber"
            )
        )
    }

    // 10. Skype
    if (isPackageInstalled(context, SKYPE_PACKAGE)) {
        list.add(
            VideoAppOption(
                id = "skype",
                name = appLabel(context, SKYPE_PACKAGE, "Skype"),
                packageName = SKYPE_PACKAGE,
                icon = appIcon(context, SKYPE_PACKAGE),
                description = "Video call through Skype"
            )
        )
    }

    // 11. imo
    if (isPackageInstalled(context, IMO_PACKAGE)) {
        list.add(
            VideoAppOption(
                id = "imo",
                name = appLabel(context, IMO_PACKAGE, "imo"),
                packageName = IMO_PACKAGE,
                icon = appIcon(context, IMO_PACKAGE),
                description = "Video call through imo"
            )
        )
    }

    // 12. Zoom — no phone-number calling, so it only opens the app.
    if (isPackageInstalled(context, ZOOM_PACKAGE)) {
        list.add(
            VideoAppOption(
                id = "zoom",
                name = appLabel(context, ZOOM_PACKAGE, "Zoom"),
                packageName = ZOOM_PACKAGE,
                icon = appIcon(context, ZOOM_PACKAGE),
                description = "Start a meeting in Zoom"
            )
        )
    }

    return list
}

/** Places a video call to [phoneNumber] through the route chosen by [optionId]. */
fun launchVideoCall(context: Context, optionId: String, phoneNumber: String) {
    when (optionId) {
        "system" -> startCarrierVideoCall(context, phoneNumber)
        "whatsapp" -> startWhatsAppVideoCall(context, phoneNumber)
        "whatsapp_business" -> startWhatsAppBusinessVideoCall(context, phoneNumber)
        "meet" -> startGoogleMeetVideoCall(context, phoneNumber)
        "telegram" -> startTelegramVideoCall(context, phoneNumber)
        "signal" -> startSignalVideoCall(context, phoneNumber)
        "wechat" -> startWeChatVideoCall(context, phoneNumber)
        "line" -> startLineVideoCall(context, phoneNumber)
        "viber" -> startViberVideoCall(context, phoneNumber)
        "skype" -> startSkypeVideoCall(context, phoneNumber)
        "imo" -> startImoVideoCall(context, phoneNumber)
        "zoom" -> startZoomVideoCall(context)
        else -> startCarrierVideoCall(context, phoneNumber)
    }
}

/**
 * Starts a carrier video call (ViLTE). If a call with this number is already connected, that call
 * is upgraded to two-way video instead of dialling a second time. Falls back from placeCall to
 * ACTION_DIAL (with the video state attached) when CALL_PHONE hasn't been granted.
 */
@Suppress("DEPRECATION")
fun startCarrierVideoCall(context: Context, phoneNumber: String): Boolean {
    val trimmed = phoneNumber.trim()
    if (trimmed.isEmpty()) {
        Toast.makeText(context, "No number to call", Toast.LENGTH_SHORT).show()
        return false
    }

    // Already talking to this person? Upgrade the existing call rather than starting another.
    try {
        val activeCall = CallService.currentCallSession.value?.call
        val videoCall = activeCall?.videoCall
        if (videoCall != null) {
            videoCall.sendSessionModifyRequest(VideoProfile(VideoProfile.STATE_BIDIRECTIONAL))
            return true
        }
    } catch (_: Exception) { /* no active call, or Telecom rejected it — place a new one */ }

    val uri = if (trimmed.contains("#")) {
        Uri.parse("tel:" + Uri.encode(trimmed))
    } else {
        Uri.fromParts("tel", trimmed, null)
    }

    val extras = Bundle().apply {
        putInt(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE, VideoProfile.STATE_BIDIRECTIONAL)
    }

    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
    if (telecomManager != null) {
        try {
            val videoAccount = telecomManager.callCapablePhoneAccounts.firstOrNull { handle ->
                val account = runCatching { telecomManager.getPhoneAccount(handle) }.getOrNull()
                account?.hasCapabilities(PhoneAccount.CAPABILITY_VIDEO_CALLING) == true
            }
            if (videoAccount != null) {
                extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, videoAccount)
            }
        } catch (_: Exception) { /* READ_PHONE_STATE missing — let Telecom pick the account */ }
    }

    if (hasCallPhone(context) && telecomManager != null) {
        try {
            telecomManager.placeCall(uri, extras)
            return true
        } catch (_: Exception) { /* fall through to the dialer */ }
    }

    return try {
        context.startActivity(
            Intent(Intent.ACTION_DIAL, uri).apply {
                putExtras(extras)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        true
    } catch (_: Exception) {
        Toast.makeText(context, "Couldn't start a video call", Toast.LENGTH_SHORT).show()
        false
    }
}

/** Signal video call via the contact's Signal call row, falling back to opening Signal. */
fun startSignalVideoCall(context: Context, phoneNumber: String): Boolean {
    if (!isPackageInstalled(context, SIGNAL_PACKAGE)) return false
    val row = findCallDataRow(
        context, phoneNumber,
        listOf(
            "vnd.android.cursor.item/vnd.org.thoughtcrime.securesms.call",
            "vnd.android.cursor.item/vnd.org.thoughtcrime.securesms.contact"
        )
    )
    if (row != null && startDataRowIntent(context, row, SIGNAL_PACKAGE)) return true
    return openApp(context, SIGNAL_PACKAGE)
}

/** WeChat video call via the contact's WeChat VoIP row, then weixin://, then the app itself. */
fun startWeChatVideoCall(context: Context, phoneNumber: String): Boolean {
    if (!isPackageInstalled(context, WECHAT_PACKAGE)) return false
    val row = findCallDataRow(
        context, phoneNumber,
        listOf(
            "vnd.android.cursor.item/vnd.com.tencent.mm.chatting.voip.video",
            "vnd.android.cursor.item/vnd.com.tencent.mm.chatting.voip",
            "vnd.android.cursor.item/vnd.com.tencent.mm.chatting.profile"
        )
    )
    if (row != null && startDataRowIntent(context, row, WECHAT_PACKAGE)) return true
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("weixin://")).apply {
                setPackage(WECHAT_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        return true
    } catch (_: Exception) { /* fall through */ }
    return openApp(context, WECHAT_PACKAGE)
}

/** LINE video call via the contact's LINE call row, then line://nv/call, then the app itself. */
fun startLineVideoCall(context: Context, phoneNumber: String): Boolean {
    if (!isPackageInstalled(context, LINE_PACKAGE)) return false
    val row = findCallDataRow(
        context, phoneNumber,
        listOf(
            "vnd.android.cursor.item/vnd.jp.naver.line.android.call",
            "vnd.android.cursor.item/vnd.jp.naver.line.android.profile"
        )
    )
    if (row != null && startDataRowIntent(context, row, LINE_PACKAGE)) return true
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("line://nv/call")).apply {
                setPackage(LINE_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        return true
    } catch (_: Exception) { /* fall through */ }
    return openApp(context, LINE_PACKAGE)
}

/** Viber video call via the contact's Viber row, then a viber:// call deep link, then the app. */
fun startViberVideoCall(context: Context, phoneNumber: String): Boolean {
    if (!isPackageInstalled(context, VIBER_PACKAGE)) return false
    val row = findCallDataRow(
        context, phoneNumber,
        listOf(
            "vnd.android.cursor.item/vnd.com.viber.voip.viber_number_call",
            "vnd.android.cursor.item/vnd.com.viber.voip.google_voice_message"
        )
    )
    if (row != null && startDataRowIntent(context, row, VIBER_PACKAGE)) return true
    val digits = e164Digits(context, phoneNumber)
    if (digits.isNotEmpty()) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("viber://call?number=%2B$digits")).apply {
                    setPackage(VIBER_PACKAGE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            return true
        } catch (_: Exception) { /* fall through */ }
    }
    return openApp(context, VIBER_PACKAGE)
}

/** Skype video call straight to the number. */
fun startSkypeVideoCall(context: Context, phoneNumber: String): Boolean {
    if (!isPackageInstalled(context, SKYPE_PACKAGE)) return false
    val digits = e164Digits(context, phoneNumber)
    if (digits.isEmpty()) return openApp(context, SKYPE_PACKAGE)
    return try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("skype:+$digits?call&video=true")).apply {
                setPackage(SKYPE_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        true
    } catch (_: Exception) { openApp(context, SKYPE_PACKAGE) }
}

/** imo video call via the contact's imo row, falling back to opening imo. */
fun startImoVideoCall(context: Context, phoneNumber: String): Boolean {
    if (!isPackageInstalled(context, IMO_PACKAGE)) return false
    val row = findCallDataRow(
        context, phoneNumber,
        listOf(
            "vnd.android.cursor.item/vnd.com.imo.android.imoim.video.call",
            "vnd.android.cursor.item/vnd.com.imo.android.imoim.call",
            "vnd.android.cursor.item/vnd.com.imo.android.imoim.profile"
        )
    )
    if (row != null && startDataRowIntent(context, row, IMO_PACKAGE)) return true
    return openApp(context, IMO_PACKAGE)
}

/** Zoom can't call a phone number, so this just opens the app to start a meeting. */
fun startZoomVideoCall(context: Context): Boolean {
    if (!isPackageInstalled(context, ZOOM_PACKAGE)) return false
    return openApp(context, ZOOM_PACKAGE)
}
