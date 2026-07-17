package com.pdnp.dailydigest.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.pdnp.dailydigest.capture.GmailImporter
import com.pdnp.dailydigest.data.Prefs

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var apiKey by remember { mutableStateOf(Prefs.apiKey(context) ?: "") }
    var aiEnabled by remember { mutableStateOf(Prefs.aiEnabled(context)) }
    var saved by remember { mutableStateOf(false) }

    val listenerEnabled =
        NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("WhatsApp & Gmail capture", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (listenerEnabled) "Notification access: GRANTED ✓"
                    else "Notification access: NOT GRANTED — WhatsApp/Gmail messages will not be captured.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }) {
                    Text("Open notification access settings")
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        GmailAccountCard()

        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("AI daily summaries (optional)", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Uses your own Claude API key. When enabled, the day's messages and " +
                        "call log are sent to the Claude API to generate a richer summary. " +
                        "Leave off to keep everything on-device.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        saved = false
                    },
                    label = { Text("Anthropic API key") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = {
                        Prefs.setApiKey(context, apiKey)
                        saved = true
                    }) {
                        Text("Save key")
                    }
                    Spacer(Modifier.width(8.dp))
                    if (saved) Text("Saved ✓", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = aiEnabled,
                        onCheckedChange = {
                            aiEnabled = it
                            Prefs.setAiEnabled(context, it)
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Include AI summary in daily background sync")
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Privacy", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "All captured messages, calls, tasks, meeting minutes and recordings are " +
                        "stored only on this device (app-private storage). Nothing leaves the " +
                        "device unless you request an AI summary.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun GmailAccountCard() {
    val context = LocalContext.current
    var connectedEmail by remember { mutableStateOf(GmailImporter.connectedEmail(context)) }

    fun signInClient(): GoogleSignInClient {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(GmailImporter.READONLY_SCOPE)
            .build()
        return GoogleSignIn.getClient(context, options)
    }

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        connectedEmail = try {
            GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(Exception::class.java)?.email
        } catch (e: Exception) {
            null
        } ?: GmailImporter.connectedEmail(context)
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Gmail sync (recommended)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                if (connectedEmail != null)
                    "Connected: $connectedEmail ✓\nFull emails (subject + body) sync via the " +
                        "official Gmail API, including recent history."
                else
                    "Connect your Google account to sync full emails via the official Gmail API " +
                        "(read-only). Without it, Gmail is captured from notification previews only.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            if (connectedEmail == null) {
                Button(onClick = { signInLauncher.launch(signInClient().signInIntent) }) {
                    Text("Connect Google account")
                }
            } else {
                Button(onClick = {
                    signInClient().signOut()
                    connectedEmail = null
                }) {
                    Text("Disconnect")
                }
            }
        }
    }
}
