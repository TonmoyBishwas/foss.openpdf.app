package app.openpdf.foss.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [RecentFileEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class OpenPdfDatabase : RoomDatabase() {
    abstract fun recentFilesDao(): RecentFilesDao
}
