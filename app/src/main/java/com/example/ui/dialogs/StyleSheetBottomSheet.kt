package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HighlightCoral
import com.example.ui.theme.HighlightCyan
import com.example.ui.theme.HighlightGold
import com.example.ui.theme.HighlightMint
import com.example.ui.theme.HighlightSaffron
import com.example.ui.theme.SaffronPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyleSheetBottomSheet(
    fontSize: Float,
    lineSpacing: Float,
    fontFamilyOption: String,
    currentHighlightHex: String?,
    onFontSizeChange: (Float) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onFontFamilyChange: (String) -> Unit,
    onHighlightChange: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Text Styling & Formatting",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = SaffronPrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Font Size Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Font Size", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(text = "${fontSize.toInt()} sp", style = MaterialTheme.typography.bodySmall, color = SaffronPrimary)
            }
            Slider(
                value = fontSize,
                onValueChange = onFontSizeChange,
                valueRange = 12f..32f,
                steps = 9,
                colors = SliderDefaults.colors(
                    thumbColor = SaffronPrimary,
                    activeTrackColor = SaffronPrimary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Line Spacing Section
            Text(text = "Line Spacing", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val spacingOptions = listOf(1.2f, 1.5f, 1.8f, 2.0f)
                for (spOpt in spacingOptions) {
                    FilterChip(
                        selected = (lineSpacing == spOpt),
                        onClick = { onLineSpacingChange(spOpt) },
                        label = { Text("${spOpt}x", fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Font Family Section
            Text(text = "Font Family", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val fonts = listOf(
                    "BENGALI" to "বাংলা (Noto)",
                    "INTER" to "Inter",
                    "MONOSPACE" to "Mono",
                    "SYSTEM" to "System"
                )
                for ((key, label) in fonts) {
                    FilterChip(
                        selected = (fontFamilyOption.equals(key, ignoreCase = true)),
                        onClick = { onFontFamilyChange(key) },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Note Highlight Background Tint
            Text(text = "Note Highlight Tint", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tints = listOf(
                    null to Color.Transparent,
                    "SAFFRON" to HighlightSaffron,
                    "GOLD" to HighlightGold,
                    "CORAL" to HighlightCoral,
                    "MINT" to HighlightMint,
                    "CYAN" to HighlightCyan
                )

                for ((tintKey, color) in tints) {
                    val isSelected = currentHighlightHex == tintKey
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (color == Color.Transparent) MaterialTheme.colorScheme.surfaceVariant else color)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) SaffronPrimary else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .clickable { onHighlightChange(tintKey) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (tintKey == null) {
                            Text("✕", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else if (isSelected) {
                            Text("✓", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
