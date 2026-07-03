package com.reader.markdown

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.reader.markdown.ui.screens.FileBrowserScreen
import com.reader.markdown.ui.screens.MarkdownViewerScreen
import com.reader.markdown.ui.theme.MarkdownReaderTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 处理 ACTION_VIEW intent（从其他app打开.md文件）
        val openFilePath = handleViewIntent(intent)

        setContent {
            MarkdownReaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // 如果是从外部打开的文件，直接跳转到阅读页面
                    val startDestination = if (openFilePath != null) {
                        "viewer/${java.net.URLEncoder.encode(openFilePath, "UTF-8")}"
                    } else {
                        "browser"
                    }

                    NavHost(navController = navController, startDestination = startDestination) {
                        composable("browser") {
                            FileBrowserScreen(
                                onFileSelected = { filePath ->
                                    navController.navigate("viewer/${java.net.URLEncoder.encode(filePath, "UTF-8")}")
                                }
                            )
                        }
                        composable("viewer/{filePath}") { backStackEntry ->
                            val encodedPath = backStackEntry.arguments?.getString("filePath") ?: ""
                            val filePath = java.net.URLDecoder.decode(encodedPath, "UTF-8")
                            MarkdownViewerScreen(
                                filePath = filePath,
                                onBack = {
                                    // 如果是从外部app打开的，返回时退出app
                                    if (openFilePath != null && navController.previousBackStackEntry == null) {
                                        finish()
                                    } else {
                                        navController.popBackStack()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 处理新的intent（app已经打开时再次接收文件）
        val openFilePath = handleViewIntent(intent)
        if (openFilePath != null) {
            // 重新创建UI以打开新文件
            recreate()
        }
    }

    /**
     * 处理 ACTION_VIEW intent，将 content:// URI 复制到缓存目录，返回本地文件路径
     */
    private fun handleViewIntent(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_VIEW) return null
        val uri = intent.data ?: return null

        return try {
            val fileName = getFileName(uri) ?: "opened_file.md"
            val tempFile = File(cacheDir, fileName)
            contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            // 清除 intent 避免重复处理
            intent.action = null
            intent.data = null
            tempFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getFileName(uri: android.net.Uri): String? {
        // 尝试从 content provider 获取文件名
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    return it.getString(nameIndex)
                }
            }
        }
        // fallback: 从 URI path 获取
        return uri.lastPathSegment
    }
}
