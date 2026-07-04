package com.reader.markdown.ui.screens

import android.view.KeyEvent
import android.widget.TextView
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.reader.markdown.settings.SettingsManager
import com.reader.markdown.agent.LlmService
import com.reader.markdown.agent.Suggestion
import com.reader.markdown.agent.SuggestionOverlay
import com.reader.markdown.agent.WritingAgent
import com.reader.markdown.agent.TocGenerator
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    val readerFontSize = settings.readerFontSize
    val readerLineHeight = settings.readerLineHeight
    val readerFontFamily = when (settings.readerFontFamily) {
        "serif" -> FontFamily.Serif
        "monospace" -> FontFamily.Monospace
        else -> FontFamily.SansSerif
    }
    val editorFontSize = settings.editorFontSize

    // AI 写作助手
    val llmService = remember { LlmService(settings) }
    val writingAgent = remember { WritingAgent(settings, llmService) }
    val suggestion by writingAgent.suggestion.collectAsState()
    val isGenerating by writingAgent.isGenerating.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var autoSuggestJob by remember { mutableStateOf<Job?>(null) }

    // 获取当前建议文本
    val suggestionText = when (val s = suggestion) {
        is Suggestion.Text -> s.content
        is Suggestion.Streaming -> s.partial
        else -> null
    }

    // 加载文件
    LaunchedEffect(filePath) {
        try {
            if (!file.exists()) { content = "文件不存在\n\n$filePath"; fileName = "错误"; return@LaunchedEffect }
            if (!file.canRead()) { content = "无法读取文件\n\n$filePath"; fileName = "错误"; return@LaunchedEffect }
            if (fileName.isBlank()) fileName = file.nameWithoutExtension
            content = file.readText()
            editContent = content
            tocItems = parseToc(content)
            if (settings.showTocByDefault && tocItems.isNotEmpty()) showToc = true
            if (content.isBlank()) { content = "（空文件）"; editContent = "" }
        } catch (e: Exception) { content = "读取异常: ${e.message}" }
    }

    fun saveFile() {
        try {
            file.writeText(editContent)
            content = editContent
            tocItems = parseToc(editContent)
            isModified = false
            Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) { Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_LONG).show() }
    }

    fun acceptSuggestion() {
        val text = suggestionText
        if (text != null) {
            editContent += text
            isModified = (editContent != content)
            writingAgent.dismiss()
        }
    }

    var showBackConfirm by remember { mutableStateOf(false) }
    if (showBackConfirm) {
        AlertDialog(
            onDismissRequest = { showBackConfirm = false },
            title = { Text("未保存的修改") },
            text = { Text("当前有未保存的修改，是否保存？") },
            confirmButton = { TextButton(onClick = { showBackConfirm = false; saveFile(); onBack() }) { Text("保存并返回") } },
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
                        if (isModified) { Spacer(Modifier.width(4.dp)); Text("●", color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (isModified) showBackConfirm = true else onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (isEditing) {
                        // 目录生成按钮
                        var showTocMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showTocMenu = true }) {
                                Icon(Icons.Default.Toc, contentDescription = "生成目录")
                            }
                            DropdownMenu(expanded = showTocMenu, onDismissRequest = { showTocMenu = false }) {
                                if (TocGenerator.hasToc(editContent)) {
                                    DropdownMenuItem(text = { Text("更新目录") }, onClick = {
                                        val (newContent, _) = TocGenerator.insertOrUpdateToc(editContent)
                                        editContent = newContent; isModified = (editContent != content); showTocMenu = false
                                    }, leadingIcon = { Icon(Icons.Default.Refresh, null) })
                                    DropdownMenuItem(text = { Text("删除目录") }, onClick = {
                                        editContent = TocGenerator.removeToc(editContent); isModified = (editContent != content); showTocMenu = false
                                    }, leadingIcon = { Icon(Icons.Default.Delete, null) })
                                } else {
                                    DropdownMenuItem(text = { Text("插入目录（2级）") }, onClick = {
                                        val (newContent, _) = TocGenerator.insertOrUpdateToc(editContent, 2); editContent = newContent; isModified = (editContent != content); showTocMenu = false
                                    }, leadingIcon = { Icon(Icons.Default.FormatListBulleted, null) })
                                    DropdownMenuItem(text = { Text("插入目录（3级）") }, onClick = {
                                        val (newContent, _) = TocGenerator.insertOrUpdateToc(editContent, 3); editContent = newContent; isModified = (editContent != content); showTocMenu = false
                                    }, leadingIcon = { Icon(Icons.Default.FormatListBulleted, null) })
                                    DropdownMenuItem(text = { Text("插入目录（4级）") }, onClick = {
                                        val (newContent, _) = TocGenerator.insertOrUpdateToc(editContent, 4); editContent = newContent; isModified = (editContent != content); showTocMenu = false
                                    }, leadingIcon = { Icon(Icons.Default.FormatListBulleted, null) })
                                }
                            }
                        }

                        // AI 续写按钮
                        IconButton(
                            onClick = {
                                if (!isGenerating) {
                                    coroutineScope.launch {
                                        writingAgent.requestCompletionStream(editContent, editContent.length)
                                    }
                                }
                            },
                            enabled = !isGenerating && settings.isLlmConfigured
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "AI续写", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
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
                    text = { Column { Text("${tempFontSize.toInt()}sp"); Slider(value = tempFontSize, onValueChange = { tempFontSize = it }, valueRange = 10f..28f, steps = 17) } },
                    confirmButton = { TextButton(onClick = {
                        if (isEditing) settings.editorFontSize = tempFontSize else settings.readerFontSize = tempFontSize; showFontDialog = false
                    }) { Text("确定") } }
                )
            }

            if (isEditing) {
                // 编辑器 + 行内建议
                Box(modifier = Modifier.fillMaxSize()) {
                    InlineEditorContent(
                        content = editContent,
                        suggestion = suggestionText,
                        fontSize = editorFontSize,
                        tabSize = settings.editorTabSize,
                        wordWrap = settings.editorWordWrap,
                        onContentChange = { newContent ->
                            editContent = newContent
                            isModified = (newContent != content)
                            // 用户输入时清除建议
                            if (suggestionText != null) {
                                writingAgent.dismiss()
                            }
                            // 自动触发续写（延迟防抖）
                            autoSuggestJob?.cancel()
                            if (settings.isLlmConfigured && !isGenerating) {
                                autoSuggestJob = coroutineScope.launch {
                                    delay(2000) // 停止输入 2 秒后自动触发
                                    writingAgent.requestCompletionStream(newContent, newContent.length)
                                }
                            }
                        },
                        onAcceptSuggestion = { acceptSuggestion() },
                        modifier = Modifier.fillMaxSize()
                    )

                    // 底部建议操作栏（当有建议时显示）
                    if (suggestionText != null) {
                        Surface(
                            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                            tonalElevation = 4.dp,
                            shadowElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("AI 建议", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.weight(1f))
                                Text("Tab 接受", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(12.dp))
                                TextButton(onClick = { writingAgent.dismiss() }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                                    Text("忽略", fontSize = 12.sp)
                                }
                                Spacer(Modifier.width(4.dp))
                                Button(onClick = { acceptSuggestion() }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                                    Text("接受", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                Row(Modifier.fillMaxSize()) {
                    BoxWithConstraints {
                        if (maxWidth > 600.dp && tocItems.isNotEmpty()) {
                            TocSidePanel(items = tocItems, onItemClick = {}, modifier = Modifier.width(220.dp).fillMaxHeight())
                        }
                    }
                    AndroidView(
                        modifier = Modifier.fillMaxSize().weight(1f).verticalScroll(rememberScrollState()),
                        factory = { ctx -> TextView(ctx).apply { this.textSize = readerFontSize; setPadding(32, 24, 32, 24) } },
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

/**
 * 行内建议编辑器 - 建议内容以幽灵文字显示在编辑器内
 */
@Composable
fun InlineEditorContent(
    content: String,
    suggestion: String?,
    fontSize: Float,
    tabSize: Int,
    wordWrap: Boolean,
    onContentChange: (String) -> Unit,
    onAcceptSuggestion: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember(content) { mutableStateOf(TextFieldValue(content)) }
    val scrollState = rememberScrollState()

    // 当外部 content 变化时同步
    LaunchedEffect(content) {
        if (textFieldValue.text != content) {
            textFieldValue = textFieldValue.copy(text = content)
        }
    }

    Box(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                onContentChange(newValue.text)
            },
            modifier = Modifier
                .fillMaxSize()
                .then(if (!wordWrap) Modifier.verticalScroll(scrollState) else Modifier)
                .onKeyEvent { event ->
                    // Tab 键接受建议
                    if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN &&
                        event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_TAB &&
                        suggestion != null
                    ) {
                        onAcceptSuggestion()
                        true
                    } else false
                },
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize.sp,
                lineHeight = (fontSize * 1.5f).sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box(Modifier.fillMaxSize()) {
                    if (textFieldValue.text.isEmpty() && suggestion == null) {
                        Text(
                            "输入 Markdown 内容...",
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = fontSize.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        )
                    }
                    innerTextField()

                    // 行内幽灵建议文字 - 显示在文本末尾
                    if (suggestion != null && suggestion.isNotBlank()) {
                        // 计算建议的第一行（只显示一行预览）
                        val firstLine = suggestion.lines().firstOrNull()?.trim() ?: ""
                        val remainingLines = suggestion.lines().size - 1
                        if (firstLine.isNotBlank()) {
                            val previewText = if (remainingLines > 0) {
                                "$firstLine  (+${remainingLines}行)"
                            } else {
                                firstLine
                            }
                            // 显示在右下角作为提示
                            Text(
                                text = "💡 Tab: $previewText",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = (fontSize - 2).sp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.align(Alignment.BottomStart),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
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
