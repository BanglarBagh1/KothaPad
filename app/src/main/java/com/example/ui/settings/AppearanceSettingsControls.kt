package com.example.ui.settings

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.editor.DEFAULT_TOOLBAR_ORDER
import com.example.ui.editor.TOOLBAR_ITEM_AUDIO_SOURCE
import com.example.ui.editor.TOOLBAR_ITEM_DICTATION_STATUS
import com.example.ui.editor.TOOLBAR_ITEM_KEYBOARD_MODE
import com.example.ui.editor.TOOLBAR_ITEM_METRICS
import com.example.ui.editor.TOOLBAR_ITEM_NOTE_TIMER
import com.example.ui.editor.TOOLBAR_ITEM_TEXT_SIZE
import com.example.ui.editor.TOOLBAR_ITEM_TEXT_STYLE
import com.example.ui.theme.SaffronPrimary
import java.io.File

// ==========================================
// 1. Full Custom Theme & Color Pickers
// ==========================================

enum class ColorTarget(val label: String) {
    PRIMARY("Primary Accent"),
    BACKGROUND("Background"),
    SURFACE("Card Surface"),
    TEXT("Main Text"),
    SECONDARY("Secondary Accent")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FullCustomThemeColorPicker(
    primaryColor: Int,
    backgroundColor: Int,
    surfaceColor: Int,
    textColor: Int,
    secondaryColor: Int,
    onColorsChanged: (primary: Int, bg: Int, surface: Int, text: Int, secondary: Int) -> Unit
) {
    var selectedTarget by remember { mutableStateOf(ColorTarget.PRIMARY) }

    val currentColorInt = when (selectedTarget) {
        ColorTarget.PRIMARY -> primaryColor
        ColorTarget.BACKGROUND -> backgroundColor
        ColorTarget.SURFACE -> surfaceColor
        ColorTarget.TEXT -> textColor
        ColorTarget.SECONDARY -> secondaryColor
    }

    val r = (currentColorInt shr 16) and 0xFF
    val g = (currentColorInt shr 8) and 0xFF
    val b = currentColorInt and 0xFF

    fun updateColor(newR: Int, newG: Int, newB: Int) {
        val newColorInt = (0xFF shl 24) or (newR shl 16) or (newG shl 8) or newB
        when (selectedTarget) {
            ColorTarget.PRIMARY -> onColorsChanged(newColorInt, backgroundColor, surfaceColor, textColor, secondaryColor)
            ColorTarget.BACKGROUND -> onColorsChanged(primaryColor, newColorInt, surfaceColor, textColor, secondaryColor)
            ColorTarget.SURFACE -> onColorsChanged(primaryColor, backgroundColor, newColorInt, textColor, secondaryColor)
            ColorTarget.TEXT -> onColorsChanged(primaryColor, backgroundColor, surfaceColor, newColorInt, secondaryColor)
            ColorTarget.SECONDARY -> onColorsChanged(primaryColor, backgroundColor, surfaceColor, textColor, newColorInt)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Text(
            text = "Custom Theme Color Picker",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = SaffronPrimary)
        )
        Text(
            text = "Select a target element below, then adjust with the palette or RGB sliders.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Target Selector Tabs
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ColorTarget.values().forEach { target ->
                val targetColor = when (target) {
                    ColorTarget.PRIMARY -> Color(primaryColor)
                    ColorTarget.BACKGROUND -> Color(backgroundColor)
                    ColorTarget.SURFACE -> Color(surfaceColor)
                    ColorTarget.TEXT -> Color(textColor)
                    ColorTarget.SECONDARY -> Color(secondaryColor)
                }

                FilterChip(
                    selected = (selectedTarget == target),
                    onClick = { selectedTarget = target },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(targetColor)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(target.label, fontSize = 11.sp)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Color Swatch Preview & Hex Value
        val hexString = String.format("#%06X", 0xFFFFFF and currentColorInt)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(currentColorInt))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isDarkColor = (r * 0.299 + g * 0.587 + b * 0.114) < 128
            val labelTint = if (isDarkColor) Color.White else Color.Black
            Text(
                text = "${selectedTarget.label} Preview",
                fontWeight = FontWeight.Bold,
                color = labelTint,
                fontSize = 12.sp
            )
            Text(
                text = hexString,
                fontWeight = FontWeight.SemiBold,
                color = labelTint,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Preset Swatches
        Text(text = "Quick Presets", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))
        val presets = listOf(
            0xFFFF7043.toInt(), // Saffron
            0xFFFF5252.toInt(), // Coral
            0xFFFFB300.toInt(), // Amber
            0xFF2E7D32.toInt(), // Emerald
            0xFF00E676.toInt(), // Mint
            0xFF00E5FF.toInt(), // Cyan
            0xFF1976D2.toInt(), // Blue
            0xFF7C4DFF.toInt(), // Purple
            0xFFFF4081.toInt(), // Pink
            0xFF000000.toInt(), // Pitch Black
            0xFF121212.toInt(), // Charcoal
            0xFF1E1E24.toInt(), // Slate
            0xFFFFFFFF.toInt()  // White
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                val isSelected = (currentColorInt == preset)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(preset))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) SaffronPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .clickable {
                            val pr = (preset shr 16) and 0xFF
                            val pg = (preset shr 8) and 0xFF
                            val pb = preset and 0xFF
                            updateColor(pr, pg, pb)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        val isDark = ((preset shr 16 and 0xFF) * 0.299 + (preset shr 8 and 0xFF) * 0.587 + (preset and 0xFF) * 0.114) < 128
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = if (isDark) Color.White else Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Red Slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("R: $r", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(44.dp))
            Slider(
                value = r.toFloat(),
                onValueChange = { updateColor(it.toInt(), g, b) },
                valueRange = 0f..255f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red)
            )
        }

        // Green Slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("G: $g", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(44.dp))
            Slider(
                value = g.toFloat(),
                onValueChange = { updateColor(r, it.toInt(), b) },
                valueRange = 0f..255f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = Color.Green, activeTrackColor = Color.Green)
            )
        }

        // Blue Slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("B: $b", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(44.dp))
            Slider(
                value = b.toFloat(),
                onValueChange = { updateColor(r, g, it.toInt()) },
                valueRange = 0f..255f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = Color.Blue, activeTrackColor = Color.Blue)
            )
        }
    }
}

// ==========================================
// 2. Toolbar Reorder & Visibility
// ==========================================

data class ToolbarItemMeta(
    val id: String,
    val name: String,
    val description: String
)

val ALL_TOOLBAR_ITEMS_META = listOf(
    ToolbarItemMeta(
        id = TOOLBAR_ITEM_DICTATION_STATUS,
        name = "Voice Dictation Status",
        description = "Shows starting or listening chip"
    ),
    ToolbarItemMeta(
        id = TOOLBAR_ITEM_METRICS,
        name = "Note Metrics",
        description = "Word count first, character count second"
    ),
    ToolbarItemMeta(
        id = TOOLBAR_ITEM_NOTE_TIMER,
        name = "Note Session Timer",
        description = "Total time spent on this specific note"
    ),
    ToolbarItemMeta(
        id = TOOLBAR_ITEM_KEYBOARD_MODE,
        name = "Keyboard Mode Switch",
        description = "Custom in-app or system keyboard"
    ),
    ToolbarItemMeta(
        id = TOOLBAR_ITEM_TEXT_SIZE,
        name = "Text Size Controls",
        description = "Zoom in and zoom out buttons"
    ),
    ToolbarItemMeta(
        id = TOOLBAR_ITEM_TEXT_STYLE,
        name = "Text Style & Highlight",
        description = "Style sheet & note background tint"
    ),
    ToolbarItemMeta(
        id = TOOLBAR_ITEM_AUDIO_SOURCE,
        name = "Audio Source & Engine",
        description = "Phone mic and cloud / offline engine"
    )
)

@Composable
fun ToolbarReorderAndVisibilityControl(
    toolbarOrderStr: String,
    hiddenItems: Set<String>,
    onMoveItem: (fromIndex: Int, toIndex: Int) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onResetOrder: () -> Unit
) {
    val currentOrder = remember(toolbarOrderStr) {
        val parsed = toolbarOrderStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val list = parsed.toMutableList()
        for (def in DEFAULT_TOOLBAR_ORDER) {
            if (!list.contains(def)) list.add(def)
        }
        list
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Toolbar Items Order & Visibility",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "Use the Up/Down arrows to change the order. Toggle the eye icon to show or hide.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            currentOrder.forEachIndexed { index, itemId ->
                val meta = ALL_TOOLBAR_ITEMS_META.find { it.id == itemId }
                    ?: ToolbarItemMeta(itemId, itemId, "")
                val isVisible = !hiddenItems.contains(itemId)

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isVisible) 0.6f else 0.25f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Position number badge
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(SaffronPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SaffronPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Title & Description
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = meta.name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isVisible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            )
                            if (meta.description.isNotBlank()) {
                                Text(
                                    text = meta.description,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isVisible) 0.8f else 0.4f)
                                    )
                                )
                            }
                        }

                        // Reorder Up
                        IconButton(
                            onClick = { if (index > 0) onMoveItem(index, index - 1) },
                            enabled = index > 0,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Move Up",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Reorder Down
                        IconButton(
                            onClick = { if (index < currentOrder.size - 1) onMoveItem(index, index + 1) },
                            enabled = index < currentOrder.size - 1,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Move Down",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Visibility Toggle
                        IconButton(
                            onClick = { onToggleVisibility(itemId) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isVisible) "Hide item" else "Show item",
                                tint = if (isVisible) SaffronPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Reset to Default Sequence Button
        OutlinedButton(
            onClick = onResetOrder,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reset to Default Sequence", fontSize = 12.sp)
        }
    }
}

// ==========================================
// 3. Custom Font File Import
// ==========================================

@Composable
fun CustomFontImportControl(
    customFontName: String,
    customFontPath: String?,
    onCustomFontSelected: (name: String, path: String?) -> Unit,
    onFontOptionChanged: (String) -> Unit
) {
    val context = LocalContext.current
    var importStatusMessage by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val fileName = queryFileName(context, uri) ?: "imported_font.ttf"
                val destFile = File(context.filesDir, "user_imported_font.ttf")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                onCustomFontSelected(fileName, destFile.absolutePath)
                onFontOptionChanged("CUSTOM")
                importStatusMessage = "Successfully imported: $fileName"
            } catch (e: Exception) {
                importStatusMessage = "Failed to load font: ${e.localizedMessage}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(12.dp)
    ) {
        Text(
            text = "User-Imported Font (.TTF / .OTF)",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
        )
        Text(
            text = if (customFontName.isNotBlank()) "Active Font: $customFontName" else "No custom font file loaded yet.",
            style = MaterialTheme.typography.bodySmall,
            color = if (customFontName.isNotBlank()) SaffronPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    filePickerLauncher.launch(arrayOf("*/*"))
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.UploadFile,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Import TTF / OTF", fontSize = 12.sp)
            }

            if (!customFontPath.isNullOrBlank()) {
                OutlinedButton(
                    onClick = {
                        onCustomFontSelected("", null)
                        onFontOptionChanged("BENGALI")
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Remove", fontSize = 12.sp)
                }
            }
        }

        if (importStatusMessage != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = importStatusMessage ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = SaffronPrimary
            )
        }
    }
}

private fun queryFileName(context: Context, uri: Uri): String? {
    var name: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx != -1) name = cursor.getString(idx)
            }
        }
    }
    return name ?: uri.lastPathSegment
}
