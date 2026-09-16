package com.coolappstore.everdialer.by.svhp.controller.util

import org.json.JSONArray

object SearchHistoryManager {
    enum class Type(val prefKey: String) {
        UNIVERSAL("search_history_universal"),
        SETTINGS("search_history_settings"),
        DIALPAD("search_history_dialpad")
    }

    private const val MAX_HISTORY_SIZE = 15

    fun getHistory(prefs: PreferenceManager, type: Type): List<String> {
        val raw = prefs.getString(type.prefKey, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(raw)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.optString(i)?.trim()
                if (!item.isNullOrBlank() && !list.contains(item)) {
                    list.add(item)
                }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addHistory(prefs: PreferenceManager, type: Type, query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val current = getHistory(prefs, type).toMutableList()
        current.removeAll { it.equals(trimmed, ignoreCase = true) }
        current.add(0, trimmed)
        val capped = current.take(MAX_HISTORY_SIZE)
        saveHistory(prefs, type, capped)
    }

    fun removeHistoryItem(prefs: PreferenceManager, type: Type, item: String) {
        val current = getHistory(prefs, type).toMutableList()
        current.removeAll { it.equals(item, ignoreCase = true) }
        saveHistory(prefs, type, current)
    }

    fun clearHistory(prefs: PreferenceManager, type: Type) {
        saveHistory(prefs, type, emptyList())
    }

    private fun saveHistory(prefs: PreferenceManager, type: Type, list: List<String>) {
        val jsonArray = JSONArray()
        list.forEach { jsonArray.put(it) }
        prefs.setString(type.prefKey, jsonArray.toString())
    }
}
