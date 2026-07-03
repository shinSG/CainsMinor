package com.reader.markdown.ui.screens

import android.widget.TextView
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import java.io.File

data class TocItem(
    val level: Int,
    val title: String,
    val lineNumber: Int
)

fun parseToc(content: String): List<TocItem> {
    val items = mutableListOf<TocItem>()
    val lines = content.lines()
    for ((index, line) in lines.withIndex()) {
        val trimmed = line.trim()
        if (trimmed.startsWith("#") && !trimmed.startsWith("####")) {
            val match = Regex("^(#{1,4})\\s+(.+)").find(trimmed)
            if (match != null) {
                val level = match.groupValues[1].length - 1
                val title = match.groupValues[2].trim()
                    .replace(Regex("[*`~_]"), "")
                items.add(TocItem(level, title, index))
            }
        }
    }
    return items
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownViewerScreen(
    filePath: String,
    displayTitle: String? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var content by remember { mutableStateOf("") }
    var editContent by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf(displayTitle ?: "") }
    var fontSize by remember { mutableStateOf(16f) }
    var showFontDialog by remember { mutableStateOf(false) }
    var debugInfo by remember { mutableStateOf("") }
    var showToc by remember { mutableStateOf(false) }
    var tocItems by remember { mutableStateOf(listOf<TocItem>()) }
    var isEditing by remember { mutableStateOf(false) }
    var isModified by remember { mutableStateOf(false) }

    // 判断文件是否可写
    val file = remember(filePath) { File(filePath) }
    val isWritable = remember(filePath) { file.exists() && file.canWrite() }

    // 加载文件内容
    LaunchedEffect(filePath) {
        val sb = StringBuilder()
        sb.appendLine("路径: $filePath")
        try {
            if (!file.exists()) {
                content = "文件不存在\n\n$filePath"
                fileName = "错误"
                debugInfo = sb.toString()
                return@LaunchedEffect
            }
            if (!file.canRead()) {
                content = "无法读取文件\n\n$filePath"
                fileName = "错误"
                debugInfo = sb.toString()
                return@LaunchedEffect
            }
            if (fileName.isBlank()) {
                fileName = file.nameWithoutExtension
            }
            content = file.readText()
            editContent = content
            tocItems = parseToc(content)
            sb.appendLine("内容长度: ${content.length}")
            sb.appendLine("目录项: ${tocItems.size}")
            if (content.isBlank()) {
                content = "（空文件）"
                editContent = ""
            }
        } catch (e: Exception) {
            content = "读取异常: ${e.message}"
            sb.appendLine("异常: ${e.stackTraceToString()}")
        }
        debugInfo = sb.toString()
    }

    // 保存文件
    fun saveFile() {
        try {
            file.writeText(editContent)
            content = editContent
            tocItems = parseToc(editContent)
            isModified = false
            Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // 返回确认（有未保存修改时）
    var showBackConfirm by remember { mutableStateOf(false) }
    if (showBackConfirm) {
        AlertDialog(
            onDismissRequest = { showBackConfirm = false },
            title = { Text("未保存的修改") },
            text = { Text("当前有未保存的修改，是否保存？") },
            confirmButton = {
                TextButton(onClick = {
                    showBackConfirm = false
                    saveFile()
                    onBack()
                }) { Text("保存并返回") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showBackConfirm = false
                        onBack()
                    }) { Text("不保存") }
                    TextButton(onClick = { showBackConfirm = false }) { Text("取消") }
                }
            }
        )
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

    // 目录底部弹出
    if (showToc) {
        ModalBottomSheet(
            onDismissRequest = { showToc = false },
            dragHandle = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxSize()
                        ) {}
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("目录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        ) {
            if (tocItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("无目录（未找到标题）", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                TocPanel(
                    items = tocItems,
                    onItemClick = { showToc = false },
                    modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = fileName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (isModified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("●", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isModified) showBackConfirm = true else onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (isEditing) {
                        // 编辑模式：保存按钮
                        IconButton(onClick = { saveFile() }) {
                            Icon(Icons.Default.Save, contentDescription = "保存", tint = MaterialTheme.colorScheme.primary)
                        }
                        // 退出编辑
                        IconButton(onClick = {
                            if (isModified) showBackConfirm = true
                            else { isEditing = false }
                        }) {
                            Icon(Icons.Default.Visibility, contentDescription = "预览")
                        }
                    } else {
                        // 预览模式
                        // 编辑按钮（只有可写的文件才显示）
                        if (isWritable) {
                            IconButton(onClick = {
                                editContent = content
                                isEditing = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "编辑")
                            }
                        }
                        if (tocItems.isNotEmpty()) {
                            IconButton(onClick = { showToc = true }) {
                                Icon(Icons.Default.List, contentDescription = "目录")
                            }
                        }
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
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 调试信息
            if (content.startsWith("文件不存在") || content.startsWith("无法读取") || content.startsWith("读取异常")) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = debugInfo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        if (com.reader.markdown.MainActivity.lastDebugLog.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "--- Intent解析日志 ---", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            Text(text = com.reader.markdown.MainActivity.lastDebugLog, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            if (isEditing) {
                // 编辑模式：Markdown 源码编辑器
                EditableMarkdownContent(
                    content = editContent,
                    onContentChange = { newContent ->
                        editContent = newContent
                        isModified = (newContent != content)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // 预览模式
                Row(modifier = Modifier.fillMaxSize()) {
                    // 大屏目录侧栏
                    BoxWithConstraints {
                        if (maxWidth > 600.dp && tocItems.isNotEmpty()) {
                            TocSidePanel(
                                items = tocItems,
                                onItemClick = {},
                                modifier = Modifier.width(220.dp).fillMaxHeight()
                            )
                        }
                    }
                    // Markdown 渲染
                    MarkdownContent(
                        content = content,
                        fontSize = fontSize,
                        modifier = Modifier.fillMaxSize().weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Markdown 源码编辑器
 */
@Composable
fun EditableMarkdownContent(
    content: String,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember(content) { mutableStateOf(content) }
    val scrollState = rememberScrollState()

    Box(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        BasicTextField(
            value = text,
            onValueChange = { newText ->
                text = newText
                onContentChange(newText)
            },
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxSize()) {
                    if (text.isEmpty()) {
                        Text(
                            "输入 Markdown 内容...",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
fun TocPanel(
    items: List<TocItem>,
    onItemClick: (TocItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.padding(horizontal = 16.dp)) {
        items(items) { item ->
            TocItemRow(item = item, onClick = { onItemClick(item) })
        }
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
fun TocSidePanel(
    items: List<TocItem>,
    onItemClick: (TocItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier, tonalElevation = 1.dp) {
        Column {
            Text("目录", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
            Divider()
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(items) { item ->
                    TocItemRow(item = item, onClick = { onItemClick(item) })
                }
            }
        }
    }
}

@Composable
fun TocItemRow(item: TocItem, onClick: () -> Unit) {
    val indent = (item.level * 16).dp
    val textStyle = when (item.level) {
        0 -> MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
        1 -> MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
        else -> MaterialTheme.typography.bodySmall
    }
    val color = when (item.level) {
        0 -> MaterialTheme.colorScheme.primary
        1 -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    ListItem(
        headlineContent = {
            Text(text = item.title, style = textStyle, color = color, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = indent))
        },
        modifier = Modifier.clickable(onClick = onClick),
        tonalElevation = 0.dp
    )
}

@Composable
fun MarkdownContent(content: String, fontSize: Float, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val markwon = remember {
        Markwon.builder(context)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(TaskListPlugin.create(context))
            .build()
    }
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
