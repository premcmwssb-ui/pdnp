package com.pdnp.dailydigest.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pdnp.dailydigest.capture.CallLogImporter
import com.pdnp.dailydigest.capture.GmailImporter
import com.pdnp.dailydigest.capture.SmsImporter
import com.pdnp.dailydigest.data.DayKeys
import com.pdnp.dailydigest.data.Prefs
import com.pdnp.dailydigest.summary.SummaryGenerator
import java.util.concurrent.TimeUnit

/**
 * Periodic background sync: imports SMS + call history and refreshes the
 * date-wise summaries. AI summarization (if enabled) runs for the completed
 * day (yesterday) so it is generated once per day over final data.
 */
class DailySyncWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        return try {
            SmsImporter.import(context)
            CallLogImporter.import(context)
            GmailImporter.import(context)

            val useAi = Prefs.aiEnabled(context)
            SummaryGenerator.generate(context, DayKeys.yesterday(), useAi)
            SummaryGenerator.generate(context, DayKeys.today(), useAi = false)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailySyncWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "daily-digest-sync",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
