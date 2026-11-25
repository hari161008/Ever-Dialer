package com.modiprincekr.thedialer.modal.data

import com.modiprincekr.thedialer.controller.util.formatDateHeader

enum class CallLogFilter(val displayName: String) {
    All("All"),
    Contacts("Known"),
    Favourites("Favourites"),
    Unknown("Unknown"),
    Missed("Missed"),
    Incoming("Incoming"),
    Outgoing("Outgoing");

    companion object {
        public fun filter(logs: List<CallLogEntry>, type: CallLogFilter): List<List<CallLogEntry>> {
            val filteredList = when (type) {
                All -> logs
                Contacts -> logs.filter { it.name != null && it.name.isNotEmpty() && it.name != it.number && !it.isCallerIdName }
                Favourites -> emptyList()
                Unknown -> logs.filter { it.contactId.isNullOrBlank() || it.name.isNullOrEmpty() || it.name == it.number || it.isCallerIdName }
                Incoming -> logs.filter { it.type == android.provider.CallLog.Calls.INCOMING_TYPE || it.type == 7 /* ANSWERED_EXTERNALLY */ || it.types.any { t -> t == android.provider.CallLog.Calls.INCOMING_TYPE || t == 7 } }
                Outgoing -> logs.filter { it.type == android.provider.CallLog.Calls.OUTGOING_TYPE || it.types.any { t -> t == android.provider.CallLog.Calls.OUTGOING_TYPE } }
                Missed -> logs.filter { it.type == android.provider.CallLog.Calls.MISSED_TYPE || it.type == 5 /* REJECTED */ || it.types.any { t -> t == android.provider.CallLog.Calls.MISSED_TYPE || t == 5 } }
            }
            return filteredList.groupBy { formatDateHeader(it.date) }.values.toList()
        }

        public fun getNames(): List<String> {
            return entries.map { it.name }
        }
    }
};