package app.openpdf.foss.feature.viewer

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.openpdf.foss.core.data.BookmarksRepository
import app.openpdf.foss.core.data.RecentFilesRepository
import app.openpdf.foss.core.data.db.BookmarkEntity
import app.openpdf.foss.core.files.SafFileManager
import app.openpdf.foss.core.pdf.PdfDocumentSession
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
            } catch (e: PdfPasswordRequiredException) {
                _uiState.value = ViewerUiState.PasswordRequired(e.wrongPasswordSupplied)
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
