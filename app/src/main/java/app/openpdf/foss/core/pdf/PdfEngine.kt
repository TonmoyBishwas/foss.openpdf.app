package app.openpdf.foss.core.pdf

import android.graphics.Bitmap
import app.openpdf.foss.core.pdf.model.OutlineNode
import app.openpdf.foss.core.pdf.model.PageSize
import app.openpdf.foss.core.pdf.model.InkStroke
import app.openpdf.foss.core.pdf.model.MarkupType
import app.openpdf.foss.core.pdf.model.NormalizedRect
import app.openpdf.foss.core.pdf.model.PageAnnotation
import app.openpdf.foss.core.pdf.model.SearchHit
import app.openpdf.foss.core.pdf.model.TextSelection

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

    /** All matches of [query] on [pageIndex]; rects are normalized 0..1 page coords. */
    suspend fun search(pageIndex: Int, query: String): List<SearchHit>

    /** Plain text of the page (reading order), for TTS and clipboard. */
    suspend fun pageText(pageIndex: Int): String

    /**
     * Word-snapped text selection between two points in normalized 0..1 page
     * coordinates (top-left origin). Null when there is no text there.
     */
    suspend fun selectText(
        pageIndex: Int,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
    ): TextSelection?

    // --- Editing (only when [isEditable]) ---

    /** Whether this document supports writing annotations (i.e. is a real PDF). */
    val isEditable: Boolean

    /** Monotonic counter bumped on every mutation; include in render cache keys. */
    val contentVersion: Int

    /** Existing annotations on the page. */
    suspend fun annotations(pageIndex: Int): List<PageAnnotation>

    /** Adds highlight/underline/strikethrough over [rects]. [argb] like 0xFFFFEB3B. */
    suspend fun addTextMarkup(pageIndex: Int, type: MarkupType, rects: List<NormalizedRect>, argb: Long)

    /** Adds a sticky-note comment at a normalized point. */
    suspend fun addNote(pageIndex: Int, x: Float, y: Float, contents: String, argb: Long)

    /** Adds freehand ink strokes. [strokeWidth] in normalized page-width units. */
    suspend fun addInk(pageIndex: Int, strokes: List<InkStroke>, argb: Long, strokeWidth: Float)

    /** Deletes the annotation at [annotIndex] (from [annotations]). */
    suspend fun deleteAnnotation(pageIndex: Int, annotIndex: Int)

    /** Writes the current document state (including edits) to [filePath]. */
    suspend fun saveTo(filePath: String)

    override fun close()
}
