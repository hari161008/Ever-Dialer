package com.coolappstore.everdialer.by.svhp.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.provider.CallLog
import android.text.format.DateUtils
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.coolappstore.everdialer.by.svhp.R
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class RecentCallsWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return RecentCallsFactory(applicationContext, intent)
    }
}

data class WidgetCallEntry(
    val number: String,
    val name: String,
    val type: Int,
    val date: Long
)

class RecentCallsFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val widgetId = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )
    private val items = mutableListOf<WidgetCallEntry>()

    companion object {
        private const val PREFS_NAME = "ever_dialer_widget_cache"
        private const val KEY_CACHED_CALLS = "cached_calls"
        private const val MAX_ITEMS = 30

        fun saveCache(context: Context, entries: List<WidgetCallEntry>) {
            val arr = JSONArray()
            entries.forEach { e ->
                arr.put(JSONObject().apply {
                    put("number", e.number)
                    put("name", e.name)
                    put("type", e.type)
                    put("date", e.date)
                })
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_CACHED_CALLS, arr.toString()).apply()
        }

        fun loadCache(context: Context): List<WidgetCallEntry> {
            val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_CACHED_CALLS, null) ?: return emptyList()
            return try {
                val arr = JSONArray(json)
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    WidgetCallEntry(
                        number = o.getString("number"),
                        name = o.getString("name"),
                        type = o.getInt("type"),
                        date = o.getLong("date")
                    )
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    override fun onCreate() { loadData() }
    override fun onDataSetChanged() { loadData() }
    override fun onDestroy() { items.clear() }
    override fun getCount(): Int = items.size
    // Use a stable ID derived from the number string, not position
    override fun getItemId(position: Int): Long =
        if (position in items.indices) items[position].number.hashCode().toLong() else position.toLong()
    override fun hasStableIds(): Boolean = false
    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1

    private fun loadData() {
        items.clear()
        val live = fetchFromContentResolver()
        if (live.isNotEmpty()) {
            items.addAll(live)
            saveCache(context, live)
        } else {
            items.addAll(loadCache(context))
        }
    }

    private fun fetchFromContentResolver(): List<WidgetCallEntry> {
        val result = mutableListOf<WidgetCallEntry>()
        return try {
            val projection = arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE
            )
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )?.use { cursor ->
                val numIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val nameIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                val typeIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                val dateIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)

                val seen = mutableSetOf<String>()
                var scanned = 0
                while (cursor.moveToNext() && result.size < MAX_ITEMS) {
                    if (++scanned > 300) break
                    val number = cursor.getString(numIdx)?.trim() ?: continue
                    if (number.isBlank()) continue
                    if (!seen.add(number)) continue
                    val rawName = cursor.getString(nameIdx)
                    val name = if (!rawName.isNullOrBlank()) rawName else number
                    result.add(WidgetCallEntry(number, name, cursor.getInt(typeIdx), cursor.getLong(dateIdx)))
                }
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun getViewAt(position: Int): RemoteViews {
        if (position < 0 || position >= items.size) {
            return RemoteViews(context.packageName, R.layout.widget_call_item)
        }
        val entry = items[position]
        val views = RemoteViews(context.packageName, R.layout.widget_call_item)

        views.setTextViewText(R.id.tv_initials, getInitials(entry.name, entry.number))
        views.setTextViewText(R.id.tv_name, entry.name)
        views.setTextViewText(R.id.tv_time, formatRelativeTime(entry.date))

        val iconRes = when (entry.type) {
            CallLog.Calls.INCOMING_TYPE -> R.drawable.ic_widget_incoming
            CallLog.Calls.OUTGOING_TYPE -> R.drawable.ic_widget_outgoing
            CallLog.Calls.MISSED_TYPE   -> R.drawable.ic_widget_missed
            else                        -> R.drawable.ic_widget_incoming
        }
        views.setImageViewResource(R.id.iv_call_type, iconRes)

        // Fill-in intent carries the phone number into the template PendingIntent.
        // Must set on the btn_dial view — the root view of a list item cannot
        // receive setOnClickFillInIntent reliably on all launchers.
        val fillIn = Intent().apply {
            putExtra(RecentCallsWidgetProvider.EXTRA_PHONE_NUMBER, entry.number)
        }
        views.setOnClickFillInIntent(R.id.btn_dial, fillIn)

        return views
    }

    private fun getInitials(name: String, number: String): String {
        if (name == number || name.isBlank()) return "#"
        val words = name.trim().split("\\s+".toRegex())
        return when {
            words.size >= 2       -> "${words[0].first().uppercaseChar()}${words[1].first().uppercaseChar()}"
            words[0].length >= 2  -> words[0].substring(0, 2).uppercase()
            else                  -> words[0].first().uppercaseChar().toString()
        }
    }

    private fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < DateUtils.MINUTE_IN_MILLIS -> "Just now"
            diff < DateUtils.HOUR_IN_MILLIS   -> "${diff / DateUtils.MINUTE_IN_MILLIS}m ago"
            diff < DateUtils.DAY_IN_MILLIS    -> "${diff / DateUtils.HOUR_IN_MILLIS}h ago"
            DateUtils.isToday(timestamp)      -> "Today"
            DateUtils.isToday(timestamp + DateUtils.DAY_IN_MILLIS) -> "Yesterday"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
        }
    }
}
