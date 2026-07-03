package com.reader.markdown.ui.screens

import android.content.Context
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.syntax.SyntaxHighlightPlugin

import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownViewerScreen(
    filePath: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var content by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var fontSize by remember { mutableStateOf(16f) }
    var showFontDialog by remember { mutableStateOf(false) }

    // 加载文件内容
    LaunchedEffect(filePath) {
        try {
            val file = File(filePath)
            fileName = file.nameWithoutExtension
            content = file.readText()
        } catch (e: Exception) {
            content = "无法读取文件: ${e.message}"
        }
    }

    // 字体大小调整对话框
    if (showFontDialog) {
        AlertDialog(
            onDismissRequest = { showFontDialog = false },
            title = { Text("字体大小") },
            text = {
                Column {
                    Text("当前大小: ${fontSize.toInt()}sp")
                    Slider(
                        value = fontSize,
                        onValueChange = { fontSize = it },
                        valueRange = 12f..24f,
                        steps = 11
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showFontDialog = false }) {
                    Text("确定")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = fileName,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { fontSize = (fontSize - 2f).coerceAtLeast(12f) }) {
                        Icon(Icons.Default.TextDecrease, contentDescription = "减小字体")
                    }
                    IconButton(onClick = { fontSize = (fontSize + 2f).coerceAtMost(24f) }) {
                        Icon(Icons.Default.TextIncrease, contentDescription = "增大字体")
                    }
                    IconButton(onClick = { showFontDialog = true }) {
                        Icon(Icons.Default.FormatSize, contentDescription = "字体设置")
                    }
                }
            )
        }
    ) { paddingValues ->
        MarkdownContent(
            content = content,
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

@Composable
fun MarkdownContent(
    content: String,
    fontSize: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // 创建Markwon实例
    val markwon = remember {
        createMarkwon(context)
    }

    // 使用AndroidView来渲染Markdown
    AndroidView(
        modifier = modifier.verticalScroll(scrollState),
        factory = { ctx ->
            TextView(ctx).apply {
                this.textSize = fontSize
                setPadding(32, 24, 32, 24)
            }
        },
        update = { textView ->
            textView.textSize = fontSize
            markwon.setMarkdown(textView, content)
        }
    )
}

private fun createMarkwon(context: Context): Markwon {
    return Markwon.builder(context)
        .usePlugin(StrikethroughPlugin.create())
        .usePlugin(TablePlugin.create(context))
        .usePlugin(TaskListPlugin.create(context))

        .build()
}