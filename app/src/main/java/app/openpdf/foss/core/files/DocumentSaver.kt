package app.openpdf.foss.core.files

import android.content.Context
import android.net.Uri
import app.openpdf.foss.core.pdf.PdfDocumentSession
import app.openpdf.foss.core.pdf.PdfEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Atomic save pipeline: write the edited document to a cache temp file,
 * verify the temp re-opens as a valid PDF, and only then stream it over the
 * destination. The destination is never written directly by the engine, so a
 * failed save can't corrupt the user's file.
 */
@Singleton
class DocumentSaver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val engine: PdfEngine,
) {
    class SaveFailedException(message: String, cause: Throwable? = null) :
        Exception(message, cause)

    /** @return true when saved; throws [SaveFailedException] on any failure. */
    suspend fun saveTo(session: PdfDocumentSession, target: Uri): Boolean =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val temp = File(dir, "save-${System.nanoTime()}.pdf")
            try {
                session.saveTo(temp.absolutePath)

                // Corruption guard: the temp file must round-trip.
                try {
                    engine.open(temp.absolutePath).close()
                } catch (e: Exception) {
                    throw SaveFailedException("Saved file failed verification", e)
                }

                val output = context.contentResolver.openOutputStream(target, "wt")
                    ?: throw SaveFailedException("Cannot write to destination")
                output.use { out ->
                    temp.inputStream().use { it.copyTo(out) }
                }
                true
            } catch (e: SaveFailedException) {
                throw e
            } catch (e: Exception) {
                throw SaveFailedException(e.message ?: "Save failed", e)
            } finally {
                temp.delete()
            }
        }

    /** Whether [uri] can be written in place (persisted write grant or file scheme). */
    fun canWriteInPlace(uri: Uri): Boolean {
        if (uri.scheme == "file") return true
        return context.contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isWritePermission
        }
    }
}
