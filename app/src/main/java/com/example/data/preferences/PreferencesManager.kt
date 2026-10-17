package com.example.data.preferences

import android.content.Context
import android.widget.Toast
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "kothapad_settings")

class PreferencesManager(private val context: Context) {

    val dataStore get() = context.dataStore

    private val syncPrefs = context.getSharedPreferences("kothapad_sync_prefs", Context.MODE_PRIVATE)

    var settingsRepository: com.example.data.vault.SettingsRepository? = null

    fun attachSettingsRepository(repo: com.example.data.vault.SettingsRepository) {
        this.settingsRepository = repo
    }

    private inline fun notifyRepo(crossinline transform: (com.example.data.vault.AppSettings) -> com.example.data.vault.AppSettings) {
        settingsRepository?.updateSettings { transform(it) }
    }

    enum class PrefType {
        BOOLEAN, INT, LONG, FLOAT, DOUBLE, STRING, STRING_SET
    }

    data class SettingRawEntry(
        val key: String,
        val type: String?,
        val value: Any?
    )

    companion object {
        val KEY_DICTATION_MODE = stringPreferencesKey("dictation_mode")
        val KEY_TIMED_MINUTES = intPreferencesKey("timed_minutes")
        val KEY_PAUSE_TOLERANCE_SECONDS = intPreferencesKey("pause_tolerance_seconds")
        val KEY_IDLE_AUTO_STOP_MINUTES = intPreferencesKey("idle_auto_stop_minutes")
        val KEY_MUTE_BEEP_METHOD = stringPreferencesKey("mute_beep_method") // "MEDIA_SYSTEM", "VIBRATE_ONCE", "OFF"
        val KEY_MUTED_BY_APP = booleanPreferencesKey("muted_by_app")
        val KEY_MUTED_MUSIC_VOL = intPreferencesKey("muted_music_vol")
        val KEY_MUTED_SYSTEM_VOL = intPreferencesKey("muted_system_vol")
        val KEY_SAVED_RINGER_MODE = intPreferencesKey("saved_ringer_mode")
        val KEY_INSERTION_MODE = stringPreferencesKey("insertion_mode")
        val KEY_OFFLINE_PREFERRED = booleanPreferencesKey("offline_preferred")
        val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")

        val KEY_DEFAULT_LANGUAGE = stringPreferencesKey("default_language")
        val KEY_FAVORITE_LANGUAGES = stringSetPreferencesKey("favorite_languages")

        val KEY_APP_THEME = stringPreferencesKey("app_theme")
        val KEY_EDITOR_FONT_SIZE = floatPreferencesKey("editor_font_size")
        val KEY_EDITOR_LINE_SPACING = floatPreferencesKey("editor_line_spacing")
        val KEY_EDITOR_FONT_FAMILY = stringPreferencesKey("editor_font_family")

        // Custom Theme Colors (ARGB ints)
        val KEY_CUSTOM_PRIMARY_COLOR = intPreferencesKey("custom_primary_color")
        val KEY_CUSTOM_BACKGROUND_COLOR = intPreferencesKey("custom_background_color")
        val KEY_CUSTOM_SURFACE_COLOR = intPreferencesKey("custom_surface_color")
        val KEY_CUSTOM_TEXT_COLOR = intPreferencesKey("custom_text_color")
        val KEY_CUSTOM_SECONDARY_COLOR = intPreferencesKey("custom_secondary_color")

        // Typography Ext
        val KEY_CUSTOM_FONT_PATH = stringPreferencesKey("custom_font_path")
        val KEY_CUSTOM_FONT_NAME = stringPreferencesKey("custom_font_name")
        val KEY_EDITOR_LETTER_SPACING = floatPreferencesKey("editor_letter_spacing")
        val KEY_EDITOR_PARAGRAPH_SPACING = intPreferencesKey("editor_paragraph_spacing")
        val KEY_EDITOR_TEXT_ALIGNMENT = stringPreferencesKey("editor_text_alignment")
        val KEY_EDITOR_MARGIN_DP = intPreferencesKey("editor_margin_dp")
        val KEY_EDITOR_MAX_WIDTH_DP = intPreferencesKey("editor_max_width_dp")

        // Toolbar Items & Order
        val KEY_TOOLBAR_ITEM_ORDER = stringPreferencesKey("toolbar_item_order")
        val KEY_TOOLBAR_HIDDEN_ITEMS = stringSetPreferencesKey("toolbar_hidden_items")

        // Last Opened Note
        val KEY_LAST_OPENED_NOTE_ID = longPreferencesKey("last_opened_note_id")

        // Mic Button & Floating Cluster
        val KEY_MIC_BUTTON_SIZE = stringPreferencesKey("mic_button_size")
        val KEY_MIC_BUTTON_SHAPE = stringPreferencesKey("mic_button_shape")
        val KEY_MIC_BUTTON_COLOR = stringPreferencesKey("mic_button_color")
        val KEY_MIC_BUTTON_POSITION = stringPreferencesKey("mic_button_position")
        val KEY_MIC_BUTTON_OFFSET_Y = intPreferencesKey("mic_button_offset_y")

        // Visual Polish, Animation & Haptics
        val KEY_STATUS_CHIP_STYLE = stringPreferencesKey("status_chip_style")
        val KEY_UI_DENSITY = stringPreferencesKey("ui_density")
        val KEY_CORNER_RADIUS_DP = intPreferencesKey("corner_radius_dp")
        val KEY_DICTATION_ANIMATION_STYLE = stringPreferencesKey("dictation_animation_style")
        val KEY_HAPTICS_LEVEL = stringPreferencesKey("haptics_level")

        val KEY_PIPELINE_SPACING = booleanPreferencesKey("pipeline_spacing")
        val KEY_PIPELINE_AUTOCAP = booleanPreferencesKey("pipeline_autocap")
        val KEY_PIPELINE_COMMANDS = booleanPreferencesKey("pipeline_commands")

        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_KEYBOARD_TYPE = stringPreferencesKey("keyboard_type") // "IN_APP" or "SYSTEM"
        val KEY_SHOW_WORD_COUNT = booleanPreferencesKey("show_word_count")
        val KEY_KEYBOARD_LAYOUT_JSON = stringPreferencesKey("keyboard_layout_json")
        val KEY_KEYBOARD_KEY_HEIGHT_DP = intPreferencesKey("keyboard_key_height_dp") // 30-56 dp, default 40
        val KEY_KEYBOARD_FONT_SIZE_SP = intPreferencesKey("keyboard_font_size_sp") // default 15
        val KEY_KEYBOARD_HAPTIC = booleanPreferencesKey("keyboard_haptic") // default true
        val KEY_KEYBOARD_HOLD_BEHAVIOR = stringPreferencesKey("keyboard_hold_behavior") // "CUSTOMIZE" or "REPEAT"
        val KEY_KEYBOARD_EDIT_MODE = booleanPreferencesKey("keyboard_edit_mode") // default false
        val KEY_TRASH_AUTO_EMPTY_DAYS = intPreferencesKey("trash_auto_empty_days") // default 7
        val KEY_DEFAULT_NEW_NOTE_FOLDER = stringPreferencesKey("default_new_note_folder") // default "General"

        // Tree Explorer & Note Metrics
        val KEY_SHOW_NOTE_METRICS_IN_LIST = booleanPreferencesKey("show_note_metrics_in_list") // default true
        val KEY_NOTE_METRICS_STYLE = stringPreferencesKey("note_metrics_style") // "COMPACT" or "DETAILED"
        val KEY_NOTE_METRICS_ORDER = stringPreferencesKey("note_metrics_order")
        val KEY_NOTE_METRICS_HIDDEN_ITEMS = stringSetPreferencesKey("note_metrics_hidden_items")
        val KEY_KEYBOARD_LAYOUT_VERSION = intPreferencesKey("keyboard_layout_version_v2")
        val KEY_EXPANDED_FOLDERS = stringSetPreferencesKey("expanded_tree_folders")
        val KEY_CUSTOM_FOLDERS = stringSetPreferencesKey("custom_folders_set")

        // Notes Manager Settings Keys
        val KEY_NEW_NOTE_LOCATION_MODE = stringPreferencesKey("new_note_location_mode")
        val KEY_FIXED_NEW_NOTE_FOLDER = stringPreferencesKey("fixed_new_note_folder")
        val KEY_ASK_BEFORE_CREATING = booleanPreferencesKey("ask_before_creating")
        val KEY_ASK_CHOOSE_TEMPLATE_TOGGLE = booleanPreferencesKey("ask_choose_template_toggle")
        val KEY_ASK_START_RECORDING_TOGGLE = booleanPreferencesKey("ask_start_recording_toggle")
        val KEY_NEW_NOTE_NAMING_RULE = stringPreferencesKey("new_note_naming_rule")
        val KEY_AFTER_CREATING_NOTE = stringPreferencesKey("after_creating_note")
        val KEY_LAST_ACTIVE_FOLDER = stringPreferencesKey("last_active_folder")
        val KEY_NOTE_SORT_OPTION = stringPreferencesKey("note_sort_option")
        val KEY_OLD_DATA_MIGRATED = booleanPreferencesKey("old_data_migrated_to_vault")

        // Floating Controls Layout ("Side Rail") Keys
        val KEY_FLOATING_USE_SIDE_RAIL = booleanPreferencesKey("floating_use_side_rail")
        val KEY_RAIL_SIDE = stringPreferencesKey("rail_side")
        val KEY_RAIL_SCALE = floatPreferencesKey("rail_scale")
        val KEY_RAIL_BOTTOM_OFFSET_DP = intPreferencesKey("rail_bottom_offset_dp")
        val KEY_RAIL_SPACING_DP = intPreferencesKey("rail_spacing_dp")
        val KEY_RAIL_KEEP_TEXT_CLEAR = booleanPreferencesKey("rail_keep_text_clear")
        val KEY_RAIL_BUTTON_ORDER = stringPreferencesKey("rail_button_order")
        val KEY_TOGGLE_SCALE = floatPreferencesKey("toggle_scale")
        val KEY_TOGGLE_BOTTOM_OFFSET_DP = intPreferencesKey("toggle_bottom_offset_dp")
        val KEY_TOGGLE_GAP_DP = intPreferencesKey("toggle_gap_dp")
        val KEY_FORMAT_BAR_SCALE = floatPreferencesKey("format_bar_scale")
        val KEY_FORMAT_BAR_BOTTOM_OFFSET_DP = intPreferencesKey("format_bar_bottom_offset_dp")
        val KEY_FORMAT_BAR_WIDTH_PERCENT = intPreferencesKey("format_bar_width_percent")
        val KEY_FORMAT_BAR_ALIGNMENT = stringPreferencesKey("format_bar_alignment")
        val KEY_WAVEFORM_SCALE = floatPreferencesKey("waveform_scale")
        val KEY_WAVEFORM_SHOW = booleanPreferencesKey("waveform_show")

        val KNOWN_PREF_TYPES: Map<String, PrefType> = mapOf(
            KEY_DICTATION_MODE.name to PrefType.STRING,
            KEY_TIMED_MINUTES.name to PrefType.INT,
            KEY_PAUSE_TOLERANCE_SECONDS.name to PrefType.INT,
            KEY_IDLE_AUTO_STOP_MINUTES.name to PrefType.INT,
            KEY_MUTE_BEEP_METHOD.name to PrefType.STRING,
            KEY_MUTED_BY_APP.name to PrefType.BOOLEAN,
            KEY_MUTED_MUSIC_VOL.name to PrefType.INT,
            KEY_MUTED_SYSTEM_VOL.name to PrefType.INT,
            KEY_SAVED_RINGER_MODE.name to PrefType.INT,
            KEY_INSERTION_MODE.name to PrefType.STRING,
            KEY_OFFLINE_PREFERRED.name to PrefType.BOOLEAN,
            KEY_KEEP_SCREEN_ON.name to PrefType.BOOLEAN,
            KEY_DEFAULT_LANGUAGE.name to PrefType.STRING,
            KEY_FAVORITE_LANGUAGES.name to PrefType.STRING_SET,
            KEY_APP_THEME.name to PrefType.STRING,
            KEY_EDITOR_FONT_SIZE.name to PrefType.FLOAT,
            KEY_EDITOR_LINE_SPACING.name to PrefType.FLOAT,
            KEY_EDITOR_FONT_FAMILY.name to PrefType.STRING,
            KEY_CUSTOM_PRIMARY_COLOR.name to PrefType.INT,
            KEY_CUSTOM_BACKGROUND_COLOR.name to PrefType.INT,
            KEY_CUSTOM_SURFACE_COLOR.name to PrefType.INT,
            KEY_CUSTOM_TEXT_COLOR.name to PrefType.INT,
            KEY_CUSTOM_SECONDARY_COLOR.name to PrefType.INT,
            KEY_CUSTOM_FONT_PATH.name to PrefType.STRING,
            KEY_CUSTOM_FONT_NAME.name to PrefType.STRING,
            KEY_EDITOR_LETTER_SPACING.name to PrefType.FLOAT,
            KEY_EDITOR_PARAGRAPH_SPACING.name to PrefType.INT,
            KEY_EDITOR_TEXT_ALIGNMENT.name to PrefType.STRING,
            KEY_EDITOR_MARGIN_DP.name to PrefType.INT,
            KEY_EDITOR_MAX_WIDTH_DP.name to PrefType.INT,
            KEY_TOOLBAR_ITEM_ORDER.name to PrefType.STRING,
            KEY_TOOLBAR_HIDDEN_ITEMS.name to PrefType.STRING_SET,
            KEY_LAST_OPENED_NOTE_ID.name to PrefType.LONG,
            KEY_MIC_BUTTON_SIZE.name to PrefType.STRING,
            KEY_MIC_BUTTON_SHAPE.name to PrefType.STRING,
            KEY_MIC_BUTTON_COLOR.name to PrefType.STRING,
            KEY_MIC_BUTTON_POSITION.name to PrefType.STRING,
            KEY_MIC_BUTTON_OFFSET_Y.name to PrefType.INT,
            KEY_STATUS_CHIP_STYLE.name to PrefType.STRING,
            KEY_UI_DENSITY.name to PrefType.STRING,
            KEY_CORNER_RADIUS_DP.name to PrefType.INT,
            KEY_DICTATION_ANIMATION_STYLE.name to PrefType.STRING,
            KEY_HAPTICS_LEVEL.name to PrefType.STRING,
            KEY_PIPELINE_SPACING.name to PrefType.BOOLEAN,
            KEY_PIPELINE_AUTOCAP.name to PrefType.BOOLEAN,
            KEY_PIPELINE_COMMANDS.name to PrefType.BOOLEAN,
            KEY_ONBOARDING_COMPLETED.name to PrefType.BOOLEAN,
            KEY_KEYBOARD_TYPE.name to PrefType.STRING,
            KEY_SHOW_WORD_COUNT.name to PrefType.BOOLEAN,
            KEY_KEYBOARD_LAYOUT_JSON.name to PrefType.STRING,
            KEY_KEYBOARD_KEY_HEIGHT_DP.name to PrefType.INT,
            KEY_KEYBOARD_FONT_SIZE_SP.name to PrefType.INT,
            KEY_KEYBOARD_HAPTIC.name to PrefType.BOOLEAN,
            KEY_KEYBOARD_HOLD_BEHAVIOR.name to PrefType.STRING,
            KEY_KEYBOARD_EDIT_MODE.name to PrefType.BOOLEAN,
            KEY_TRASH_AUTO_EMPTY_DAYS.name to PrefType.INT,
            KEY_DEFAULT_NEW_NOTE_FOLDER.name to PrefType.STRING,
            KEY_SHOW_NOTE_METRICS_IN_LIST.name to PrefType.BOOLEAN,
            KEY_NOTE_METRICS_STYLE.name to PrefType.STRING,
            KEY_NOTE_METRICS_ORDER.name to PrefType.STRING,
            KEY_NOTE_METRICS_HIDDEN_ITEMS.name to PrefType.STRING_SET,
            KEY_KEYBOARD_LAYOUT_VERSION.name to PrefType.INT,
            KEY_EXPANDED_FOLDERS.name to PrefType.STRING_SET,
            KEY_CUSTOM_FOLDERS.name to PrefType.STRING_SET,
            KEY_NEW_NOTE_LOCATION_MODE.name to PrefType.STRING,
            KEY_FIXED_NEW_NOTE_FOLDER.name to PrefType.STRING,
            KEY_ASK_BEFORE_CREATING.name to PrefType.BOOLEAN,
            KEY_ASK_CHOOSE_TEMPLATE_TOGGLE.name to PrefType.BOOLEAN,
            KEY_ASK_START_RECORDING_TOGGLE.name to PrefType.BOOLEAN,
            KEY_NEW_NOTE_NAMING_RULE.name to PrefType.STRING,
            KEY_AFTER_CREATING_NOTE.name to PrefType.STRING,
            KEY_LAST_ACTIVE_FOLDER.name to PrefType.STRING,
            KEY_NOTE_SORT_OPTION.name to PrefType.STRING,
            "md_enabled" to PrefType.BOOLEAN,
            "md_headings" to PrefType.BOOLEAN,
            "md_bold_italic" to PrefType.BOOLEAN,
            "md_strikethrough" to PrefType.BOOLEAN,
            "md_highlight" to PrefType.BOOLEAN,
            "md_inline_code" to PrefType.BOOLEAN,
            "md_code_blocks" to PrefType.BOOLEAN,
            "md_blockquotes" to PrefType.BOOLEAN,
            "md_lists" to PrefType.BOOLEAN,
            "md_task_lists" to PrefType.BOOLEAN,
            "md_links" to PrefType.BOOLEAN,
            "md_tables" to PrefType.BOOLEAN,
            "md_horizontal_rule" to PrefType.BOOLEAN,
            "md_reading_font_scale" to PrefType.FLOAT,
            "md_live_styling" to PrefType.BOOLEAN,
            "md_dim_syntax" to PrefType.BOOLEAN,
            "md_heading_scale" to PrefType.FLOAT,
            "md_code_monospace" to PrefType.BOOLEAN,
            "md_auto_continue_lists" to PrefType.BOOLEAN,
            "md_format_bar_enabled" to PrefType.BOOLEAN,
            "md_format_bar_buttons" to PrefType.STRING,
            "md_tappable_checkboxes" to PrefType.BOOLEAN,
            KEY_FLOATING_USE_SIDE_RAIL.name to PrefType.BOOLEAN,
            KEY_RAIL_SIDE.name to PrefType.STRING,
            KEY_RAIL_SCALE.name to PrefType.FLOAT,
            KEY_RAIL_BOTTOM_OFFSET_DP.name to PrefType.INT,
            KEY_RAIL_SPACING_DP.name to PrefType.INT,
            KEY_RAIL_KEEP_TEXT_CLEAR.name to PrefType.BOOLEAN,
            KEY_RAIL_BUTTON_ORDER.name to PrefType.STRING,
            KEY_TOGGLE_SCALE.name to PrefType.FLOAT,
            KEY_TOGGLE_BOTTOM_OFFSET_DP.name to PrefType.INT,
            KEY_TOGGLE_GAP_DP.name to PrefType.INT,
            KEY_FORMAT_BAR_SCALE.name to PrefType.FLOAT,
            KEY_FORMAT_BAR_BOTTOM_OFFSET_DP.name to PrefType.INT,
            KEY_FORMAT_BAR_WIDTH_PERCENT.name to PrefType.INT,
            KEY_FORMAT_BAR_ALIGNMENT.name to PrefType.STRING,
            KEY_WAVEFORM_SCALE.name to PrefType.FLOAT,
            KEY_WAVEFORM_SHOW.name to PrefType.BOOLEAN
        )

        val KNOWN_SYNC_PREF_TYPES: Map<String, PrefType> = mapOf(
            "last_opened_note_id" to PrefType.LONG,
            "onboarding_completed" to PrefType.BOOLEAN,
            "expanded_folders" to PrefType.STRING_SET,
            "last_active_folder" to PrefType.STRING
        )

        fun safeToLong(value: Any?): Long {
            return when (value) {
                is Number -> value.toLong()
                is String -> value.toLongOrNull()
                    ?: throw IllegalArgumentException("Cannot convert string '$value' to Long")
                is Boolean -> if (value) 1L else 0L
                else -> throw IllegalArgumentException("Cannot convert $value of type ${value?.javaClass?.simpleName} to Long")
            }
        }

        fun safeToInt(value: Any?): Int {
            return when (value) {
                is Number -> value.toInt()
                is String -> value.toIntOrNull()
                    ?: throw IllegalArgumentException("Cannot convert string '$value' to Int")
                is Boolean -> if (value) 1 else 0
                else -> throw IllegalArgumentException("Cannot convert $value of type ${value?.javaClass?.simpleName} to Int")
            }
        }

        fun safeToFloat(value: Any?): Float {
            return when (value) {
                is Number -> value.toFloat()
                is String -> value.toFloatOrNull()
                    ?: throw IllegalArgumentException("Cannot convert string '$value' to Float")
                else -> throw IllegalArgumentException("Cannot convert $value of type ${value?.javaClass?.simpleName} to Float")
            }
        }

        fun safeToDouble(value: Any?): Double {
            return when (value) {
                is Number -> value.toDouble()
                is String -> value.toDoubleOrNull()
                    ?: throw IllegalArgumentException("Cannot convert string '$value' to Double")
                else -> throw IllegalArgumentException("Cannot convert $value of type ${value?.javaClass?.simpleName} to Double")
            }
        }

        fun safeToBoolean(value: Any?): Boolean {
            return when (value) {
                is Boolean -> value
                is Number -> value.toInt() != 0
                is String -> when {
                    value.equals("true", ignoreCase = true) -> true
                    value.equals("false", ignoreCase = true) -> false
                    value == "1" -> true
                    value == "0" -> false
                    else -> throw IllegalArgumentException("Cannot convert string '$value' to Boolean")
                }
                else -> throw IllegalArgumentException("Cannot convert $value of type ${value?.javaClass?.simpleName} to Boolean")
            }
        }

        fun safeToString(value: Any?): String {
            return when (value) {
                null -> ""
                is String -> value
                else -> value.toString()
            }
        }

        fun safeToStringSet(value: Any?): Set<String> {
            return when (value) {
                is JSONArray -> {
                    val set = mutableSetOf<String>()
                    for (i in 0 until value.length()) {
                        val item = value.opt(i)
                        if (item != null && item != JSONObject.NULL) {
                            set.add(item.toString())
                        }
                    }
                    set
                }
                is Collection<*> -> value.mapNotNull { it?.toString() }.toSet()
                is String -> setOf(value)
                else -> throw IllegalArgumentException("Cannot convert $value to Set<String>")
            }
        }
    }

    // Dictation Flow
    val dictationMode: Flow<String> = context.dataStore.data.map { it[KEY_DICTATION_MODE] ?: "CONTINUOUS" }
    val timedMinutes: Flow<Int> = context.dataStore.data.map { it[KEY_TIMED_MINUTES] ?: 3 }
    val pauseToleranceSeconds: Flow<Int> = context.dataStore.data.map { it[KEY_PAUSE_TOLERANCE_SECONDS] ?: 30 }
    val idleAutoStopMinutes: Flow<Int> = context.dataStore.data.map { it[KEY_IDLE_AUTO_STOP_MINUTES] ?: 0 }
    val muteBeepMethod: Flow<String> = context.dataStore.data.map { it[KEY_MUTE_BEEP_METHOD] ?: "MEDIA_SYSTEM" }
    val mutedByApp: Flow<Boolean> = context.dataStore.data.map { it[KEY_MUTED_BY_APP] ?: false }
    val mutedMusicVol: Flow<Int> = context.dataStore.data.map { it[KEY_MUTED_MUSIC_VOL] ?: -1 }
    val mutedSystemVol: Flow<Int> = context.dataStore.data.map { it[KEY_MUTED_SYSTEM_VOL] ?: -1 }
    val savedRingerMode: Flow<Int> = context.dataStore.data.map { it[KEY_SAVED_RINGER_MODE] ?: -1 }
    val insertionMode: Flow<String> = context.dataStore.data.map { it[KEY_INSERTION_MODE] ?: "AT_CURSOR" }
    val offlinePreferred: Flow<Boolean> = context.dataStore.data.map { it[KEY_OFFLINE_PREFERRED] ?: false }
    val keepScreenOn: Flow<Boolean> = context.dataStore.data.map { it[KEY_KEEP_SCREEN_ON] ?: true }

    // Language Flow
    val defaultLanguage: Flow<String> = context.dataStore.data.map { it[KEY_DEFAULT_LANGUAGE] ?: "bn-BD" }
    val favoriteLanguages: Flow<Set<String>> = context.dataStore.data.map {
        it[KEY_FAVORITE_LANGUAGES] ?: setOf("bn-BD", "bn-IN", "en-US")
    }

    // Appearance Flow
    val appTheme: Flow<String> = context.dataStore.data.map { it[KEY_APP_THEME] ?: "AMOLED" }
    val editorFontSize: Flow<Float> = context.dataStore.data.map { it[KEY_EDITOR_FONT_SIZE] ?: 18f }
    val editorLineSpacing: Flow<Float> = context.dataStore.data.map { it[KEY_EDITOR_LINE_SPACING] ?: 1.5f }
    val editorFontFamily: Flow<String> = context.dataStore.data.map { it[KEY_EDITOR_FONT_FAMILY] ?: "BENGALI" }

    // Custom Theme Colors Flow
    val customPrimaryColor: Flow<Int> = context.dataStore.data.map { it[KEY_CUSTOM_PRIMARY_COLOR] ?: 0xFFFF7043.toInt() }
    val customBackgroundColor: Flow<Int> = context.dataStore.data.map { it[KEY_CUSTOM_BACKGROUND_COLOR] ?: 0xFF000000.toInt() }
    val customSurfaceColor: Flow<Int> = context.dataStore.data.map { it[KEY_CUSTOM_SURFACE_COLOR] ?: 0xFF121212.toInt() }
    val customTextColor: Flow<Int> = context.dataStore.data.map { it[KEY_CUSTOM_TEXT_COLOR] ?: 0xFFFFFFFF.toInt() }
    val customSecondaryColor: Flow<Int> = context.dataStore.data.map { it[KEY_CUSTOM_SECONDARY_COLOR] ?: 0xFFFF8A65.toInt() }

    // Typography Ext Flow
    val customFontPath: Flow<String?> = context.dataStore.data.map { it[KEY_CUSTOM_FONT_PATH] }
    val customFontName: Flow<String> = context.dataStore.data.map { it[KEY_CUSTOM_FONT_NAME] ?: "" }
    val editorLetterSpacing: Flow<Float> = context.dataStore.data.map { it[KEY_EDITOR_LETTER_SPACING] ?: 0f }
    val editorParagraphSpacing: Flow<Int> = context.dataStore.data.map { it[KEY_EDITOR_PARAGRAPH_SPACING] ?: 8 }
    val editorTextAlignment: Flow<String> = context.dataStore.data.map { it[KEY_EDITOR_TEXT_ALIGNMENT] ?: "LEFT" }
    val editorMarginDp: Flow<Int> = context.dataStore.data.map { it[KEY_EDITOR_MARGIN_DP] ?: 16 }
    val editorMaxWidthDp: Flow<Int> = context.dataStore.data.map { it[KEY_EDITOR_MAX_WIDTH_DP] ?: 0 }

    // Toolbar Flow
    val toolbarItemOrder: Flow<String> = context.dataStore.data.map {
        it[KEY_TOOLBAR_ITEM_ORDER] ?: "DICTATION_STATUS,METRICS,NOTE_TIMER,KEYBOARD_MODE,TEXT_SIZE,TEXT_STYLE,AUDIO_SOURCE"
    }
    val toolbarHiddenItems: Flow<Set<String>> = context.dataStore.data.map { it[KEY_TOOLBAR_HIDDEN_ITEMS] ?: emptySet() }

    // Mic Button Flow
    val micButtonSize: Flow<String> = context.dataStore.data.map { it[KEY_MIC_BUTTON_SIZE] ?: "MEDIUM" }
    val micButtonShape: Flow<String> = context.dataStore.data.map { it[KEY_MIC_BUTTON_SHAPE] ?: "CIRCLE" }
    val micButtonColor: Flow<String> = context.dataStore.data.map { it[KEY_MIC_BUTTON_COLOR] ?: "DEFAULT" }
    val micButtonPosition: Flow<String> = context.dataStore.data.map { it[KEY_MIC_BUTTON_POSITION] ?: "RIGHT" }
    val micButtonOffsetY: Flow<Int> = context.dataStore.data.map { it[KEY_MIC_BUTTON_OFFSET_Y] ?: 8 }

    // Visual Polish, Animation & Haptics Flow
    val statusChipStyle: Flow<String> = context.dataStore.data.map { it[KEY_STATUS_CHIP_STYLE] ?: "PILL" }
    val uiDensity: Flow<String> = context.dataStore.data.map { it[KEY_UI_DENSITY] ?: "NORMAL" }
    val cornerRadiusDp: Flow<Int> = context.dataStore.data.map { it[KEY_CORNER_RADIUS_DP] ?: 12 }
    val dictationAnimationStyle: Flow<String> = context.dataStore.data.map { it[KEY_DICTATION_ANIMATION_STYLE] ?: "PULSE_RING" }
    val hapticsLevel: Flow<String> = context.dataStore.data.map { it[KEY_HAPTICS_LEVEL] ?: "MEDIUM" }

    // Text Pipeline Flow
    val pipelineSpacing: Flow<Boolean> = context.dataStore.data.map { it[KEY_PIPELINE_SPACING] ?: true }
    val pipelineAutoCap: Flow<Boolean> = context.dataStore.data.map { it[KEY_PIPELINE_AUTOCAP] ?: true }
    val pipelineCommands: Flow<Boolean> = context.dataStore.data.map { it[KEY_PIPELINE_COMMANDS] ?: true }

    // General Flow
    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map {
        it[KEY_ONBOARDING_COMPLETED] ?: isOnboardingCompletedSync()
    }
    val keyboardType: Flow<String> = context.dataStore.data.map { it[KEY_KEYBOARD_TYPE] ?: "IN_APP" }
    val showWordCount: Flow<Boolean> = context.dataStore.data.map { it[KEY_SHOW_WORD_COUNT] ?: true }

    // Keyboard Flows
    val keyboardLayoutJson: Flow<String?> = context.dataStore.data.map { it[KEY_KEYBOARD_LAYOUT_JSON] }
    val keyboardKeyHeightDp: Flow<Int> = context.dataStore.data.map { it[KEY_KEYBOARD_KEY_HEIGHT_DP] ?: 40 }
    val keyboardFontSizeSp: Flow<Int> = context.dataStore.data.map { it[KEY_KEYBOARD_FONT_SIZE_SP] ?: 15 }
    val keyboardHaptic: Flow<Boolean> = context.dataStore.data.map { it[KEY_KEYBOARD_HAPTIC] ?: true }
    val keyboardHoldBehavior: Flow<String> = context.dataStore.data.map { it[KEY_KEYBOARD_HOLD_BEHAVIOR] ?: "CUSTOMIZE" }
    val keyboardEditMode: Flow<Boolean> = context.dataStore.data.map { it[KEY_KEYBOARD_EDIT_MODE] ?: false }
    val trashAutoEmptyDays: Flow<Int> = context.dataStore.data.map { it[KEY_TRASH_AUTO_EMPTY_DAYS] ?: 7 }
    val defaultNewNoteFolder: Flow<String> = context.dataStore.data.map { it[KEY_DEFAULT_NEW_NOTE_FOLDER] ?: "General" }
    val showNoteMetricsInList: Flow<Boolean> = context.dataStore.data.map { it[KEY_SHOW_NOTE_METRICS_IN_LIST] ?: true }
    val noteMetricsStyle: Flow<String> = context.dataStore.data.map { it[KEY_NOTE_METRICS_STYLE] ?: "COMPACT" }
    val noteMetricsOrder: Flow<String> = context.dataStore.data.map { it[KEY_NOTE_METRICS_ORDER] ?: com.example.ui.notes.NoteMetricsConfig.DEFAULT_ORDER_STRING }
    val noteMetricsHiddenItems: Flow<Set<String>> = context.dataStore.data.map { it[KEY_NOTE_METRICS_HIDDEN_ITEMS] ?: emptySet() }
    val keyboardLayoutVersion: Flow<Int> = context.dataStore.data.map { it[KEY_KEYBOARD_LAYOUT_VERSION] ?: 1 }
    val expandedFolders: Flow<Set<String>> = context.dataStore.data.map { it[KEY_EXPANDED_FOLDERS] ?: setOf("General", "Templates") }
    val customFolders: Flow<Set<String>> = context.dataStore.data.map { it[KEY_CUSTOM_FOLDERS] ?: emptySet() }
    val noteSortOption: Flow<String> = context.dataStore.data.map { it[KEY_NOTE_SORT_OPTION] ?: "MODIFIED_DESC" }
    val isOldDataMigrated: Flow<Boolean> = context.dataStore.data.map { it[KEY_OLD_DATA_MIGRATED] ?: false }

    suspend fun setNoteSortOption(option: String) {
        context.dataStore.edit { it[KEY_NOTE_SORT_OPTION] = option }
        syncPrefs.edit().putString("note_sort_option", option).apply()
    }

    fun getNoteSortOptionSync(): String {
        return syncPrefs.getString("note_sort_option", "MODIFIED_DESC") ?: "MODIFIED_DESC"
    }

    suspend fun setOldDataMigrated(migrated: Boolean) {
        context.dataStore.edit { it[KEY_OLD_DATA_MIGRATED] = migrated }
    }

    // Update helpers
    suspend fun setDictationMode(mode: String) {
        context.dataStore.edit { it[KEY_DICTATION_MODE] = mode }
        notifyRepo { it.copy(dictationMode = mode) }
    }
    suspend fun setTimedMinutes(minutes: Int) {
        context.dataStore.edit { it[KEY_TIMED_MINUTES] = minutes }
        notifyRepo { it.copy(timedMinutes = minutes) }
    }
    suspend fun setPauseToleranceSeconds(seconds: Int) {
        context.dataStore.edit { it[KEY_PAUSE_TOLERANCE_SECONDS] = seconds }
        notifyRepo { it.copy(pauseToleranceSeconds = seconds) }
    }
    suspend fun setIdleAutoStopMinutes(minutes: Int) {
        context.dataStore.edit { it[KEY_IDLE_AUTO_STOP_MINUTES] = minutes }
        notifyRepo { it.copy(idleAutoStopMinutes = minutes) }
    }
    suspend fun setMuteBeepMethod(method: String) {
        context.dataStore.edit { it[KEY_MUTE_BEEP_METHOD] = method }
        notifyRepo { it.copy(muteBeepMethod = method) }
    }

    suspend fun saveMutedRecord(musicVol: Int, systemVol: Int, ringerMode: Int) = context.dataStore.edit {
        it[KEY_MUTED_BY_APP] = true
        it[KEY_MUTED_MUSIC_VOL] = musicVol
        it[KEY_MUTED_SYSTEM_VOL] = systemVol
        it[KEY_SAVED_RINGER_MODE] = ringerMode
    }

    suspend fun clearMutedRecord() = context.dataStore.edit {
        it[KEY_MUTED_BY_APP] = false
        it.remove(KEY_MUTED_MUSIC_VOL)
        it.remove(KEY_MUTED_SYSTEM_VOL)
        it.remove(KEY_SAVED_RINGER_MODE)
    }

    suspend fun setInsertionMode(mode: String) {
        context.dataStore.edit { it[KEY_INSERTION_MODE] = mode }
        notifyRepo { it.copy(insertionMode = mode) }
    }
    suspend fun setOfflinePreferred(offline: Boolean) {
        context.dataStore.edit { it[KEY_OFFLINE_PREFERRED] = offline }
        notifyRepo { it.copy(offlinePreferred = offline) }
    }
    suspend fun setKeepScreenOn(keep: Boolean) {
        context.dataStore.edit { it[KEY_KEEP_SCREEN_ON] = keep }
        notifyRepo { it.copy(keepScreenOn = keep) }
    }

    suspend fun setDefaultLanguage(lang: String) {
        context.dataStore.edit { it[KEY_DEFAULT_LANGUAGE] = lang }
        notifyRepo { it.copy(defaultLanguage = lang) }
    }
    suspend fun setFavoriteLanguages(langs: Set<String>) {
        context.dataStore.edit { it[KEY_FAVORITE_LANGUAGES] = langs }
        notifyRepo { it.copy(favoriteLanguages = langs) }
    }

    suspend fun setAppTheme(theme: String) {
        context.dataStore.edit { it[KEY_APP_THEME] = theme }
        notifyRepo { it.copy(appTheme = theme) }
    }
    suspend fun setEditorFontSize(size: Float) {
        context.dataStore.edit { it[KEY_EDITOR_FONT_SIZE] = size }
        notifyRepo { it.copy(editorFontSize = size) }
    }
    suspend fun setEditorLineSpacing(spacing: Float) {
        context.dataStore.edit { it[KEY_EDITOR_LINE_SPACING] = spacing }
        notifyRepo { it.copy(editorLineSpacing = spacing) }
    }
    suspend fun setEditorFontFamily(family: String) {
        context.dataStore.edit { it[KEY_EDITOR_FONT_FAMILY] = family }
        notifyRepo { it.copy(editorFontFamily = family) }
    }

    suspend fun setPipelineSpacing(enabled: Boolean) {
        context.dataStore.edit { it[KEY_PIPELINE_SPACING] = enabled }
        notifyRepo { it.copy(pipelineSpacing = enabled) }
    }
    suspend fun setPipelineAutoCap(enabled: Boolean) {
        context.dataStore.edit { it[KEY_PIPELINE_AUTOCAP] = enabled }
        notifyRepo { it.copy(pipelineAutoCap = enabled) }
    }
    suspend fun setPipelineCommands(enabled: Boolean) {
        context.dataStore.edit { it[KEY_PIPELINE_COMMANDS] = enabled }
        notifyRepo { it.copy(pipelineCommands = enabled) }
    }

    fun isOnboardingCompletedSync(): Boolean {
        if (syncPrefs.contains("onboarding_completed")) {
            return syncPrefs.getBoolean("onboarding_completed", false)
        }
        val dbFile1 = context.getDatabasePath("kothapad.db")
        val dbFile2 = context.getDatabasePath("kothapad_database")
        if ((dbFile1.exists() && dbFile1.length() > 0) || (dbFile2.exists() && dbFile2.length() > 0)) {
            syncPrefs.edit().putBoolean("onboarding_completed", true).commit()
            return true
        }
        return false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        syncPrefs.edit().putBoolean("onboarding_completed", completed).commit()
        context.dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }

    fun getLastOpenedNoteIdSync(): Long {
        return syncPrefs.getLong("last_opened_note_id", -1L)
    }

    fun setLastOpenedNoteIdSync(noteId: Long) {
        syncPrefs.edit().putLong("last_opened_note_id", noteId).apply()
    }

    suspend fun setLastOpenedNoteId(noteId: Long) {
        setLastOpenedNoteIdSync(noteId)
        context.dataStore.edit { it[KEY_LAST_OPENED_NOTE_ID] = noteId }
        notifyRepo { it.copy(lastOpenedNoteId = noteId) }
    }
    suspend fun setKeyboardType(type: String) {
        context.dataStore.edit { it[KEY_KEYBOARD_TYPE] = type }
        notifyRepo { it.copy(keyboardType = type) }
    }
    suspend fun setShowWordCount(show: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_WORD_COUNT] = show }
        notifyRepo { it.copy(showWordCount = show) }
    }

    suspend fun setKeyboardLayoutJson(json: String?) {
        context.dataStore.edit {
            if (json != null) it[KEY_KEYBOARD_LAYOUT_JSON] = json else it.remove(KEY_KEYBOARD_LAYOUT_JSON)
        }
        notifyRepo { it.copy(keyboardLayoutJson = json) }
    }
    suspend fun setKeyboardKeyHeightDp(dp: Int) {
        context.dataStore.edit { it[KEY_KEYBOARD_KEY_HEIGHT_DP] = dp }
        notifyRepo { it.copy(keyboardKeyHeightDp = dp) }
    }
    suspend fun setKeyboardFontSizeSp(sp: Int) {
        context.dataStore.edit { it[KEY_KEYBOARD_FONT_SIZE_SP] = sp }
        notifyRepo { it.copy(keyboardFontSizeSp = sp) }
    }
    suspend fun setKeyboardHaptic(enabled: Boolean) {
        context.dataStore.edit { it[KEY_KEYBOARD_HAPTIC] = enabled }
        notifyRepo { it.copy(keyboardHaptic = enabled) }
    }
    suspend fun setKeyboardHoldBehavior(behavior: String) {
        context.dataStore.edit { it[KEY_KEYBOARD_HOLD_BEHAVIOR] = behavior }
        notifyRepo { it.copy(keyboardHoldBehavior = behavior) }
    }
    suspend fun setKeyboardEditMode(editMode: Boolean) {
        context.dataStore.edit { it[KEY_KEYBOARD_EDIT_MODE] = editMode }
        notifyRepo { it.copy(keyboardEditMode = editMode) }
    }
    suspend fun setTrashAutoEmptyDays(days: Int) {
        context.dataStore.edit { it[KEY_TRASH_AUTO_EMPTY_DAYS] = days }
        notifyRepo { it.copy(trashAutoEmptyDays = days) }
    }
    suspend fun setDefaultNewNoteFolder(folder: String) {
        context.dataStore.edit { it[KEY_DEFAULT_NEW_NOTE_FOLDER] = folder }
        notifyRepo { it.copy(defaultNewNoteFolder = folder) }
    }

    suspend fun setCustomThemeColors(primary: Int, bg: Int, surface: Int, text: Int, secondary: Int) {
        context.dataStore.edit {
            it[KEY_CUSTOM_PRIMARY_COLOR] = primary
            it[KEY_CUSTOM_BACKGROUND_COLOR] = bg
            it[KEY_CUSTOM_SURFACE_COLOR] = surface
            it[KEY_CUSTOM_TEXT_COLOR] = text
            it[KEY_CUSTOM_SECONDARY_COLOR] = secondary
        }
        notifyRepo {
            it.copy(
                customPrimaryColor = primary,
                customBackgroundColor = bg,
                customSurfaceColor = surface,
                customTextColor = text,
                customSecondaryColor = secondary
            )
        }
    }

    suspend fun setCustomFont(name: String, path: String?) {
        context.dataStore.edit {
            it[KEY_CUSTOM_FONT_NAME] = name
            if (path != null) it[KEY_CUSTOM_FONT_PATH] = path else it.remove(KEY_CUSTOM_FONT_PATH)
        }
        notifyRepo {
            it.copy(
                customFontName = name,
                customFontPath = path
            )
        }
    }

    suspend fun setEditorLetterSpacing(spacing: Float) {
        context.dataStore.edit { it[KEY_EDITOR_LETTER_SPACING] = spacing }
        notifyRepo { it.copy(editorLetterSpacing = spacing) }
    }
    suspend fun setEditorParagraphSpacing(spacing: Int) {
        context.dataStore.edit { it[KEY_EDITOR_PARAGRAPH_SPACING] = spacing }
        notifyRepo { it.copy(editorParagraphSpacing = spacing) }
    }
    suspend fun setEditorTextAlignment(align: String) {
        context.dataStore.edit { it[KEY_EDITOR_TEXT_ALIGNMENT] = align }
        notifyRepo { it.copy(editorTextAlignment = align) }
    }
    suspend fun setEditorMarginDp(margin: Int) {
        context.dataStore.edit { it[KEY_EDITOR_MARGIN_DP] = margin }
        notifyRepo { it.copy(editorMarginDp = margin) }
    }
    suspend fun setEditorMaxWidthDp(width: Int) {
        context.dataStore.edit { it[KEY_EDITOR_MAX_WIDTH_DP] = width }
        notifyRepo { it.copy(editorMaxWidthDp = width) }
    }

    suspend fun setToolbarItemOrder(order: String) {
        context.dataStore.edit { it[KEY_TOOLBAR_ITEM_ORDER] = order }
        notifyRepo { it.copy(toolbarItemOrder = order) }
    }
    suspend fun setToolbarHiddenItems(hidden: Set<String>) {
        context.dataStore.edit { it[KEY_TOOLBAR_HIDDEN_ITEMS] = hidden }
        notifyRepo { it.copy(toolbarHiddenItems = hidden) }
    }

    suspend fun setMicButtonSize(size: String) {
        context.dataStore.edit { it[KEY_MIC_BUTTON_SIZE] = size }
        notifyRepo { it.copy(micButtonSize = size) }
    }
    suspend fun setMicButtonShape(shape: String) {
        context.dataStore.edit { it[KEY_MIC_BUTTON_SHAPE] = shape }
        notifyRepo { it.copy(micButtonShape = shape) }
    }
    suspend fun setMicButtonColor(color: String) {
        context.dataStore.edit { it[KEY_MIC_BUTTON_COLOR] = color }
        notifyRepo { it.copy(micButtonColor = color) }
    }
    suspend fun setMicButtonPosition(pos: String) {
        context.dataStore.edit { it[KEY_MIC_BUTTON_POSITION] = pos }
        notifyRepo { it.copy(micButtonPosition = pos) }
    }
    suspend fun setMicButtonOffsetY(offset: Int) {
        context.dataStore.edit { it[KEY_MIC_BUTTON_OFFSET_Y] = offset }
        notifyRepo { it.copy(micButtonOffsetY = offset) }
    }

    suspend fun setStatusChipStyle(style: String) {
        context.dataStore.edit { it[KEY_STATUS_CHIP_STYLE] = style }
        notifyRepo { it.copy(statusChipStyle = style) }
    }
    suspend fun setUiDensity(density: String) {
        context.dataStore.edit { it[KEY_UI_DENSITY] = density }
        notifyRepo { it.copy(uiDensity = density) }
    }
    suspend fun setCornerRadiusDp(radius: Int) {
        context.dataStore.edit { it[KEY_CORNER_RADIUS_DP] = radius }
        notifyRepo { it.copy(cornerRadiusDp = radius) }
    }
    suspend fun setDictationAnimationStyle(style: String) {
        context.dataStore.edit { it[KEY_DICTATION_ANIMATION_STYLE] = style }
        notifyRepo { it.copy(dictationAnimationStyle = style) }
    }
    suspend fun setHapticsLevel(level: String) {
        context.dataStore.edit { it[KEY_HAPTICS_LEVEL] = level }
        notifyRepo { it.copy(hapticsLevel = level) }
    }

    fun getExpandedFoldersSync(): Set<String> {
        return syncPrefs.getStringSet("expanded_folders", null) ?: setOf("General", "Templates")
    }

    fun setExpandedFoldersSync(folders: Set<String>) {
        syncPrefs.edit().putStringSet("expanded_folders", folders).apply()
    }

    suspend fun setShowNoteMetricsInList(show: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_NOTE_METRICS_IN_LIST] = show }
        notifyRepo { it.copy(showNoteMetricsInList = show) }
    }
    suspend fun setNoteMetricsStyle(style: String) {
        context.dataStore.edit { it[KEY_NOTE_METRICS_STYLE] = style }
        notifyRepo { it.copy(noteMetricsStyle = style) }
    }
    suspend fun setNoteMetricsOrder(order: String) {
        context.dataStore.edit { it[KEY_NOTE_METRICS_ORDER] = order }
        notifyRepo { it.copy(noteMetricsOrder = order) }
    }
    suspend fun setNoteMetricsHiddenItems(hidden: Set<String>) {
        context.dataStore.edit { it[KEY_NOTE_METRICS_HIDDEN_ITEMS] = hidden }
        notifyRepo { it.copy(noteMetricsHiddenItems = hidden) }
    }
    suspend fun setKeyboardLayoutVersion(version: Int) {
        context.dataStore.edit { it[KEY_KEYBOARD_LAYOUT_VERSION] = version }
        notifyRepo { it.copy(keyboardLayoutVersion = version) }
    }
    suspend fun setExpandedFolders(folders: Set<String>) {
        setExpandedFoldersSync(folders)
        context.dataStore.edit { it[KEY_EXPANDED_FOLDERS] = folders }
        notifyRepo { it.copy(expandedFolders = folders) }
    }
    suspend fun setCustomFolders(folders: Set<String>) {
        context.dataStore.edit { it[KEY_CUSTOM_FOLDERS] = folders }
        notifyRepo { it.copy(customFolders = folders) }
    }

    // Notes Manager Settings Flows
    val newNoteLocationMode: Flow<String> = context.dataStore.data.map { it[KEY_NEW_NOTE_LOCATION_MODE] ?: "LAST_WORKED_ON" }
    val fixedNewNoteFolder: Flow<String> = context.dataStore.data.map { it[KEY_FIXED_NEW_NOTE_FOLDER] ?: "General" }
    val askBeforeCreating: Flow<Boolean> = context.dataStore.data.map { it[KEY_ASK_BEFORE_CREATING] ?: false }
    val askChooseTemplateToggle: Flow<Boolean> = context.dataStore.data.map { it[KEY_ASK_CHOOSE_TEMPLATE_TOGGLE] ?: true }
    val askStartRecordingToggle: Flow<Boolean> = context.dataStore.data.map { it[KEY_ASK_START_RECORDING_TOGGLE] ?: false }
    val newNoteNamingRule: Flow<String> = context.dataStore.data.map { it[KEY_NEW_NOTE_NAMING_RULE] ?: "UNTITLED" }
    val afterCreatingNote: Flow<String> = context.dataStore.data.map { it[KEY_AFTER_CREATING_NOTE] ?: "OPEN_EDITOR" }
    val lastActiveFolder: Flow<String> = context.dataStore.data.map { it[KEY_LAST_ACTIVE_FOLDER] ?: "General" }

    suspend fun setNewNoteLocationMode(mode: String) {
        context.dataStore.edit { it[KEY_NEW_NOTE_LOCATION_MODE] = mode }
        notifyRepo { it.copy(newNoteLocationMode = mode) }
    }
    suspend fun setFixedNewNoteFolder(folder: String) {
        context.dataStore.edit { it[KEY_FIXED_NEW_NOTE_FOLDER] = folder }
        notifyRepo { it.copy(fixedNewNoteFolder = folder) }
    }
    suspend fun setAskBeforeCreating(ask: Boolean) {
        context.dataStore.edit { it[KEY_ASK_BEFORE_CREATING] = ask }
        notifyRepo { it.copy(askBeforeCreating = ask) }
    }
    suspend fun setAskChooseTemplateToggle(toggle: Boolean) {
        context.dataStore.edit { it[KEY_ASK_CHOOSE_TEMPLATE_TOGGLE] = toggle }
        notifyRepo { it.copy(askChooseTemplateToggle = toggle) }
    }
    suspend fun setAskStartRecordingToggle(toggle: Boolean) {
        context.dataStore.edit { it[KEY_ASK_START_RECORDING_TOGGLE] = toggle }
        notifyRepo { it.copy(askStartRecordingToggle = toggle) }
    }
    suspend fun setNewNoteNamingRule(rule: String) {
        context.dataStore.edit { it[KEY_NEW_NOTE_NAMING_RULE] = rule }
        notifyRepo { it.copy(newNoteNamingRule = rule) }
    }
    suspend fun setAfterCreatingNote(action: String) {
        context.dataStore.edit { it[KEY_AFTER_CREATING_NOTE] = action }
        notifyRepo { it.copy(afterCreatingNote = action) }
    }

    fun getLastActiveFolderSync(): String {
        return syncPrefs.getString("last_active_folder", "General") ?: "General"
    }
    fun setLastActiveFolderSync(folder: String) {
        if (folder.isNotBlank()) {
            syncPrefs.edit().putString("last_active_folder", folder).apply()
        }
    }
    suspend fun setLastActiveFolder(folder: String) {
        if (folder.isNotBlank()) {
            setLastActiveFolderSync(folder)
            context.dataStore.edit { it[KEY_LAST_ACTIVE_FOLDER] = folder }
            notifyRepo { it.copy(lastActiveFolder = folder) }
        }
    }

    // Floating Controls Layout ("Side Rail") Flow & Setters
    val floatingLayoutSettings: Flow<com.example.data.model.FloatingLayoutSettings> = context.dataStore.data.map { prefs ->
        com.example.data.model.FloatingLayoutSettings(
            useSideRail = prefs[KEY_FLOATING_USE_SIDE_RAIL] ?: true,
            railSide = prefs[KEY_RAIL_SIDE] ?: "RIGHT",
            railScale = prefs[KEY_RAIL_SCALE] ?: 1.0f,
            railBottomOffsetDp = prefs[KEY_RAIL_BOTTOM_OFFSET_DP] ?: 8,
            railSpacingDp = prefs[KEY_RAIL_SPACING_DP] ?: 6,
            keepTextClearOfRail = prefs[KEY_RAIL_KEEP_TEXT_CLEAR] ?: true,
            railButtonOrder = com.example.data.model.FloatingLayoutSettings.parseOrder(prefs[KEY_RAIL_BUTTON_ORDER]),
            toggleScale = prefs[KEY_TOGGLE_SCALE] ?: 1.0f,
            toggleBottomOffsetDp = prefs[KEY_TOGGLE_BOTTOM_OFFSET_DP] ?: 8,
            toggleGapDp = prefs[KEY_TOGGLE_GAP_DP] ?: 8,
            formatBarScale = prefs[KEY_FORMAT_BAR_SCALE] ?: 1.0f,
            formatBarBottomOffsetDp = prefs[KEY_FORMAT_BAR_BOTTOM_OFFSET_DP] ?: 8,
            formatBarWidthPercent = prefs[KEY_FORMAT_BAR_WIDTH_PERCENT] ?: 100,
            formatBarAlignment = prefs[KEY_FORMAT_BAR_ALIGNMENT] ?: "LEFT",
            waveformScale = prefs[KEY_WAVEFORM_SCALE] ?: 1.0f,
            showWaveform = prefs[KEY_WAVEFORM_SHOW] ?: true
        )
    }

    suspend fun setFloatingUseSideRail(enabled: Boolean) {
        context.dataStore.edit { it[KEY_FLOATING_USE_SIDE_RAIL] = enabled }
        notifyRepo { it.copy(floatingUseSideRail = enabled) }
    }
    suspend fun setRailSide(side: String) {
        context.dataStore.edit { it[KEY_RAIL_SIDE] = side }
        notifyRepo { it.copy(railSide = side) }
    }
    suspend fun setRailScale(scale: Float) {
        context.dataStore.edit { it[KEY_RAIL_SCALE] = scale }
        notifyRepo { it.copy(railScale = scale) }
    }
    suspend fun setRailBottomOffsetDp(offsetDp: Int) {
        context.dataStore.edit { it[KEY_RAIL_BOTTOM_OFFSET_DP] = offsetDp }
        notifyRepo { it.copy(railBottomOffsetDp = offsetDp) }
    }
    suspend fun setRailSpacingDp(spacingDp: Int) {
        context.dataStore.edit { it[KEY_RAIL_SPACING_DP] = spacingDp }
        notifyRepo { it.copy(railSpacingDp = spacingDp) }
    }
    suspend fun setRailKeepTextClear(keepClear: Boolean) {
        context.dataStore.edit { it[KEY_RAIL_KEEP_TEXT_CLEAR] = keepClear }
        notifyRepo { it.copy(railKeepTextClear = keepClear) }
    }
    suspend fun setRailButtonOrder(order: List<String>) {
        context.dataStore.edit { it[KEY_RAIL_BUTTON_ORDER] = com.example.data.model.FloatingLayoutSettings.orderToString(order) }
        notifyRepo { it.copy(railButtonOrder = order) }
    }
    suspend fun setToggleScale(scale: Float) {
        context.dataStore.edit { it[KEY_TOGGLE_SCALE] = scale }
        notifyRepo { it.copy(toggleScale = scale) }
    }
    suspend fun setToggleBottomOffsetDp(offsetDp: Int) {
        context.dataStore.edit { it[KEY_TOGGLE_BOTTOM_OFFSET_DP] = offsetDp }
        notifyRepo { it.copy(toggleBottomOffsetDp = offsetDp) }
    }
    suspend fun setToggleGapDp(gapDp: Int) {
        context.dataStore.edit { it[KEY_TOGGLE_GAP_DP] = gapDp }
        notifyRepo { it.copy(toggleGapDp = gapDp) }
    }
    suspend fun setFormatBarScale(scale: Float) {
        context.dataStore.edit { it[KEY_FORMAT_BAR_SCALE] = scale }
        notifyRepo { it.copy(formatBarScale = scale) }
    }
    suspend fun setFormatBarBottomOffsetDp(offsetDp: Int) {
        context.dataStore.edit { it[KEY_FORMAT_BAR_BOTTOM_OFFSET_DP] = offsetDp }
        notifyRepo { it.copy(formatBarBottomOffsetDp = offsetDp) }
    }
    suspend fun setFormatBarWidthPercent(percent: Int) {
        context.dataStore.edit { it[KEY_FORMAT_BAR_WIDTH_PERCENT] = percent }
        notifyRepo { it.copy(formatBarWidthPercent = percent) }
    }
    suspend fun setFormatBarAlignment(alignment: String) {
        context.dataStore.edit { it[KEY_FORMAT_BAR_ALIGNMENT] = alignment }
        notifyRepo { it.copy(formatBarAlignment = alignment) }
    }
    suspend fun setWaveformScale(scale: Float) {
        context.dataStore.edit { it[KEY_WAVEFORM_SCALE] = scale }
        notifyRepo { it.copy(waveformScale = scale) }
    }
    suspend fun setWaveformShow(show: Boolean) {
        context.dataStore.edit { it[KEY_WAVEFORM_SHOW] = show }
        notifyRepo { it.copy(waveformShow = show) }
    }

    suspend fun resetFloatingLayoutToDefaults() {
        val defaults = com.example.data.model.FloatingLayoutSettings()
        context.dataStore.edit { prefs ->
            prefs[KEY_FLOATING_USE_SIDE_RAIL] = defaults.useSideRail
            prefs[KEY_RAIL_SIDE] = defaults.railSide
            prefs[KEY_RAIL_SCALE] = defaults.railScale
            prefs[KEY_RAIL_BOTTOM_OFFSET_DP] = defaults.railBottomOffsetDp
            prefs[KEY_RAIL_SPACING_DP] = defaults.railSpacingDp
            prefs[KEY_RAIL_KEEP_TEXT_CLEAR] = defaults.keepTextClearOfRail
            prefs[KEY_RAIL_BUTTON_ORDER] = com.example.data.model.FloatingLayoutSettings.orderToString(defaults.railButtonOrder)
            prefs[KEY_TOGGLE_SCALE] = defaults.toggleScale
            prefs[KEY_TOGGLE_BOTTOM_OFFSET_DP] = defaults.toggleBottomOffsetDp
            prefs[KEY_TOGGLE_GAP_DP] = defaults.toggleGapDp
            prefs[KEY_FORMAT_BAR_SCALE] = defaults.formatBarScale
            prefs[KEY_FORMAT_BAR_BOTTOM_OFFSET_DP] = defaults.formatBarBottomOffsetDp
            prefs[KEY_FORMAT_BAR_WIDTH_PERCENT] = defaults.formatBarWidthPercent
            prefs[KEY_FORMAT_BAR_ALIGNMENT] = defaults.formatBarAlignment
            prefs[KEY_WAVEFORM_SCALE] = defaults.waveformScale
            prefs[KEY_WAVEFORM_SHOW] = defaults.showWaveform
        }
        notifyRepo {
            it.copy(
                floatingUseSideRail = defaults.useSideRail,
                railSide = defaults.railSide,
                railScale = defaults.railScale,
                railBottomOffsetDp = defaults.railBottomOffsetDp,
                railSpacingDp = defaults.railSpacingDp,
                railKeepTextClear = defaults.keepTextClearOfRail,
                railButtonOrder = defaults.railButtonOrder,
                toggleScale = defaults.toggleScale,
                toggleBottomOffsetDp = defaults.toggleBottomOffsetDp,
                toggleGapDp = defaults.toggleGapDp,
                formatBarScale = defaults.formatBarScale,
                formatBarBottomOffsetDp = defaults.formatBarBottomOffsetDp,
                formatBarWidthPercent = defaults.formatBarWidthPercent,
                formatBarAlignment = defaults.formatBarAlignment,
                waveformScale = defaults.waveformScale,
                waveformShow = defaults.showWaveform
            )
        }
    }

    suspend fun getAllPreferencesForBackup(): JSONObject {
        val prefs = context.dataStore.data.first()
        val prefsArray = JSONArray()
        for ((key, value) in prefs.asMap()) {
            val obj = JSONObject()
            obj.put("key", key.name)
            when (value) {
                is Boolean -> {
                    obj.put("type", "boolean")
                    obj.put("value", value)
                }
                is Int -> {
                    obj.put("type", "int")
                    obj.put("value", value)
                }
                is Long -> {
                    obj.put("type", "long")
                    obj.put("value", value)
                }
                is Float -> {
                    obj.put("type", "float")
                    obj.put("value", value.toDouble())
                }
                is Double -> {
                    obj.put("type", "double")
                    obj.put("value", value)
                }
                is String -> {
                    obj.put("type", "string")
                    obj.put("value", value)
                }
                is Set<*> -> {
                    obj.put("type", "stringSet")
                    val arr = JSONArray()
                    for (s in value) {
                        if (s is String) arr.put(s)
                    }
                    obj.put("value", arr)
                }
                else -> {
                    obj.put("type", "string")
                    obj.put("value", value?.toString() ?: "")
                }
            }
            prefsArray.put(obj)
        }

        val root = JSONObject()
        root.put("preferences", prefsArray)

        val syncArray = JSONArray()
        for ((key, value) in syncPrefs.all) {
            val obj = JSONObject()
            obj.put("key", key)
            when (value) {
                is Boolean -> {
                    obj.put("type", "boolean")
                    obj.put("value", value)
                }
                is Int -> {
                    obj.put("type", "int")
                    obj.put("value", value)
                }
                is Long -> {
                    obj.put("type", "long")
                    obj.put("value", value)
                }
                is Float -> {
                    obj.put("type", "float")
                    obj.put("value", value.toDouble())
                }
                is Double -> {
                    obj.put("type", "double")
                    obj.put("value", value)
                }
                is String -> {
                    obj.put("type", "string")
                    obj.put("value", value)
                }
                is Set<*> -> {
                    obj.put("type", "stringSet")
                    val arr = JSONArray()
                    for (s in value) {
                        if (s is String) arr.put(s)
                    }
                    obj.put("value", arr)
                }
                else -> {
                    obj.put("type", "string")
                    obj.put("value", value?.toString() ?: "")
                }
            }
            syncArray.put(obj)
        }
        root.put("syncPrefs", syncArray)
        return root
    }

    suspend fun restoreAllPreferencesFromBackup(root: JSONObject): Int {
        var skippedCount = 0

        // Parse DataStore entries (supports array, object, or root)
        val dataStoreEntries = mutableListOf<SettingRawEntry>()
        val prefsArray = root.optJSONArray("preferences")
        if (prefsArray != null) {
            for (i in 0 until prefsArray.length()) {
                val item = prefsArray.optJSONObject(i) ?: continue
                val key = item.optString("key")
                val type = if (item.has("type") && !item.isNull("type")) item.optString("type") else null
                val value = item.opt("value")
                if (key.isNotBlank()) {
                    dataStoreEntries.add(SettingRawEntry(key, type, value))
                }
            }
        } else {
            val prefsObj = root.optJSONObject("preferences")
            if (prefsObj != null) {
                val keys = prefsObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val raw = prefsObj.opt(key)
                    if (raw is JSONObject && (raw.has("value") || raw.has("type"))) {
                        val type = if (raw.has("type") && !raw.isNull("type")) raw.optString("type") else null
                        val value = raw.opt("value")
                        dataStoreEntries.add(SettingRawEntry(key, type, value))
                    } else {
                        dataStoreEntries.add(SettingRawEntry(key, null, raw))
                    }
                }
            }
        }

        context.dataStore.edit { preferences ->
            preferences.clear()
            for (entry in dataStoreEntries) {
                try {
                    restoreSingleDataStorePreference(preferences, entry)
                } catch (_: Exception) {
                    skippedCount++
                }
            }
        }

        // Parse syncPrefs entries (supports array or legacy object)
        val syncEntries = mutableListOf<SettingRawEntry>()
        val syncRaw = root.opt("syncPrefs")
        if (syncRaw is JSONArray) {
            for (i in 0 until syncRaw.length()) {
                val item = syncRaw.optJSONObject(i) ?: continue
                val key = item.optString("key")
                val type = if (item.has("type") && !item.isNull("type")) item.optString("type") else null
                val value = item.opt("value")
                if (key.isNotBlank()) {
                    syncEntries.add(SettingRawEntry(key, type, value))
                }
            }
        } else if (syncRaw is JSONObject) {
            val keys = syncRaw.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val raw = syncRaw.opt(key)
                if (raw is JSONObject && (raw.has("value") || raw.has("type"))) {
                    val type = if (raw.has("type") && !raw.isNull("type")) raw.optString("type") else null
                    val value = raw.opt("value")
                    syncEntries.add(SettingRawEntry(key, type, value))
                } else {
                    syncEntries.add(SettingRawEntry(key, null, raw))
                }
            }
        }

        val editor = syncPrefs.edit().clear()
        for (entry in syncEntries) {
            try {
                restoreSingleSyncPreference(editor, entry)
            } catch (_: Exception) {
                skippedCount++
            }
        }
        editor.apply()

        if (skippedCount > 0) {
            try {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Restored with $skippedCount skipped setting(s)",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (_: Throwable) {
                // Ignore if in test or non-Looper environment
            }
        }

        return skippedCount
    }

    private fun restoreSingleDataStorePreference(
        preferences: MutablePreferences,
        entry: SettingRawEntry
    ) {
        val keyName = entry.key
        val rawValue = entry.value
        if (rawValue == null || rawValue == JSONObject.NULL) {
            return
        }

        val targetType: PrefType = resolvePrefType(keyName, entry.type, rawValue)

        when (targetType) {
            PrefType.BOOLEAN -> {
                preferences[booleanPreferencesKey(keyName)] = safeToBoolean(rawValue)
            }
            PrefType.INT -> {
                preferences[intPreferencesKey(keyName)] = safeToInt(rawValue)
            }
            PrefType.LONG -> {
                preferences[longPreferencesKey(keyName)] = safeToLong(rawValue)
            }
            PrefType.FLOAT -> {
                preferences[floatPreferencesKey(keyName)] = safeToFloat(rawValue)
            }
            PrefType.DOUBLE -> {
                preferences[doublePreferencesKey(keyName)] = safeToDouble(rawValue)
            }
            PrefType.STRING -> {
                preferences[stringPreferencesKey(keyName)] = safeToString(rawValue)
            }
            PrefType.STRING_SET -> {
                preferences[stringSetPreferencesKey(keyName)] = safeToStringSet(rawValue)
            }
        }
    }

    private fun resolvePrefType(keyName: String, explicitType: String?, rawValue: Any): PrefType {
        if (!explicitType.isNullOrBlank()) {
            when (explicitType.lowercase()) {
                "boolean" -> return PrefType.BOOLEAN
                "int", "integer" -> return PrefType.INT
                "long" -> return PrefType.LONG
                "float" -> return PrefType.FLOAT
                "double" -> return PrefType.DOUBLE
                "string" -> return PrefType.STRING
                "stringset", "string_set" -> return PrefType.STRING_SET
            }
        }

        // Fallback: look up in KNOWN_PREF_TYPES
        KNOWN_PREF_TYPES[keyName]?.let { return it }

        // Fallback: infer from rawValue
        return when (rawValue) {
            is Boolean -> PrefType.BOOLEAN
            is String -> PrefType.STRING
            is JSONArray -> PrefType.STRING_SET
            is Set<*> -> PrefType.STRING_SET
            is Float, is Double -> PrefType.FLOAT
            is Number -> {
                val num = (rawValue as Number).toLong()
                if (num > Int.MAX_VALUE || num < Int.MIN_VALUE ||
                    keyName.contains("id", ignoreCase = true) ||
                    keyName.contains("time", ignoreCase = true)
                ) {
                    PrefType.LONG
                } else {
                    PrefType.INT
                }
            }
            else -> PrefType.STRING
        }
    }

    private fun restoreSingleSyncPreference(
        editor: android.content.SharedPreferences.Editor,
        entry: SettingRawEntry
    ) {
        val keyName = entry.key
        val rawValue = entry.value
        if (rawValue == null || rawValue == JSONObject.NULL) {
            return
        }

        val targetType: PrefType = if (!entry.type.isNullOrBlank()) {
            when (entry.type.lowercase()) {
                "boolean" -> PrefType.BOOLEAN
                "int", "integer" -> PrefType.INT
                "long" -> PrefType.LONG
                "float" -> PrefType.FLOAT
                "double" -> PrefType.DOUBLE
                "string" -> PrefType.STRING
                "stringset", "string_set" -> PrefType.STRING_SET
                else -> KNOWN_SYNC_PREF_TYPES[keyName] ?: KNOWN_PREF_TYPES[keyName] ?: PrefType.STRING
            }
        } else {
            KNOWN_SYNC_PREF_TYPES[keyName] ?: KNOWN_PREF_TYPES[keyName] ?: when (rawValue) {
                is Boolean -> PrefType.BOOLEAN
                is String -> PrefType.STRING
                is JSONArray -> PrefType.STRING_SET
                is Set<*> -> PrefType.STRING_SET
                is Float, is Double -> PrefType.FLOAT
                is Number -> {
                    val num = (rawValue as Number).toLong()
                    if (num > Int.MAX_VALUE || num < Int.MIN_VALUE ||
                        keyName.contains("id", ignoreCase = true) ||
                        keyName.contains("time", ignoreCase = true)
                    ) {
                        PrefType.LONG
                    } else {
                        PrefType.INT
                    }
                }
                else -> PrefType.STRING
            }
        }

        when (targetType) {
            PrefType.BOOLEAN -> editor.putBoolean(keyName, safeToBoolean(rawValue))
            PrefType.INT -> editor.putInt(keyName, safeToInt(rawValue))
            PrefType.LONG -> editor.putLong(keyName, safeToLong(rawValue))
            PrefType.FLOAT, PrefType.DOUBLE -> editor.putFloat(keyName, safeToFloat(rawValue))
            PrefType.STRING -> editor.putString(keyName, safeToString(rawValue))
            PrefType.STRING_SET -> editor.putStringSet(keyName, safeToStringSet(rawValue))
        }
    }
}
