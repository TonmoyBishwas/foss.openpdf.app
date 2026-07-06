package app.openpdf.foss.feature.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.openpdf.foss.core.data.RecentFilesRepository
import app.openpdf.foss.core.data.db.RecentFileEntity
import app.openpdf.foss.core.files.DocumentSaver
import app.openpdf.foss.core.files.SafFileManager
import app.openpdf.foss.core.pdf.PdfEngine
import app.openpdf.foss.core.util.parsePageRanges
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ToolState(
    val busy: Boolean = false,
    val message: String? = null,
    /** URIs picked for a merge, waiting for an output target. */
    val pendingMergeSources: List<Uri> = emptyList(),
    /** URI picked for a split, waiting for ranges + output. */
    val pendingSplitSource: Uri? = null,
    val pendingSplitPageCount: Int = 0,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val recentsRepository: RecentFilesRepository,
    private val engine: PdfEngine,
    private val fileManager: SafFileManager,
    private val documentSaver: DocumentSaver,
) : ViewModel() {

    val recents: StateFlow<List<RecentFileEntity>> = recentsRepository.recents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _toolState = MutableStateFlow(ToolState())
    val toolState: StateFlow<ToolState> = _toolState.asStateFlow()

    fun removeRecent(uri: String) {
        viewModelScope.launch { recentsRepository.remove(uri) }
    }

    // --- Merge ---

    fun onMergeSourcesPicked(uris: List<Uri>) {
        if (uris.size < 2) {
            _toolState.value = ToolState(message = "Pick at least two PDFs")
            return
        }
        _toolState.value = ToolState(pendingMergeSources = uris)
    }

    fun mergeInto(target: Uri) {
        val sources = _toolState.value.pendingMergeSources
        if (sources.isEmpty()) return
        _toolState.value = ToolState(busy = true)
        viewModelScope.launch {
            try {
                val paths = sources.map { fileManager.materialize(it).absolutePath }
                documentSaver.saveWith(target) { tempPath ->
                    engine.merge(paths, tempPath)
                }
                _toolState.value = ToolState(message = "PDFs merged")
            } catch (e: Exception) {
                _toolState.value = ToolState(message = e.message ?: "Merge failed")
            }
        }
    }

    // --- Split ---

    fun onSplitSourcePicked(uri: Uri) {
        _toolState.value = ToolState(busy = true)
        viewModelScope.launch {
            try {
                val file = fileManager.materialize(uri)
                val session = engine.open(file.absolutePath)
                val count = session.pageCount
                session.close()
                _toolState.value = ToolState(pendingSplitSource = uri, pendingSplitPageCount = count)
            } catch (e: Exception) {
                _toolState.value = ToolState(message = e.message ?: "Could not open PDF")
            }
        }
    }

    /** Extracts [ranges] (e.g. "1-3,7") from the pending split source into [target]. */
    fun splitInto(ranges: String, target: Uri) {
        val state = _toolState.value
        val source = state.pendingSplitSource ?: return
        val pages = parsePageRanges(ranges, state.pendingSplitPageCount)
        if (pages == null) {
            _toolState.value = state.copy(message = "No valid pages in \"$ranges\"")
            return
        }
        _toolState.value = ToolState(busy = true)
        viewModelScope.launch {
            try {
                val file = fileManager.materialize(source)
                val session = engine.open(file.absolutePath)
                try {
                    documentSaver.saveWith(target) { tempPath ->
                        session.exportArrangement(pages, emptyMap(), tempPath)
                    }
                } finally {
                    session.close()
                }
                _toolState.value = ToolState(message = "Pages exported")
            } catch (e: Exception) {
                _toolState.value = ToolState(message = e.message ?: "Split failed")
            }
        }
    }

    fun dismissTool() {
        _toolState.value = ToolState()
    }
}
