package com.coolappstore.everdialer.by.svhp.controller.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.coolappstore.everdialer.by.svhp.MainActivity
import kotlin.math.abs

/**
 * Builds and pins home-screen shortcuts for a contact from the Contact Info screen's "Add to
 * Home Screen" button. Two kinds of shortcuts are supported:
 *  - "Open contact info": lands back on this contact's Contact Info page.
 *  - "Call directly": places the call immediately when tapped, no extra taps needed.
 *
 * Both reuse [MainActivity]'s existing intent routing (see MainActivity.handleIntent) instead of
 * adding any new manifest entry points, so the same "contact_id" extra / tel: ACTION_CALL paths
 * already used by widgets and Assistant "call X" shortcuts drive these too.
 */
object ContactShortcutUtils {

    private val avatarColors = listOf(
        Color.parseColor("#C62828"), Color.parseColor("#AD1457"), Color.parseColor("#6A1B9A"), Color.parseColor("#4527A0"),
        Color.parseColor("#283593"), Color.parseColor("#1565C0"), Color.parseColor("#0277BD"), Color.parseColor("#00838F"),
        Color.parseColor("#00695C"), Color.parseColor("#2E7D32"), Color.parseColor("#558B2F"), Color.parseColor("#9E9D24"),
        Color.parseColor("#F9A825"), Color.parseColor("#FF8F00"), Color.parseColor("#E65100"), Color.parseColor("#BF360C")
    )

    fun isPinningSupported(context: Context): Boolean =
        ShortcutManagerCompat.isRequestPinShortcutSupported(context)

    /** Pins a shortcut that opens straight to this contact's Contact Info page. */
    fun pinOpenContactShortcut(context: Context, contactId: String, displayName: String, photoUri: String?) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("contact_id", contactId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        pin(
            context = context,
            id = "contact_info_$contactId",
            label = displayName,
            intent = intent,
            icon = buildIconBitmap(context, displayName, photoUri)
        )
    }

    /** Pins a shortcut that dials [number] immediately when tapped, skipping the app entirely. */
    fun pinCallShortcut(context: Context, keyId: String, displayName: String, number: String, photoUri: String?) {
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).apply {
            setClass(context, MainActivity::class.java)
            putExtra("contact_id", keyId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        pin(
            context = context,
            id = "call_${keyId}_${number.hashCode()}",
            label = displayName,
            intent = intent,
            icon = buildIconBitmap(context, displayName, photoUri)
        )
    }

    private fun pin(context: Context, id: String, label: String, intent: Intent, icon: Bitmap): Boolean {
        if (!isPinningSupported(context)) return false
        val safeLabel = label.trim().ifBlank { "Contact" }
        return try {
            val shortcut = ShortcutInfoCompat.Builder(context, id)
                .setShortLabel(safeLabel.take(15))
                .setLongLabel(safeLabel)
                .setIcon(IconCompat.createWithBitmap(icon))
                .setIntent(intent)
                .build()
            ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
        } catch (_: Exception) {
            false
        }
    }

    /** The contact's saved photo (cropped to a circle) if available, else a colored circle with
     *  their initial — matching RivoAvatar's look so shortcuts stay visually distinct per contact. */
    private fun buildIconBitmap(context: Context, name: String, photoUri: String?): Bitmap {
        val size = 192
        if (!photoUri.isNullOrBlank()) {
            try {
                context.contentResolver.openInputStream(Uri.parse(photoUri))?.use { input ->
                    val decoded = BitmapFactory.decodeStream(input)
                    if (decoded != null) {
                        val scaled = Bitmap.createScaledBitmap(decoded, size, size, true)
                        return cropToCircle(scaled)
                    }
                }
            } catch (_: Exception) {
                // Fall through to initials avatar below.
            }
        }
        return initialsBitmap(name, size)
    }

    private fun cropToCircle(src: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val radius = minOf(src.width, src.height) / 2f
        canvas.drawCircle(src.width / 2f, src.height / 2f, radius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return output
    }

    private fun initialsBitmap(name: String, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val trimmed = name.trim()
        val hasName = trimmed.isNotEmpty()
        val colorKey = if (hasName) trimmed else "unknown_caller"
        val bgColor = avatarColors[abs(colorKey.hashCode()) % avatarColors.size]

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, bgPaint)

        val letter = if (hasName) trimmed.take(1).uppercase() else "?"
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = size * 0.45f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        val textY = size / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(letter, size / 2f, textY, textPaint)
        return bitmap
    }
}
