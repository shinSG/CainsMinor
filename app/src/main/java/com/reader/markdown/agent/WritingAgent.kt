package com.reader.markdown.agent

import android.util.Log
import com.reader.markdown.settings.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Markdown 辅助写作 Agent
 * 基于当前编辑上下文，提供内联写作建议
 */
class WritingAgent(
    private val settings: SettingsManager,
    private val llmService: LlmService
) {
    companion object {
        private const val TAG = "WritingAgent"

        // 上下文窗口：光标前后的字符数
        private const val CONTEXT_BEFORE = 2000
        private const val CONTEXT_AFTER = 500
    }

    // 当前建议状态
    private val _suggestion = MutableStateFlow<Suggestion>(Suggestion.Empty)
    val suggestion: StateFlow<Suggestion> = _suggestion

    // 是否正在生成
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    /**
     * 请求续写建议
     * @param fullContent 文档完整内容
     * @param cursorPosition 光标位置（字符索引）
     */
    suspend fun requestCompletion(fullContent: String, cursorPosition: Int) {
        if (!settings.isLlmConfigured) {
            _suggestion.value = Suggestion.Error("请先在设置中配置 LLM API")
            return
        }

        _isGenerating.value = true
        _suggestion.value = Suggestion.Loading

        try {
            // 提取光标前后上下文
            val before = fullContent.substring(
                maxOf(0, cursorPosition - CONTEXT_BEFORE),
                cursorPosition
            )
            val after = fullContent.substring(
                cursorPosition,
                minOf(fullContent.length, cursorPosition + CONTEXT_AFTER)
            )

            val messages = buildMessages(before, after, "completion")

            val result = llmService.chatCompletion(messages, stream = false)
            result.fold(
                onSuccess = { text ->
                    val cleaned = cleanSuggestion(text, before)
                    if (cleaned.isNotBlank()) {
                        _suggestion.value = Suggestion.Text(cleaned)
                    } else {
                        _suggestion.value = Suggestion.Empty
                    }
                },
                onFailure = { e ->
                    _suggestion.value = Suggestion.Error(e.message ?: "生成失败")
                }
            )
        } catch (e: Exception) {
            _suggestion.value = Suggestion.Error(e.message ?: "未知错误")
        } finally {
            _isGenerating.value = false
        }
    }

    /**
     * 请求流式续写
     */
    suspend fun requestCompletionStream(fullContent: String, cursorPosition: Int) {
        if (!settings.isLlmConfigured) {
            _suggestion.value = Suggestion.Error("请先在设置中配置 LLM API")
            return
        }

        _isGenerating.value = true
        _suggestion.value = Suggestion.Loading

        val before = fullContent.substring(maxOf(0, cursorPosition - CONTEXT_BEFORE), cursorPosition)
        val after = fullContent.substring(cursorPosition, minOf(fullContent.length, cursorPosition + CONTEXT_AFTER))
        val messages = buildMessages(before, after, "completion")

        val result = StringBuilder()
        llmService.chatCompletionStream(
            messages = messages,
            onChunk = { chunk ->
                result.append(chunk)
                _suggestion.value = Suggestion.Streaming(result.toString())
            },
            onDone = {
                val cleaned = cleanSuggestion(result.toString(), before)
                _suggestion.value = if (cleaned.isNotBlank()) Suggestion.Text(cleaned) else Suggestion.Empty
                _isGenerating.value = false
            },
            onError = { e ->
                _suggestion.value = Suggestion.Error(e.message ?: "生成失败")
                _isGenerating.value = false
            }
        )
    }

    /**
     * 请求改写/润色
     */
    suspend fun requestRewrite(selectedText: String, instruction: String) {
        if (!settings.isLlmConfigured) {
            _suggestion.value = Suggestion.Error("请先在设置中配置 LLM API")
            return
        }

        _isGenerating.value = true
        _suggestion.value = Suggestion.Loading

        val messages = listOf(
            ChatMessage("system", settings.llmSystemPrompt),
            ChatMessage("user", "请根据以下指令改写这段Markdown文本，只返回改写后的内容，不要解释：\n\n指令：$instruction\n\n原文：\n$selectedText")
        )

        val result = llmService.chatCompletion(messages, stream = false)
        result.fold(
            onSuccess = { text ->
                val cleaned = text.trim().removeSurrounding("```")
                _suggestion.value = Suggestion.Text(cleaned)
            },
            onFailure = { e ->
                _suggestion.value = Suggestion.Error(e.message ?: "改写失败")
            }
        )
        _isGenerating.value = false
    }

    /**
     * 清除当前建议
     */
    fun dismiss() {
        _suggestion.value = Suggestion.Empty
    }

    // ── 内部方法 ──

    private fun buildMessages(before: String, after: String, mode: String): List<ChatMessage> {
        val systemPrompt = settings.llmSystemPrompt.ifBlank {
            "你是一个Markdown文档助手。"
        }

        val userPrompt = buildString {
            appendLine("请根据上下文续写Markdown内容。只返回续写的文本，不要包含已有内容，不要解释。")
            appendLine()
            appendLine("【光标前的内容】")
            appendLine(before)
            if (after.isNotBlank()) {
                appendLine()
                appendLine("【光标后的内容】")
                appendLine(after)
            }
            appendLine()
            appendLine("请从光标位置开始续写：")
        }

        return listOf(
            ChatMessage("system", systemPrompt),
            ChatMessage("user", userPrompt)
        )
    }

    /**
     * 清理建议文本，去掉重复的前缀
     */
    private fun cleanSuggestion(suggestion: String, before: String): String {
        var cleaned = suggestion.trimStart('\n')

        // 如果建议以光标前文本的结尾开头，去掉重复部分
        val lastLine = before.lines().lastOrNull()?.trim() ?: ""
        if (lastLine.isNotBlank() && cleaned.startsWith(lastLine)) {
            cleaned = cleaned.removePrefix(lastLine).trimStart('\n')
        }

        // 去掉 markdown 代码块包裹
        if (cleaned.startsWith("```") && cleaned.endsWith("```")) {
            val firstNewline = cleaned.indexOf('\n')
            if (firstNewline > 0) {
                cleaned = cleaned.substring(firstNewline + 1, cleaned.length - 3).trim()
            }
        }

        return cleaned
    }
}

/**
 * 建议类型
 */
sealed class Suggestion {
    object Empty : Suggestion()
    object Loading : Suggestion()
    data class Text(val content: String) : Suggestion()
    data class Streaming(val partial: String) : Suggestion()
    data class Error(val message: String) : Suggestion()
}
