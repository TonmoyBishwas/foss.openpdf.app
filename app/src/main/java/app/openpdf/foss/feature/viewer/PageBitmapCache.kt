package app.openpdf.foss.feature.viewer

import android.graphics.Bitmap
import android.util.LruCache
import app.openpdf.foss.core.pdf.PdfDocumentSession
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * LRU cache of rendered page bitmaps, keyed by (session, contentVersion, page,
 * render width). Sized in bytes so a handful of large pages can't evict
 * everything. Render width is clamped so a deep zoom on a big page can't
 * allocate a multi-hundred-MB bitmap and OOM — beyond the cap the viewport
 * upscales the bitmap instead.
 */
object PageBitmapCache {

    private const val MAX_BYTES = 96 * 1024 * 1024

    /** Hard cap on rendered bitmap width in px (height follows aspect). */
    const val MAX_RENDER_WIDTH = 2600

    /** Low-res width used for the instant progressive preview. */
    const val PREVIEW_WIDTH = 720

    private val cache = object : LruCache<String, Bitmap>(MAX_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    // Per-key locks so two visible pages can render concurrently while a
    // duplicate request for the same key waits and reuses the result.
    private val locks = HashMap<String, Mutex>()
    private val locksGuard = Mutex()

    private fun key(session: PdfDocumentSession, page: Int, width: Int) =
        "${System.identityHashCode(session)}v${session.contentVersion}:$page@$width"

    private suspend fun lockFor(key: String): Mutex = locksGuard.withLock {
        locks.getOrPut(key) { Mutex() }
    }

    fun clampWidth(width: Int): Int = width.coerceIn(1, MAX_RENDER_WIDTH)

    /** Cached bitmap for this exact key, if present — no rendering. */
    fun peek(session: PdfDocumentSession, page: Int, width: Int): Bitmap? =
        cache.get(key(session, page, clampWidth(width)))

    suspend fun getOrRender(session: PdfDocumentSession, page: Int, width: Int): Bitmap {
        val w = clampWidth(width)
        val k = key(session, page, w)
        cache.get(k)?.let { return it }
        lockFor(k).withLock {
            cache.get(k)?.let { return it }
            val bitmap = session.renderPage(page, w)
            cache.put(k, bitmap)
            return bitmap
        }
    }

    fun clear() {
        cache.evictAll()
    }
}
