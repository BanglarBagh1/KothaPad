package com.example.markdown

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import com.example.data.preferences.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Single source of truth for Markdown configuration.
 *
 * Phase 2 and Phase 3 will ADD new fields here with defaults. Never remove or rename existing fields.
 */
data class MarkdownSettings(
    val markdownEnabled: Boolean = true,
    val headings: Boolean = true,
    val boldItalic: Boolean = true,
    val strikethrough: Boolean = true,
    val highlight: Boolean = true,
    val inlineCode: Boolean = true,
    val codeBlocks: Boolean = true,
    val blockquotes: Boolean = true,
    val lists: Boolean = true,
    val taskLists: Boolean = true,
    val links: Boolean = true,
    val tables: Boolean = true,
    val horizontalRule: Boolean = true,
    val readingFontScale: Float = 1.0f,
    // Phase 2 additions:
    val liveStylingEnabled: Boolean = true,
    val dimSyntaxSymbols: Boolean = true,
    val headingSizeScale: Float = 1.0f,
    val codeFontMonospace: Boolean = true,
    val autoContinueLists: Boolean = true
) {
    companion object {
        val KEY_MD_ENABLED = booleanPreferencesKey("md_enabled")
        val KEY_MD_HEADINGS = booleanPreferencesKey("md_headings")
        val KEY_MD_BOLD_ITALIC = booleanPreferencesKey("md_bold_italic")
        val KEY_MD_STRIKETHROUGH = booleanPreferencesKey("md_strikethrough")
        val KEY_MD_HIGHLIGHT = booleanPreferencesKey("md_highlight")
        val KEY_MD_INLINE_CODE = booleanPreferencesKey("md_inline_code")
        val KEY_MD_CODE_BLOCKS = booleanPreferencesKey("md_code_blocks")
        val KEY_MD_BLOCKQUOTES = booleanPreferencesKey("md_blockquotes")
        val KEY_MD_LISTS = booleanPreferencesKey("md_lists")
        val KEY_MD_TASK_LISTS = booleanPreferencesKey("md_task_lists")
        val KEY_MD_LINKS = booleanPreferencesKey("md_links")
        val KEY_MD_TABLES = booleanPreferencesKey("md_tables")
        val KEY_MD_HORIZONTAL_RULE = booleanPreferencesKey("md_horizontal_rule")
        val KEY_MD_READING_FONT_SCALE = floatPreferencesKey("md_reading_font_scale")

        // Phase 2 Keys
        val KEY_MD_LIVE_STYLING = booleanPreferencesKey("md_live_styling")
        val KEY_MD_DIM_SYNTAX = booleanPreferencesKey("md_dim_syntax")
        val KEY_MD_HEADING_SCALE = floatPreferencesKey("md_heading_scale")
        val KEY_MD_CODE_MONOSPACE = booleanPreferencesKey("md_code_monospace")
        val KEY_MD_AUTO_CONTINUE_LISTS = booleanPreferencesKey("md_auto_continue_lists")
    }
}

class MarkdownSettingsRepository(private val preferencesManager: PreferencesManager) {

    val settingsFlow: Flow<MarkdownSettings> = preferencesManager.dataStore.data.map { prefs ->
        MarkdownSettings(
            markdownEnabled = prefs[MarkdownSettings.KEY_MD_ENABLED] ?: true,
            headings = prefs[MarkdownSettings.KEY_MD_HEADINGS] ?: true,
            boldItalic = prefs[MarkdownSettings.KEY_MD_BOLD_ITALIC] ?: true,
            strikethrough = prefs[MarkdownSettings.KEY_MD_STRIKETHROUGH] ?: true,
            highlight = prefs[MarkdownSettings.KEY_MD_HIGHLIGHT] ?: true,
            inlineCode = prefs[MarkdownSettings.KEY_MD_INLINE_CODE] ?: true,
            codeBlocks = prefs[MarkdownSettings.KEY_MD_CODE_BLOCKS] ?: true,
            blockquotes = prefs[MarkdownSettings.KEY_MD_BLOCKQUOTES] ?: true,
            lists = prefs[MarkdownSettings.KEY_MD_LISTS] ?: true,
            taskLists = prefs[MarkdownSettings.KEY_MD_TASK_LISTS] ?: true,
            links = prefs[MarkdownSettings.KEY_MD_LINKS] ?: true,
            tables = prefs[MarkdownSettings.KEY_MD_TABLES] ?: true,
            horizontalRule = prefs[MarkdownSettings.KEY_MD_HORIZONTAL_RULE] ?: true,
            readingFontScale = prefs[MarkdownSettings.KEY_MD_READING_FONT_SCALE] ?: 1.0f,
            liveStylingEnabled = prefs[MarkdownSettings.KEY_MD_LIVE_STYLING] ?: true,
            dimSyntaxSymbols = prefs[MarkdownSettings.KEY_MD_DIM_SYNTAX] ?: true,
            headingSizeScale = prefs[MarkdownSettings.KEY_MD_HEADING_SCALE] ?: 1.0f,
            codeFontMonospace = prefs[MarkdownSettings.KEY_MD_CODE_MONOSPACE] ?: true,
            autoContinueLists = prefs[MarkdownSettings.KEY_MD_AUTO_CONTINUE_LISTS] ?: true
        )
    }

    suspend fun updateSettings(transform: (MarkdownSettings) -> MarkdownSettings) {
        preferencesManager.dataStore.edit { prefs ->
            val current = MarkdownSettings(
                markdownEnabled = prefs[MarkdownSettings.KEY_MD_ENABLED] ?: true,
                headings = prefs[MarkdownSettings.KEY_MD_HEADINGS] ?: true,
                boldItalic = prefs[MarkdownSettings.KEY_MD_BOLD_ITALIC] ?: true,
                strikethrough = prefs[MarkdownSettings.KEY_MD_STRIKETHROUGH] ?: true,
                highlight = prefs[MarkdownSettings.KEY_MD_HIGHLIGHT] ?: true,
                inlineCode = prefs[MarkdownSettings.KEY_MD_INLINE_CODE] ?: true,
                codeBlocks = prefs[MarkdownSettings.KEY_MD_CODE_BLOCKS] ?: true,
                blockquotes = prefs[MarkdownSettings.KEY_MD_BLOCKQUOTES] ?: true,
                lists = prefs[MarkdownSettings.KEY_MD_LISTS] ?: true,
                taskLists = prefs[MarkdownSettings.KEY_MD_TASK_LISTS] ?: true,
                links = prefs[MarkdownSettings.KEY_MD_LINKS] ?: true,
                tables = prefs[MarkdownSettings.KEY_MD_TABLES] ?: true,
                horizontalRule = prefs[MarkdownSettings.KEY_MD_HORIZONTAL_RULE] ?: true,
                readingFontScale = prefs[MarkdownSettings.KEY_MD_READING_FONT_SCALE] ?: 1.0f,
                liveStylingEnabled = prefs[MarkdownSettings.KEY_MD_LIVE_STYLING] ?: true,
                dimSyntaxSymbols = prefs[MarkdownSettings.KEY_MD_DIM_SYNTAX] ?: true,
                headingSizeScale = prefs[MarkdownSettings.KEY_MD_HEADING_SCALE] ?: 1.0f,
                codeFontMonospace = prefs[MarkdownSettings.KEY_MD_CODE_MONOSPACE] ?: true,
                autoContinueLists = prefs[MarkdownSettings.KEY_MD_AUTO_CONTINUE_LISTS] ?: true
            )
            val updated = transform(current)
            prefs[MarkdownSettings.KEY_MD_ENABLED] = updated.markdownEnabled
            prefs[MarkdownSettings.KEY_MD_HEADINGS] = updated.headings
            prefs[MarkdownSettings.KEY_MD_BOLD_ITALIC] = updated.boldItalic
            prefs[MarkdownSettings.KEY_MD_STRIKETHROUGH] = updated.strikethrough
            prefs[MarkdownSettings.KEY_MD_HIGHLIGHT] = updated.highlight
            prefs[MarkdownSettings.KEY_MD_INLINE_CODE] = updated.inlineCode
            prefs[MarkdownSettings.KEY_MD_CODE_BLOCKS] = updated.codeBlocks
            prefs[MarkdownSettings.KEY_MD_BLOCKQUOTES] = updated.blockquotes
            prefs[MarkdownSettings.KEY_MD_LISTS] = updated.lists
            prefs[MarkdownSettings.KEY_MD_TASK_LISTS] = updated.taskLists
            prefs[MarkdownSettings.KEY_MD_LINKS] = updated.links
            prefs[MarkdownSettings.KEY_MD_TABLES] = updated.tables
            prefs[MarkdownSettings.KEY_MD_HORIZONTAL_RULE] = updated.horizontalRule
            prefs[MarkdownSettings.KEY_MD_READING_FONT_SCALE] = updated.readingFontScale
            prefs[MarkdownSettings.KEY_MD_LIVE_STYLING] = updated.liveStylingEnabled
            prefs[MarkdownSettings.KEY_MD_DIM_SYNTAX] = updated.dimSyntaxSymbols
            prefs[MarkdownSettings.KEY_MD_HEADING_SCALE] = updated.headingSizeScale
            prefs[MarkdownSettings.KEY_MD_CODE_MONOSPACE] = updated.codeFontMonospace
            prefs[MarkdownSettings.KEY_MD_AUTO_CONTINUE_LISTS] = updated.autoContinueLists
        }
    }

    suspend fun setMarkdownEnabled(enabled: Boolean) = updateSettings { it.copy(markdownEnabled = enabled) }
    suspend fun setHeadings(enabled: Boolean) = updateSettings { it.copy(headings = enabled) }
    suspend fun setBoldItalic(enabled: Boolean) = updateSettings { it.copy(boldItalic = enabled) }
    suspend fun setStrikethrough(enabled: Boolean) = updateSettings { it.copy(strikethrough = enabled) }
    suspend fun setHighlight(enabled: Boolean) = updateSettings { it.copy(highlight = enabled) }
    suspend fun setInlineCode(enabled: Boolean) = updateSettings { it.copy(inlineCode = enabled) }
    suspend fun setCodeBlocks(enabled: Boolean) = updateSettings { it.copy(codeBlocks = enabled) }
    suspend fun setBlockquotes(enabled: Boolean) = updateSettings { it.copy(blockquotes = enabled) }
    suspend fun setLists(enabled: Boolean) = updateSettings { it.copy(lists = enabled) }
    suspend fun setTaskLists(enabled: Boolean) = updateSettings { it.copy(taskLists = enabled) }
    suspend fun setLinks(enabled: Boolean) = updateSettings { it.copy(links = enabled) }
    suspend fun setTables(enabled: Boolean) = updateSettings { it.copy(tables = enabled) }
    suspend fun setHorizontalRule(enabled: Boolean) = updateSettings { it.copy(horizontalRule = enabled) }
    suspend fun setReadingFontScale(scale: Float) = updateSettings { it.copy(readingFontScale = scale) }

    // Phase 2 setters
    suspend fun setLiveStylingEnabled(enabled: Boolean) = updateSettings { it.copy(liveStylingEnabled = enabled) }
    suspend fun setDimSyntaxSymbols(enabled: Boolean) = updateSettings { it.copy(dimSyntaxSymbols = enabled) }
    suspend fun setHeadingSizeScale(scale: Float) = updateSettings { it.copy(headingSizeScale = scale) }
    suspend fun setCodeFontMonospace(enabled: Boolean) = updateSettings { it.copy(codeFontMonospace = enabled) }
    suspend fun setAutoContinueLists(enabled: Boolean) = updateSettings { it.copy(autoContinueLists = enabled) }

    suspend fun resetToDefaults() {
        updateSettings { MarkdownSettings() }
    }
}
