package app.openpdf.foss.feature.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.openpdf.foss.core.data.RecentFilesRepository
import app.openpdf.foss.core.data.db.RecentFileEntity
import android.content.Context
import app.openpdf.foss.core.files.DocumentSaver
import app.openpdf.foss.core.files.SafFileManager
import app.openpdf.foss.core.pdf.PdfEngine
import app.openpdf.foss.core.pdf.pdfbox.PdfBoxCreator
import app.openpdf.foss.core.util.parsePageRanges
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
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
    /** Images picked for image→PDF, waiting for an output target. */
    val pendingImageSources: List<Uri> = emptyList(),
    /** Captured scan photo paths in the current scan session. */
    val scanPages: List<String> = emptyList(),
    /** Text waiting for an output target. */
    val pendingText: String? = null,
    val blackAndWhite: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recentsRepository: RecentFilesRepository,
    private val engine: PdfEngine,
    private val fileManager: SafFileManager,
    private val documentSaver: DocumentSaver,
    private val creator: PdfBoxCreator,
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

    // --- Create: images / scan / text ---

    fun onImagesPicked(uris: List<Uri>) {
        if (uris.isEmpty()) return
        _toolState.value = ToolState(pendingImageSources = uris)
    }

    fun setBlackAndWhite(enabled: Boolean) {
        _toolState.value = _toolState.value.copy(blackAndWhite = enabled)
    }

    fun createImagesInto(target: Uri) {
        val state = _toolState.value
        val sources = state.pendingImageSources
        val scanPages = state.scanPages
        if (sources.isEmpty() && scanPages.isEmpty()) return
        _toolState.value = ToolState(busy = true)
        viewModelScope.launch {
            try {
                val paths = scanPages.ifEmpty {
                    sources.map { fileManager.materialize(it).absolutePath }
                }
                documentSaver.saveWith(target) { tempPath ->
                    creator.createFromImages(paths, tempPath, state.blackAndWhite)
                }
                _toolState.value = ToolState(message = "PDF created")
            } catch (e: Exception) {
                _toolState.value = ToolState(message = e.message ?: "Create failed")
            }
        }
    }

    /** @return a fresh cache file for the camera to write a scan page into. */
    fun newScanTarget(): File {
        val dir = File(context.cacheDir, "scans").apply { mkdirs() }
        return File(dir, "scan-${System.currentTimeMillis()}.jpg")
    }

    fun onScanCaptured(path: String) {
        _toolState.value = _toolState.value.copy(scanPages = _toolState.value.scanPages + path)
    }

    fun clearScan() {
        _toolState.value = ToolState()
    }

    fun onTextEntered(text: String) {
        if (text.isBlank()) return
        _toolState.value = ToolState(pendingText = text)
    }

    fun createTextInto(target: Uri) {
        val text = _toolState.value.pendingText ?: return
        _toolState.value = ToolState(busy = true)
        viewModelScope.launch {
            try {
                documentSaver.saveWith(target) { tempPath ->
                    creator.createFromText(text, tempPath)
                }
                _toolState.value = ToolState(message = "PDF created")
            } catch (e: Exception) {
                _toolState.value = ToolState(message = e.message ?: "Create failed")
            }
        }
    }
}
