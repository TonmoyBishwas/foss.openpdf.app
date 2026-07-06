package app.openpdf.foss.feature.viewer

import android.graphics.Bitmap
import android.util.LruCache
import app.openpdf.foss.core.pdf.PdfDocumentSession
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * LRU cache of rendered page bitmaps, keyed by (session, page, render width).
 * Sized in bytes (~64 MB) so a handful of large pages can't evict everything.
 */
object PageBitmapCache {

    private const val MAX_BYTES = 64 * 1024 * 1024

    private val cache = object : LruCache<String, Bitmap>(MAX_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    private val renderMutex = Mutex()

    private fun key(session: PdfDocumentSession, page: Int, width: Int) =
        "${System.identityHashCode(session)}v${session.contentVersion}:$page@$width"

    suspend fun getOrRender(session: PdfDocumentSession, page: Int, width: Int): Bitmap {
        cache.get(key(session, page, width))?.let { return it }
        // Serialize sibling requests so the same page isn't rendered twice.
        renderMutex.withLock {
            cache.get(key(session, page, width))?.let { return it }
            val bitmap = session.renderPage(page, width)
            cache.put(key(session, page, width), bitmap)
            return bitmap
        }
    }

    fun clear() = cache.evictAll()
}
