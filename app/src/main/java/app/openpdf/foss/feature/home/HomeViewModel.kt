package app.openpdf.foss.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.openpdf.foss.core.data.RecentFilesRepository
import app.openpdf.foss.core.data.db.RecentFileEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val recentsRepository: RecentFilesRepository,
) : ViewModel() {

    val recents: StateFlow<List<RecentFileEntity>> = recentsRepository.recents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun removeRecent(uri: String) {
        viewModelScope.launch { recentsRepository.remove(uri) }
    }
}
