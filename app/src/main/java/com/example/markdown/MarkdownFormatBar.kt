package com.example.markdown

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmoledBorder
import com.example.ui.theme.HighlightGold
import com.example.ui.theme.SaffronPrimary

/**
 * Compact horizontally scrollable format bar for inserting Markdown syntax.
 * Shown only while the editor has focus and keyboard is open, hidden during recording and Reading Mode.
 */
@Composable
fun MarkdownFormatBar(
    enabledButtonIds: List<String>,
    onAction: (buttonId: String) -> Unit,
    modifier: Modifier = Modifier,
    scale: Float = 1.0f
) {
    val view = LocalView.current
    val allButtons = remember { getFormatBarButtonDefinitions() }
    val visibleButtons = remember(enabledButtonIds) {
        allButtons.filter { enabledButtonIds.contains(it.id) }
    }

    if (visibleButtons.isEmpty()) return

    val btnSize = (38 * scale).dp
    val barCorner = (14 * scale).dp
    val padH = (6 * scale).dp
    val padV = (2 * scale).dp
    val spacing = (4 * scale).dp

    Surface(
        shape = RoundedCornerShape(barCorner),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, AmoledBorder.copy(alpha = 0.45f)),
        shadowElevation = 5.dp,
        modifier = modifier.testTag("markdown_format_bar")
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = padH, vertical = padV),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            visibleButtons.forEach { btn ->
                Box(
                    modifier = Modifier
                        .size(btnSize)
                        .clip(RoundedCornerShape((8 * scale).dp))
                        .clickable {
                            try {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            } catch (_: Exception) {}
                            onAction(btn.id)
                        }
                        .testTag("format_btn_${btn.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.scale(scale.coerceIn(0.6f, 1.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        btn.renderIcon()
                    }
                }
            }
        }
    }
}

data class FormatBarButtonDef(
    val id: String,
    val label: String,
    val contentDescription: String,
    val renderIcon: @Composable () -> Unit
)

fun getFormatBarButtonDefinitions(): List<FormatBarButtonDef> {
    return listOf(
        FormatBarButtonDef(
            id = "heading",
            label = "Heading",
            contentDescription = "Heading (cycles H1 to H3)",
            renderIcon = {
                Text(
                    text = "H",
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = SaffronPrimary
                )
            }
        ),
        FormatBarButtonDef(
            id = "bold",
            label = "Bold",
            contentDescription = "Bold",
            renderIcon = {
                Icon(
                    imageVector = Icons.Default.FormatBold,
                    contentDescription = "Bold",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        ),
        FormatBarButtonDef(
            id = "italic",
            label = "Italic",
            contentDescription = "Italic",
            renderIcon = {
                Icon(
                    imageVector = Icons.Default.FormatItalic,
                    contentDescription = "Italic",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        ),
        FormatBarButtonDef(
            id = "strikethrough",
            label = "Strikethrough",
            contentDescription = "Strikethrough",
            renderIcon = {
                Icon(
                    imageVector = Icons.Default.FormatStrikethrough,
                    contentDescription = "Strikethrough",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        ),
        FormatBarButtonDef(
            id = "highlight",
            label = "Highlight",
            contentDescription = "Highlight",
            renderIcon = {
                Icon(
                    imageVector = Icons.Default.Highlight,
                    contentDescription = "Highlight",
                    tint = HighlightGold,
                    modifier = Modifier.size(20.dp)
                )
            }
        ),
        FormatBarButtonDef(
            id = "bullet_list",
            label = "Bullet list",
            contentDescription = "Bullet list",
            renderIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                    contentDescription = "Bullet list",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        ),
        FormatBarButtonDef(
            id = "numbered_list",
            label = "Numbered list",
            contentDescription = "Numbered list",
            renderIcon = {
                Icon(
                    imageVector = Icons.Default.FormatListNumbered,
                    contentDescription = "Numbered list",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        ),
        FormatBarButtonDef(
            id = "checkbox",
            label = "Checkbox",
            contentDescription = "Task checkbox",
            renderIcon = {
                Icon(
                    imageVector = Icons.Filled.CheckBox,
                    contentDescription = "Task checkbox",
                    tint = SaffronPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        ),
        FormatBarButtonDef(
            id = "quote",
            label = "Quote",
            contentDescription = "Blockquote",
            renderIcon = {
                Text(
                    text = "“ ”",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        ),
        FormatBarButtonDef(
            id = "inline_code",
            label = "Inline code",
            contentDescription = "Inline code",
            renderIcon = {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = "Inline code",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        ),
        FormatBarButtonDef(
            id = "code_block",
            label = "Code block",
            contentDescription = "Fenced code block",
            renderIcon = {
                Text(
                    text = "{ }",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ),
        FormatBarButtonDef(
            id = "link",
            label = "Link",
            contentDescription = "Markdown link",
            renderIcon = {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = "Markdown link",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        ),
        FormatBarButtonDef(
            id = "horizontal_rule",
            label = "Divider",
            contentDescription = "Horizontal rule",
            renderIcon = {
                Icon(
                    imageVector = Icons.Default.HorizontalRule,
                    contentDescription = "Horizontal rule",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        )
    )
}
