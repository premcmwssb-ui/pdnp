package com.pdnp.dailydigest.summary

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import com.pdnp.dailydigest.data.CallEntry
import com.pdnp.dailydigest.data.CapturedMessage
import com.pdnp.dailydigest.data.DayKeys

/**
 * Optional AI summary of the day via the Claude API (Anthropic Java SDK).
 * The user supplies their own API key in Settings; nothing is sent anywhere
 * unless AI summaries are explicitly requested.
 */
object ClaudeSummarizer {

    private const val SYSTEM_PROMPT =
        "You summarize one day of a user's personal communications " +
            "(WhatsApp, Gmail, SMS and phone calls). Write a concise daily summary " +
            "with these sections: Overview, Key conversations, Emails, Calls, " +
            "Follow-ups / action items. Be specific about people and topics, and " +
            "flag anything urgent or awaiting a reply. Keep it under 400 words."

    @Volatile
    private var cachedClient: Pair<String, AnthropicClient>? = null

    private fun client(apiKey: String): AnthropicClient {
        cachedClient?.let { (key, client) -> if (key == apiKey) return client }
        val client = AnthropicOkHttpClient.builder().apiKey(apiKey).build()
        cachedClient = apiKey to client
        return client
    }

    /** Blocking network call — invoke from a background dispatcher. */
    fun summarize(
        apiKey: String,
        dayKey: String,
        messages: List<CapturedMessage>,
        calls: List<CallEntry>
    ): String {
        val digest = buildString {
            appendLine("Date: $dayKey")
            appendLine()
            appendLine("--- Calls (${calls.size}) ---")
            calls.forEach {
                appendLine(
                    "${DayKeys.timeOf(it.timestamp)} ${it.type} " +
                        "${it.contactName.ifBlank { it.number }} (${it.durationSec}s)"
                )
            }
            appendLine()
            appendLine("--- Messages (${messages.size}) ---")
            // Cap the payload: latest 200 messages, 300 chars each
            messages.takeLast(200).forEach {
                appendLine("${DayKeys.timeOf(it.timestamp)} [${it.source}] ${it.sender}: ${it.body.take(300)}")
            }
        }

        val params = MessageCreateParams.builder()
            .model(Model.CLAUDE_OPUS_4_8)
            .maxTokens(2048L)
            .system(SYSTEM_PROMPT)
            .addUserMessage(digest)
            .build()

        val response = client(apiKey).messages().create(params)
        return response.content()
            .mapNotNull { block -> block.text().map { it.text() }.orElse(null) }
            .joinToString("\n")
            .trim()
            .ifEmpty { "No summary returned." }
    }
}
