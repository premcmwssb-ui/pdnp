package com.pdnp.dailydigest.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A message captured from WhatsApp / Gmail notifications or imported from SMS. */
@Entity(
    tableName = "messages",
    indices = [Index(value = ["dedupeKey"], unique = true), Index("dayKey")]
)
data class CapturedMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: String,   // WHATSAPP | GMAIL | SMS
    val sender: String,
    val body: String,
    val timestamp: Long,
    val dayKey: String,   // yyyy-MM-dd in the device timezone
    val dedupeKey: String
)

@Entity(
    tableName = "calls",
    indices = [Index(value = ["dedupeKey"], unique = true), Index("dayKey")]
)
data class CallEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val contactName: String,
    val type: String,     // INCOMING | OUTGOING | MISSED | REJECTED | OTHER
    val durationSec: Long,
    val timestamp: Long,
    val dayKey: String,
    val dedupeKey: String
)

@Entity(tableName = "tasks")
data class TaskItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    val priority: Int = 1,        // 0 = low, 1 = normal, 2 = high
    val done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "meetings")
data class Meeting(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val audioPath: String? = null,
    val attendees: String = "",
    val minutes: String = "",
    val actionItems: String = ""  // one item per line
)

@Entity(tableName = "day_summaries")
data class DaySummary(
    @PrimaryKey val dayKey: String,
    val generatedAt: Long,
    val summaryText: String,
    val aiSummaryText: String? = null
)
