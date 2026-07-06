package app.openpdf.foss.feature.viewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import app.openpdf.foss.R
import app.openpdf.foss.core.pdf.model.InkStroke

/** Draw-your-signature dialog. Strokes are captured in normalized pad coords. */
@Composable
fun SignaturePadDialog(
    onConfirm: (List<InkStroke>) -> Unit,
    onDismiss: () -> Unit,
) {
    var strokes by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var current by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var padSize by remember { mutableStateOf(Offset(1f, 1f)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.signature_dialog_title)) },
        text = {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2.2f)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .pointerInput(Unit) {
                        padSize = Offset(size.width.toFloat(), size.height.toFloat())
                        detectDragGestures(
                            onDragStart = { current = listOf(it) },
                            onDrag = { change, _ -> current = current + change.position },
                            onDragEnd = {
                                if (current.size > 1) strokes = strokes + listOf(current)
                                current = emptyList()
                            },
                            onDragCancel = { current = emptyList() },
                        )
                    },
            ) {
                val ink = Color(0xFF1A237E)
                val style = Stroke(width = 5f, cap = StrokeCap.Round)
                (strokes + listOf(current)).forEach { stroke ->
                    if (stroke.size > 1) {
                        val path = Path().apply {
                            moveTo(stroke.first().x, stroke.first().y)
                            stroke.drop(1).forEach { lineTo(it.x, it.y) }
                        }
                        drawPath(path, ink, style = style)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = strokes.isNotEmpty(),
                onClick = {
                    val normalized = strokes.map { stroke ->
                        InkStroke(
                            stroke.map {
                                (it.x / padSize.x).coerceIn(0f, 1f) to
                                    (it.y / padSize.y).coerceIn(0f, 1f)
                            }
                        )
                    }
                    onConfirm(normalized)
                },
            ) { Text(stringResource(R.string.signature_use)) }
        },
        dismissButton = {
            TextButton(onClick = { strokes = emptyList() }) {
                Text(stringResource(R.string.signature_clear))
            }
        },
    )
}
