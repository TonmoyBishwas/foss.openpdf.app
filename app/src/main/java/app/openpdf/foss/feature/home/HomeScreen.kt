package app.openpdf.foss.feature.home

import android.net.Uri
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.openpdf.foss.R
import app.openpdf.foss.core.data.db.RecentFileEntity
import app.openpdf.foss.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenDocument: (Uri) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val recents by viewModel.recents.collectAsStateWithLifecycle()
    val toolState by viewModel.toolState.collectAsStateWithLifecycle()

    val openDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let(onOpenDocument)
    }

    val mergeSourcesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris -> if (uris.isNotEmpty()) viewModel.onMergeSourcesPicked(uris) }

    val mergeTargetLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { target -> if (target != null) viewModel.mergeInto(target) else viewModel.dismissTool() }

    val splitSourceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::onSplitSourcePicked) }

    var splitRanges by remember { mutableStateOf("") }
    val splitTargetLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { target ->
        if (target != null) viewModel.splitInto(splitRanges, target)
        else viewModel.dismissTool()
    }

    // Merge: once sources are picked, immediately ask where to save.
    LaunchedEffect(toolState.pendingMergeSources) {
        if (toolState.pendingMergeSources.isNotEmpty()) {
            mergeTargetLauncher.launch("merged.pdf")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { openDocumentLauncher.launch(arrayOf("application/pdf")) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.home_open_pdf)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                ),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ToolsRow(
                enabled = !toolState.busy,
                onMerge = { mergeSourcesLauncher.launch(arrayOf("application/pdf")) },
                onSplit = { splitSourceLauncher.launch(arrayOf("application/pdf")) },
            )
            if (recents.isEmpty()) {
                EmptyState(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Spacing.screenEdge,
                        end = Spacing.screenEdge,
                        bottom = Spacing.screenEdge,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    items(recents, key = { it.uri }) { recent ->
                        RecentFileCard(
                            recent = recent,
                            onClick = { onOpenDocument(Uri.parse(recent.uri)) },
                            onRemove = { viewModel.removeRecent(recent.uri) },
                        )
                    }
                }
            }
        }
    }

    // Split: ranges dialog once the source's page count is known.
    if (toolState.pendingSplitSource != null) {
        SplitRangesDialog(
            pageCount = toolState.pendingSplitPageCount,
            onConfirm = { ranges ->
                splitRanges = ranges
                splitTargetLauncher.launch("split.pdf")
            },
            onDismiss = viewModel::dismissTool,
        )
    }

    toolState.message?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissTool,
            title = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissTool) {
                    Text(stringResource(R.string.action_ok))
                }
            },
        )
    }
}

@Composable
private fun ToolsRow(
    enabled: Boolean,
    onMerge: () -> Unit,
    onSplit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenEdge, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        OutlinedButton(onClick = onMerge, enabled = enabled) {
            Icon(Icons.Default.CallMerge, contentDescription = null)
            Text(
                stringResource(R.string.tool_merge),
                modifier = Modifier.padding(start = Spacing.xs),
            )
        }
        OutlinedButton(onClick = onSplit, enabled = enabled) {
            Icon(Icons.Default.CallSplit, contentDescription = null)
            Text(
                stringResource(R.string.tool_split),
                modifier = Modifier.padding(start = Spacing.xs),
            )
        }
    }
}

@Composable
private fun SplitRangesDialog(
    pageCount: Int,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var ranges by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.split_dialog_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.split_dialog_hint, pageCount),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = Spacing.sm),
                )
                OutlinedTextField(
                    value = ranges,
                    onValueChange = { ranges = it },
                    label = { Text(stringResource(R.string.split_dialog_label)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(ranges) }, enabled = ranges.isNotBlank()) {
                Text(stringResource(R.string.action_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentFileCard(
    recent: RecentFileEntity,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Spacing.lg),
            ) {
                Text(
                    text = recent.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        R.string.home_recent_subtitle,
                        pluralStringResource(R.plurals.page_count, recent.pageCount, recent.pageCount),
                        DateUtils.getRelativeTimeSpanString(recent.lastOpenedAt).toString(),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.home_remove_recent),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                )
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(Spacing.screenEdge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        )
        Text(
            text = stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = Spacing.lg),
        )
        Text(
            text = stringResource(R.string.home_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = Spacing.sm),
        )
    }
}
