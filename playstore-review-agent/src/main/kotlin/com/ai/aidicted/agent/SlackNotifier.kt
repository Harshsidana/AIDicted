package com.ai.aidicted.agent

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Posts a Slack message using an Incoming Webhook URL.
 *
 * Setup:
 *  1. Go to https://api.slack.com/apps → Create App → From Scratch
 *  2. Features → Incoming Webhooks → Activate → Add New Webhook to Workspace
 *  3. Choose your channel, copy the Webhook URL
 *  4. Set: export SLACK_WEBHOOK_URL="https://hooks.slack.com/services/..."
 */
class SlackNotifier(private val webhookUrl: String) {

    private val json = Json { encodeDefaults = true }

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json(json) }
    }

    /**
     * Sends [text] as a Slack mrkdwn message via the simplest possible payload.
     * Returns (success, message).
     */
    suspend fun send(text: String): Pair<Boolean, String> {
        return try {
            // Use the plain {"text": "..."} payload — works with all Incoming Webhooks,
            // no Block Kit complexity that can cause "invalid_blocks" errors.
            val payloadJson = buildJsonObject {
                put("text", text.take(3000))  // Slack message limit
            }.toString()

            println("[Slack] Sending payload (${payloadJson.length} chars)...")

            val response: HttpResponse = httpClient.post(webhookUrl) {
                contentType(ContentType.Application.Json)
                setBody(payloadJson)
            }

            if (response.status.isSuccess()) {
                println("[Slack] ✅ Message sent successfully (${response.status})")
                true to "OK"
            } else {
                val body = response.bodyAsText()
                System.err.println("[Slack] ❌ Failed: ${response.status} — $body")
                false to "${response.status}: $body"
            }
        } catch (e: Exception) {
            System.err.println("[Slack] ❌ Exception: ${e.message}")
            false to (e.message ?: "Unknown error")
        } finally {
            httpClient.close()
        }
    }
}
