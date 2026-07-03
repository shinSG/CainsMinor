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
