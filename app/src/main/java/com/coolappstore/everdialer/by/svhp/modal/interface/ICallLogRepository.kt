package com.coolappstore.everdialer.by.svhp.modal.`interface`

import com.coolappstore.everdialer.by.svhp.modal.data.CallLogEntry

interface ICallLogRepository {
    fun getCallLogs(): List<CallLogEntry>
    fun deleteCallLog(entry: CallLogEntry): Boolean
    fun deleteCallLogs(entries: Collection<CallLogEntry>): Boolean
}