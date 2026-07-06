package app.openpdf.foss.core.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarksDao {

    @Query("SELECT * FROM bookmarks WHERE uri = :uri ORDER BY pageIndex")
    fun observeForDocument(uri: String): Flow<List<BookmarkEntity>>

    @Upsert
    suspend fun upsert(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE uri = :uri AND pageIndex = :pageIndex")
    suspend fun delete(uri: String, pageIndex: Int)
}
