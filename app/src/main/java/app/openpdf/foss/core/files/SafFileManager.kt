package app.openpdf.foss.core.files

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SafFileManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Persists read+write permission when possible, read-only otherwise. */
    fun tryPersistReadPermission(uri: Uri): Boolean {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            return true
        } catch (_: SecurityException) {
            // Provider may not grant write; fall back to read-only.
        }
        return try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun displayName(uri: Uri): String {
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) {
                    cursor.getString(index)?.let { return it }
                }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "document.pdf"
    }

    /**
     * MuPDF opens documents from a file path, so content URIs are materialized
     * into the app's cache. The copy is keyed by URI hash and reused while the
     * source is unchanged in size.
     */
    suspend fun materialize(uri: Uri): File = withContext(Dispatchers.IO) {
        if (uri.scheme == "file") return@withContext File(requireNotNull(uri.path))

        val dir = File(context.cacheDir, "open").apply { mkdirs() }
        val target = File(dir, "${uri.toString().hashCode().toUInt()}.pdf")
        val sourceSize = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        if (target.exists() && sourceSize > 0 && target.length() == sourceSize) {
            return@withContext target
        }
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("Cannot read $uri")
        target
    }
}
