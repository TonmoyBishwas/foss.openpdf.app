package app.openpdf.foss.feature.organize

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.openpdf.foss.core.data.RecentFilesRepository
import app.openpdf.foss.core.files.DocumentSaver
import app.openpdf.foss.core.files.SafFileManager
import app.openpdf.foss.core.pdf.PdfDocumentSession
import app.openpdf.foss.core.pdf.PdfEngine
import app.openpdf.foss.core.pdf.model.PageSize
import app.openpdf.foss.navigation.OrganizeRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * One entry in the working arrangement. [sourcePage] is the page in the
 * original document; [rotation] is extra clockwise rotation to apply on save.
 */
data class ArrangedPage(
    val sourcePage: Int,
    val rotation: Int = 0,
)

sealed interface OrganizeUiState {
    data object Loading : OrganizeUiState
    data class Error(val message: String) : OrganizeUiState
    data class Ready(
        val displayName: String,
        val pageSizes: List<PageSize>,
        val pages: List<ArrangedPage>,
        val selection: Set<Int> = emptySet(),
        val moveMode: Boolean = false,
        val dirty: Boolean = false,
        val saving: Boolean = false,
        val canWriteInPlace: Boolean = false,
        val message: String? = null,
    ) : OrganizeUiState
}

@HiltViewModel
class OrganizeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val engine: PdfEngine,
    private val fileManager: SafFileManager,
    private val documentSaver: DocumentSaver,
    private val recents: RecentFilesRepository,
) : ViewModel() {

    private val route: OrganizeRoute = savedStateHandle.toRoute()
    val uri: Uri = Uri.parse(route.uri)

    private val _uiState = MutableStateFlow<OrganizeUiState>(OrganizeUiState.Loading)
    val uiState: StateFlow<OrganizeUiState> = _uiState.asStateFlow()

    var session: PdfDocumentSession? = null
        private set

    init {
        viewModelScope.launch {
            try {
                val file = fileManager.materialize(uri)
                val newSession = engine.open(file.absolutePath)
                session = newSession
                val sizes = (0 until newSession.pageCount).map { newSession.pageSize(it) }
                _uiState.value = OrganizeUiState.Ready(
                    displayName = fileManager.displayName(uri),
                    pageSizes = sizes,
                    pages = (0 until newSession.pageCount).map { ArrangedPage(it) },
                    canWriteInPlace = documentSaver.canWriteInPlace(uri),
                )
            } catch (e: Exception) {
                _uiState.value = OrganizeUiState.Error(e.message ?: "Could not open PDF")
            }
        }
    }

    private inline fun updateReady(block: (OrganizeUiState.Ready) -> OrganizeUiState.Ready) {
        _uiState.update { if (it is OrganizeUiState.Ready) block(it) else it }
    }

    fun toggleSelection(position: Int) {
        updateReady { state ->
            if (state.moveMode) return@updateReady state
            val selection = state.selection.toMutableSet()
            if (!selection.remove(position)) selection.add(position)
            state.copy(selection = selection)
        }
    }

    fun rotateSelected() {
        updateReady { state ->
            state.copy(
                pages = state.pages.mapIndexed { index, page ->
                    if (index in state.selection) page.copy(rotation = (page.rotation + 90) % 360)
                    else page
                },
                dirty = true,
            )
        }
    }

    fun duplicateSelected() {
        updateReady { state ->
            val result = mutableListOf<ArrangedPage>()
            state.pages.forEachIndexed { index, page ->
                result.add(page)
                if (index in state.selection) result.add(page.copy())
            }
            state.copy(pages = result, selection = emptySet(), dirty = true)
        }
    }

    fun deleteSelected() {
        updateReady { state ->
            val remaining = state.pages.filterIndexed { index, _ -> index !in state.selection }
            if (remaining.isEmpty()) state.copy(message = "Cannot delete every page")
            else state.copy(pages = remaining, selection = emptySet(), dirty = true)
        }
    }

    fun startMove() {
        updateReady { state ->
            if (state.selection.size == 1) state.copy(moveMode = true) else state
        }
    }

    fun moveTo(targetPosition: Int) {
        updateReady { state ->
            val from = state.selection.singleOrNull() ?: return@updateReady state
            val pages = state.pages.toMutableList()
            val page = pages.removeAt(from)
            pages.add(targetPosition.coerceIn(0, pages.size), page)
            state.copy(pages = pages, selection = emptySet(), moveMode = false, dirty = true)
        }
    }

    fun cancelMove() {
        updateReady { it.copy(moveMode = false, selection = emptySet()) }
    }

    fun dismissMessage() = updateReady { it.copy(message = null) }

    /** Saves the current arrangement over [target] (or the source when null). */
    fun save(target: Uri? = null) {
        val state = _uiState.value as? OrganizeUiState.Ready ?: return
        val currentSession = session ?: return
        val destination = target ?: uri
        updateReady { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                documentSaver.saveWith(destination) { tempPath ->
                    exportWithRotations(currentSession, state.pages, tempPath)
                }
                if (destination == uri) {
                    recents.recordOpened(
                        uri.toString(),
                        state.displayName,
                        state.pages.size,
                    )
                }
                updateReady { it.copy(saving = false, dirty = false, message = "Saved") }
            } catch (e: Exception) {
                updateReady { it.copy(saving = false, message = e.message ?: "Save failed") }
            }
        }
    }

    private suspend fun exportWithRotations(
        session: PdfDocumentSession,
        pages: List<ArrangedPage>,
        outPath: String,
    ) {
        // exportArrangement keys rotations by source index; when duplicates
        // rotate differently the last value wins — acceptable for now.
        session.exportArrangement(
            order = pages.map { it.sourcePage },
            rotations = pages.filter { it.rotation != 0 }
                .associate { it.sourcePage to it.rotation },
            outFilePath = outPath,
        )
    }

    /** Exports only the selected pages to [target]. */
    fun extractSelectedTo(target: Uri) {
        val state = _uiState.value as? OrganizeUiState.Ready ?: return
        val currentSession = session ?: return
        val selected = state.pages.filterIndexed { index, _ -> index in state.selection }
        if (selected.isEmpty()) return
        updateReady { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                documentSaver.saveWith(target) { tempPath ->
                    exportWithRotations(currentSession, selected, tempPath)
                }
                updateReady {
                    it.copy(saving = false, selection = emptySet(), message = "Pages extracted")
                }
            } catch (e: Exception) {
                updateReady { it.copy(saving = false, message = e.message ?: "Extract failed") }
            }
        }
    }

    override fun onCleared() {
        session?.close()
        session = null
    }
}
