package com.reader.markdown.agent

import android.util.Log
import com.reader.markdown.settings.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * OpenAI 兼容的 LLM API 客户端
 */
class LlmService(private val settings: SettingsManager) {

    companion object {
        private const val TAG = "LlmService"
        private const val TIMEOUT = 30000
    }

    /**
     * 同步调用 LLM API（chat/completions）
     * @param messages 消息列表，每条包含 role 和 content
     * @param stream 是否流式返回
     * @return 完整的回复文本
     */
    suspend fun chatCompletion(
        messages: List<ChatMessage>,
        temperature: Float = settings.llmTemperature,
        maxTokens: Int = settings.llmMaxTokens,
        stream: Boolean = true
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = settings.llmBaseUrl.trimEnd('/')
            val apiKey = settings.llmApiKey
            val model = settings.llmModelName

            if (baseUrl.isBlank() || apiKey.isBlank()) {
                return@withContext Result.failure(Exception("请先配置 LLM API"))
            }

            val url = URL("$baseUrl/chat/completions")
            val body = JSONObject().apply {
                put("model", model)
                put("temperature", temperature)
                put("max_tokens", maxTokens)
                put("stream", stream)
                put("messages", JSONArray().apply {
                    messages.forEach { msg ->
                        put(JSONObject().apply {
                            put("role", msg.role)
                            put("content", msg.content)
                        })
                    }
                })
            }

            Log.d(TAG, "Request: $url model=$model stream=$stream")

            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.connectTimeout = TIMEOUT
            conn.readTimeout = TIMEOUT
            conn.doOutput = true

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }

            if (conn.responseCode != 200) {
                val error = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP ${conn.responseCode}"
                Log.e(TAG, "API error: $error")
                return@withContext Result.failure(Exception("API 错误: ${conn.responseCode}"))
            }

            if (stream) {
                // 流式读取 SSE
                val result = StringBuilder()
                BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val l = line ?: continue
                        if (!l.startsWith("data: ")) continue
                        val data = l.removePrefix("data: ").trim()
                        if (data == "[DONE]") break
                        try {
                            val json = JSONObject(data)
                            val delta = json.getJSONArray("choices")
                                .getJSONObject(0)
                                .optJSONObject("delta")
                            val content = delta?.optString("content")
                            if (content != null) {
                                result.append(content)
                            }
                        } catch (e: Exception) {
                            // skip malformed chunks
                        }
                    }
                }
                conn.disconnect()
                Log.d(TAG, "Stream result: ${result.length} chars")
                Result.success(result.toString())
            } else {
                // 非流式
                val response = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                val json = JSONObject(response)
                val content = json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                Log.d(TAG, "Result: ${content.length} chars")
                Result.success(content)
            }
        } catch (e: Exception) {
            Log.e(TAG, "chatCompletion failed", e)
            Result.failure(e)
        }
    }

    /**
     * 流式调用，逐 chunk 回调
     */
    suspend fun chatCompletionStream(
        messages: List<ChatMessage>,
        onChunk: (String) -> Unit,
        onDone: () -> Unit,
        onError: (Exception) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val baseUrl = settings.llmBaseUrl.trimEnd('/')
            val apiKey = settings.llmApiKey
            val model = settings.llmModelName

            if (baseUrl.isBlank() || apiKey.isBlank()) {
                onError(Exception("请先配置 LLM API"))
                return@withContext
            }

            val url = URL("$baseUrl/chat/completions")
            val body = JSONObject().apply {
                put("model", model)
                put("temperature", settings.llmTemperature)
                put("max_tokens", settings.llmMaxTokens)
                put("stream", true)
                put("messages", JSONArray().apply {
                    messages.forEach { msg ->
                        put(JSONObject().apply {
                            put("role", msg.role)
                            put("content", msg.content)
                        })
                    }
                })
            }

            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.connectTimeout = TIMEOUT
            conn.readTimeout = TIMEOUT
            conn.doOutput = true

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }

            if (conn.responseCode != 200) {
                val error = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP ${conn.responseCode}"
                onError(Exception("API 错误: ${conn.responseCode}"))
                return@withContext
            }

            BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val l = line ?: continue
                    if (!l.startsWith("data: ")) continue
                    val data = l.removePrefix("data: ").trim()
                    if (data == "[DONE]") break
                    try {
                        val json = JSONObject(data)
                        val delta = json.getJSONArray("choices")
                            .getJSONObject(0)
                            .optJSONObject("delta")
                        val content = delta?.optString("content")
                        if (content != null) {
                            onChunk(content)
                        }
                    } catch (_: Exception) {}
                }
            }
            conn.disconnect()
            onDone()
        } catch (e: Exception) {
            Log.e(TAG, "stream failed", e)
            onError(e)
        }
    }
}

data class ChatMessage(
    val role: String,  // "system" / "user" / "assistant"
    val content: String
)
