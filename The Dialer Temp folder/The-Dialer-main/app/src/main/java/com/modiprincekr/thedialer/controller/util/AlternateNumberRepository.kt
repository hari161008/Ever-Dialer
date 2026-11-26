package com.modiprincekr.thedialer.controller.util

import android.content.Context
import android.content.SharedPreferences
import com.modiprincekr.thedialer.modal.data.AlternateNumber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.util.concurrent.atomic.AtomicBoolean

object AlternateNumberRepository {
    const val PREFS_NAME = "alternate_numbers_prefs"
    private const val KEY_NUMBERS = "alternate_numbers_json"

    private val _alternateNumbersFlow = MutableStateFlow<List<AlternateNumber>>(emptyList())
    val alternateNumbersFlow: StateFlow<List<AlternateNumber>> = _alternateNumbersFlow.asStateFlow()

    private val isInitialized = AtomicBoolean(false)

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun ensureLoaded(context: Context) {
        if (!isInitialized.get()) {
            synchronized(this) {
                if (!isInitialized.get()) {
                    val list = loadFromPrefs(context)
                    _alternateNumbersFlow.value = list
                    isInitialized.set(true)
                }
            }
        }
    }

    private fun loadFromPrefs(context: Context): List<AlternateNumber> {
        val jsonStr = getPrefs(context).getString(KEY_NUMBERS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<AlternateNumber>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                AlternateNumber.fromJson(obj)?.let { list.add(it) }
            }
            list.sortedByDescending { it.createdAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persist(context: Context, list: List<AlternateNumber>) {
        val arr = JSONArray()
        for (item in list) {
            arr.put(item.toJson())
        }
        getPrefs(context).edit().putString(KEY_NUMBERS, arr.toString()).apply()
        _alternateNumbersFlow.value = list
    }

    @Synchronized
    fun getAll(context: Context): List<AlternateNumber> {
        ensureLoaded(context)
        return _alternateNumbersFlow.value
    }

    @Synchronized
    fun findById(context: Context, id: String): AlternateNumber? {
        ensureLoaded(context)
        return _alternateNumbersFlow.value.firstOrNull { it.id == id }
    }

    @Synchronized
    fun findByNumber(context: Context, number: String): AlternateNumber? {
        if (number.isBlank()) return null
        ensureLoaded(context)
        val digits = number.filter { it.isDigit() }
        val current = _alternateNumbersFlow.value

        // Direct normalized equality
        if (digits.isNotEmpty()) {
            val direct = current.firstOrNull { it.normalizedNumber == digits }
            if (direct != null) return direct
        }

        // Last 10 or 7 digits match
        if (digits.length >= 7) {
            val querySuffix = if (digits.length >= 10) digits.takeLast(10) else digits.takeLast(7)
            val suffixMatch = current.firstOrNull { item ->
                val altDigits = item.normalizedNumber
                if (altDigits.length >= querySuffix.length) {
                    altDigits.takeLast(querySuffix.length) == querySuffix
                } else false
            }
            if (suffixMatch != null) return suffixMatch
        }

        // Best effort numbersLikelyMatch
        return current.firstOrNull { numbersLikelyMatch(it.phoneNumber, number) }
    }

    @Synchronized
    fun save(context: Context, item: AlternateNumber): Boolean {
        ensureLoaded(context)
        val current = _alternateNumbersFlow.value.toMutableList()

        // 1. If matching ID exists, update in-place
        val indexById = current.indexOfFirst { it.id == item.id }
        if (indexById >= 0) {
            current[indexById] = item
            persist(context, current)
            return true
        }

        // 2. If matching phone number exists, update that existing record
        val indexByNumber = current.indexOfFirst {
            val d1 = it.normalizedNumber
            val d2 = item.normalizedNumber
            d1 == d2 || (d1.length >= 7 && d2.length >= 7 && d1.takeLast(7) == d2.takeLast(7))
        }
        if (indexByNumber >= 0) {
            val existing = current[indexByNumber]
            current[indexByNumber] = item.copy(
                id = existing.id,
                createdAt = existing.createdAt
            )
            persist(context, current)
            return true
        }

        // 3. Otherwise add new entry at top
        current.add(0, item)
        persist(context, current)
        return true
    }

    @Synchronized
    fun delete(context: Context, id: String): Boolean {
        ensureLoaded(context)
        val current = _alternateNumbersFlow.value.toMutableList()
        val removed = current.removeAll { it.id == id }
        if (removed) {
            persist(context, current)
        }
        return removed
    }

    @Synchronized
    fun search(context: Context, query: String, tag: String? = null): List<AlternateNumber> {
        ensureLoaded(context)
        val q = query.trim().lowercase()
        val digits = q.filter { it.isDigit() }
        val all = _alternateNumbersFlow.value

        return all.filter { item ->
            val matchesTag = tag.isNullOrBlank() || tag.equals(AlternateNumber.TAG_ALL, ignoreCase = true) || item.categoryTag.equals(tag, ignoreCase = true)
            if (!matchesTag) return@filter false

            if (q.isEmpty()) return@filter true

            val nameMatch = item.name.lowercase().contains(q)
            val noteMatch = item.note.lowercase().contains(q)
            val numberMatch = item.phoneNumber.contains(q) || (digits.isNotEmpty() && item.normalizedNumber.contains(digits))
            val tagMatch = item.categoryTag.lowercase().contains(q)

            nameMatch || noteMatch || numberMatch || tagMatch
        }
    }

    @Synchronized
    fun exportToJsonArray(context: Context): JSONArray {
        ensureLoaded(context)
        val arr = JSONArray()
        for (item in _alternateNumbersFlow.value) {
            arr.put(item.toJson())
        }
        return arr
    }

    @Synchronized
    fun importFromJsonArray(context: Context, jsonArray: JSONArray): Int {
        ensureLoaded(context)
        var count = 0
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i) ?: continue
            val item = AlternateNumber.fromJson(obj) ?: continue
            save(context, item)
            count++
        }
        return count
    }

    @Synchronized
    fun clearAll(context: Context) {
        getPrefs(context).edit().remove(KEY_NUMBERS).apply()
        _alternateNumbersFlow.value = emptyList()
    }
}
