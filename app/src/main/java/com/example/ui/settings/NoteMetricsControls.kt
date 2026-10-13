package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.notes.NoteMetricsConfig
import com.example.ui.theme.SaffronPrimary

@Composable
fun NoteMetricsReorderAndVisibilityControl(
    metricsOrderStr: String,
    hiddenItems: Set<String>,
    metricsStyle: String,
    onMoveItem: (fromIndex: Int, toIndex: Int) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onResetOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOrder = remember(metricsOrderStr) {
        NoteMetricsConfig.parseOrder(metricsOrderStr)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Metrics Components & Serial Order",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "Use the Up/Down arrows to change the serial order. Tap the eye icon to show or hide a component in the note list.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Serial Items list
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            currentOrder.forEachIndexed { index, itemKey ->
                val meta = NoteMetricsConfig.ALL_ITEMS.find { it.key == itemKey }
                    ?: NoteMetricsConfig.ItemMeta(itemKey, itemKey, "", itemKey, itemKey)
                val isVisible = !hiddenItems.contains(itemKey)

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isVisible) 0.55f else 0.22f),
                    border = if (isVisible) {
                        androidx.compose.foundation.BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.25f))
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("metric_item_$itemKey")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Position badge (Serial number)
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isVisible) SaffronPrimary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isVisible) SaffronPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Title, sample chip & description in a consistent, balanced column
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 4.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = meta.label,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isVisible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )

                                // Small sample badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    val isDetailed = metricsStyle.equals("DETAILED", ignoreCase = true)
                                    Text(
                                        text = if (isDetailed) meta.detailedSample else meta.compactSample,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isVisible) SaffronPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        maxLines = 1
                                    )
                                }
                            }

                            if (meta.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = meta.description,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isVisible) 0.8f else 0.4f)
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Reorder Up
                        IconButton(
                            onClick = { if (index > 0) onMoveItem(index, index - 1) },
                            enabled = index > 0,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Move ${meta.label} Up",
                                tint = if (index > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        // Reorder Down
                        IconButton(
                            onClick = { if (index < currentOrder.size - 1) onMoveItem(index, index + 1) },
                            enabled = index < currentOrder.size - 1,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Move ${meta.label} Down",
                                tint = if (index < currentOrder.size - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        // Visibility Toggle
                        IconButton(
                            onClick = { onToggleVisibility(itemKey) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isVisible) "Hide ${meta.label}" else "Show ${meta.label}",
                                tint = if (isVisible) SaffronPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }

        // Reset Order button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onResetOrder,
                modifier = Modifier.testTag("reset_metrics_order_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = SaffronPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Reset metrics order & visibility",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = SaffronPrimary
                )
            }
        }

        // Live Preview of metrics under note item
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "LIVE NOTE LIST PREVIEW",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = SaffronPrimary
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Simulated note item container
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Meeting Minutes & Ideas",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        val previewText = NoteMetricsConfig.formatSampleMetrics(
                            style = metricsStyle,
                            orderStr = metricsOrderStr,
                            hiddenItems = hiddenItems
                        )

                        if (previewText.isNotBlank()) {
                            Text(
                                text = previewText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                    lineHeight = 14.sp
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = "(All metrics hidden for note items)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
