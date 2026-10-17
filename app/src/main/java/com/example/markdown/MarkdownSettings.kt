package com.example.markdown

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.data.preferences.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Single source of truth for Markdown configuration.
 *
 * Phase 2 and Phase 3 ADD new fields here with defaults. Never remove or rename existing fields.
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
    val autoContinueLists: Boolean = true,
    // Phase 3 additions:
    val formatBarEnabled: Boolean = true,
    val formatBarButtons: List<String> = DEFAULT_FORMAT_BAR_BUTTONS,
    val tappableCheckboxes: Boolean = true
) {
    companion object {
        val DEFAULT_FORMAT_BAR_BUTTONS = listOf(
            "heading",
            "bold",
            "italic",
            "strikethrough",
            "highlight",
            "bullet_list",
            "numbered_list",
            "checkbox",
            "quote",
            "inline_code",
            "code_block",
            "link",
            "horizontal_rule"
        )

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

        // Phase 3 Keys
        val KEY_MD_FORMAT_BAR_ENABLED = booleanPreferencesKey("md_format_bar_enabled")
        val KEY_MD_FORMAT_BAR_BUTTONS = stringPreferencesKey("md_format_bar_buttons")
        val KEY_MD_TAPPABLE_CHECKBOXES = booleanPreferencesKey("md_tappable_checkboxes")
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
            autoContinueLists = prefs[MarkdownSettings.KEY_MD_AUTO_CONTINUE_LISTS] ?: true,
            formatBarEnabled = prefs[MarkdownSettings.KEY_MD_FORMAT_BAR_ENABLED] ?: true,
            formatBarButtons = prefs[MarkdownSettings.KEY_MD_FORMAT_BAR_BUTTONS]?.let { str ->
                val list = str.split(",").filter { it.isNotBlank() }
                if (list.isNotEmpty()) list else MarkdownSettings.DEFAULT_FORMAT_BAR_BUTTONS
            } ?: MarkdownSettings.DEFAULT_FORMAT_BAR_BUTTONS,
            tappableCheckboxes = prefs[MarkdownSettings.KEY_MD_TAPPABLE_CHECKBOXES] ?: true
        )
    }

    suspend fun updateSettings(transform: (MarkdownSettings) -> MarkdownSettings) {
        var updatedSettings: MarkdownSettings? = null
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
                autoContinueLists = prefs[MarkdownSettings.KEY_MD_AUTO_CONTINUE_LISTS] ?: true,
                formatBarEnabled = prefs[MarkdownSettings.KEY_MD_FORMAT_BAR_ENABLED] ?: true,
                formatBarButtons = prefs[MarkdownSettings.KEY_MD_FORMAT_BAR_BUTTONS]?.let { str ->
                    val list = str.split(",").filter { it.isNotBlank() }
                    if (list.isNotEmpty()) list else MarkdownSettings.DEFAULT_FORMAT_BAR_BUTTONS
                } ?: MarkdownSettings.DEFAULT_FORMAT_BAR_BUTTONS,
                tappableCheckboxes = prefs[MarkdownSettings.KEY_MD_TAPPABLE_CHECKBOXES] ?: true
            )
            val updated = transform(current)
            updatedSettings = updated
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
            prefs[MarkdownSettings.KEY_MD_FORMAT_BAR_ENABLED] = updated.formatBarEnabled
            prefs[MarkdownSettings.KEY_MD_FORMAT_BAR_BUTTONS] = updated.formatBarButtons.joinToString(",")
            prefs[MarkdownSettings.KEY_MD_TAPPABLE_CHECKBOXES] = updated.tappableCheckboxes
        }
        updatedSettings?.let { updated ->
            preferencesManager.settingsRepository?.updateSettings {
                it.copy(
                    mdEnabled = updated.markdownEnabled,
                    mdHeadings = updated.headings,
                    mdBoldItalic = updated.boldItalic,
                    mdStrikethrough = updated.strikethrough,
                    mdHighlight = updated.highlight,
                    mdInlineCode = updated.inlineCode,
                    mdCodeBlocks = updated.codeBlocks,
                    mdBlockquotes = updated.blockquotes,
                    mdLists = updated.lists,
                    mdTaskLists = updated.taskLists,
                    mdLinks = updated.links,
                    mdTables = updated.tables,
                    mdHorizontalRule = updated.horizontalRule,
                    mdReadingFontScale = updated.readingFontScale,
                    mdLiveStyling = updated.liveStylingEnabled,
                    mdDimSyntax = updated.dimSyntaxSymbols,
                    mdHeadingScale = updated.headingSizeScale,
                    mdCodeMonospace = updated.codeFontMonospace,
                    mdAutoContinueLists = updated.autoContinueLists,
                    mdFormatBarEnabled = updated.formatBarEnabled,
                    mdFormatBarButtons = updated.formatBarButtons,
                    mdTappableCheckboxes = updated.tappableCheckboxes
                )
            }
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

    // Phase 3 setters
    suspend fun setFormatBarEnabled(enabled: Boolean) = updateSettings { it.copy(formatBarEnabled = enabled) }
    suspend fun setFormatBarButtons(buttons: List<String>) = updateSettings { it.copy(formatBarButtons = buttons) }
    suspend fun setTappableCheckboxes(enabled: Boolean) = updateSettings { it.copy(tappableCheckboxes = enabled) }

    suspend fun resetToDefaults() {
        updateSettings { MarkdownSettings() }
    }
}
