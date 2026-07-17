package com.pdnp.dailydigest.capture

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.GmailScopes
import com.google.api.services.gmail.model.MessagePart
import com.pdnp.dailydigest.data.AppDatabase
import com.pdnp.dailydigest.data.CapturedMessage
import com.pdnp.dailydigest.data.DayKeys
import com.pdnp.dailydigest.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Imports full emails via the official Gmail API (OAuth, `gmail.readonly` scope).
 *
 * When a Google account is connected (Settings tab), this replaces
 * notification-based Gmail capture: it backfills history and stores complete
 * subjects and bodies instead of notification previews.
 */
object GmailImporter {

    private const val LAST_SYNC_KEY = "last_gmail_api_sync"
    private const val FIRST_SYNC_DAYS = 7L
    private const val MAX_MESSAGES_PER_SYNC = 300
    private const val BODY_CHAR_LIMIT = 2_000

    val READONLY_SCOPE = Scope(GmailScopes.GMAIL_READONLY)

    /** True when a Google account with the Gmail read scope is connected. */
    fun isConnected(context: Context): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return false
        return GoogleSignIn.hasPermissions(account, READONLY_SCOPE)
    }

    fun connectedEmail(context: Context): String? =
        GoogleSignIn.getLastSignedInAccount(context)
            ?.takeIf { GoogleSignIn.hasPermissions(it, READONLY_SCOPE) }
            ?.email

    suspend fun import(context: Context) = withContext(Dispatchers.IO) {
        val googleAccount = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext
        if (!GoogleSignIn.hasPermissions(googleAccount, READONLY_SCOPE)) return@withContext
        val androidAccount = googleAccount.account ?: return@withContext

        val credential = GoogleAccountCredential
            .usingOAuth2(context, listOf(GmailScopes.GMAIL_READONLY))
            .apply { selectedAccount = androidAccount }

        val gmail = Gmail.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
            .setApplicationName("Daily Digest")
            .build()

        val since = Prefs.lastSync(context, LAST_SYNC_KEY)
        // First sync: last 7 days. Later syncs: overlap by 1h; dedupe is by Gmail
        // message id, so overlapping windows never create duplicates.
        val afterSeconds = if (since == 0L) {
            System.currentTimeMillis() / 1000 - FIRST_SYNC_DAYS * 24 * 3600
        } else {
            since / 1000 - 3600
        }
        val query = "after:$afterSeconds -in:spam -in:trash"

        try {
            val ids = mutableListOf<String>()
            var pageToken: String? = null
            do {
                val response = gmail.users().messages().list("me")
                    .setQ(query)
                    .setMaxResults(100L)
                    .setPageToken(pageToken)
                    .execute()
                response.messages?.forEach { ids += it.id }
                pageToken = response.nextPageToken
            } while (pageToken != null && ids.size < MAX_MESSAGES_PER_SYNC)

            val items = mutableListOf<CapturedMessage>()
            var newest = since

            for (id in ids.take(MAX_MESSAGES_PER_SYNC)) {
                val message = try {
                    gmail.users().messages().get("me", id).setFormat("full").execute()
                } catch (e: Exception) {
                    continue
                }
                val timestamp = message.internalDate ?: continue
                if (timestamp > newest) newest = timestamp

                val headers = message.payload?.headers.orEmpty()
                fun header(name: String) =
                    headers.firstOrNull { it.name.equals(name, ignoreCase = true) }?.value ?: ""

                val subject = header("Subject")
                val bodyText = extractBody(message.payload)
                    .ifBlank { message.snippet ?: "" }
                val body = buildString {
                    if (subject.isNotBlank()) appendLine("Subject: $subject")
                    append(bodyText.take(BODY_CHAR_LIMIT))
                }.trim()
                if (body.isEmpty()) continue

                items += CapturedMessage(
                    source = "GMAIL",
                    sender = displayName(header("From")),
                    body = body,
                    timestamp = timestamp,
                    dayKey = DayKeys.of(timestamp),
                    dedupeKey = "gmail:${message.id}"
                )
            }

            if (items.isNotEmpty()) {
                AppDatabase.get(context).messageDao().insertAll(items)
            }
            if (newest > since) Prefs.setLastSync(context, LAST_SYNC_KEY, newest)
        } catch (e: Exception) {
            // Network/auth failure — leave lastSync untouched and retry next sync.
            // A UserRecoverableAuthIOException resolves once the user re-connects
            // the account in Settings.
        }
    }

    /** Walks the MIME tree preferring text/plain; falls back to tag-stripped HTML. */
    private fun extractBody(part: MessagePart?): String {
        if (part == null) return ""
        val plain = findByMime(part, "text/plain")
        if (plain.isNotBlank()) return plain
        val html = findByMime(part, "text/html")
        if (html.isNotBlank()) {
            return html
                .replace(Regex("(?is)<(style|script).*?</\\1>"), " ")
                .replace(Regex("<[^>]+>"), " ")
                .replace(Regex("&nbsp;|&#160;"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }
        return ""
    }

    private fun findByMime(part: MessagePart, mime: String): String {
        if (part.mimeType == mime) {
            val data = part.body?.decodeData()
            if (data != null) return String(data, Charsets.UTF_8)
        }
        part.parts?.forEach { child ->
            val found = findByMime(child, mime)
            if (found.isNotBlank()) return found
        }
        return ""
    }

    /** "Jane Doe <jane@x.com>" -> "Jane Doe"; bare addresses pass through. */
    private fun displayName(from: String): String {
        val name = from.substringBefore('<').trim().trim('"')
        return name.ifBlank { from.trim().removeSurrounding("<", ">") }.ifBlank { "unknown" }
    }
}
