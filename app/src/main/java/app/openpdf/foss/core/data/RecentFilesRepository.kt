package app.openpdf.foss.core.data

import app.openpdf.foss.core.data.db.RecentFileEntity
import app.openpdf.foss.core.data.db.RecentFilesDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentFilesRepository @Inject constructor(
    private val dao: RecentFilesDao,
) {
    val recents: Flow<List<RecentFileEntity>> = dao.observeRecents()

    suspend fun lastPage(uri: String): Int = dao.get(uri)?.lastPage ?: 0

    suspend fun recordOpened(uri: String, displayName: String, pageCount: Int) {
        val existing = dao.get(uri)
        dao.upsert(
            RecentFileEntity(
                uri = uri,
                displayName = displayName,
                pageCount = pageCount,
                lastPage = existing?.lastPage ?: 0,
                lastOpenedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun updateLastPage(uri: String, lastPage: Int) = dao.updateLastPage(uri, lastPage)

    suspend fun remove(uri: String) = dao.delete(uri)
}
