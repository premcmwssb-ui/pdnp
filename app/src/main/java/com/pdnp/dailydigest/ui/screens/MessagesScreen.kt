package com.pdnp.dailydigest.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pdnp.dailydigest.AppViewModel
import com.pdnp.dailydigest.data.CallEntry
import com.pdnp.dailydigest.data.CapturedMessage
import com.pdnp.dailydigest.data.DayKeys

@Composable
fun MessagesScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val messages by vm.messages.collectAsState()
    val calls by vm.calls.collectAsState()
    var filter by rememberSaveable { mutableStateOf("ALL") }

    Column(modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        DaySelector(vm)

        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL", "WHATSAPP", "GMAIL", "SMS", "CALLS").forEach { option ->
                FilterChip(
                    selected = filter == option,
                    onClick = { filter = option },
                    label = { Text(option.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        val showCalls = filter == "ALL" || filter == "CALLS"
        val shownMessages =
            if (filter == "CALLS") emptyList()
            else messages.filter { filter == "ALL" || it.source == filter }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (showCalls && calls.isNotEmpty()) {
                item { Text("Calls", style = MaterialTheme.typography.titleSmall) }
                items(calls, key = { "c${it.id}" }) { CallRow(it) }
            }
            if (shownMessages.isNotEmpty()) {
                item { Text("Messages", style = MaterialTheme.typography.titleSmall) }
                items(shownMessages, key = { "m${it.id}" }) { MessageRow(it) }
            }
            if (calls.isEmpty() && shownMessages.isEmpty()) {
                item {
                    Text(
                        "Nothing captured for this day yet.\n\n" +
                            "WhatsApp & Gmail need Notification Access (Settings tab). " +
                            "SMS and calls import on refresh or in the background.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun MessageRow(message: CapturedMessage) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(message.sender, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${message.source} · ${DayKeys.timeOf(message.timestamp)}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(message.body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun CallRow(call: CallEntry) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    call.contactName.ifBlank { call.number },
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    "${call.type.lowercase().replaceFirstChar { it.uppercase() }} · " +
                        "${call.durationSec / 60}m ${call.durationSec % 60}s",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(DayKeys.timeOf(call.timestamp), style = MaterialTheme.typography.labelSmall)
        }
    }
}
