package com.pdnp.dailydigest.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.pdnp.dailydigest.AppViewModel

@Composable
fun TasksScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val tasks by vm.tasks.collectAsState()
    var newTitle by rememberSaveable { mutableStateOf("") }

    Column(modifier.fillMaxSize().padding(12.dp)) {
        Text("Tasks", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newTitle,
                onValueChange = { newTitle = it },
                label = { Text("New task") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = {
                vm.addTask(newTitle)
                newTitle = ""
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add task")
            }
        }
        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tasks, key = { it.id }) { task ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = task.done, onCheckedChange = { vm.toggleTask(task) })
                        Column(Modifier.weight(1f)) {
                            Text(
                                task.title,
                                style = MaterialTheme.typography.bodyLarge,
                                textDecoration = if (task.done) TextDecoration.LineThrough else null
                            )
                            if (task.notes.isNotBlank()) {
                                Text(task.notes, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        AssistChip(
                            onClick = { vm.cyclePriority(task) },
                            label = { Text(listOf("Low", "Med", "High")[task.priority]) }
                        )
                        IconButton(onClick = { vm.deleteTask(task) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete task")
                        }
                    }
                }
            }
            if (tasks.isEmpty()) {
                item {
                    Text(
                        "No tasks yet. Add one above, or send meeting action items here.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
            }
        }
    }
}
