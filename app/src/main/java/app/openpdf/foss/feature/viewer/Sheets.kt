package app.openpdf.foss.feature.viewer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.openpdf.foss.R
import app.openpdf.foss.core.data.db.BookmarkEntity
import app.openpdf.foss.core.pdf.model.OutlineNode
import app.openpdf.foss.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutlineSheet(
    outline: List<OutlineNode>,
    onNavigate: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        SheetTitle(stringResource(R.string.outline_title))
        if (outline.isEmpty()) {
            EmptySheetHint(stringResource(R.string.outline_empty))
        } else {
            val flattened = remember(outline) { flatten(outline) }
            LazyColumn {
                items(flattened) { (node, depth) ->
                    Text(
                        text = node.title.ifBlank { stringResource(R.string.outline_untitled) },
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate(node.pageIndex) }
                            .padding(
                                start = Spacing.lg + (depth * 16).dp,
                                end = Spacing.lg,
                                top = Spacing.md,
                                bottom = Spacing.md,
                            ),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksSheet(
    bookmarks: List<BookmarkEntity>,
    onNavigate: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        SheetTitle(stringResource(R.string.bookmarks_title))
        if (bookmarks.isEmpty()) {
            EmptySheetHint(stringResource(R.string.bookmarks_empty))
        } else {
            LazyColumn {
                items(bookmarks) { bookmark ->
                    Text(
                        text = bookmark.label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate(bookmark.pageIndex) }
                            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    )
}

@Composable
private fun EmptySheetHint(text: String) {
    Column(modifier = Modifier.padding(Spacing.lg)) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = Spacing.xxl),
        )
    }
}

private fun flatten(
    nodes: List<OutlineNode>,
    depth: Int = 0,
): List<Pair<OutlineNode, Int>> = nodes.flatMap { node ->
    listOf(node to depth) + flatten(node.children, depth + 1)
}
