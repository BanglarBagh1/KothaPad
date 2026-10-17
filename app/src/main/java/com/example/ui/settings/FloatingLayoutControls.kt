package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FloatingLayoutSettings
import com.example.ui.theme.SaffronPrimary

data class RailButtonMeta(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector
)

val ALL_RAIL_BUTTONS_META = listOf(
    RailButtonMeta(
        id = FloatingLayoutSettings.BUTTON_COPY,
        name = "Copy",
        description = "One-tap clipboard copy",
        icon = Icons.Default.ContentCopy
    ),
    RailButtonMeta(
        id = FloatingLayoutSettings.BUTTON_REDO,
        name = "Redo",
        description = "Reapply undone edits",
        icon = Icons.AutoMirrored.Filled.Redo
    ),
    RailButtonMeta(
        id = FloatingLayoutSettings.BUTTON_UNDO,
        name = "Undo",
        description = "Revert recent changes",
        icon = Icons.AutoMirrored.Filled.Undo
    ),
    RailButtonMeta(
        id = FloatingLayoutSettings.BUTTON_MIC,
        name = "Mic (Record)",
        description = "Start/stop speech dictation",
        icon = Icons.Default.Mic
    )
)

@Composable
fun RailButtonOrderReorderControl(
    railButtonOrder: List<String>,
    onMoveItem: (fromIndex: Int, toIndex: Int) -> Unit,
    onResetOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOrder = remember(railButtonOrder) {
        val list = railButtonOrder.toMutableList()
        for (btnId in FloatingLayoutSettings.ALL_BUTTON_IDS) {
            if (!list.contains(btnId)) list.add(btnId)
        }
        list
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("rail_button_order_control")
    ) {
        Text(
            text = "Rail Button Order (Top to Bottom)",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "Arrange buttons from top to bottom as they appear in the side rail column.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            currentOrder.forEachIndexed { index, buttonId ->
                val meta = ALL_RAIL_BUTTONS_META.find { it.id == buttonId }
                    ?: RailButtonMeta(buttonId, buttonId, "", Icons.Default.Mic)

                val positionLabel = when (index) {
                    0 -> "Top"
                    currentOrder.size - 1 -> "Bottom"
                    else -> "#${index + 1}"
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Badge indicating position
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(SaffronPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = meta.icon,
                                contentDescription = null,
                                tint = SaffronPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = meta.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "($positionLabel)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = SaffronPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                            Text(
                                text = meta.description,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        // Up Button
                        IconButton(
                            onClick = { if (index > 0) onMoveItem(index, index - 1) },
                            enabled = index > 0,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("move_up_${buttonId}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Move Up",
                                tint = if (index > 0) SaffronPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Down Button
                        IconButton(
                            onClick = { if (index < currentOrder.size - 1) onMoveItem(index, index + 1) },
                            enabled = index < currentOrder.size - 1,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("move_down_${buttonId}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Move Down",
                                tint = if (index < currentOrder.size - 1) SaffronPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onResetOrder,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reset_rail_order_button"),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reset order (default: Mic, Undo, Redo, Copy)", fontSize = 12.sp)
        }
    }
}
