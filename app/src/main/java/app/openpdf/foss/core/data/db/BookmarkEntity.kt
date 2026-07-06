package app.openpdf.foss.core.data.db

import androidx.room.Entity

/** A user bookmark: one page of one document. */
@Entity(tableName = "bookmarks", primaryKeys = ["uri", "pageIndex"])
data class BookmarkEntity(
    val uri: String,
    val pageIndex: Int,
    val label: String,
    val createdAt: Long,
)
