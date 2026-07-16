package com.pdnp.dailydigest.capture

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.pdnp.dailydigest.data.AppDatabase
import com.pdnp.dailydigest.data.CapturedMessage
import com.pdnp.dailydigest.data.DayKeys
import com.pdnp.dailydigest.data.Prefs

/** Imports SMS (received and sent) from the system SMS provider. */
object SmsImporter {

    private const val LAST_SYNC_KEY = "last_sms_sync"

    suspend fun import(context: Context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val since = Prefs.lastSync(context, LAST_SYNC_KEY)
        val items = mutableListOf<CapturedMessage>()
        var newest = since

        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE
            ),
            "${Telephony.Sms.DATE} > ?",
            arrayOf(since.toString()),
            "${Telephony.Sms.DATE} ASC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val address = cursor.getString(0) ?: "unknown"
                val body = cursor.getString(1) ?: continue
                val date = cursor.getLong(2)
                val type = cursor.getInt(3)
                if (date > newest) newest = date

                val sender = if (type == Telephony.Sms.MESSAGE_TYPE_SENT) "Me → $address" else address
                items += CapturedMessage(
                    source = "SMS",
                    sender = sender,
                    body = body,
                    timestamp = date,
                    dayKey = DayKeys.of(date),
                    dedupeKey = DayKeys.dedupe("SMS", sender, body, date.toString())
                )
            }
        }

        if (items.isNotEmpty()) {
            AppDatabase.get(context).messageDao().insertAll(items)
        }
        if (newest > since) Prefs.setLastSync(context, LAST_SYNC_KEY, newest)
    }
}
