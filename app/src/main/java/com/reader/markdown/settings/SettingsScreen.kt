package com.reader.markdown.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settings = remember { SettingsManager.getInstance(context) }

    // 本地状态（与 SharedPreferences 同步）
    var readerFontSize by remember { mutableStateOf(settings.readerFontSize) }
    var readerLineHeight by remember { mutableStateOf(settings.readerLineHeight) }
    var readerFontFamily by remember { mutableStateOf(settings.readerFontFamily) }
    var editorFontSize by remember { mutableStateOf(settings.editorFontSize) }
    var editorTabSize by remember { mutableStateOf(settings.editorTabSize) }
    var editorWordWrap by remember { mutableStateOf(settings.editorWordWrap) }
    var themeMode by remember { mutableStateOf(settings.themeMode) }
    var dynamicColor by remember { mutableStateOf(settings.dynamicColor) }
    var showTocByDefault by remember { mutableStateOf(settings.showTocByDefault) }
    var showHiddenFiles by remember { mutableStateOf(settings.showHiddenFiles) }
    var sortBy by remember { mutableStateOf(settings.sortBy) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // ═══════════════════════════════════════
            // 阅读设置
            // ═══════════════════════════════════════
            SettingsSection("阅读")

            // 字体大小
            SettingsSlider(
                title = "字体大小",
                value = readerFontSize,
                valueRange = 12f..28f,
                steps = 15,
                suffix = "sp",
                onValueChange = {
                    readerFontSize = it
                    settings.readerFontSize = it
                }
            )

            // 行间距
            SettingsSlider(
                title = "行间距",
                value = readerLineHeight,
                valueRange = 1.0f..2.5f,
                steps = 14,
                suffix = "x",
                onValueChange = {
                    readerLineHeight = it
                    settings.readerLineHeight = it
                }
            )

            // 字体
            SettingsDropdown(
                title = "字体",
                options = listOf("sans-serif" to "无衬线", "serif" to "衬线", "monospace" to "等宽"),
                selected = readerFontFamily,
                onSelect = {
                    readerFontFamily = it
                    settings.readerFontFamily = it
                }
            )

            // ═══════════════════════════════════════
            // 编辑设置
            // ═══════════════════════════════════════
            SettingsSection("编辑")

            // 编辑器字体大小
            SettingsSlider(
                title = "编辑器字体大小",
                value = editorFontSize,
                valueRange = 12f..24f,
                steps = 11,
                suffix = "sp",
                onValueChange = {
                    editorFontSize = it
                    settings.editorFontSize = it
                }
            )

            // Tab 大小
            SettingsSlider(
                title = "Tab 缩进",
                value = editorTabSize.toFloat(),
                valueRange = 2f..8f,
                steps = 5,
                suffix = "空格",
                onValueChange = {
                    editorTabSize = it.toInt()
                    settings.editorTabSize = it.toInt()
                }
            )

            // 自动换行
            SettingsSwitch(
                title = "自动换行",
                subtitle = "编辑时自动折行显示",
                checked = editorWordWrap,
                onCheckedChange = {
                    editorWordWrap = it
                    settings.editorWordWrap = it
                }
            )

            // ═══════════════════════════════════════
            // 外观设置
            // ═══════════════════════════════════════
            SettingsSection("外观")

            // 主题
            SettingsDropdown(
                title = "主题",
                options = listOf("system" to "跟随系统", "light" to "浅色", "dark" to "深色"),
                selected = themeMode,
                onSelect = {
                    themeMode = it
                    settings.themeMode = it
                }
            )

            // 动态取色
            SettingsSwitch(
                title = "动态取色",
                subtitle = "Android 12+ 壁纸取色（Material You）",
                checked = dynamicColor,
                onCheckedChange = {
                    dynamicColor = it
                    settings.dynamicColor = it
                }
            )

            // ═══════════════════════════════════════
            // 文件浏览设置
            // ═══════════════════════════════════════
            SettingsSection("文件浏览")

            // 排序方式
            SettingsDropdown(
                title = "默认排序",
                options = listOf("date" to "修改时间", "name" to "文件名", "size" to "文件大小"),
                selected = sortBy,
                onSelect = {
                    sortBy = it
                    settings.sortBy = it
                }
            )

            // 显示隐藏文件
            SettingsSwitch(
                title = "显示隐藏文件",
                subtitle = "显示以 . 开头的文件",
                checked = showHiddenFiles,
                onCheckedChange = {
                    showHiddenFiles = it
                    settings.showHiddenFiles = it
                }
            )

            // ═══════════════════════════════════════
            // LLM API 设置
            // ═══════════════════════════════════════
            SettingsSection("LLM API")

            SettingsHint("配置 OpenAI 兼容的 API 接口，用于 AI 辅助功能")

            // Base URL
            var llmBaseUrl by remember { mutableStateOf(settings.llmBaseUrl) }
            SettingsTextField(
                title = "Base URL",
                value = llmBaseUrl,
                placeholder = "https://api.openai.com/v1",
                onValueChange = {
                    llmBaseUrl = it
                    settings.llmBaseUrl = it
                }
            )

            // API Key
            var llmApiKey by remember { mutableStateOf(settings.llmApiKey) }
            SettingsTextField(
                title = "API Key",
                value = llmApiKey,
                placeholder = "sk-...",
                isPassword = true,
                onValueChange = {
                    llmApiKey = it
                    settings.llmApiKey = it
                }
            )

            // Model Name
            var llmModelName by remember { mutableStateOf(settings.llmModelName) }
            SettingsTextField(
                title = "模型名称",
                value = llmModelName,
                placeholder = "gpt-4o",
                onValueChange = {
                    llmModelName = it
                    settings.llmModelName = it
                }
            )

            // Temperature
            var llmTemperature by remember { mutableStateOf(settings.llmTemperature) }
            SettingsSlider(
                title = "Temperature",
                value = llmTemperature,
                valueRange = 0f..2f,
                steps = 19,
                suffix = "",
                onValueChange = {
                    llmTemperature = it
                    settings.llmTemperature = it
                }
            )

            // Max Tokens
            var llmMaxTokens by remember { mutableStateOf(settings.llmMaxTokens) }
            SettingsSlider(
                title = "Max Tokens",
                value = llmMaxTokens.toFloat(),
                valueRange = 256f..16384f,
                steps = 63,
                suffix = "",
                onValueChange = {
                    llmMaxTokens = it.toInt()
                    settings.llmMaxTokens = it.toInt()
                }
            )

            // System Prompt
            var llmSystemPrompt by remember { mutableStateOf(settings.llmSystemPrompt) }
            SettingsTextField(
                title = "System Prompt",
                value = llmSystemPrompt,
                placeholder = "你是一个Markdown文档助手...",
                isMultiline = true,
                onValueChange = {
                    llmSystemPrompt = it
                    settings.llmSystemPrompt = it
                }
            )

            // 测试连接
            SettingsTestButton(settings)

            // ═══════════════════════════════════════
            // 其他
            // ═══════════════════════════════════════
            SettingsSection("其他")

            // 目录
            SettingsSwitch(
                title = "默认显示目录",
                subtitle = "打开文件时自动展开目录面板",
                checked = showTocByDefault,
                onCheckedChange = {
                    showTocByDefault = it
                    settings.showTocByDefault = it
                }
            )

            // 关于
            SettingsAbout()

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ═══════════════════════════════════════
// 通用组件
// ═══════════════════════════════════════

@Composable
fun SettingsSection(title: String) {
    Column {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
fun SettingsSlider(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    suffix: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                if (suffix == "sp" || suffix == "空格") "${value.toInt()}$suffix"
                else "${"%.1f".format(value)}$suffix",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SettingsSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDropdown(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val displayText = options.find { it.first == selected }?.second ?: selected

    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Text(displayText, style = MaterialTheme.typography.bodySmall)
        },
        trailingContent = {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (key, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onSelect(key)
                            expanded = false
                        },
                        leadingIcon = {
                            if (key == selected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )
                }
            }
        }
    )
}

@Composable
fun SettingsAbout() {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("CainsMinor") },
            text = {
                Column {
                    Text("一款简洁的 Android Markdown 阅读器")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("版本: 1.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("功能特性:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Text("• Markdown 渲染（表格、任务列表、代码块）", style = MaterialTheme.typography.bodySmall)
                    Text("• 源码编辑模式", style = MaterialTheme.typography.bodySmall)
                    Text("• 目录导航", style = MaterialTheme.typography.bodySmall)
                    Text("• 自定义主题与字体", style = MaterialTheme.typography.bodySmall)
                    Text("• SAF 文件浏览", style = MaterialTheme.typography.bodySmall)
                    Text("• LLM API 集成", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("GitHub: shinSG/CainsMinor", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text("确定") }
            }
        )
    }

    ListItem(
        headlineContent = { Text("关于 CainsMinor") },
        supportingContent = { Text("版本 1.0", style = MaterialTheme.typography.bodySmall) },
        leadingContent = {
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        modifier = Modifier.clickable { showDialog = true }
    )
}

@Composable
fun SettingsHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
fun SettingsTextField(
    title: String,
    value: String,
    placeholder: String,
    isPassword: Boolean = false,
    isMultiline: Boolean = false,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = !isMultiline,
            maxLines = if (isMultiline) 4 else 1,
            visualTransformation = if (isPassword) {
                androidx.compose.ui.text.input.PasswordVisualTransformation()
            } else {
                androidx.compose.ui.text.input.VisualTransformation.None
            },
            textStyle = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun SettingsTestButton(settings: SettingsManager) {
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Button(
            onClick = {
                isTesting = true
                testResult = null
                // 简单的连接测试
                val url = settings.llmBaseUrl.trimEnd('/')
                val apiKey = settings.llmApiKey
                val model = settings.llmModelName

                if (url.isBlank() || apiKey.isBlank()) {
                    testResult = "❌ 请先填写 Base URL 和 API Key"
                    isTesting = false
                    return@Button
                }

                try {
                    val testUrl = "$url/models"
                    val connection = java.net.URL(testUrl).openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("Authorization", "Bearer $apiKey")
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000

                    val code = connection.responseCode
                    if (code == 200) {
                        testResult = "✅ 连接成功！模型: $model"
                    } else {
                        val error = connection.errorStream?.bufferedReader()?.readText()?.take(100) ?: ""
                        testResult = "❌ HTTP $code: $error"
                    }
                    connection.disconnect()
                } catch (e: Exception) {
                    testResult = "❌ 连接失败: ${e.message?.take(80)}"
                }
                isTesting = false
            },
            enabled = !isTesting && settings.llmBaseUrl.isNotBlank() && settings.llmApiKey.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isTesting) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("测试中...")
            } else {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("测试连接")
            }
        }

        if (testResult != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = testResult!!,
                style = MaterialTheme.typography.bodySmall,
                color = if (testResult!!.startsWith("✅")) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
            )
        }
    }
}
