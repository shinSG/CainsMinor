package com.reader.markdown.ui.screens

import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import java.text.SimpleDateFormat
import java.util.*

data class FileItem(
    val name: String,
    val uri: Uri,
    val isDirectory: Boolean,
    val lastModified: Long,
    val size: Long,
    val mimeType: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    onFileSelected: (String) -> Unit
) {
    val context = LocalContext.current
    var files by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var currentTreeUri by remember { mutableStateOf<Uri?>(null) }
    var currentPath by remember { mutableStateOf("请选择文件夹") }
    var parentUri by remember { mutableStateOf<Uri?>(null) }
    var hasPermission by remember { mutableStateOf(false) }

    // SAF 目录选择器
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            // 持久化权限
            context.contentResolver.takePersistableUriPermission(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            currentTreeUri = it
            currentPath = DocumentFile.fromTreeUri(context, it)?.name ?: "已选择文件夹"
            parentUri = null
            hasPermission = true
            files = loadFilesFromUri(context, it)
        }
    }

    // 也支持直接打开单个 .md 文件
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            // 把 content:// URI 复制到临时文件以便读取
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val tempFile = java.io.File(context.cacheDir, "temp_opened.md")
                inputStream?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                onFileSelected(tempFile.absolutePath)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentPath,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    if (parentUri != null) {
                        IconButton(onClick = {
                            parentUri?.let { pUri ->
                                currentTreeUri = pUri
                                files = loadFilesFromUri(context, pUri)
                                // 获取父目录的父目录
                                parentUri = getParentUri(context, pUri)
                                currentPath = DocumentFile.fromTreeUri(context, pUri)?.name ?: "..."
                            }
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回上级")
                        }
                    }
                },
                actions = {
                    // 直接打开文件
                    IconButton(onClick = {
                        filePickerLauncher.launch(arrayOf("text/markdown", "text/plain"))
                    }) {
                        Icon(Icons.Default.FileOpen, contentDescription = "打开文件")
                    }
                    // 选择文件夹
                    IconButton(onClick = {
                        folderPickerLauncher.launch(null)
                    }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "选择文件夹")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (!hasPermission) {
            // 首次进入，显示引导界面
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "CainsMinor",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        "请选择包含Markdown文件的文件夹",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { folderPickerLauncher.launch(null) }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("选择文件夹")
                    }
                    OutlinedButton(onClick = {
                        filePickerLauncher.launch(arrayOf("text/markdown", "text/plain"))
                    }) {
                        Icon(Icons.Default.FileOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("打开单个文件")
                    }
                }
            }
        } else if (files.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FolderOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("此文件夹为空")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(files) { file ->
                    FileListItem(
                        file = file,
                        onClick = {
                            if (file.isDirectory) {
                                parentUri = currentTreeUri
                                currentTreeUri = file.uri
                                currentPath = file.name
                                files = loadFilesFromUri(context, file.uri)
                            } else if (file.name.endsWith(".md", ignoreCase = true) ||
                                       file.name.endsWith(".markdown", ignoreCase = true)) {
                                // 复制文件到临时目录再打开
                                try {
                                    val inputStream = context.contentResolver.openInputStream(file.uri)
                                    val tempFile = java.io.File(context.cacheDir, file.name)
                                    inputStream?.use { input ->
                                        tempFile.outputStream().use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    onFileSelected(tempFile.absolutePath)
                                } catch (e: Exception) {
                                    // ignore
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FileListItem(
    file: FileItem,
    onClick: () -> Unit
) {
    val isMarkdown = file.name.endsWith(".md", ignoreCase = true) ||
                     file.name.endsWith(".markdown", ignoreCase = true)

    ListItem(
        headlineContent = {
            Text(
                text = file.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = if (file.isDirectory) "文件夹" else formatFileSize(file.size),
                style = MaterialTheme.typography.bodySmall
            )
        },
        leadingContent = {
            Icon(
                imageVector = when {
                    file.isDirectory -> Icons.Default.Folder
                    isMarkdown -> Icons.Default.Description
                    else -> Icons.Default.InsertDriveFile
                },
                contentDescription = null,
                tint = when {
                    file.isDirectory -> MaterialTheme.colorScheme.primary
                    isMarkdown -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
    Divider(modifier = Modifier.padding(horizontal = 16.dp))
}

private fun loadFilesFromUri(context: android.content.Context, treeUri: Uri): List<FileItem> {
    val files = mutableListOf<FileItem>()
    val documentFile = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()

    documentFile.listFiles().forEach { file ->
        files.add(
            FileItem(
                name = file.name ?: "未知",
                uri = file.uri,
                isDirectory = file.isDirectory,
                lastModified = file.lastModified(),
                size = file.length(),
                mimeType = file.type
            )
        )
    }

    // 排序：文件夹在前，然后按修改时间倒序
    return files.sortedWith(
        compareByDescending<FileItem> { it.isDirectory }
            .thenByDescending { it.lastModified }
    )
}

private fun getParentUri(context: android.content.Context, uri: Uri): Uri? {
    val docId = DocumentsContract.getTreeDocumentId(uri) ?: return null
    // 根目录没有父目录
    if (docId.contains(":").not()) return null
    val parentId = docId.substringBeforeLast(":")
    if (parentId.isEmpty()) return null
    return DocumentsContract.buildTreeDocumentUri(
        uri.authority ?: return null,
        "$parentId:"
    )
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 0 -> "未知"
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
    }
}