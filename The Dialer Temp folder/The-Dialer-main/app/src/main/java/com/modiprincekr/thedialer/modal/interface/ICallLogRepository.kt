package com.modiprincekr.thedialer.modal.`interface`

import com.modiprincekr.thedialer.modal.data.CallLogEntry

interface ICallLogRepository {
    fun getCallLogs(limit: Int? = null): List<CallLogEntry>
    fun getLatestCallDate(): Long?
    fun deleteCallLog(entry: CallLogEntry): Boolean
    fun deleteCallLogs(entries: Collection<CallLogEntry>): Boolean
    fun invalidateContactIndex()
}