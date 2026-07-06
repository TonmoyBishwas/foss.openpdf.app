package app.openpdf.foss.feature.viewer

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.openpdf.foss.core.data.RecentFilesRepository
import app.openpdf.foss.core.files.SafFileManager
import app.openpdf.foss.core.pdf.PdfDocumentSession
import app.openpdf.foss.core.pdf.PdfEngine
import app.openpdf.foss.core.pdf.PdfPasswordRequiredException
import app.openpdf.foss.core.pdf.model.PageSize
import app.openpdf.foss.navigation.ViewerRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    ) : ViewerUiState
}

@HiltViewModel
class ViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val engine: PdfEngine,
    private val fileManager: SafFileManager,
    private val recents: RecentFilesRepository,
) : ViewModel() {

    private val route: ViewerRoute = savedStateHandle.toRoute()
    val uri: Uri = Uri.parse(route.uri)

    private val _uiState = MutableStateFlow<ViewerUiState>(ViewerUiState.Loading)
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    /** The open document; only non-null in [ViewerUiState.Ready]. */
    var session: PdfDocumentSession? = null
        private set

    init {
        open(password = null)
    }

    fun submitPassword(password: String) = open(password)

    private fun open(password: String?) {
        _uiState.value = ViewerUiState.Loading
        viewModelScope.launch {
            try {
                val file = fileManager.materialize(uri)
                val newSession = engine.open(file.absolutePath, password)
                session?.close()
                session = newSession
                val sizes = (0 until newSession.pageCount).map { newSession.pageSize(it) }
                val name = fileManager.displayName(uri)
                val lastPage = recents.lastPage(uri.toString())
                    .coerceIn(0, (newSession.pageCount - 1).coerceAtLeast(0))
                recents.recordOpened(uri.toString(), name, newSession.pageCount)
                _uiState.value = ViewerUiState.Ready(
                    displayName = name,
                    pageCount = newSession.pageCount,
                    pageSizes = sizes,
                    initialPage = lastPage,
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

    override fun onCleared() {
        session?.close()
        session = null
    }
}
