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
import com.reader.markdown.settings.SettingsManager
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
                val title = match.groupValues[2].trim().replace(Regex("[*`~_]"), "")
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
    settings: SettingsManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var content by remember { mutableStateOf("") }
    var editContent by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf(displayTitle ?: "") }
    var showFontDialog by remember { mutableStateOf(false) }
    var debugInfo by remember { mutableStateOf("") }
    var showToc by remember { mutableStateOf(false) }
    var tocItems by remember { mutableStateOf(listOf<TocItem>()) }
    var isEditing by remember { mutableStateOf(false) }
    var isModified by remember { mutableStateOf(false) }

    val file = remember(filePath) { File(filePath) }
    val isWritable = remember(filePath) { file.exists() && file.canWrite() }

    // 从设置读取
    val readerFontSize = settings.readerFontSize
    val readerLineHeight = settings.readerLineHeight
    val readerFontFamily = when (settings.readerFontFamily) {
        "serif" -> FontFamily.Serif
        "monospace" -> FontFamily.Monospace
        else -> FontFamily.SansSerif
    }
    val editorFontSize = settings.editorFontSize

    // 加载文件
    LaunchedEffect(filePath) {
        try {
            if (!file.exists()) {
                content = "文件不存在\n\n$filePath"
                fileName = "错误"
                return@LaunchedEffect
            }
            if (!file.canRead()) {
                content = "无法读取文件\n\n$filePath"
                fileName = "错误"
                return@LaunchedEffect
            }
            if (fileName.isBlank()) fileName = file.nameWithoutExtension
            content = file.readText()
            editContent = content
            tocItems = parseToc(content)
            if (settings.showTocByDefault && tocItems.isNotEmpty()) {
                showToc = true
            }
            if (content.isBlank()) { content = "（空文件）"; editContent = "" }
        } catch (e: Exception) {
            content = "读取异常: ${e.message}"
        }
    }

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

    var showBackConfirm by remember { mutableStateOf(false) }
    if (showBackConfirm) {
        AlertDialog(
            onDismissRequest = { showBackConfirm = false },
            title = { Text("未保存的修改") },
            text = { Text("当前有未保存的修改，是否保存？") },
            confirmButton = {
                TextButton(onClick = { showBackConfirm = false; saveFile(); onBack() }) { Text("保存并返回") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showBackConfirm = false; onBack() }) { Text("不保存") }
                    TextButton(onClick = { showBackConfirm = false }) { Text("取消") }
                }
            }
        )
    }

    // 目录弹窗
    if (showToc) {
        ModalBottomSheet(onDismissRequest = { showToc = false }, dragHandle = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(8.dp))
                Box(Modifier.width(40.dp).height(4.dp)) {
                    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.fillMaxSize()) {}
                }
                Spacer(Modifier.height(12.dp))
                Text("目录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
            }
        }) {
            if (tocItems.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("无目录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                TocPanel(items = tocItems, onItemClick = { showToc = false }, modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(fileName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (isModified) {
                            Spacer(Modifier.width(4.dp))
                            Text("●", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (isModified) showBackConfirm = true else onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { saveFile() }) {
                            Icon(Icons.Default.Save, contentDescription = "保存", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { if (isModified) showBackConfirm = true else isEditing = false }) {
                            Icon(Icons.Default.Visibility, contentDescription = "预览")
                        }
                    } else {
                        if (isWritable) {
                            IconButton(onClick = { editContent = content; isEditing = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "编辑")
                            }
                        }
                        if (tocItems.isNotEmpty()) {
                            IconButton(onClick = { showToc = true }) {
                                Icon(Icons.Default.List, contentDescription = "目录")
                            }
                        }
                        IconButton(onClick = { showFontDialog = true }) {
                            Icon(Icons.Default.FormatSize, contentDescription = "字体设置")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues)) {
            // 调试信息
            if (content.startsWith("文件不存在") || content.startsWith("无法读取") || content.startsWith("读取异常")) {
                Card(Modifier.fillMaxWidth().padding(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(debugInfo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        if (com.reader.markdown.MainActivity.lastDebugLog.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text("--- Intent解析日志 ---", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            Text(com.reader.markdown.MainActivity.lastDebugLog, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            // 字体调整对话框
            if (showFontDialog) {
                var tempFontSize by remember { mutableStateOf(if (isEditing) editorFontSize else readerFontSize) }
                AlertDialog(
                    onDismissRequest = { showFontDialog = false },
                    title = { Text("字体大小") },
                    text = {
                        Column {
                            Text("${tempFontSize.toInt()}sp")
                            Slider(value = tempFontSize, onValueChange = { tempFontSize = it }, valueRange = 10f..28f, steps = 17)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (isEditing) settings.editorFontSize = tempFontSize else settings.readerFontSize = tempFontSize
                            showFontDialog = false
                        }) { Text("确定") }
                    }
                )
            }

            if (isEditing) {
                // 编辑器
                EditorContent(
                    content = editContent,
                    fontSize = editorFontSize,
                    tabSize = settings.editorTabSize,
                    wordWrap = settings.editorWordWrap,
                    onContentChange = { newContent ->
                        editContent = newContent
                        isModified = (newContent != content)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Row(Modifier.fillMaxSize()) {
                    BoxWithConstraints {
                        if (maxWidth > 600.dp && tocItems.isNotEmpty()) {
                            TocSidePanel(items = tocItems, onItemClick = {}, modifier = Modifier.width(220.dp).fillMaxHeight())
                        }
                    }
                    AndroidView(
                        modifier = Modifier.fillMaxSize().weight(1f).verticalScroll(rememberScrollState()),
                        factory = { ctx ->
                            TextView(ctx).apply {
                                this.textSize = readerFontSize
                                setPadding(32, 24, 32, 24)
                            }
                        },
                        update = { textView ->
                            textView.textSize = readerFontSize
                            textView.setLineSpacing(0f, readerLineHeight)
                            textView.typeface = when (settings.readerFontFamily) {
                                "serif" -> android.graphics.Typeface.SERIF
                                "monospace" -> android.graphics.Typeface.MONOSPACE
                                else -> android.graphics.Typeface.SANS_SERIF
                            }
                            val markwon = Markwon.builder(context)
                                .usePlugin(StrikethroughPlugin.create())
                                .usePlugin(TablePlugin.create(context))
                                .usePlugin(TaskListPlugin.create(context))
                                .build()
                            markwon.setMarkdown(textView, content)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EditorContent(
    content: String,
    fontSize: Float,
    tabSize: Int,
    wordWrap: Boolean,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember(content) { mutableStateOf(content) }
    val scrollState = rememberScrollState()
    val indent = " ".repeat(tabSize)

    Box(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        BasicTextField(
            value = text,
            onValueChange = { newText ->
                text = newText
                onContentChange(newText)
            },
            modifier = Modifier.fillMaxSize().then(
                if (!wordWrap) Modifier.verticalScroll(scrollState) else Modifier
            ),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize.sp,
                lineHeight = (fontSize * 1.5f).sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box(Modifier.fillMaxSize()) {
                    if (text.isEmpty()) {
                        Text("输入 Markdown 内容...", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = fontSize.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)))
                    }
                    innerTextField()
                }
            }
        )
    }
}

// ── 目录组件 ──

@Composable
fun TocPanel(items: List<TocItem>, onItemClick: (TocItem) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.padding(horizontal = 16.dp)) {
        items(items) { TocItemRow(it) { onItemClick(it) } }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
fun TocSidePanel(items: List<TocItem>, onItemClick: (TocItem) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, tonalElevation = 1.dp) {
        Column {
            Text("目录", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
            Divider()
            LazyColumn(Modifier.weight(1f)) {
                items(items) { TocItemRow(it) { onItemClick(it) } }
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
        headlineContent = { Text(item.title, style = textStyle, color = color, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = indent)) },
        modifier = Modifier.clickable(onClick = onClick),
        tonalElevation = 0.dp
    )
}
