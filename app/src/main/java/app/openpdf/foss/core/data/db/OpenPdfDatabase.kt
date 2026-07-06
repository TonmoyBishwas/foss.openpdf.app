package app.openpdf.foss.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [RecentFileEntity::class, BookmarkEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class OpenPdfDatabase : RoomDatabase() {
    abstract fun recentFilesDao(): RecentFilesDao
    abstract fun bookmarksDao(): BookmarksDao
}
