package com.pdnp.dailydigest.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pdnp.dailydigest.AppViewModel
import com.pdnp.dailydigest.data.DayKeys
import com.pdnp.dailydigest.data.Meeting

@Composable
fun MeetingsScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val meetings by vm.meetings.collectAsState()
    val recordingId by vm.recordingMeetingId.collectAsState()
    val playingPath by vm.playingPath.collectAsState()
    var title by rememberSaveable { mutableStateOf("") }

    Column(modifier.fillMaxSize().padding(12.dp)) {
        Text("Meetings", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Meeting title") },
                modifier = Modifier.weight(1f),
                enabled = recordingId == null
            )
            Spacer(Modifier.width(8.dp))
            if (recordingId == null) {
                Button(onClick = {
                    vm.startMeeting(title)
                    title = ""
                }) {
                    Icon(Icons.Filled.Mic, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Start")
                }
            } else {
                Button(onClick = { vm.stopMeeting() }) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Stop")
                }
            }
        }
        if (recordingId != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                "● Recording…",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelLarge
            )
        }
        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(meetings, key = { it.id }) { meeting ->
                MeetingCard(
                    meeting = meeting,
                    isPlaying = playingPath != null && playingPath == meeting.audioPath,
                    vm = vm
                )
            }
            if (meetings.isEmpty()) {
                item {
                    Text(
                        "No meetings recorded yet. Enter a title and tap Start to record " +
                            "audio and take minutes.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MeetingCard(meeting: Meeting, isPlaying: Boolean, vm: AppViewModel) {
    var expanded by rememberSaveable(meeting.id) { mutableStateOf(false) }
    var attendees by remember(meeting.id, meeting.attendees) { mutableStateOf(meeting.attendees) }
    var minutes by remember(meeting.id, meeting.minutes) { mutableStateOf(meeting.minutes) }
    var actionItems by remember(meeting.id, meeting.actionItems) { mutableStateOf(meeting.actionItems) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(meeting.title, style = MaterialTheme.typography.titleSmall)
                    val duration = meeting.endedAt?.let { end ->
                        val min = (end - meeting.startedAt) / 60_000
                        " · ${min} min"
                    } ?: " · in progress"
                    Text(
                        DayKeys.of(meeting.startedAt) + " " +
                            DayKeys.timeOf(meeting.startedAt) + duration,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                if (meeting.audioPath != null) {
                    IconButton(onClick = { vm.togglePlayback(meeting.audioPath) }) {
                        Icon(
                            if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Stop playback" else "Play recording"
                        )
                    }
                }
                IconButton(onClick = { vm.deleteMeeting(meeting) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete meeting")
                }
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = attendees,
                    onValueChange = { attendees = it },
                    label = { Text("Attendees") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { minutes = it },
                    label = { Text("Minutes of the meeting") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = actionItems,
                    onValueChange = { actionItems = it },
                    label = { Text("Action items (one per line)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        vm.updateMeeting(
                            meeting.copy(
                                attendees = attendees,
                                minutes = minutes,
                                actionItems = actionItems
                            )
                        )
                        expanded = false
                    }) {
                        Text("Save")
                    }
                    OutlinedButton(
                        onClick = {
                            vm.updateMeeting(
                                meeting.copy(
                                    attendees = attendees,
                                    minutes = minutes,
                                    actionItems = actionItems
                                )
                            )
                            vm.actionItemsToTasks(meeting.copy(actionItems = actionItems))
                        },
                        enabled = actionItems.isNotBlank()
                    ) {
                        Text("Action items → Tasks")
                    }
                }
            }
        }
    }
}
