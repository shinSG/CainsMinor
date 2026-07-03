package com.reader.markdown.agent

/**
 * Markdown 目录生成器
 * 解析文档标题结构，生成可点击的目录（Markdown 锚点链接）
 */
object TocGenerator {

    /**
     * 从 Markdown 内容中解析标题结构
     */
    fun parseHeadings(content: String): List<Heading> {
        val headings = mutableListOf<Heading>()
        val lines = content.lines()
        var inCodeBlock = false

        for ((index, line) in lines.withIndex()) {
            val trimmed = line.trim()

            // 跳过代码块内的内容
            if (trimmed.startsWith("```")) {
                inCodeBlock = !inCodeBlock
                continue
            }
            if (inCodeBlock) continue

            // 匹配标题（1-6级）
            val match = Regex("^(#{1,6})\\s+(.+)").find(trimmed)
            if (match != null) {
                val level = match.groupValues[1].length
                val rawTitle = match.groupValues[2].trim()
                val cleanTitle = rawTitle
                    .replace(Regex("\\[([^]]+)]\\([^)]*\\)"), "$1")  // 去掉链接，保留文字
                    .replace(Regex("[*`~_]"), "")                      // 去掉格式符号
                    .trim()
                val anchor = generateAnchor(cleanTitle)
                headings.add(Heading(level, cleanTitle, anchor, index))
            }
        }
        return headings
    }

    /**
     * 生成 Markdown 格式的目录
     * @param headings 标题列表
     * @param maxLevel 最大标题层级（1-6），默认3级
     * @return Markdown 格式的目录文本
     */
    fun generateToc(headings: List<Heading>, maxLevel: Int = 3): String {
        if (headings.isEmpty()) return ""

        val sb = StringBuilder()
        sb.appendLine("**目录**")
        sb.appendLine()

        for (heading in headings) {
            if (heading.level > maxLevel) continue

            val indent = "  ".repeat(heading.level - 1)
            val link = "[${heading.title}](#${heading.anchor})"
            sb.appendLine("$indent- $link")
        }

        return sb.toString()
    }

    /**
     * 生成 GitHub 风格的锚点
     */
    private fun generateAnchor(title: String): String {
        return title
            .lowercase()
            .replace(Regex("[^a-z0-9\\s\\u4e00-\\u9fa5-]"), "")  // 保留英文、数字、中文、连字符
            .replace(Regex("\\s+"), "-")                            // 空格转连字符
            .replace(Regex("-+"), "-")                              // 合并多个连字符
            .trim('-')
    }

    /**
     * 在文档中插入或更新目录
     * @param content 原始文档内容
     * @param maxLevel 目录最大层级
     * @return 插入目录后的文档内容
     */
    fun insertOrUpdateToc(content: String, maxLevel: Int = 3): Pair<String, Int> {
        val headings = parseHeadings(content)
        if (headings.isEmpty()) return Pair(content, 0)

        val toc = generateToc(headings, maxLevel)
        val tocBlock = "<!-- TOC -->\n$toc\n<!-- /TOC -->"
        val lines = content.lines().toMutableList()

        // 检查是否已有 TOC 标记
        val tocStart = lines.indexOfFirst { it.trim() == "<!-- TOC -->" }
        val tocEnd = lines.indexOfFirst { it.trim() == "<!-- /TOC -->" }

        if (tocStart >= 0 && tocEnd > tocStart) {
            // 更新现有 TOC
            val newLines = lines.subList(0, tocStart) +
                tocBlock.lines() +
                lines.subList(tocEnd + 1, lines.size)
            val newContent = newLines.joinToString("\n")
            return Pair(newContent, tocStart)
        } else {
            // 插入新 TOC：在第一个标题之后（如果有空行则在空行之后）
            val firstHeading = headings.first()
            var insertLine = firstHeading.lineNumber + 1

            // 跳过标题后的空行
            while (insertLine < lines.size && lines[insertLine].isBlank()) {
                insertLine++
            }

            lines.add(insertLine, "")
            for ((i, tocLine) in tocBlock.lines().withIndex()) {
                lines.add(insertLine + i, tocLine)
            }
            lines.add(insertLine + tocBlock.lines().size, "")

            val newContent = lines.joinToString("\n")
            return Pair(newContent, insertLine)
        }
    }

    /**
     * 检查文档是否已有 TOC
     */
    fun hasToc(content: String): Boolean {
        return content.contains("<!-- TOC -->") && content.contains("<!-- /TOC -->")
    }

    /**
     * 移除文档中的 TOC
     */
    fun removeToc(content: String): String {
        val lines = content.lines().toMutableList()
        val tocStart = lines.indexOfFirst { it.trim() == "<!-- TOC -->" }
        val tocEnd = lines.indexOfFirst { it.trim() == "<!-- /TOC -->" }

        if (tocStart >= 0 && tocEnd > tocStart) {
            // 移除 TOC 块及前后的空行
            var start = tocStart
            var end = tocEnd
            while (start > 0 && lines[start - 1].isBlank()) start--
            while (end < lines.size - 1 && lines[end + 1].isBlank()) end++
            for (i in end downTo start) {
                lines.removeAt(i)
            }
        }
        return lines.joinToString("\n")
    }
}

/**
 * 标题数据类
 */
data class Heading(
    val level: Int,       // 1-6
    val title: String,
    val anchor: String,
    val lineNumber: Int   // 行号（0-indexed）
)
