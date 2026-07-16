package com.pdnp.dailydigest.capture

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.pdnp.dailydigest.data.AppDatabase
import com.pdnp.dailydigest.data.CapturedMessage
import com.pdnp.dailydigest.data.DayKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Captures WhatsApp and Gmail messages from their notifications.
 *
 * WhatsApp has no public API for reading chats, so notification capture is the
 * only viable on-device approach. The user must grant Notification Access in
 * system settings (a button in the app's Settings screen opens that page).
 */
class NotificationCaptureService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val source = when (sbn.packageName) {
            "com.whatsapp", "com.whatsapp.w4b" -> "WHATSAPP"
            "com.google.android.gm", "com.google.android.gm.lite" -> "GMAIL"
            else -> return
        }

        val notification = sbn.notification ?: return
        // Skip group-summary notifications ("5 new messages")
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: return
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString()?.trim() ?: return
        if (title.isEmpty() || text.isEmpty()) return

        // Skip WhatsApp's own aggregate notifications
        if (source == "WHATSAPP" &&
            (title.equals("WhatsApp", ignoreCase = true) ||
                text.matches(Regex("""\d+ new messages?( from \d+ chats?)?""")))
        ) return

        val timestamp = sbn.postTime
        val message = CapturedMessage(
            source = source,
            sender = title,
            body = text,
            timestamp = timestamp,
            dayKey = DayKeys.of(timestamp),
            // Dedupe on content only: re-posted/updated notifications with the
            // same sender+text are stored once.
            dedupeKey = DayKeys.dedupe(source, title, text)
        )
        scope.launch {
            AppDatabase.get(applicationContext).messageDao().insert(message)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
