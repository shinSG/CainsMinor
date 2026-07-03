package com.reader.markdown.settings

import android.content.Context
import android.content.SharedPreferences

/**
 * 应用设置管理器，基于 SharedPreferences
 */
class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("cainsminor_settings", Context.MODE_PRIVATE)

    // ── 阅读设置 ──
    var readerFontSize: Float
        get() = prefs.getFloat(KEY_READER_FONT_SIZE, 16f)
        set(value) { prefs.edit().putFloat(KEY_READER_FONT_SIZE, value).apply() }

    var readerLineHeight: Float
        get() = prefs.getFloat(KEY_READER_LINE_HEIGHT, 1.5f)
        set(value) { prefs.edit().putFloat(KEY_READER_LINE_HEIGHT, value).apply() }

    var readerFontFamily: String
        get() = prefs.getString(KEY_READER_FONT_FAMILY, "sans-serif") ?: "sans-serif"
        set(value) { prefs.edit().putString(KEY_READER_FONT_FAMILY, value).apply() }

    // ── 编辑设置 ──
    var editorFontSize: Float
        get() = prefs.getFloat(KEY_EDITOR_FONT_SIZE, 14f)
        set(value) { prefs.edit().putFloat(KEY_EDITOR_FONT_SIZE, value).apply() }

    var editorTabSize: Int
        get() = prefs.getInt(KEY_EDITOR_TAB_SIZE, 4)
        set(value) { prefs.edit().putInt(KEY_EDITOR_TAB_SIZE, value).apply() }

    var editorWordWrap: Boolean
        get() = prefs.getBoolean(KEY_EDITOR_WORD_WRAP, true)
        set(value) { prefs.edit().putBoolean(KEY_EDITOR_WORD_WRAP, value).apply() }

    // ── 主题设置 ──
    var themeMode: String  // "system" / "light" / "dark"
        get() = prefs.getString(KEY_THEME_MODE, "system") ?: "system"
        set(value) { prefs.edit().putString(KEY_THEME_MODE, value).apply() }

    var dynamicColor: Boolean
        get() = prefs.getBoolean(KEY_DYNAMIC_COLOR, true)
        set(value) { prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, value).apply() }

    // ── 目录设置 ──
    var showTocByDefault: Boolean
        get() = prefs.getBoolean(KEY_SHOW_TOC_DEFAULT, false)
        set(value) { prefs.edit().putBoolean(KEY_SHOW_TOC_DEFAULT, value).apply() }

    // ── 文件浏览设置 ──
    var sortBy: String  // "name" / "date" / "size"
        get() = prefs.getString(KEY_SORT_BY, "date") ?: "date"
        set(value) { prefs.edit().putString(KEY_SORT_BY, value).apply() }

    var showHiddenFiles: Boolean
        get() = prefs.getBoolean(KEY_SHOW_HIDDEN, false)
        set(value) { prefs.edit().putBoolean(KEY_SHOW_HIDDEN, value).apply() }

    companion object {
        private const val KEY_READER_FONT_SIZE = "reader_font_size"
        private const val KEY_READER_LINE_HEIGHT = "reader_line_height"
        private const val KEY_READER_FONT_FAMILY = "reader_font_family"
        private const val KEY_EDITOR_FONT_SIZE = "editor_font_size"
        private const val KEY_EDITOR_TAB_SIZE = "editor_tab_size"
        private const val KEY_EDITOR_WORD_WRAP = "editor_word_wrap"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_SHOW_TOC_DEFAULT = "show_toc_default"
        private const val KEY_SORT_BY = "sort_by"
        private const val KEY_SHOW_HIDDEN = "show_hidden"

        @Volatile
        private var instance: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return instance ?: synchronized(this) {
                instance ?: SettingsManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
