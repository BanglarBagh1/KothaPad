package com.example.ui.keyboard

import com.example.data.model.KeyActionType
import com.example.data.model.KeyboardKey
import com.example.data.model.KeyboardLayout
import com.example.data.model.KeyboardRow
import org.json.JSONArray
import org.json.JSONObject

object KeyboardLayouts {

    fun getDefault3x10Layout(): KeyboardLayout {
        // User-configured Minimal 3x10 Keyboard layout
        // Row 1: ো, া, ে, ি, ।, ,, ?, ., ⌫, ⌫
        val row1 = listOf(
            KeyboardKey(id = "k_0_0", label = "ো", actionType = KeyActionType.INSERT, outputText = "ো", smartSpacing = false, dateFormat = "dd MMM yyyy", timeFormat = "hh:mm a"),
            KeyboardKey(id = "k_0_1", label = "া", actionType = KeyActionType.INSERT, outputText = "া", smartSpacing = false, dateFormat = "dd MMM yyyy", timeFormat = "hh:mm a"),
            KeyboardKey(id = "k_0_2", label = "ে", actionType = KeyActionType.INSERT, outputText = "ে", smartSpacing = false, dateFormat = "dd MMM yyyy", timeFormat = "hh:mm a"),
            KeyboardKey(id = "k_0_3", label = "ি", actionType = KeyActionType.INSERT, outputText = "ি", smartSpacing = false, dateFormat = "dd MMM yyyy", timeFormat = "hh:mm a"),
            KeyboardKey(id = "k_0_4", label = "।", actionType = KeyActionType.INSERT, outputText = "।", smartSpacing = true, dateFormat = "dd MMM yyyy", timeFormat = "hh:mm a"),
            KeyboardKey(id = "k_0_5", label = ",", actionType = KeyActionType.INSERT, outputText = ",", smartSpacing = true, dateFormat = "dd MMM yyyy", timeFormat = "hh:mm a"),
            KeyboardKey(id = "k_0_6", label = "?", actionType = KeyActionType.INSERT, outputText = "?", smartSpacing = true, dateFormat = "dd MMM yyyy", timeFormat = "hh:mm a"),
            KeyboardKey(id = "k_0_7", label = ".", actionType = KeyActionType.INSERT, outputText = ".", smartSpacing = true, dateFormat = "dd MMM yyyy", timeFormat = "hh:mm a"),
            KeyboardKey(id = "k_0_8", label = "⌫", actionType = KeyActionType.BACKSPACE, outputText = null, smartSpacing = false, dateFormat = "dd MMM yyyy", timeFormat = "hh:mm a"),
            KeyboardKey(id = "k_0_9", label = "⌫", actionType = KeyActionType.BACKSPACE, outputText = null, smartSpacing = false, dateFormat = null, timeFormat = null)
        )

        // Row 2: ◀, ▲, ▼, ▶, ↵, 📅, 🕒, 📋, ¶, 🌐
        val row2 = listOf(
            KeyboardKey(id = "k_1_0", label = "◀", actionType = KeyActionType.CURSOR_LEFT, outputText = null, smartSpacing = false, dateFormat = null, timeFormat = null),
            KeyboardKey(id = "k_1_1", label = "▲", actionType = KeyActionType.CURSOR_UP, outputText = null, smartSpacing = false, dateFormat = null, timeFormat = null),
            KeyboardKey(id = "k_1_2", label = "▼", actionType = KeyActionType.CURSOR_DOWN, outputText = null, smartSpacing = false, dateFormat = null, timeFormat = null),
            KeyboardKey(id = "k_1_3", label = "▶", actionType = KeyActionType.CURSOR_RIGHT, outputText = null, smartSpacing = false, dateFormat = null, timeFormat = null),
            KeyboardKey(id = "k_1_4", label = "↵", actionType = KeyActionType.ENTER, outputText = null, smartSpacing = false, dateFormat = "dd MMM yyyy", timeFormat = "hh:mm a"),
            KeyboardKey(id = "k_1_5", label = "📅", actionType = KeyActionType.INSERT, outputText = "{date}", smartSpacing = false, dateFormat = null, timeFormat = null),
            KeyboardKey(id = "k_1_6", label = "🕒", actionType = KeyActionType.INSERT, outputText = "{time}", smartSpacing = false, dateFormat = null, timeFormat = null),
            KeyboardKey(id = "k_1_7", label = "📋", actionType = KeyActionType.OPEN_TEMPLATES, outputText = null, smartSpacing = false, dateFormat = null, timeFormat = null),
            KeyboardKey(id = "k_1_8", label = "¶", actionType = KeyActionType.NEW_PARAGRAPH, outputText = null, smartSpacing = false, dateFormat = null, timeFormat = null),
            KeyboardKey(id = "k_1_9", label = "🌐", actionType = KeyActionType.CHANGE_LANGUAGE, outputText = null, smartSpacing = false, dateFormat = "dd MMM yyyy", timeFormat = "hh:mm a")
        )

        // Row 3: -, (, ),  ,  , ই, ও, ঁ, হ, ␣
        val row3 = listOf(
            KeyboardKey(id = "k_2_0", label = "-", actionType = KeyActionType.INSERT, outputText = "-", smartSpacing = false, dateFormat = null, timeFormat = null),
            KeyboardKey(id = "k_2_1", label = "(", actionType = KeyActionType.INSERT, outputText = "(", smartSpacing = false, dateFormat = null, timeFormat = null),
            KeyboardKey(id = "k_2_2", label = ")", actionType = KeyActionType.INSERT, outputText = ")", smartSpacing = false, dateFormat = null, timeFormat = null),
            KeyboardKey(id = "k_2_3", label = " ", actionType = KeyActionType.INSERT, outputText = " ", smartSpacing = false, dateFormat = "dd MMM yyyy", timeFormat = "hh:mm a"),
            KeyboardKey(id = "k_2_4", label = " ", actionType = KeyActionType.INSERT, outputText = " ", smartSpacing = false, dateFormat = "dd MMM yyyy", timeFormat = "hh:mm a"),
            KeyboardKey(id = "k_2_5", label = "ই", actionType = KeyActionType.INSERT, outputText = "ই", smartSpacing = true, dateFormat = "dd MMM yyyy", timeFormat = "hh:mm a"),
            KeyboardKey(id = "k_2_6", label = "ও", actionType = KeyActionType.INSERT, outputText = "ও", smartSpacing = true, dateFormat = "dd MMM yyyy", timeFormat = "hh:mm a"),
            KeyboardKey(id = "k_2_7", label = "ঁ", actionType = KeyActionType.INSERT, outputText = "ঁ", smartSpacing = false, dateFormat = null, timeFormat = null),
            KeyboardKey(id = "k_2_8", label = "হ", actionType = KeyActionType.INSERT, outputText = "হ", smartSpacing = false, dateFormat = "dd MMM yyyy", timeFormat = "hh:mm a"),
            KeyboardKey(id = "k_2_9", label = "␣", actionType = KeyActionType.SPACE, outputText = null, smartSpacing = false, dateFormat = null, timeFormat = null)
        )

        return KeyboardLayout(
            id = "custom_3x10",
            name = "Minimal 3x10 Keyboard",
            languageCode = "all",
            rows = listOf(KeyboardRow(row1), KeyboardRow(row2), KeyboardRow(row3))
        )
    }

    fun serializeToJson(layout: KeyboardLayout): String {
        val root = JSONObject()
        root.put("id", layout.id)
        root.put("name", layout.name)
        root.put("languageCode", layout.languageCode)

        val rowsArray = JSONArray()
        for (row in layout.rows) {
            val keysArray = JSONArray()
            for (key in row.keys) {
                val keyObj = JSONObject()
                keyObj.put("id", key.id)
                keyObj.put("label", key.label)
                keyObj.put("actionType", key.actionType.name)
                keyObj.put("outputText", key.outputText ?: JSONObject.NULL)
                keyObj.put("smartSpacing", key.smartSpacing)
                keyObj.put("dateFormat", key.dateFormat ?: JSONObject.NULL)
                keyObj.put("timeFormat", key.timeFormat ?: JSONObject.NULL)
                keysArray.put(keyObj)
            }
            rowsArray.put(keysArray)
        }
        root.put("rows", rowsArray)
        return root.toString()
    }

    fun deserializeFromJson(jsonStr: String): KeyboardLayout {
        val root = JSONObject(jsonStr)
        val id = root.optString("id", "custom_3x10")
        val name = root.optString("name", "Minimal 3x10 Keyboard")
        val lang = root.optString("languageCode", "all")

        val rowsArray = root.getJSONArray("rows")
        val rows = mutableListOf<KeyboardRow>()

        for (r in 0 until minOf(3, rowsArray.length())) {
            val keysArray = rowsArray.getJSONArray(r)
            val keys = mutableListOf<KeyboardKey>()
            for (k in 0 until minOf(10, keysArray.length())) {
                val keyObj = keysArray.getJSONObject(k)
                val keyId = keyObj.optString("id", "k_${r}_$k")
                val label = keyObj.optString("label", "")
                val actionTypeName = keyObj.optString("actionType", KeyActionType.INSERT.name)
                val actionType = try {
                    KeyActionType.valueOf(actionTypeName)
                } catch (_: Exception) {
                    KeyActionType.INSERT
                }
                val outputText = if (keyObj.isNull("outputText")) null else keyObj.optString("outputText")
                val smartSpacing = keyObj.optBoolean("smartSpacing", false)
                val dateFormat = if (keyObj.isNull("dateFormat")) null else keyObj.optString("dateFormat")
                val timeFormat = if (keyObj.isNull("timeFormat")) null else keyObj.optString("timeFormat")

                keys.add(
                    KeyboardKey(
                        id = keyId,
                        label = label,
                        actionType = actionType,
                        outputText = outputText,
                        smartSpacing = smartSpacing,
                        dateFormat = dateFormat,
                        timeFormat = timeFormat
                    )
                )
            }
            // Pad to 10 if needed
            while (keys.size < 10) {
                keys.add(KeyboardKey(id = "k_${r}_${keys.size}", label = " ", actionType = KeyActionType.INSERT, outputText = " "))
            }
            rows.add(KeyboardRow(keys))
        }

        // Ensure 3 rows
        while (rows.size < 3) {
            val r = rows.size
            val keys = (0 until 10).map { k ->
                KeyboardKey(id = "k_${r}_$k", label = " ", actionType = KeyActionType.INSERT, outputText = " ")
            }
            rows.add(KeyboardRow(keys))
        }

        return KeyboardLayout(id, name, lang, rows)
    }
}
