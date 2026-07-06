package app.openpdf.foss.core.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentFilesDao {

    @Query("SELECT * FROM recent_files ORDER BY lastOpenedAt DESC LIMIT 50")
    fun observeRecents(): Flow<List<RecentFileEntity>>

    @Query("SELECT * FROM recent_files WHERE uri = :uri")
    suspend fun get(uri: String): RecentFileEntity?

    @Upsert
    suspend fun upsert(entity: RecentFileEntity)

    @Query("UPDATE recent_files SET lastPage = :lastPage WHERE uri = :uri")
    suspend fun updateLastPage(uri: String, lastPage: Int)

    @Query("DELETE FROM recent_files WHERE uri = :uri")
    suspend fun delete(uri: String)
}
