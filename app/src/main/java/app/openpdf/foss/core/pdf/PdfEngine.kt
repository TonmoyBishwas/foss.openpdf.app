package app.openpdf.foss.core.pdf

import android.graphics.Bitmap
import app.openpdf.foss.core.pdf.model.OutlineNode
import app.openpdf.foss.core.pdf.model.PageSize

/** Thrown when a document is encrypted and no/wrong password was supplied. */
class PdfPasswordRequiredException(val wrongPasswordSupplied: Boolean) : Exception()

/** Thrown when a file cannot be parsed as a PDF at all. */
class PdfOpenException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Abstraction over the PDF engine (MuPDF today) so the implementation can be
 * swapped without touching features. All methods may block; callers must not
 * invoke them on the main thread.
 */
interface PdfEngine {
    /**
     * Opens the PDF at [filePath].
     * @throws PdfPasswordRequiredException when encrypted and [password] is null or wrong
     * @throws PdfOpenException when the file is not a readable PDF
     */
    suspend fun open(filePath: String, password: String? = null): PdfDocumentSession
}

/**
 * An open document. Implementations serialize access internally — callers may
 * invoke from any dispatcher, but heavy calls should stay off the main thread.
 */
interface PdfDocumentSession : AutoCloseable {
    val pageCount: Int

    /** Page size in PDF points (72 dpi), after page rotation. */
    suspend fun pageSize(pageIndex: Int): PageSize

    /**
     * Renders [pageIndex] to a bitmap [targetWidth] pixels wide (height follows
     * the page aspect ratio).
     */
    suspend fun renderPage(pageIndex: Int, targetWidth: Int): Bitmap

    /** Document outline (table of contents); empty when the PDF has none. */
    suspend fun outline(): List<OutlineNode>

    override fun close()
}
