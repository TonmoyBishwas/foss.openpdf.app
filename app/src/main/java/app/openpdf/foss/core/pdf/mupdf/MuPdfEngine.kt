package app.openpdf.foss.core.pdf.mupdf

import android.graphics.Bitmap
import app.openpdf.foss.core.pdf.PdfDocumentSession
import app.openpdf.foss.core.pdf.PdfEngine
import app.openpdf.foss.core.pdf.PdfOpenException
import app.openpdf.foss.core.pdf.PdfPasswordRequiredException
import app.openpdf.foss.core.pdf.model.NormalizedRect
import app.openpdf.foss.core.pdf.model.OutlineNode
import app.openpdf.foss.core.pdf.model.PageSize
import app.openpdf.foss.core.pdf.model.SearchHit
import app.openpdf.foss.core.pdf.model.TextSelection
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.Outline
import com.artifex.mupdf.fitz.Page
import com.artifex.mupdf.fitz.Point
import com.artifex.mupdf.fitz.Quad
import com.artifex.mupdf.fitz.StructuredText
import com.artifex.mupdf.fitz.android.AndroidDrawDevice
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MuPdfEngine @Inject constructor() : PdfEngine {

    override suspend fun open(filePath: String, password: String?): PdfDocumentSession {
        // MuPDF is not thread-safe; every call for a given document must run on
        // the same single-threaded executor the document was opened on.
        @Suppress("OPT_IN_USAGE")
        val dispatcher = Dispatchers.IO.limitedParallelism(1)
        return withContext(dispatcher) {
            val document = try {
                Document.openDocument(filePath)
            } catch (e: Exception) {
                throw PdfOpenException("Cannot open PDF: ${e.message}", e)
            }
            if (document.needsPassword()) {
                if (password == null) {
                    document.destroy()
                    throw PdfPasswordRequiredException(wrongPasswordSupplied = false)
                }
                if (!document.authenticatePassword(password)) {
                    document.destroy()
                    throw PdfPasswordRequiredException(wrongPasswordSupplied = true)
                }
            }
            MuPdfSession(document, dispatcher)
        }
    }
}

internal class MuPdfSession(
    private val document: Document,
    private val dispatcher: CoroutineDispatcher,
) : PdfDocumentSession {

    override val pageCount: Int = document.countPages()

    override suspend fun pageSize(pageIndex: Int): PageSize = withContext(dispatcher) {
        val page = document.loadPage(pageIndex)
        try {
            val b = page.bounds
            PageSize(b.x1 - b.x0, b.y1 - b.y0)
        } finally {
            page.destroy()
        }
    }

    override suspend fun renderPage(pageIndex: Int, targetWidth: Int): Bitmap =
        withContext(dispatcher) {
            val page = document.loadPage(pageIndex)
            try {
                val bounds = page.bounds
                val pageWidth = bounds.x1 - bounds.x0
                val pageHeight = bounds.y1 - bounds.y0
                val scale = targetWidth / pageWidth
                val width = targetWidth.coerceAtLeast(1)
                val height = (pageHeight * scale).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val matrix = Matrix(scale, scale)
                val device = AndroidDrawDevice(bitmap, 0, 0, 0, 0, width, height)
                try {
                    page.run(device, matrix, null)
                } finally {
                    device.close()
                    device.destroy()
                }
                bitmap
            } finally {
                page.destroy()
            }
        }

    override suspend fun search(pageIndex: Int, query: String): List<SearchHit> =
        withContext(dispatcher) {
            withPage(pageIndex) { page ->
                val bounds = page.bounds
                val hits: Array<Array<Quad>> = page.search(query) ?: return@withPage emptyList()
                hits.map { quads ->
                    SearchHit(
                        pageIndex = pageIndex,
                        rects = quads.map { it.toNormalizedRect(bounds) },
                    )
                }
            }
        }

    override suspend fun pageText(pageIndex: Int): String = withContext(dispatcher) {
        withPage(pageIndex) { page ->
            val structured = page.toStructuredText()
            try {
                structured.copy(
                    Point(page.bounds.x0, page.bounds.y0),
                    Point(page.bounds.x1, page.bounds.y1),
                ) ?: ""
            } finally {
                structured.destroy()
            }
        }
    }

    override suspend fun selectText(
        pageIndex: Int,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
    ): TextSelection? = withContext(dispatcher) {
        withPage(pageIndex) { page ->
            val bounds = page.bounds
            val width = bounds.x1 - bounds.x0
            val height = bounds.y1 - bounds.y0
            val a = Point(bounds.x0 + startX * width, bounds.y0 + startY * height)
            val b = Point(bounds.x0 + endX * width, bounds.y0 + endY * height)
            val structured = page.toStructuredText()
            try {
                val quads = structured.highlight(a, b) ?: emptyArray()
                val text = structured.copy(a, b) ?: ""
                if (text.isBlank() || quads.isEmpty()) {
                    null
                } else {
                    TextSelection(
                        text = text,
                        rects = quads.map { it.toNormalizedRect(bounds) },
                    )
                }
            } finally {
                structured.destroy()
            }
        }
    }

    private inline fun <T> withPage(pageIndex: Int, block: (Page) -> T): T {
        val page = document.loadPage(pageIndex)
        try {
            return block(page)
        } finally {
            page.destroy()
        }
    }

    private fun Quad.toNormalizedRect(bounds: com.artifex.mupdf.fitz.Rect): NormalizedRect {
        val width = bounds.x1 - bounds.x0
        val height = bounds.y1 - bounds.y0
        val left = minOf(ul_x, ll_x)
        val right = maxOf(ur_x, lr_x)
        val top = minOf(ul_y, ur_y)
        val bottom = maxOf(ll_y, lr_y)
        return NormalizedRect(
            left = ((left - bounds.x0) / width).coerceIn(0f, 1f),
            top = ((top - bounds.y0) / height).coerceIn(0f, 1f),
            right = ((right - bounds.x0) / width).coerceIn(0f, 1f),
            bottom = ((bottom - bounds.y0) / height).coerceIn(0f, 1f),
        )
    }

    override suspend fun outline(): List<OutlineNode> = withContext(dispatcher) {
        document.loadOutline()?.map { it.toNode() } ?: emptyList()
    }

    private fun Outline.toNode(): OutlineNode = OutlineNode(
        title = title ?: "",
        pageIndex = document.pageNumberFromLocation(document.resolveLink(this)),
        children = down?.map { it.toNode() } ?: emptyList(),
    )

    override fun close() {
        document.destroy()
    }
}
