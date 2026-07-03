# CainsMinor - Markdown阅读器

一个简洁的Android应用，用于在移动端阅读和浏览Markdown文件。

## 功能特性

- 📁 **文件浏览**: 浏览设备存储中的文件和文件夹
- 📄 **Markdown渲染**: 高质量的Markdown渲染，支持多种语法
- 🔍 **语法支持**:
  - 标准Markdown语法
  - 表格
  - 任务列表
  - 删除线
  - 代码高亮
  - 图片显示
- 📱 **响应式设计**: 适配不同屏幕尺寸
- 🌙 **深色模式**: 跟随系统主题
- 🔤 **字体调整**: 可调整阅读字体大小

## 技术栈

- **语言**: Kotlin
- **UI框架**: Jetpack Compose
- **Markdown渲染**: Markwon
- **架构**: MVVM with Navigation Compose

## 项目结构

```
markdown-reader/
├── app/
│   ├── src/main/
│   │   ├── java/com/reader/markdown/
│   │   │   ├── MainActivity.kt          # 主Activity
│   │   │   └── ui/
│   │   │       ├── screens/
│   │   │       │   ├── FileBrowserScreen.kt  # 文件浏览界面
│   │   │       │   └── MarkdownViewerScreen.kt # Markdown查看界面
│   │   │       └── theme/
│   │   │           └── Theme.kt          # 主题配置
│   │   ├── res/
│   │   │   └── values/
│   │   │       ├── strings.xml
│   │   │       └── themes.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## 编译与运行

### 前提条件

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 34

### 编译步骤

1. 使用Android Studio打开项目
2. 等待Gradle同步完成
3. 连接Android设备或启动模拟器
4. 点击运行按钮或使用命令行:

```bash
./gradlew assembleDebug
```

### 安装APK

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 权限说明

应用需要以下权限:

- `READ_EXTERNAL_STORAGE`: 读取设备存储中的文件 (Android 12及以下)
- `READ_MEDIA_IMAGES`: 读取媒体文件 (Android 13+)

## 支持的Markdown语法

### 基础语法
```markdown
# 标题
## 二级标题

**粗体** *斜体* ~~删除线~~

- 无序列表
1. 有序列表

[链接](https://example.com)
![图片](image.png)

> 引用块

`行内代码`

​```kotlin
代码块
​```
```

### 扩展语法

**表格:**
```markdown
| 列1 | 列2 | 列3 |
|-----|-----|-----|
| 内容 | 内容 | 内容 |
```

**任务列表:**
```markdown
- [x] 已完成任务
- [ ] 未完成任务
```

## 后续改进方向

- [ ] 添加书签功能
- [ ] 支持文件搜索
- [ ] 添加最近打开文件列表
- [ ] 支持从云存储打开文件
- [ ] 添加阅读进度记忆
- [ ] 支持Markdown编辑模式
- [ ] 导出为PDF功能

## License

MIT License