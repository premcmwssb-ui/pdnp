# Daily Digest (PDNP)

An Android app that **reads, stores and summarises your daily WhatsApp, Gmail, call
history and SMS** — date-wise — plus a built-in **task manager** and **meeting
minutes recorder**.

All data stays on the device (Room database + app-private storage). An optional
AI daily summary via the Claude API can be enabled with your own API key.

## Features

| Area | What it does |
|---|---|
| **WhatsApp & Gmail capture** | Captures incoming messages/emails from their notifications via a `NotificationListenerService` (WhatsApp has no public API, so notification capture is the only on-device approach). |
| **SMS** | Imports received and sent SMS from the system SMS provider. |
| **Call history** | Imports incoming/outgoing/missed/rejected calls with durations from the system call log. |
| **Date-wise daily summary** | Browse any day with ‹ › navigation. On-device summary: message counts per source, most active conversations, missed calls, longest calls, and "possibly important" messages (urgent/meeting/payment/deadline keywords). |
| **AI summary (optional)** | Sends the selected day's digest to the Claude API (`claude-opus-4-8`) and produces a structured summary: Overview, Key conversations, Emails, Calls, Follow-ups/action items. Requires your own Anthropic API key (Settings). |
| **Task manager** | Add/complete/delete tasks with Low/Med/High priority. |
| **Minutes of meeting recorder** | Record meeting audio (.m4a), take minutes, list attendees and action items — and push action items straight into the task list. Playback built in. |
| **Background sync** | WorkManager job every 6 hours imports SMS/calls and refreshes summaries; yesterday's summary can include the AI version automatically. |

## Project structure

```
app/src/main/java/com/pdnp/dailydigest/
├── DigestApp.kt                     # Application: schedules background sync
├── MainActivity.kt                  # Compose UI shell + runtime permissions
├── AppViewModel.kt                  # State & actions for all screens
├── capture/
│   ├── NotificationCaptureService.kt  # WhatsApp/Gmail notification listener
│   ├── SmsImporter.kt                 # SMS provider import
│   └── CallLogImporter.kt             # Call log provider import
├── data/                            # Room entities, DAOs, DB, prefs, date utils
├── summary/
│   ├── SummaryGenerator.kt          # On-device rule-based daily summary
│   └── ClaudeSummarizer.kt          # Optional AI summary (Anthropic Java SDK)
├── meeting/MeetingRecorder.kt       # MediaRecorder + playback
├── work/DailySyncWorker.kt          # Periodic background sync
└── ui/                              # Compose theme + 5 screens
```

Tech stack: **Kotlin, Jetpack Compose (Material 3), Room, WorkManager,
EncryptedSharedPreferences, Anthropic Java SDK**. Min SDK 26 (Android 8.0),
target SDK 35.

## Building

1. Open the project in **Android Studio** (Koala or newer). The Gradle wrapper
   (Gradle 8.9, AGP 8.5.2, Kotlin 2.0.20) downloads everything else.
2. `Run` on a device — or `./gradlew assembleDebug` for an APK.

## First-run setup

1. **Grant runtime permissions** when prompted (SMS, call log, microphone,
   notifications).
2. **Grant Notification Access** — Settings tab → *Open notification access
   settings* → enable **Daily Digest message capture**. Without this,
   WhatsApp/Gmail messages are not captured.
3. (Optional) **AI summaries** — Settings tab → paste your Anthropic API key →
   *Save key*. Tap **AI summary** on the Digest tab, or enable the switch to
   include an AI summary in the daily background sync.

## How capture works & limitations

- **WhatsApp/Gmail**: only messages that produce a notification are captured
  (capture starts from the moment access is granted; there is no history
  backfill, and muted chats or notifications dismissed before delivery are
  missed). Message previews must be enabled in each app's notification
  settings.
- **SMS & calls**: full history is available from the system providers, so
  these backfill correctly.
- **AI summary**: the selected day's messages/calls are sent to the Claude API
  only when you tap *AI summary* (or enable it for the background sync).
  Everything else is fully offline.

## Privacy & distribution note

`READ_SMS` / `READ_CALL_LOG` are restricted permissions on Google Play — this
app is intended for **personal use / sideloading**, not Play Store
distribution. All data is stored in the app's private Room database; meeting
recordings live in app-private storage.

## Ideas for later

- Speech-to-text transcription of meeting recordings
- Export summaries to PDF / share sheet
- Contact name resolution for SMS/call numbers
- Search across all captured messages
