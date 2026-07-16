package com.pdnp.dailydigest.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pdnp.dailydigest.AppViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DaySelector(vm: AppViewModel) {
    val day by vm.selectedDay.collectAsState()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { vm.shiftDay(-1) }) {
            Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Previous day")
        }
        Text(
            text = day.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")),
            style = MaterialTheme.typography.titleMedium
        )
        IconButton(onClick = { vm.shiftDay(1) }, enabled = day.isBefore(LocalDate.now())) {
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Next day")
        }
    }
}

@Composable
fun DashboardScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val summary by vm.summary.collectAsState()
    val messages by vm.messages.collectAsState()
    val calls by vm.calls.collectAsState()
    val generating by vm.generating.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        DaySelector(vm)
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("Messages", messages.size.toString(), Modifier.weight(1f))
            StatCard("Calls", calls.size.toString(), Modifier.weight(1f))
            StatCard("Call min", (calls.sumOf { it.durationSec } / 60).toString(), Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { vm.refreshAndSummarize(useAi = false) }, enabled = !generating) {
                Text("Refresh summary")
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { vm.refreshAndSummarize(useAi = true) }, enabled = !generating) {
                Text("AI summary")
            }
            if (generating) {
                Spacer(Modifier.width(12.dp))
                CircularProgressIndicator(Modifier.size(22.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Daily summary", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    summary?.summaryText
                        ?: "No summary yet for this day. Tap \"Refresh summary\".",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        summary?.aiSummaryText?.let { ai ->
            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("AI summary (Claude)", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(ai, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
