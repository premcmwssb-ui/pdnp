package com.pdnp.dailydigest.summary

import android.content.Context
import com.pdnp.dailydigest.data.AppDatabase
import com.pdnp.dailydigest.data.CallEntry
import com.pdnp.dailydigest.data.CapturedMessage
import com.pdnp.dailydigest.data.DaySummary
import com.pdnp.dailydigest.data.DayKeys
import com.pdnp.dailydigest.data.Prefs

/** Builds the date-wise daily summary: an on-device digest plus an optional AI summary. */
object SummaryGenerator {

    suspend fun generate(context: Context, dayKey: String, useAi: Boolean): DaySummary {
        val db = AppDatabase.get(context)
        val messages = db.messageDao().byDayOnce(dayKey)
        val calls = db.callDao().byDayOnce(dayKey)

        val localSummary = buildLocalSummary(messages, calls)

        var aiSummary: String? = null
        if (useAi) {
            val apiKey = Prefs.apiKey(context)
            aiSummary = if (apiKey.isNullOrBlank()) {
                "AI summary skipped: no Claude API key set (see Settings)."
            } else if (messages.isEmpty() && calls.isEmpty()) {
                null
            } else {
                try {
                    ClaudeSummarizer.summarize(apiKey, dayKey, messages, calls)
                } catch (e: Exception) {
                    "AI summary failed: ${e.message}"
                }
            }
        }

        val summary = DaySummary(
            dayKey = dayKey,
            generatedAt = System.currentTimeMillis(),
            summaryText = localSummary,
            aiSummaryText = aiSummary
        )
        db.summaryDao().upsert(summary)
        return summary
    }

    private fun buildLocalSummary(messages: List<CapturedMessage>, calls: List<CallEntry>): String {
        if (messages.isEmpty() && calls.isEmpty()) {
            return "No activity recorded for this day."
        }

        val sb = StringBuilder()

        val bySource = messages.groupBy { it.source }
        sb.appendLine("Activity")
        sb.appendLine(
            "• ${messages.size} messages " +
                "(WhatsApp ${bySource["WHATSAPP"]?.size ?: 0}, " +
                "Gmail ${bySource["GMAIL"]?.size ?: 0}, " +
                "SMS ${bySource["SMS"]?.size ?: 0})"
        )
        sb.appendLine("• ${calls.size} calls, ${calls.sumOf { it.durationSec } / 60} min on the phone")

        val topContacts = messages.groupBy { it.sender }
            .entries.sortedByDescending { it.value.size }.take(5)
        if (topContacts.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("Most active conversations")
            topContacts.forEach { (sender, msgs) ->
                sb.appendLine("• $sender — ${msgs.size} messages")
            }
        }

        val missed = calls.filter { it.type == "MISSED" || it.type == "REJECTED" }
        if (missed.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("Missed calls")
            missed.forEach {
                sb.appendLine("• ${it.contactName.ifBlank { it.number }} at ${DayKeys.timeOf(it.timestamp)}")
            }
        }

        val longestCalls = calls.filter { it.durationSec > 0 }
            .sortedByDescending { it.durationSec }.take(3)
        if (longestCalls.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("Longest calls")
            longestCalls.forEach {
                sb.appendLine(
                    "• ${it.contactName.ifBlank { it.number }} — " +
                        "${it.durationSec / 60}m ${it.durationSec % 60}s (${it.type.lowercase()})"
                )
            }
        }

        val keywords = listOf("urgent", "asap", "meeting", "payment", "invoice", "deadline", "reminder", "otp")
        val flagged = messages.filter { m -> keywords.any { m.body.contains(it, ignoreCase = true) } }.take(5)
        if (flagged.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("Possibly important")
            flagged.forEach {
                sb.appendLine("• [${it.source}] ${it.sender}: ${it.body.take(80)}")
            }
        }

        return sb.toString().trim()
    }
}
