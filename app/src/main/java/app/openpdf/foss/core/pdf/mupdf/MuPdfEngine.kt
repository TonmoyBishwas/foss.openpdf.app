package app.openpdf.foss.core.pdf.mupdf

import android.graphics.Bitmap
import app.openpdf.foss.core.pdf.PdfDocumentSession
import app.openpdf.foss.core.pdf.PdfEngine
import app.openpdf.foss.core.pdf.PdfOpenException
import app.openpdf.foss.core.pdf.PdfPasswordRequiredException
import app.openpdf.foss.core.pdf.model.OutlineNode
import app.openpdf.foss.core.pdf.model.PageSize
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.Outline
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
