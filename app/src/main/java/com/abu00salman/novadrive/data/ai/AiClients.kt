package com.abu00salman.novadrive.data.ai

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.Tool
import com.anthropic.models.messages.ToolResultBlockParam
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** One finished exchange kept as context for the follow-up question. */
data class Turn(val user: String, val assistant: String)

/** Runs a tool call for the model and returns the factual result text. */
typealias ToolRunner = suspend (name: String, argsJson: String) -> String

/** Called before each tool runs, so the UI can show "doing …". */
typealias ToolNotice = (name: String) -> Unit

interface AiEngine {
    suspend fun reply(system: String, history: List<Turn>, user: String, tools: List<ToolSpec>, run: ToolRunner, notice: ToolNotice): String
}

class AiException(message: String) : Exception(message)

private const val MAX_TOOL_ROUNDS = 6

/** Claude through the official Anthropic Java SDK, with a manual tool-use loop. */
class ClaudeEngine(private val apiKey: String, private val model: String, private val proxyUrl: String?) : AiEngine {
    private val mapper = ObjectMapper()

    override suspend fun reply(system: String, history: List<Turn>, user: String, tools: List<ToolSpec>, run: ToolRunner, notice: ToolNotice): String {
        val client = withContext(Dispatchers.IO) {
            AnthropicOkHttpClient.builder().apiKey(apiKey).apply { if (!proxyUrl.isNullOrBlank()) baseUrl(proxyUrl) }.build()
        }
        try {
            val params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(2048L)
                .system(system)
                // Voice replies should come back fast; low effort keeps answers short and quick.
                .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).build())
                // Server-side fallback if the model declines a request.
                .putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
                .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
            tools.forEach { t ->
                params.addTool(
                    Tool.builder().name(t.name).description(t.description)
                        .inputSchema(
                            Tool.InputSchema.builder()
                                .properties(Tool.InputSchema.Properties.builder().apply {
                                    t.properties.forEach { (k, v) -> putAdditionalProperty(k, JsonValue.from(v)) }
                                }.build())
                                .required(t.required)
                                .build()
                        ).build()
                )
            }
            history.forEach { params.addUserMessage(it.user); params.addAssistantMessage(it.assistant) }
            params.addUserMessage(user)

            repeat(MAX_TOOL_ROUNDS) {
                val response = withContext(Dispatchers.IO) { client.messages().create(params.build()) }
                if (response.stopReason().map { it.toString() }.orElse("") == "refusal") return "عذرًا، لا أستطيع المساعدة في هذا الطلب."
                params.addMessage(response)
                val uses = response.content().mapNotNull { it.toolUse().orElse(null) }
                if (uses.isEmpty()) {
                    return response.content().mapNotNull { it.text().orElse(null)?.text() }.joinToString(" ").trim()
                }
                val results = uses.map { use ->
                    notice(use.name())
                    val out = run(use.name(), mapper.writeValueAsString(use._input()))
                    ContentBlockParam.ofToolResult(ToolResultBlockParam.builder().toolUseId(use.id()).content(out).build())
                }
                params.addUserMessageOfContentBlockParams(results)
            }
            return ""
        } catch (e: AnthropicServiceException) {
            throw AiException("Claude: " + (e.message ?: "HTTP ${e.statusCode()}"))
        } finally {
            withContext(Dispatchers.IO) { runCatching { client.close() } }
        }
    }
}

/** OpenAI Chat Completions with function calling (plain HTTPS; no OpenAI SDK on Android). */
class OpenAiEngine(private val apiKey: String, private val model: String, private val proxyUrl: String?) : AiEngine {
    override suspend fun reply(system: String, history: List<Turn>, user: String, tools: List<ToolSpec>, run: ToolRunner, notice: ToolNotice): String {
        val messages = JSONArray().put(JSONObject().put("role", "system").put("content", system))
        history.forEach {
            messages.put(JSONObject().put("role", "user").put("content", it.user))
            messages.put(JSONObject().put("role", "assistant").put("content", it.assistant))
        }
        messages.put(JSONObject().put("role", "user").put("content", user))
        val toolJson = JSONArray().apply {
            tools.forEach { t ->
                put(JSONObject().put("type", "function").put("function", JSONObject()
                    .put("name", t.name).put("description", t.description)
                    .put("parameters", JSONObject().put("type", "object").put("properties", JSONObject(t.properties)).put("required", JSONArray(t.required)))))
            }
        }
        repeat(MAX_TOOL_ROUNDS) {
            val body = JSONObject().put("model", model).put("messages", messages).put("tools", toolJson).put("max_tokens", 600)
            val res = post(body)
            val msg = res.getJSONArray("choices").getJSONObject(0).getJSONObject("message")
            val calls = msg.optJSONArray("tool_calls")
            if (calls == null || calls.length() == 0) return msg.optString("content").trim()
            messages.put(msg)
            for (i in 0 until calls.length()) {
                val call = calls.getJSONObject(i)
                val fn = call.getJSONObject("function")
                notice(fn.getString("name"))
                val out = run(fn.getString("name"), fn.optString("arguments", "{}"))
                messages.put(JSONObject().put("role", "tool").put("tool_call_id", call.getString("id")).put("content", out))
            }
        }
        return ""
    }

    private suspend fun post(body: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val base = proxyUrl?.trimEnd('/')?.takeIf { it.isNotBlank() } ?: "https://api.openai.com"
        val conn = (URL("$base/v1/chat/completions").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; doOutput = true; connectTimeout = 15_000; readTimeout = 60_000
            setRequestProperty("Content-Type", "application/json")
            if (apiKey.isNotBlank()) setRequestProperty("Authorization", "Bearer $apiKey")
        }
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw AiException("OpenAI HTTP $code: " + runCatching { JSONObject(text).getJSONObject("error").getString("message") }.getOrDefault(text.take(200)))
            JSONObject(text)
        } finally { conn.disconnect() }
    }
}
