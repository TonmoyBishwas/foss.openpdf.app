package app.openpdf.foss.feature.viewer

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.openpdf.foss.core.data.BookmarksRepository
import app.openpdf.foss.core.data.RecentFilesRepository
import app.openpdf.foss.core.data.db.BookmarkEntity
import app.openpdf.foss.core.files.DocumentSaver
import app.openpdf.foss.core.files.SafFileManager
import app.openpdf.foss.core.pdf.PdfDocumentSession
import app.openpdf.foss.core.pdf.model.FormField
import app.openpdf.foss.core.pdf.model.FormFieldType
import app.openpdf.foss.core.pdf.model.InkStroke
import app.openpdf.foss.core.pdf.model.MarkupType
import app.openpdf.foss.core.pdf.model.NormalizedRect
import app.openpdf.foss.core.pdf.model.PageAnnotation
import app.openpdf.foss.core.pdf.model.ShapeType
import app.openpdf.foss.core.pdf.PdfEngine
import app.openpdf.foss.core.pdf.PdfPasswordRequiredException
import app.openpdf.foss.core.pdf.model.OutlineNode
import app.openpdf.foss.core.pdf.model.PageSize
import app.openpdf.foss.core.pdf.model.SearchHit
import app.openpdf.foss.core.pdf.model.TextSelection
import app.openpdf.foss.navigation.ViewerRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/** Color treatment applied to rendered pages. */
enum class ReadingMode { NORMAL, NIGHT, SEPIA }

/** Active annotation tool. */
enum class AnnotationTool { NONE, NOTE, INK, TEXT, SHAPE, SIGN, ERASE }

/** Ink stroke color choices offered by the toolbar (ARGB). */
val InkColors = listOf(0xFF2C3E50L, 0xFFBA1A1AL, 0xFF1B7F4BL, 0xFFF9A825L)
const val HighlightColor = 0xFFFFEB3BL

data class AnnotationUiState(
    val tool: AnnotationTool = AnnotationTool.NONE,
    val toolbarVisible: Boolean = false,
    val inkColor: Long = 0xFF2C3E50L,
    val shapeType: ShapeType = ShapeType.RECTANGLE,
    val signatureStrokes: List<InkStroke> = emptyList(),
    val dirty: Boolean = false,
    val saving: Boolean = false,
    val canWriteInPlace: Boolean = false,
    val saveError: String? = null,
    val savedTick: Int = 0,
)

/** Scroll orientation of the viewer. */
enum class ViewMode { VERTICAL, HORIZONTAL }

sealed interface ViewerUiState {
    data object Loading : ViewerUiState
    data class PasswordRequired(val wrongPassword: Boolean) : ViewerUiState
    data class Error(val message: String) : ViewerUiState
    data class Ready(
        val displayName: String,
        val pageCount: Int,
        val pageSizes: List<PageSize>,
        val initialPage: Int,
        val currentPage: Int = initialPage,
        val outline: List<OutlineNode> = emptyList(),
        val readingMode: ReadingMode = ReadingMode.NORMAL,
        val viewMode: ViewMode = ViewMode.VERTICAL,
    ) : ViewerUiState
}

data class SelectionState(
    val pageIndex: Int,
    val selection: TextSelection,
)

data class SearchState(
    val active: Boolean = false,
    val query: String = "",
    val searching: Boolean = false,
    val hits: List<SearchHit> = emptyList(),
    val currentHit: Int = -1,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val engine: PdfEngine,
    private val fileManager: SafFileManager,
    private val recents: RecentFilesRepository,
    private val bookmarksRepository: BookmarksRepository,
    private val documentSaver: DocumentSaver,
    val readAloud: ReadAloudController,
) : ViewModel() {

    private val route: ViewerRoute = savedStateHandle.toRoute()
    val uri: Uri = Uri.parse(route.uri)

    private val _uiState = MutableStateFlow<ViewerUiState>(ViewerUiState.Loading)
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    private val _searchState = MutableStateFlow(SearchState())
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    private val _selection = MutableStateFlow<SelectionState?>(null)
    val selection: StateFlow<SelectionState?> = _selection.asStateFlow()

    private val _annotationState = MutableStateFlow(AnnotationUiState())
    val annotationState: StateFlow<AnnotationUiState> = _annotationState.asStateFlow()

    /** Bumped after every document mutation so page bitmaps re-render. */
    private val _docVersion = MutableStateFlow(0)
    val docVersion: StateFlow<Int> = _docVersion.asStateFlow()

    /** Annotations on the current page, for the eraser tool. */
    private val _pageAnnotations = MutableStateFlow<List<PageAnnotation>>(emptyList())
    val pageAnnotations: StateFlow<List<PageAnnotation>> = _pageAnnotations.asStateFlow()

    /** Interactive form fields on the current page. */
    private val _formFields = MutableStateFlow<List<FormField>>(emptyList())
    val formFields: StateFlow<List<FormField>> = _formFields.asStateFlow()

    val bookmarks: StateFlow<List<BookmarkEntity>> =
        bookmarksRepository.forDocument(route.uri)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The open document; only non-null in [ViewerUiState.Ready]. */
    var session: PdfDocumentSession? = null
        private set

    /** Local file backing the document (for share/print). */
    var documentFile: File? = null
        private set

    private var searchJob: Job? = null

    init {
        open(password = null)
        readAloud.onPageFinished = { advanceReadAloud() }
    }

    fun submitPassword(password: String) = open(password)

    private fun open(password: String?) {
        _uiState.value = ViewerUiState.Loading
        viewModelScope.launch {
            try {
                val file = fileManager.materialize(uri)
                documentFile = file
                val newSession = engine.open(file.absolutePath, password)
                session?.close()
                session = newSession
                val sizes = (0 until newSession.pageCount).map { newSession.pageSize(it) }
                val outline = newSession.outline()
                val name = fileManager.displayName(uri)
                val lastPage = recents.lastPage(uri.toString())
                    .coerceIn(0, (newSession.pageCount - 1).coerceAtLeast(0))
                recents.recordOpened(uri.toString(), name, newSession.pageCount)
                _uiState.value = ViewerUiState.Ready(
                    displayName = name,
                    pageCount = newSession.pageCount,
                    pageSizes = sizes,
                    initialPage = lastPage,
                    outline = outline,
                )
                _annotationState.update {
                    it.copy(canWriteInPlace = documentSaver.canWriteInPlace(uri))
                }
                refreshFormFields()
            } catch (e: PdfPasswordRequiredException) {
                _uiState.value = ViewerUiState.PasswordRequired(e.wrongPasswordSupplied)
            } catch (e: app.openpdf.foss.core.files.SafFileManager.PermissionLostException) {
                // The stale recent entry is useless now — drop it.
                recents.remove(uri.toString())
                _uiState.value = ViewerUiState.Error(
                    "Access to this file was lost. Open it again from your files to restore access."
                )
            } catch (e: Exception) {
                _uiState.value = ViewerUiState.Error(e.message ?: "Could not open PDF")
            }
        }
    }

    fun onPageChanged(page: Int) {
        _uiState.update { state ->
            if (state is ViewerUiState.Ready) state.copy(currentPage = page) else state
        }
        viewModelScope.launch { recents.updateLastPage(uri.toString(), page) }
        if (_annotationState.value.tool == AnnotationTool.ERASE) refreshPageAnnotations()
        refreshFormFields()
    }

    fun cycleReadingMode() {
        _uiState.update { state ->
            if (state is ViewerUiState.Ready) {
                val next = ReadingMode.entries[(state.readingMode.ordinal + 1) % ReadingMode.entries.size]
                state.copy(readingMode = next)
            } else state
        }
    }

    fun toggleViewMode() {
        _uiState.update { state ->
            if (state is ViewerUiState.Ready) {
                state.copy(
                    viewMode = if (state.viewMode == ViewMode.VERTICAL) ViewMode.HORIZONTAL
                    else ViewMode.VERTICAL
                )
            } else state
        }
    }

    fun toggleBookmark() {
        val ready = _uiState.value as? ViewerUiState.Ready ?: return
        val bookmarked = bookmarks.value.any { it.pageIndex == ready.currentPage }
        viewModelScope.launch {
            bookmarksRepository.toggle(uri.toString(), ready.currentPage, bookmarked)
        }
    }

    // --- Search ---

    fun setSearchActive(active: Boolean) {
        _searchState.value = if (active) SearchState(active = true) else SearchState()
        if (!active) searchJob?.cancel()
    }

    fun search(query: String) {
        _searchState.update { it.copy(query = query) }
        searchJob?.cancel()
        val currentSession = session ?: return
        if (query.length < 2) {
            _searchState.update { it.copy(hits = emptyList(), currentHit = -1, searching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            _searchState.update { it.copy(searching = true, hits = emptyList(), currentHit = -1) }
            val all = mutableListOf<SearchHit>()
            for (page in 0 until currentSession.pageCount) {
                all += currentSession.search(page, query)
                if (all.isNotEmpty() && _searchState.value.currentHit == -1) {
                    _searchState.update { it.copy(hits = all.toList(), currentHit = 0) }
                } else {
                    _searchState.update { it.copy(hits = all.toList()) }
                }
            }
            _searchState.update { it.copy(searching = false) }
        }
    }

    fun goToHit(index: Int) {
        val state = _searchState.value
        if (state.hits.isEmpty()) return
        val wrapped = ((index % state.hits.size) + state.hits.size) % state.hits.size
        _searchState.update { it.copy(currentHit = wrapped) }
    }

    // --- Annotations ---

    fun setAnnotationToolbarVisible(visible: Boolean) {
        _annotationState.update {
            it.copy(toolbarVisible = visible, tool = if (visible) it.tool else AnnotationTool.NONE)
        }
        if (!visible) clearSelection()
    }

    fun setAnnotationTool(tool: AnnotationTool) {
        _annotationState.update {
            it.copy(tool = if (it.tool == tool) AnnotationTool.NONE else tool)
        }
        refreshPageAnnotations()
    }

    fun setInkColor(argb: Long) {
        _annotationState.update { it.copy(inkColor = argb) }
    }

    /** Applies highlight/underline/strikethrough to the current text selection. */
    fun markupSelection(type: MarkupType) {
        val sel = _selection.value ?: return
        val currentSession = session ?: return
        viewModelScope.launch {
            val color = if (type == MarkupType.HIGHLIGHT) HighlightColor
            else _annotationState.value.inkColor
            currentSession.addTextMarkup(sel.pageIndex, type, sel.selection.rects, color)
            clearSelection()
            onDocumentMutated()
        }
    }

    fun addNote(page: Int, x: Float, y: Float, contents: String) {
        val currentSession = session ?: return
        viewModelScope.launch {
            currentSession.addNote(page, x, y, contents, 0xFFF9A825L)
            onDocumentMutated()
        }
    }

    fun addInk(page: Int, strokes: List<InkStroke>) {
        val currentSession = session ?: return
        if (strokes.isEmpty()) return
        viewModelScope.launch {
            currentSession.addInk(
                page, strokes, _annotationState.value.inkColor, strokeWidth = 0.004f,
            )
            onDocumentMutated()
        }
    }

    /** Erase tool: deletes the topmost annotation containing the tapped point. */
    fun eraseAnnotationAt(page: Int, x: Float, y: Float) {
        val currentSession = session ?: return
        viewModelScope.launch {
            val annots = currentSession.annotations(page)
            val target = annots.lastOrNull { annot ->
                annot.rects.any { r -> x in r.left..r.right && y in r.top..r.bottom }
            } ?: return@launch
            currentSession.deleteAnnotation(page, target.index)
            onDocumentMutated()
        }
    }

    fun setShapeType(type: ShapeType) {
        _annotationState.update { it.copy(shapeType = type) }
    }

    fun addFreeText(page: Int, x: Float, y: Float, text: String) {
        val currentSession = session ?: return
        val ready = _uiState.value as? ViewerUiState.Ready ?: return
        viewModelScope.launch {
            val aspect = ready.pageSizes.getOrNull(page)?.aspectRatio ?: 0.7071f
            // ~40% of page width, height scaled by line count.
            val width = 0.4f
            val lines = text.lines().size.coerceAtLeast(1)
            val height = (0.035f * lines * aspect).coerceAtMost(0.5f)
            val rect = NormalizedRect(
                left = x.coerceIn(0f, 1f - width),
                top = y.coerceIn(0f, 1f - height),
                right = (x + width).coerceAtMost(1f),
                bottom = (y + height).coerceAtMost(1f),
            )
            currentSession.addFreeText(page, rect, text, fontSize = 12f, argb = _annotationState.value.inkColor)
            onDocumentMutated()
        }
    }

    fun addShape(page: Int, rect: NormalizedRect) {
        val currentSession = session ?: return
        viewModelScope.launch {
            currentSession.addShape(
                page, _annotationState.value.shapeType, rect,
                _annotationState.value.inkColor, strokeWidth = 0.004f,
            )
            onDocumentMutated()
        }
    }

    fun setSignature(strokes: List<InkStroke>) {
        _annotationState.update { it.copy(signatureStrokes = strokes) }
    }

    /** Places the stored signature centered at the tapped point (~40% page width). */
    fun placeSignature(page: Int, x: Float, y: Float) {
        val strokes = _annotationState.value.signatureStrokes
        if (strokes.isEmpty()) return
        val currentSession = session ?: return
        val ready = _uiState.value as? ViewerUiState.Ready ?: return
        viewModelScope.launch {
            val aspect = ready.pageSizes.getOrNull(page)?.aspectRatio ?: 0.7071f
            val width = 0.4f
            val height = width * 0.4f * aspect
            val left = (x - width / 2).coerceIn(0f, 1f - width)
            val top = (y - height / 2).coerceIn(0f, 1f - height)
            val scaled = strokes.map { stroke ->
                InkStroke(stroke.points.map { (px, py) -> (left + px * width) to (top + py * height) })
            }
            currentSession.addInk(page, scaled, 0xFF1A237EL, strokeWidth = 0.005f)
            onDocumentMutated()
        }
    }

    // --- Forms ---

    fun refreshFormFields() {
        val currentSession = session ?: return
        val ready = _uiState.value as? ViewerUiState.Ready ?: return
        viewModelScope.launch {
            _formFields.value = runCatching {
                currentSession.formFields(ready.currentPage)
            }.getOrDefault(emptyList())
        }
    }

    /** @return the form field at the point, or null. */
    fun formFieldAt(page: Int, x: Float, y: Float): FormField? {
        val ready = _uiState.value as? ViewerUiState.Ready ?: return null
        if (page != ready.currentPage) return null
        return _formFields.value.lastOrNull { f ->
            x in f.rect.left..f.rect.right && y in f.rect.top..f.rect.bottom
        }
    }

    fun setFormValue(field: FormField, value: String) {
        val currentSession = session ?: return
        val ready = _uiState.value as? ViewerUiState.Ready ?: return
        viewModelScope.launch {
            currentSession.setFormFieldValue(ready.currentPage, field.index, value)
            onDocumentMutated()
            refreshFormFields()
        }
    }

    fun toggleFormField(field: FormField) {
        val currentSession = session ?: return
        val ready = _uiState.value as? ViewerUiState.Ready ?: return
        viewModelScope.launch {
            currentSession.toggleFormField(ready.currentPage, field.index)
            onDocumentMutated()
            refreshFormFields()
        }
    }

    private fun onDocumentMutated() {
        _docVersion.update { it + 1 }
        _annotationState.update { it.copy(dirty = true) }
        refreshPageAnnotations()
    }

    private fun refreshPageAnnotations() {
        val currentSession = session ?: return
        val ready = _uiState.value as? ViewerUiState.Ready ?: return
        viewModelScope.launch {
            _pageAnnotations.value = runCatching {
                currentSession.annotations(ready.currentPage)
            }.getOrDefault(emptyList())
        }
    }

    fun save() {
        val currentSession = session ?: return
        _annotationState.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            try {
                documentSaver.saveTo(currentSession, uri)
                _annotationState.update {
                    it.copy(saving = false, dirty = false, savedTick = it.savedTick + 1)
                }
            } catch (e: Exception) {
                _annotationState.update {
                    it.copy(saving = false, saveError = e.message ?: "Save failed")
                }
            }
        }
    }

    fun saveTo(target: Uri) {
        val currentSession = session ?: return
        _annotationState.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            try {
                documentSaver.saveTo(currentSession, target)
                _annotationState.update {
                    it.copy(saving = false, dirty = false, savedTick = it.savedTick + 1)
                }
            } catch (e: Exception) {
                _annotationState.update {
                    it.copy(saving = false, saveError = e.message ?: "Save failed")
                }
            }
        }
    }

    fun dismissSaveError() {
        _annotationState.update { it.copy(saveError = null) }
    }

    // --- Protect & metadata ---

    val isEncrypted: Boolean get() = session?.isEncrypted == true

    fun protectTo(target: Uri, password: String) {
        val currentSession = session ?: return
        _annotationState.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            try {
                documentSaver.saveWith(target) { tempPath ->
                    currentSession.saveEncrypted(tempPath, password, password)
                }
                _annotationState.update { it.copy(saving = false, savedTick = it.savedTick + 1) }
            } catch (e: Exception) {
                _annotationState.update {
                    it.copy(saving = false, saveError = e.message ?: "Protect failed")
                }
            }
        }
    }

    fun removePasswordTo(target: Uri) {
        val currentSession = session ?: return
        _annotationState.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            try {
                documentSaver.saveWith(target) { tempPath ->
                    currentSession.saveDecrypted(tempPath)
                }
                _annotationState.update { it.copy(saving = false, savedTick = it.savedTick + 1) }
            } catch (e: Exception) {
                _annotationState.update {
                    it.copy(saving = false, saveError = e.message ?: "Could not remove password")
                }
            }
        }
    }

    private val _metadata = MutableStateFlow<Map<String, String>>(emptyMap())
    val metadata: StateFlow<Map<String, String>> = _metadata.asStateFlow()

    fun loadMetadata() {
        val currentSession = session ?: return
        viewModelScope.launch {
            _metadata.value = runCatching { currentSession.metadata() }.getOrDefault(emptyMap())
        }
    }

    fun updateMetadata(values: Map<String, String>) {
        val currentSession = session ?: return
        viewModelScope.launch {
            currentSession.setMetadata(values)
            _annotationState.update { it.copy(dirty = true) }
            _metadata.value = runCatching { currentSession.metadata() }.getOrDefault(emptyMap())
        }
    }

    // --- Text selection ---

    private var selectionJob: Job? = null

    fun selectText(page: Int, sx: Float, sy: Float, ex: Float, ey: Float) {
        val currentSession = session ?: return
        selectionJob?.cancel()
        selectionJob = viewModelScope.launch {
            val result = currentSession.selectText(page, sx, sy, ex, ey)
            _selection.value = result?.let { SelectionState(page, it) }
        }
    }

    fun clearSelection() {
        selectionJob?.cancel()
        _selection.value = null
    }

    // --- Read aloud ---

    fun startReadAloud() {
        val ready = _uiState.value as? ViewerUiState.Ready ?: return
        speakPage(ready.currentPage)
    }

    fun stopReadAloud() = readAloud.stop()

    private fun speakPage(page: Int) {
        val currentSession = session ?: return
        viewModelScope.launch {
            val text = currentSession.pageText(page)
            readAloud.speak(text)
        }
    }

    private fun advanceReadAloud() {
        val ready = _uiState.value as? ViewerUiState.Ready ?: return
        val next = ready.currentPage + 1
        if (next < ready.pageCount && readAloud.isSpeaking.value) {
            onPageChanged(next)
            speakPage(next)
        } else {
            readAloud.stop()
        }
    }

    override fun onCleared() {
        readAloud.shutdown()
        session?.close()
        session = null
    }
}
