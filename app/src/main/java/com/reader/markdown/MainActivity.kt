package com.reader.markdown

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.reader.markdown.settings.SettingsManager
import com.reader.markdown.settings.SettingsScreen
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

        val externalFile = resolveIntent(intent)
        Log.d(TAG, "onCreate externalFile=$externalFile")

        setContent {
            val context = LocalContext.current
            val settings = remember { SettingsManager.getInstance(context) }

            // 主题模式
            val darkTheme = when (settings.themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            MarkdownReaderTheme(darkTheme = darkTheme, dynamicColor = settings.dynamicColor) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentFilePath by remember { mutableStateOf(externalFile?.first) }
                    var currentDisplayName by remember { mutableStateOf(externalFile?.second) }
                    var fromExternal by remember { mutableStateOf(externalFile != null) }
                    var showSettings by remember { mutableStateOf(false) }

                    when {
                        showSettings -> {
                            SettingsScreen(onBack = { showSettings = false })
                        }
                        currentFilePath != null -> {
                            MarkdownViewerScreen(
                                filePath = currentFilePath!!,
                                displayTitle = currentDisplayName,
                                settings = settings,
                                onBack = {
                                    if (fromExternal) {
                                        finish()
                                    } else {
                                        currentFilePath = null
                                        currentDisplayName = null
                                        fromExternal = false
                                    }
                                }
                            )
                        }
                        else -> {
                            FileBrowserScreen(
                                settings = settings,
                                onFileSelected = { filePath ->
                                    currentFilePath = filePath
                                    currentDisplayName = null
                                    fromExternal = false
                                },
                                onSettingsClick = { showSettings = true }
                            )
                        }
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
            recreate()
        }
    }

    private fun resolveIntent(intent: Intent?): Pair<String, String>? {
        if (intent == null) { lastDebugLog = "intent is null"; return null }

        val sb = StringBuilder()
        sb.appendLine("action=${intent.action}")

        val uri: Uri? = when {
            intent.action == Intent.ACTION_VIEW && intent.data != null -> { intent.data }
            intent.action == Intent.ACTION_SEND -> { getParcelableExtraCompat(intent, Intent.EXTRA_STREAM) }
            intent.clipData != null && intent.clipData!!.itemCount > 0 -> { intent.clipData!!.getItemAt(0).uri }
            intent.data != null -> { intent.data }
            else -> {
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

        if (uri == null) { lastDebugLog = sb.toString(); return null }

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

        return try {
            val content = contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
            if (content.isNullOrEmpty()) { lastDebugLog = sb.toString(); return null }

            val cacheFile = File(cacheDir, cacheName)
            cacheFile.writeText(content)
            intent.action = null; intent.data = null; intent.removeExtra(Intent.EXTRA_STREAM)
            lastDebugLog = sb.toString()
            Pair(cacheFile.absolutePath, displayName)
        } catch (e: Exception) {
            lastDebugLog = sb.toString()
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
