package app.openpdf.foss.core.data

import app.openpdf.foss.core.data.db.BookmarkEntity
import app.openpdf.foss.core.data.db.BookmarksDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarksRepository @Inject constructor(
    private val dao: BookmarksDao,
) {
    fun forDocument(uri: String): Flow<List<BookmarkEntity>> = dao.observeForDocument(uri)

    suspend fun toggle(uri: String, pageIndex: Int, currentlyBookmarked: Boolean) {
        if (currentlyBookmarked) {
            dao.delete(uri, pageIndex)
        } else {
            dao.upsert(
                BookmarkEntity(
                    uri = uri,
                    pageIndex = pageIndex,
                    label = "Page ${pageIndex + 1}",
                    createdAt = System.currentTimeMillis(),
                )
            )
        }
    }
}
