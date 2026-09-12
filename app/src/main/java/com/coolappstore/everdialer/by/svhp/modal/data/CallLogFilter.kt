package com.coolappstore.everdialer.by.svhp.modal.data

import com.coolappstore.everdialer.by.svhp.controller.util.formatDateHeader

enum class CallLogFilter(val displayName: String) {
    All("All"),
    Contacts("Known"),
    Favourites("Favourites"),
    Missed("Missed"),
    Incoming("Incoming"),
    Outgoing("Outgoing");

    companion object {
        public fun filter(logs: List<CallLogEntry>, type: CallLogFilter): List<List<CallLogEntry>> {
            val filteredList = when (type) {
                All -> logs
                Contacts -> logs.filter { it.name != null && it.name.isNotEmpty() }
                Favourites -> emptyList()
                Incoming -> logs.filter { it.type == android.provider.CallLog.Calls.INCOMING_TYPE }
                Outgoing -> logs.filter { it.type == android.provider.CallLog.Calls.OUTGOING_TYPE }
                Missed -> logs.filter { it.type == android.provider.CallLog.Calls.MISSED_TYPE }
            }
            return filteredList.groupBy { formatDateHeader(it.date) }.values.toList()
        }

        public fun getNames(): List<String> {
            return entries.map { it.name }
        }
    }
};