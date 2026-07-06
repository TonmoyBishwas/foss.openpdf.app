package app.openpdf.foss.feature.organize

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.openpdf.foss.R
import app.openpdf.foss.core.pdf.PdfDocumentSession
import app.openpdf.foss.feature.viewer.PageBitmapCache
import app.openpdf.foss.ui.theme.Spacing
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizeScreen(
    onBack: () -> Unit,
    viewModel: OrganizeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val saveAsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { target -> target?.let { viewModel.save(it) } }

    val extractLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { target -> target?.let(viewModel::extractSelectedTo) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = (uiState as? OrganizeUiState.Ready)?.displayName
                            ?: stringResource(R.string.organize_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    (uiState as? OrganizeUiState.Ready)?.let { state ->
                        if (state.saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = Spacing.lg),
                            )
                        } else {
                            IconButton(
                                onClick = {
                                    if (state.canWriteInPlace) viewModel.save()
                                    else saveAsLauncher.launch(state.displayName)
                                },
                                enabled = state.dirty,
                            ) {
                                Icon(
                                    Icons.Default.Save,
                                    contentDescription = stringResource(R.string.action_save),
                                    tint = if (state.dirty) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            (uiState as? OrganizeUiState.Ready)?.let { state ->
                if (state.selection.isNotEmpty() || state.moveMode) {
                    OrganizeActionBar(
                        state = state,
                        onRotate = viewModel::rotateSelected,
                        onDuplicate = viewModel::duplicateSelected,
                        onDelete = viewModel::deleteSelected,
                        onMove = viewModel::startMove,
                        onCancelMove = viewModel::cancelMove,
                        onExtract = { extractLauncher.launch("extracted.pdf") },
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is OrganizeUiState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                is OrganizeUiState.Error -> Text(
                    text = state.message,
                    modifier = Modifier.align(Alignment.Center).padding(Spacing.xl),
                )

                is OrganizeUiState.Ready -> {
                    val session = viewModel.session ?: return@Box
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(Spacing.lg),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        items(count = state.pages.size) { position ->
                            PageThumbnail(
                                session = session,
                                page = state.pages[position],
                                position = position,
                                aspectRatio = state.pageSizes
                                    .getOrNull(state.pages[position].sourcePage)
                                    ?.aspectRatio ?: 0.7071f,
                                selected = position in state.selection,
                                moveMode = state.moveMode,
                                onClick = {
                                    if (state.moveMode) viewModel.moveTo(position)
                                    else viewModel.toggleSelection(position)
                                },
                            )
                        }
                    }

                    state.message?.let { message ->
                        LaunchedEffect(message) {
                            delay(2500)
                            viewModel.dismissMessage()
                        }
                        Snackbar(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(Spacing.lg),
                        ) { Text(message) }
                    }
                    if (state.moveMode) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopCenter).padding(Spacing.sm),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Text(
                                text = stringResource(R.string.organize_move_hint),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(
                                    horizontal = Spacing.md, vertical = Spacing.xs,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PageThumbnail(
    session: PdfDocumentSession,
    page: ArrangedPage,
    position: Int,
    aspectRatio: Float,
    selected: Boolean,
    moveMode: Boolean,
    onClick: () -> Unit,
) {
    val bitmap by produceState<Bitmap?>(initialValue = null, session, page.sourcePage) {
        value = runCatching {
            PageBitmapCache.getOrRender(session, page.sourcePage, 300)
        }.getOrNull()
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (aspectRatio > 0f) aspectRatio else 0.7071f)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = when {
                        selected -> MaterialTheme.colorScheme.primary
                        moveMode -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.outlineVariant
                    },
                    shape = MaterialTheme.shapes.extraSmall,
                )
                .clickable(onClick = onClick),
        ) {
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().rotate(page.rotation.toFloat()),
                    contentScale = ContentScale.Fit,
                )
            }
        }
        Text(
            text = "${position + 1}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = Spacing.xs),
        )
    }
}

@Composable
private fun OrganizeActionBar(
    state: OrganizeUiState.Ready,
    onRotate: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit,
    onCancelMove: () -> Unit,
    onExtract: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.moveMode) {
                Text(
                    text = stringResource(R.string.organize_move_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f).padding(Spacing.md),
                )
                IconButton(onClick = onCancelMove) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.action_cancel),
                    )
                }
            } else {
                IconButton(onClick = onRotate) {
                    Icon(
                        Icons.Default.Rotate90DegreesCw,
                        contentDescription = stringResource(R.string.organize_rotate),
                    )
                }
                IconButton(onClick = onDuplicate) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.organize_duplicate),
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.organize_delete),
                    )
                }
                IconButton(onClick = onMove, enabled = state.selection.size == 1) {
                    Icon(
                        Icons.Default.DriveFileMove,
                        contentDescription = stringResource(R.string.organize_move),
                    )
                }
                IconButton(onClick = onExtract) {
                    Icon(
                        Icons.Default.FileUpload,
                        contentDescription = stringResource(R.string.organize_extract),
                    )
                }
            }
        }
    }
}
