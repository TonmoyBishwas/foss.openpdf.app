package app.openpdf.foss.feature.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.openpdf.foss.R

/**
 * Bottom toolbar shown in annotate mode: note / ink / eraser tools, ink color
 * dots, and save. Text markups (highlight/underline/strikethrough) are applied
 * from the selection snackbar instead, since they need selected text.
 */
@Composable
fun AnnotationToolbar(
    state: AnnotationUiState,
    onToolSelected: (AnnotationTool) -> Unit,
    onColorSelected: (Long) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolButton(
                icon = Icons.AutoMirrored.Filled.Comment,
                description = stringResource(R.string.tool_note),
                selected = state.tool == AnnotationTool.NOTE,
                onClick = { onToolSelected(AnnotationTool.NOTE) },
            )
            ToolButton(
                icon = Icons.Default.Draw,
                description = stringResource(R.string.tool_ink),
                selected = state.tool == AnnotationTool.INK,
                onClick = { onToolSelected(AnnotationTool.INK) },
            )
            ToolButton(
                icon = Icons.Default.Close,
                description = stringResource(R.string.tool_erase),
                selected = state.tool == AnnotationTool.ERASE,
                onClick = { onToolSelected(AnnotationTool.ERASE) },
            )

            if (state.tool == AnnotationTool.INK) {
                InkColors.forEach { argb ->
                    ColorDot(
                        argb = argb,
                        selected = state.inkColor == argb,
                        onClick = { onColorSelected(argb) },
                    )
                }
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))

            if (state.dirty) {
                Text(
                    text = stringResource(R.string.annotate_unsaved),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            if (state.saving) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(2.dp))
            } else {
                IconButton(onClick = onSave, enabled = state.dirty) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = stringResource(R.string.action_save),
                        tint = if (state.dirty) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    )
                }
            }
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.action_close_annotate),
                )
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: ImageVector,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = if (selected) Modifier.background(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            CircleShape,
        ) else Modifier,
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ColorDot(argb: Long, selected: Boolean, onClick: () -> Unit) {
    val color = Color(argb.toInt())
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .padding(4.dp)
            .size(if (selected) 28.dp else 22.dp)
            .background(color, CircleShape)
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
    )
}
