package com.ai.aidicted.agent

import ai.koog.agents.core.agent.AIAgentService
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.ext.agent.reActStrategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.tools
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.params.LLMParams
import kotlinx.coroutines.runBlocking

/**
 * Entry point — AIDicted Play Store Review Agent.
 *
 * ──────────────────────────────────────────────────────────────
 * REQUIRED ENVIRONMENT VARIABLES
 * ──────────────────────────────────────────────────────────────
 *  GEMINI_API_KEY               → Google AI Studio API key
 *                                 https://aistudio.google.com/app/apikey
 *  GOOGLE_SERVICE_ACCOUNT_JSON  → Absolute path to service-account JSON key
 *                                 See .env.example for setup steps
 *  SLACK_WEBHOOK_URL            → Slack Incoming Webhook URL
 *                                 See .env.example for setup steps
 * ──────────────────────────────────────────────────────────────
 *
 * HOW TO RUN LOCALLY
 * ──────────────────────────────────────────────────────────────
 *  export GEMINI_API_KEY="your_key"
 *  export GOOGLE_SERVICE_ACCOUNT_JSON="/path/to/service-account.json"
 *  export SLACK_WEBHOOK_URL="https://hooks.slack.com/services/..."
 *  ./gradlew run
 * ──────────────────────────────────────────────────────────────
 */
fun main() = runBlocking {
    println("═══════════════════════════════════════════════════════")
    println("  AIDicted Play Store Review Agent — Starting up 🚀")
    println("═══════════════════════════════════════════════════════")

    // ── Validate required env vars ────────────────────────────────────────────
    val geminiApiKey = System.getenv("GEMINI_API_KEY")
        ?: error("❌ GEMINI_API_KEY env var not set. Get one at https://aistudio.google.com/app/apikey")

    val slackUrl = System.getenv("SLACK_WEBHOOK_URL")
        ?: error("❌ SLACK_WEBHOOK_URL env var not set. See .env.example for setup steps.")

    // ── Build tool dependencies ───────────────────────────────────────────────
    val fetcher = PlayStoreReviewFetcher()
    val slack   = SlackNotifier(slackUrl)
    val toolSet = ReviewAgentTools(fetcher, slack)

    // ── Register tools ─────────────────────────────────────────────────────────
    val toolRegistry = ToolRegistry {
        tools(toolSet)   // extension fn from ai.koog.agents.core.tools.reflect
    }

    // ── Prompt & model config ─────────────────────────────────────────────────
    val agentConfig = AIAgentConfig(
        prompt = prompt("aidicted-review-agent", LLMParams()) {
            system(
                """
                You are an expert App Store Review Analyst for the AIDicted app on Google Play Store.
                
                Your job: produce a clear, actionable daily review report and deliver it to Slack.
                
                Always follow these steps in order — do NOT skip any:
                1. Call `fetch_play_reviews` with maxResults=100.
                2. Call `compute_review_stats` with the JSON returned from step 1.
                3. Write a comprehensive report in Slack mrkdwn format:
                   - Header: "📱 *AIDicted Daily Review Report*"
                   - One-line sentiment summary (Positive / Mixed / Needs Attention)
                   - *📊 Overview*: total reviews, average rating as ★ stars
                   - *⭐ Rating Breakdown*: each star count from 5★ to 1★
                   - *👍 What Users Love*: 3 highlights with short quoted snippets
                   - *⚠️ Pain Points*: 3 key complaints with short quoted snippets
                   - *🚀 Recommendations*: 3 concrete, actionable suggestions
                4. Call `send_slack_report` with the complete formatted report.
                5. Confirm the report was sent and stop.
                
                Rules:
                - Never fabricate review content. Only use data returned by tools.
                - If there are 0 reviews, still call send_slack_report with a "no reviews yet" message.
                - Keep the full report under 2800 characters to fit Slack block limits.
                - Use Slack mrkdwn: *bold*, _italic_, • bullets, emoji encouraged.
                """.trimIndent()
            )
        },
        model = GoogleModels.Gemini2_5Flash,
        maxAgentIterations = 15,
    )

    // ── Create agent service & run ────────────────────────────────────────────
    val agentService = AIAgentService(
        promptExecutor = simpleGoogleAIExecutor(geminiApiKey),
        agentConfig    = agentConfig,
        strategy       = reActStrategy(20),
        toolRegistry   = toolRegistry,
    )

    println("\n🤖 Agent starting analysis...\n")

    val result = agentService.createAgentAndRun(
        "Fetch all recent Play Store reviews for the AIDicted app, " +
        "analyse them to identify key trends and pain points, " +
        "then write and send a comprehensive daily report to our Slack channel.",
        "review-reporter",
        toolRegistry,
        agentConfig,
    )

    println("\n═══════════════════════════════════════════════════════")
    println("  ✅ Agent completed.")
    println("  Result: $result")
    println("═══════════════════════════════════════════════════════\n")
}
