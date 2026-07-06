package app.openpdf.foss.feature.viewer

import android.content.Context
import android.content.Intent
import android.print.PrintManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.openpdf.foss.R
import app.openpdf.foss.core.pdf.model.MarkupType
import app.openpdf.foss.core.pdf.model.NormalizedRect
import app.openpdf.foss.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    onBack: () -> Unit,
    viewModel: ViewerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val selection by viewModel.selection.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.readAloud.isSpeaking.collectAsStateWithLifecycle()
    val annotationState by viewModel.annotationState.collectAsStateWithLifecycle()
    val docVersion by viewModel.docVersion.collectAsStateWithLifecycle()
    val pageAnnotations by viewModel.pageAnnotations.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    var showGoToPage by remember { mutableStateOf(false) }
    var showOutline by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var jumpToPage by remember { mutableStateOf<Int?>(null) }
    var notePlacement by remember { mutableStateOf<Triple<Int, Float, Float>?>(null) }
    var textPlacement by remember { mutableStateOf<Triple<Int, Float, Float>?>(null) }
    var showSignaturePad by remember { mutableStateOf(false) }
    var formFieldEditing by remember { mutableStateOf<app.openpdf.foss.core.pdf.model.FormField?>(null) }
    val formFields by viewModel.formFields.collectAsStateWithLifecycle()

    val saveAsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { target -> target?.let(viewModel::saveTo) }

    fun requestSave() {
        if (annotationState.canWriteInPlace) {
            viewModel.save()
        } else {
            val name = (uiState as? ViewerUiState.Ready)?.displayName ?: "document.pdf"
            saveAsLauncher.launch(name)
        }
    }

    // Jump to the page of the current search hit as the user steps through hits.
    LaunchedEffect(searchState.currentHit) {
        if (searchState.currentHit >= 0) {
            searchState.hits.getOrNull(searchState.currentHit)?.let { hit ->
                jumpToPage = hit.pageIndex
            }
        }
    }

    Scaffold(
        topBar = {
            if (searchState.active) {
                SearchTopBar(
                    state = searchState,
                    onQueryChange = viewModel::search,
                    onPrev = { viewModel.goToHit(searchState.currentHit - 1) },
                    onNext = { viewModel.goToHit(searchState.currentHit + 1) },
                    onClose = { viewModel.setSearchActive(false) },
                )
            } else {
                ViewerTopBar(
                    uiState = uiState,
                    bookmarked = (uiState as? ViewerUiState.Ready)?.let { ready ->
                        bookmarks.any { it.pageIndex == ready.currentPage }
                    } ?: false,
                    onBack = onBack,
                    onPageIndicatorClick = { showGoToPage = true },
                    onSearchClick = { viewModel.setSearchActive(true) },
                    onOutlineClick = { showOutline = true },
                    onBookmarkToggle = viewModel::toggleBookmark,
                    onMenuClick = { showMenu = true },
                    menuExpanded = showMenu,
                    onMenuDismiss = { showMenu = false },
                    onShowBookmarks = { showBookmarks = true; showMenu = false },
                    onReadAloud = {
                        showMenu = false
                        if (isSpeaking) viewModel.stopReadAloud() else viewModel.startReadAloud()
                    },
                    isSpeaking = isSpeaking,
                    onShare = {
                        showMenu = false
                        shareDocument(context, viewModel)
                    },
                    onPrint = {
                        showMenu = false
                        printDocument(context, viewModel)
                    },
                    onCycleReadingMode = { viewModel.cycleReadingMode(); showMenu = false },
                    onToggleViewMode = { viewModel.toggleViewMode(); showMenu = false },
                    onAnnotate = {
                        viewModel.setAnnotationToolbarVisible(!annotationState.toolbarVisible)
                    },
                    annotateActive = annotationState.toolbarVisible,
                )
            }
        },
        bottomBar = {
            if (annotationState.toolbarVisible && uiState is ViewerUiState.Ready) {
                AnnotationToolbar(
                    state = annotationState,
                    onToolSelected = { tool ->
                        if (tool == AnnotationTool.SIGN &&
                            annotationState.tool != AnnotationTool.SIGN
                        ) {
                            showSignaturePad = true
                        }
                        viewModel.setAnnotationTool(tool)
                    },
                    onColorSelected = viewModel::setInkColor,
                    onShapeSelected = viewModel::setShapeType,
                    onSave = { requestSave() },
                    onClose = { viewModel.setAnnotationToolbarVisible(false) },
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is ViewerUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is ViewerUiState.PasswordRequired -> {
                    PasswordDialog(
                        wrongPassword = state.wrongPassword,
                        onSubmit = viewModel::submitPassword,
                        onCancel = onBack,
                    )
                }

                is ViewerUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(Spacing.xl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        Text(
                            text = stringResource(R.string.viewer_error_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }

                is ViewerUiState.Ready -> {
                    val session = viewModel.session ?: return@Box

                    // Group search hits by page once per result set.
                    val hitsByPage = remember(searchState.hits) {
                        searchState.hits.groupBy { it.pageIndex }
                    }
                    val currentHit = searchState.hits.getOrNull(searchState.currentHit)

                    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        PdfViewport(
                            session = session,
                            pageSizes = state.pageSizes,
                            initialPage = state.initialPage,
                            onPageChanged = viewModel::onPageChanged,
                            readingMode = state.readingMode,
                            viewMode = state.viewMode,
                            docVersion = docVersion,
                            annotationTool = annotationState.tool,
                            overlaysForPage = { page ->
                                PageOverlays(
                                    searchRects = hitsByPage[page]
                                        ?.flatMap { it.rects }
                                        ?: emptyList<NormalizedRect>(),
                                    currentSearchRects = if (currentHit?.pageIndex == page)
                                        currentHit.rects else emptyList(),
                                    selectionRects = if (selection?.pageIndex == page)
                                        selection?.selection?.rects ?: emptyList()
                                    else emptyList(),
                                    inkColor = annotationState.inkColor,
                                    annotationOutlines = if (
                                        annotationState.tool == AnnotationTool.ERASE &&
                                        page == state.currentPage
                                    ) pageAnnotations.flatMap { it.rects } else emptyList(),
                                    formFieldRects = if (page == state.currentPage)
                                        formFields.map { it.rect } else emptyList(),
                                )
                            },
                            onSelectText = viewModel::selectText,
                            onClearSelection = viewModel::clearSelection,
                            onPageTap = { page, x, y ->
                                when (annotationState.tool) {
                                    AnnotationTool.NOTE -> notePlacement = Triple(page, x, y)
                                    AnnotationTool.TEXT -> textPlacement = Triple(page, x, y)
                                    AnnotationTool.SIGN -> viewModel.placeSignature(page, x, y)
                                    AnnotationTool.ERASE -> viewModel.eraseAnnotationAt(page, x, y)
                                    AnnotationTool.NONE -> {
                                        val field = viewModel.formFieldAt(page, x, y)
                                        if (field != null) {
                                            when (field.type) {
                                                app.openpdf.foss.core.pdf.model.FormFieldType.CHECKBOX,
                                                app.openpdf.foss.core.pdf.model.FormFieldType.RADIO ->
                                                    viewModel.toggleFormField(field)

                                                app.openpdf.foss.core.pdf.model.FormFieldType.TEXT,
                                                app.openpdf.foss.core.pdf.model.FormFieldType.COMBO,
                                                app.openpdf.foss.core.pdf.model.FormFieldType.LIST ->
                                                    formFieldEditing = field

                                                else -> Unit
                                            }
                                        } else {
                                            viewModel.clearSelection()
                                        }
                                    }

                                    else -> Unit
                                }
                            },
                            onInkStroke = { page, stroke ->
                                viewModel.addInk(page, listOf(stroke))
                            },
                            onShapeDrawn = viewModel::addShape,
                            jumpToPage = jumpToPage,
                            onJumpHandled = { jumpToPage = null },
                        )
                    }

                    notePlacement?.let { (page, x, y) ->
                        NoteDialog(
                            onConfirm = { text ->
                                viewModel.addNote(page, x, y, text)
                                notePlacement = null
                            },
                            onDismiss = { notePlacement = null },
                        )
                    }

                    textPlacement?.let { (page, x, y) ->
                        NoteDialog(
                            onConfirm = { text ->
                                viewModel.addFreeText(page, x, y, text)
                                textPlacement = null
                            },
                            onDismiss = { textPlacement = null },
                        )
                    }

                    if (showSignaturePad) {
                        SignaturePadDialog(
                            onConfirm = { strokes ->
                                viewModel.setSignature(strokes)
                                showSignaturePad = false
                            },
                            onDismiss = { showSignaturePad = false },
                        )
                    }

                    formFieldEditing?.let { field ->
                        FormFieldDialog(
                            field = field,
                            onConfirm = { value ->
                                viewModel.setFormValue(field, value)
                                formFieldEditing = null
                            },
                            onDismiss = { formFieldEditing = null },
                        )
                    }

                    annotationState.saveError?.let { error ->
                        LaunchedEffect(error) { }
                        Snackbar(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(Spacing.lg),
                            action = {
                                TextButton(onClick = viewModel::dismissSaveError) {
                                    Text(stringResource(R.string.action_cancel))
                                }
                            },
                        ) { Text(error) }
                    }

                    // Copy / markup affordances while a selection exists.
                    selection?.let { sel ->
                        Snackbar(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(Spacing.lg),
                            action = {
                                Row {
                                    IconButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(sel.selection.text))
                                            viewModel.clearSelection()
                                        },
                                    ) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = stringResource(R.string.action_copy),
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.markupSelection(MarkupType.HIGHLIGHT) },
                                    ) {
                                        Icon(
                                            Icons.Default.BorderColor,
                                            contentDescription = stringResource(R.string.tool_highlight),
                                            tint = Color(0xFFFFEB3B),
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.markupSelection(MarkupType.UNDERLINE) },
                                    ) {
                                        Icon(
                                            Icons.Default.FormatUnderlined,
                                            contentDescription = stringResource(R.string.tool_underline),
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.markupSelection(MarkupType.STRIKEOUT) },
                                    ) {
                                        Icon(
                                            Icons.Default.FormatStrikethrough,
                                            contentDescription = stringResource(R.string.tool_strikethrough),
                                        )
                                    }
                                }
                            },
                        ) {
                            Text(
                                text = sel.selection.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    if (showGoToPage) {
                        GoToPageDialog(
                            pageCount = state.pageCount,
                            onGo = { page ->
                                showGoToPage = false
                                jumpToPage = page
                            },
                            onDismiss = { showGoToPage = false },
                        )
                    }
                    if (showOutline) {
                        OutlineSheet(
                            outline = state.outline,
                            onNavigate = { page ->
                                showOutline = false
                                jumpToPage = page
                            },
                            onDismiss = { showOutline = false },
                        )
                    }
                    if (showBookmarks) {
                        BookmarksSheet(
                            bookmarks = bookmarks,
                            onNavigate = { page ->
                                showBookmarks = false
                                jumpToPage = page
                            },
                            onDismiss = { showBookmarks = false },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewerTopBar(
    uiState: ViewerUiState,
    bookmarked: Boolean,
    onBack: () -> Unit,
    onPageIndicatorClick: () -> Unit,
    onSearchClick: () -> Unit,
    onOutlineClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onMenuClick: () -> Unit,
    menuExpanded: Boolean,
    onMenuDismiss: () -> Unit,
    onShowBookmarks: () -> Unit,
    onReadAloud: () -> Unit,
    isSpeaking: Boolean,
    onShare: () -> Unit,
    onPrint: () -> Unit,
    onCycleReadingMode: () -> Unit,
    onToggleViewMode: () -> Unit,
    onAnnotate: () -> Unit,
    annotateActive: Boolean,
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = (uiState as? ViewerUiState.Ready)?.displayName
                        ?: stringResource(R.string.app_name),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge,
                )
                (uiState as? ViewerUiState.Ready)?.let { ready ->
                    Text(
                        text = stringResource(
                            R.string.viewer_page_indicator,
                            ready.currentPage + 1,
                            ready.pageCount,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.clickable(onClick = onPageIndicatorClick),
                    )
                }
            }
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
            if (uiState is ViewerUiState.Ready) {
                IconButton(onClick = onAnnotate) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.action_annotate),
                        tint = if (annotateActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = onSearchClick) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = stringResource(R.string.action_search),
                    )
                }
                IconButton(onClick = onOutlineClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = stringResource(R.string.outline_title),
                    )
                }
                IconButton(onClick = onBookmarkToggle) {
                    Icon(
                        if (bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = stringResource(R.string.action_bookmark),
                        tint = if (bookmarked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
                Box {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.action_more),
                        )
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = onMenuDismiss) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.bookmarks_title)) },
                            onClick = onShowBookmarks,
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        if (isSpeaking) R.string.action_stop_reading
                                        else R.string.action_read_aloud
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                            },
                            onClick = onReadAloud,
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_reading_mode)) },
                            onClick = onCycleReadingMode,
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_view_mode)) },
                            onClick = onToggleViewMode,
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_share)) },
                            onClick = onShare,
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_print)) },
                            onClick = onPrint,
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    state: SearchState,
    onQueryChange: (String) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
) {
    TopAppBar(
        title = {
            TextField(
                value = state.query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.search_hint)) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.action_cancel),
                )
            }
        },
        actions = {
            if (state.searching) {
                CircularProgressIndicator(modifier = Modifier.padding(end = Spacing.sm))
            } else if (state.hits.isNotEmpty()) {
                Text(
                    text = "${state.currentHit + 1}/${state.hits.size}",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            IconButton(onClick = onPrev, enabled = state.hits.isNotEmpty()) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = stringResource(R.string.search_prev))
            }
            IconButton(onClick = onNext, enabled = state.hits.isNotEmpty()) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(R.string.search_next))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

private fun shareDocument(context: Context, viewModel: ViewerViewModel) {
    val file = viewModel.documentFile ?: return
    val uri = if (viewModel.uri.scheme == "content") {
        viewModel.uri
    } else {
        FileProvider.getUriForFile(context, "app.openpdf.foss.fileprovider", file)
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, null))
}

private fun printDocument(context: Context, viewModel: ViewerViewModel) {
    val file = viewModel.documentFile ?: return
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val name = file.name.ifBlank { "document.pdf" }
    printManager.print(name, PdfPrintAdapter(file, name), null)
}

@Composable
private fun FormFieldDialog(
    field: app.openpdf.foss.core.pdf.model.FormField,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by rememberSaveable { mutableStateOf(field.value) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.form_field_title)) },
        text = {
            if (field.options.isNotEmpty()) {
                Column {
                    field.options.forEach { option ->
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (option == value) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { value = option }
                                .padding(vertical = Spacing.sm),
                        )
                    }
                }
            } else {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(stringResource(R.string.form_field_value)) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) {
                Text(stringResource(R.string.action_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun NoteDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.note_dialog_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.note_dialog_label)) },
                minLines = 3,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) {
                Text(stringResource(R.string.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun PasswordDialog(
    wrongPassword: Boolean,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var password by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.password_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                if (wrongPassword) {
                    Text(
                        text = stringResource(R.string.password_dialog_wrong),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    label = { Text(stringResource(R.string.password_dialog_label)) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(password) }, enabled = password.isNotEmpty()) {
                Text(stringResource(R.string.action_open))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun GoToPageDialog(
    pageCount: Int,
    onGo: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    val page = text.toIntOrNull()?.minus(1)
    val valid = page != null && page in 0 until pageCount
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.go_to_page_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                label = { Text(stringResource(R.string.go_to_page_label, pageCount)) },
            )
        },
        confirmButton = {
            TextButton(onClick = { onGo(page!!) }, enabled = valid) {
                Text(stringResource(R.string.action_go))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
