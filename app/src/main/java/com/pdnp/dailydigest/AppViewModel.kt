package com.pdnp.dailydigest

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pdnp.dailydigest.capture.CallLogImporter
import com.pdnp.dailydigest.capture.SmsImporter
import com.pdnp.dailydigest.data.AppDatabase
import com.pdnp.dailydigest.data.Meeting
import com.pdnp.dailydigest.data.TaskItem
import com.pdnp.dailydigest.meeting.MeetingRecorder
import com.pdnp.dailydigest.summary.SummaryGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val recorder = MeetingRecorder(app)

    // ---- Date-wise browsing ----
    val selectedDay = MutableStateFlow(LocalDate.now())

    val messages = selectedDay
        .flatMapLatest { db.messageDao().byDay(it.toString()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val calls = selectedDay
        .flatMapLatest { db.callDao().byDay(it.toString()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val summary = selectedDay
        .flatMapLatest { db.summaryDao().byDay(it.toString()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val tasks = db.taskDao().all()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val meetings = db.meetingDao().all()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val generating = MutableStateFlow(false)

    fun shiftDay(days: Long) {
        val next = selectedDay.value.plusDays(days)
        if (!next.isAfter(LocalDate.now())) selectedDay.value = next
    }

    /** Re-imports SMS/call log and regenerates the summary for the selected day. */
    fun refreshAndSummarize(useAi: Boolean) {
        if (generating.value) return
        val context = getApplication<Application>()
        viewModelScope.launch {
            generating.value = true
            try {
                withContext(Dispatchers.IO) {
                    SmsImporter.import(context)
                    CallLogImporter.import(context)
                    SummaryGenerator.generate(context, selectedDay.value.toString(), useAi)
                }
            } finally {
                generating.value = false
            }
        }
    }

    // ---- Tasks ----
    fun addTask(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { db.taskDao().insert(TaskItem(title = title.trim())) }
    }

    fun toggleTask(task: TaskItem) {
        viewModelScope.launch { db.taskDao().update(task.copy(done = !task.done)) }
    }

    fun cyclePriority(task: TaskItem) {
        viewModelScope.launch { db.taskDao().update(task.copy(priority = (task.priority + 1) % 3)) }
    }

    fun deleteTask(task: TaskItem) {
        viewModelScope.launch { db.taskDao().delete(task) }
    }

    // ---- Meetings ----
    val recordingMeetingId = MutableStateFlow<Long?>(null)
    val playingPath = MutableStateFlow<String?>(null)

    fun startMeeting(title: String) {
        if (recordingMeetingId.value != null) return
        viewModelScope.launch {
            val audioPath = try {
                recorder.start()
            } catch (e: Exception) {
                null // mic permission missing/unavailable — keep the meeting, skip audio
            }
            val id = db.meetingDao().insert(
                Meeting(
                    title = title.trim().ifBlank { "Meeting" },
                    startedAt = System.currentTimeMillis(),
                    audioPath = audioPath
                )
            )
            recordingMeetingId.value = id
        }
    }

    fun stopMeeting() {
        val id = recordingMeetingId.value ?: return
        viewModelScope.launch {
            recorder.stop()
            db.meetingDao().byId(id)?.let {
                db.meetingDao().update(it.copy(endedAt = System.currentTimeMillis()))
            }
            recordingMeetingId.value = null
        }
    }

    fun updateMeeting(meeting: Meeting) {
        viewModelScope.launch { db.meetingDao().update(meeting) }
    }

    fun deleteMeeting(meeting: Meeting) {
        viewModelScope.launch {
            if (playingPath.value == meeting.audioPath) togglePlayback(meeting.audioPath ?: "")
            db.meetingDao().delete(meeting)
        }
    }

    /** Copies each non-blank line of the meeting's action items into the task list. */
    fun actionItemsToTasks(meeting: Meeting) {
        viewModelScope.launch {
            meeting.actionItems.lines()
                .map { it.trim().trimStart('-', '*', '•').trim() }
                .filter { it.isNotBlank() }
                .forEach { db.taskDao().insert(TaskItem(title = it, notes = "From meeting: ${meeting.title}")) }
        }
    }

    fun togglePlayback(path: String) {
        if (playingPath.value == path) {
            recorder.stopPlayback()
            playingPath.value = null
        } else {
            try {
                recorder.play(path) { playingPath.value = null }
                playingPath.value = path
            } catch (e: Exception) {
                playingPath.value = null
            }
        }
    }

    override fun onCleared() {
        recorder.stopPlayback()
        super.onCleared()
    }
}
