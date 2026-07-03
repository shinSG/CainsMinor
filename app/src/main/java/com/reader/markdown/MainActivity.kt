package com.reader.markdown

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.reader.markdown.ui.screens.FileBrowserScreen
import com.reader.markdown.ui.screens.MarkdownViewerScreen
import com.reader.markdown.ui.theme.MarkdownReaderTheme
import java.io.File

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "CainsMinor"
        var lastDebugLog = ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 在 setContent 之前解析 intent，读取文件内容写入缓存
        val externalFile = resolveIntent(intent)
        Log.d(TAG, "onCreate externalFile=$externalFile")

        setContent {
            MarkdownReaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 用状态管理当前显示的文件路径，null = 显示文件浏览器
                    var currentFilePath by remember { mutableStateOf(externalFile?.first) }
                    var currentDisplayName by remember { mutableStateOf(externalFile?.second) }
                    var fromExternal by remember { mutableStateOf(externalFile != null) }

                    if (currentFilePath != null) {
                        // 显示 Markdown 阅读页
                        MarkdownViewerScreen(
                            filePath = currentFilePath!!,
                            displayTitle = currentDisplayName,
                            onBack = {
                                if (fromExternal) {
                                    finish() // 从外部打开的，返回桌面
                                } else {
                                    currentFilePath = null
                                    currentDisplayName = null
                                    fromExternal = false
                                }
                            }
                        )
                    } else {
                        // 显示文件浏览器
                        FileBrowserScreen(
                            onFileSelected = { filePath ->
                                currentFilePath = filePath
                                currentDisplayName = null
                                fromExternal = false
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent: action=${intent.action}")
        val externalFile = resolveIntent(intent)
        if (externalFile != null) {
            // 重新创建 Activity 来打开新文件
            recreate()
        }
    }

    /**
     * 解析 intent，读取 content:// 文件内容写入缓存。
     * @return Pair(缓存文件绝对路径, 原始文件名) 或 null
     */
    private fun resolveIntent(intent: Intent?): Pair<String, String>? {
        if (intent == null) {
            lastDebugLog = "intent is null"
            return null
        }

        val sb = StringBuilder()
        sb.appendLine("action=${intent.action}")
        sb.appendLine("type=${intent.type}")
        sb.appendLine("data=${intent.data}")

        val uri: Uri? = when {
            intent.action == Intent.ACTION_VIEW && intent.data != null -> {
                sb.appendLine("来源: ACTION_VIEW")
                intent.data
            }
            intent.action == Intent.ACTION_SEND -> {
                sb.appendLine("来源: ACTION_SEND")
                getParcelableExtraCompat(intent, Intent.EXTRA_STREAM)
            }
            intent.clipData != null && intent.clipData!!.itemCount > 0 -> {
                sb.appendLine("来源: clipData")
                intent.clipData!!.getItemAt(0).uri
            }
            intent.data != null -> {
                sb.appendLine("来源: data fallback")
                intent.data
            }
            else -> {
                sb.appendLine("来源: extras扫描")
                var found: Uri? = null
                intent.extras?.let { extras ->
                    for (key in extras.keySet()) {
                        val v = extras.get(key)
                        if (v is Uri) { found = v; break }
                        if (v is String && v.startsWith("content://")) { found = Uri.parse(v); break }
                    }
                }
                found
            }
        }

        sb.appendLine("uri=$uri")

        if (uri == null) {
            lastDebugLog = sb.toString()
            return null
        }

        var displayName = "shared.md"
        try {
            contentResolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) c.getString(idx)?.let { displayName = it }
                }
            }
        } catch (_: Exception) {}
        if (displayName.isBlank()) displayName = "shared.md"

        val cacheName = "${System.currentTimeMillis()}.md"
        sb.appendLine("显示名=$displayName, 缓存名=$cacheName")

        return try {
            val content = contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }

            if (content.isNullOrEmpty()) {
                sb.appendLine("内容为空")
                lastDebugLog = sb.toString()
                return null
            }

            sb.appendLine("内容长度=${content.length}")

            val cacheFile = File(cacheDir, cacheName)
            cacheFile.writeText(content)

            intent.action = null
            intent.data = null
            intent.removeExtra(Intent.EXTRA_STREAM)

            lastDebugLog = sb.toString()
            Log.d(TAG, lastDebugLog)

            Pair(cacheFile.absolutePath, displayName)
        } catch (e: Exception) {
            sb.appendLine("异常: ${e.message}")
            lastDebugLog = sb.toString()
            Log.e(TAG, lastDebugLog, e)
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun <T> getParcelableExtraCompat(intent: Intent, key: String): T? {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(key, Uri::class.java) as? T
        } else {
            intent.getParcelableExtra(key) as? T
        }
    }
}
