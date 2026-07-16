package com.pdnp.dailydigest.capture

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import androidx.core.content.ContextCompat
import com.pdnp.dailydigest.data.AppDatabase
import com.pdnp.dailydigest.data.CallEntry
import com.pdnp.dailydigest.data.DayKeys
import com.pdnp.dailydigest.data.Prefs

/** Imports the device call history from the system call log provider. */
object CallLogImporter {

    private const val LAST_SYNC_KEY = "last_call_sync"

    suspend fun import(context: Context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val since = Prefs.lastSync(context, LAST_SYNC_KEY)
        val items = mutableListOf<CallEntry>()
        var newest = since

        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DURATION,
                CallLog.Calls.DATE
            ),
            "${CallLog.Calls.DATE} > ?",
            arrayOf(since.toString()),
            "${CallLog.Calls.DATE} ASC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val number = cursor.getString(0) ?: "unknown"
                val name = cursor.getString(1) ?: ""
                val type = when (cursor.getInt(2)) {
                    CallLog.Calls.INCOMING_TYPE -> "INCOMING"
                    CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
                    CallLog.Calls.MISSED_TYPE -> "MISSED"
                    CallLog.Calls.REJECTED_TYPE -> "REJECTED"
                    else -> "OTHER"
                }
                val duration = cursor.getLong(3)
                val date = cursor.getLong(4)
                if (date > newest) newest = date

                items += CallEntry(
                    number = number,
                    contactName = name,
                    type = type,
                    durationSec = duration,
                    timestamp = date,
                    dayKey = DayKeys.of(date),
                    dedupeKey = DayKeys.dedupe("CALL", number, type, date.toString())
                )
            }
        }

        if (items.isNotEmpty()) {
            AppDatabase.get(context).callDao().insertAll(items)
        }
        if (newest > since) Prefs.setLastSync(context, LAST_SYNC_KEY, newest)
    }
}
