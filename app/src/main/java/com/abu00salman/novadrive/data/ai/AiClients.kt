package com.abu00salman.novadrive.data.ai

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.abu00salman.novadrive.data.voice.ProviderException
import com.anthropic.errors.AnthropicIoException
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.errors.BadRequestException
import com.anthropic.errors.InternalServerException
import com.anthropic.errors.NotFoundException
import com.anthropic.errors.PermissionDeniedException
import com.anthropic.errors.RateLimitException
import com.anthropic.errors.UnauthorizedException
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.Tool
import com.anthropic.models.messages.ToolResultBlockParam
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.time.Duration

/** One finished exchange kept as context for the follow-up question. */
data class Turn(val user: String, val assistant: String)

/** Runs a tool call for the model and returns the factual result text. */
typealias ToolRunner = suspend (name: String, argsJson: String) -> String

/** Called before each tool runs, so the UI can show "doing …". */
typealias ToolNotice = (name: String) -> Unit

interface AiEngine {
    /** Throws [ProviderException] with what went wrong (bad key, no credit, no connection, …). */
    suspend fun reply(system: String, history: List<Turn>, user: String, tools: List<ToolSpec>, run: ToolRunner, notice: ToolNotice): String

    /** Settings › Connection test: one tiny real request; returns the model's short answer. */
    suspend fun ping(): String = reply("Reply with the single word: OK", emptyList(), "ping", emptyList(), { _, _ -> "" }, { })
}

private const val MAX_TOOL_ROUNDS = 6

/** Claude through the official Anthropic Java SDK, with a manual tool-use loop. */
class ClaudeEngine(private val apiKey: String, private val model: String, private val proxyUrl: String?, private val timeoutMs: Long = 30_000) : AiEngine {
    private val mapper = ObjectMapper()

    override suspend fun reply(system: String, history: List<Turn>, user: String, tools: List<ToolSpec>, run: ToolRunner, notice: ToolNotice): String {
        val client = withContext(Dispatchers.IO) {
            AnthropicOkHttpClient.builder().apiKey(apiKey)
                .apply { if (!proxyUrl.isNullOrBlank()) baseUrl(proxyUrl) }
                // One retry at most (only for connection/overload errors), never a pile of paid duplicates.
                .timeout(Duration.ofMillis(timeoutMs)).maxRetries(1)
                .build()
        }
        try {
            val params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(2048L)
                .system(system)
                // Voice replies should come back fast; low effort keeps answers short and quick.
                .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).build())
                .apply {
                    // Server-side fallback if the model declines a request (first-party API only, not via a proxy).
                    if (proxyUrl.isNullOrBlank()) {
                        putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
                        putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
                    }
                }
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
                // Async + await: Stop/timeout returns at once instead of waiting for the blocked call.
                val response = client.async().messages().create(params.build()).await()
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
        } catch (e: UnauthorizedException) {
            throw ProviderException(ProviderException.Kind.AUTH, e.message)
        } catch (e: PermissionDeniedException) {
            throw ProviderException(ProviderException.Kind.AUTH, e.message)
        } catch (e: NotFoundException) {
            throw ProviderException(ProviderException.Kind.MODEL, e.message)
        } catch (e: RateLimitException) {
            throw ProviderException(ProviderException.Kind.RATE, e.message)
        } catch (e: BadRequestException) {
            val m = e.message.orEmpty()
            throw ProviderException(if (m.contains("credit", true) || m.contains("billing", true)) ProviderException.Kind.QUOTA
                else if (m.contains("model", true)) ProviderException.Kind.MODEL else ProviderException.Kind.OTHER, m)
        } catch (e: InternalServerException) {
            throw ProviderException(ProviderException.Kind.SERVER, e.message)
        } catch (e: AnthropicServiceException) {
            throw ProviderException(if (e.statusCode() == 529) ProviderException.Kind.SERVER else ProviderException.Kind.OTHER, e.message)
        } catch (e: AnthropicIoException) {
            throw ProviderException(if (e.cause is SocketTimeoutException || e.message.orEmpty().contains("timeout", true)) ProviderException.Kind.TIMEOUT else ProviderException.Kind.NETWORK, e.message)
        } finally {
            runCatching { client.close() }
        }
    }
}

/** OpenAI Chat Completions with function calling (plain HTTPS; no OpenAI SDK on Android). */
class OpenAiEngine(private val apiKey: String, private val model: String, private val proxyUrl: String?, private val timeoutMs: Long = 30_000) : AiEngine {
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
            val body = JSONObject().put("model", model).put("messages", messages).put("max_tokens", 600)
            if (tools.isNotEmpty()) body.put("tools", toolJson)
            val res = post(body)
            val msg = res.getJSONArray("choices").getJSONObject(0).getJSONObject("message")
            val calls = msg.optJSONArray("tool_calls")
            if (calls == null || calls.length() == 0) return if (msg.isNull("content")) "" else msg.optString("content").trim()
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

    private suspend fun post(body: JSONObject): JSONObject {
        val base = proxyUrl?.trimEnd('/')?.takeIf { it.isNotBlank() } ?: "https://api.openai.com"
        val conn = (URL("$base/v1/chat/completions").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; doOutput = true; connectTimeout = 15_000; readTimeout = timeoutMs.toInt()
            setRequestProperty("Content-Type", "application/json")
            if (apiKey.isNotBlank()) setRequestProperty("Authorization", "Bearer $apiKey")
        }
        val call = CompletableFuture.supplyAsync({ exchange(conn, body) }, Dispatchers.IO.asExecutor())
        return try {
            call.await()
        } catch (e: CancellationException) {
            // Stop button / new question / timeout: abort the socket instead of waiting for the answer.
            runCatching { conn.disconnect() }
            throw e
        }
    }

    private fun exchange(conn: HttpURLConnection, body: JSONObject): JSONObject = try {
        conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val text = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) {
            val err = runCatching { JSONObject(text).getJSONObject("error") }.getOrNull()
            val type = err?.optString("code").orEmpty() + " " + err?.optString("type").orEmpty()
            throw ProviderException(openAiKind(code, type), "HTTP $code " + (err?.optString("message") ?: text.take(200)))
        }
        JSONObject(text)
    } catch (e: SocketTimeoutException) {
        throw ProviderException(ProviderException.Kind.TIMEOUT, e.message)
    } catch (e: IOException) {
        throw ProviderException(ProviderException.Kind.NETWORK, e.message)
    } finally {
        conn.disconnect()
    }
}

/** OpenAI HTTP status + error code → what to tell the driver. */
internal fun openAiKind(code: Int, type: String): ProviderException.Kind = when {
    code == 401 || code == 403 -> ProviderException.Kind.AUTH
    code == 429 && type.contains("insufficient_quota") -> ProviderException.Kind.QUOTA
    code == 429 -> ProviderException.Kind.RATE
    code == 404 || type.contains("model_not_found") -> ProviderException.Kind.MODEL
    code >= 500 -> ProviderException.Kind.SERVER
    else -> ProviderException.Kind.OTHER
}
