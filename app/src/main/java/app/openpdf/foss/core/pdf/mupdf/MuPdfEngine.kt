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
import app.openpdf.foss.core.pdf.model.FormField
import app.openpdf.foss.core.pdf.model.FormFieldType
import app.openpdf.foss.core.pdf.model.InkStroke
import app.openpdf.foss.core.pdf.model.MarkupType
import app.openpdf.foss.core.pdf.model.PageAnnotation
import app.openpdf.foss.core.pdf.model.ShapeType
import com.artifex.mupdf.fitz.PDFWidget
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.Outline
import com.artifex.mupdf.fitz.PDFAnnotation
import com.artifex.mupdf.fitz.PDFDocument
import com.artifex.mupdf.fitz.PDFPage
import com.artifex.mupdf.fitz.Page
import com.artifex.mupdf.fitz.Point
import com.artifex.mupdf.fitz.Quad
import com.artifex.mupdf.fitz.Rect
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

    // --- Editing ---

    override val isEditable: Boolean = document is PDFDocument

    @Volatile
    override var contentVersion: Int = 0
        private set

    private fun markMutated() {
        contentVersion++
    }

    private inline fun <T> withPdfPage(pageIndex: Int, block: (PDFPage) -> T): T =
        withPage(pageIndex) { page ->
            block(page as? PDFPage ?: error("Document is not editable"))
        }

    private fun Long.toRgb(): FloatArray = floatArrayOf(
        ((this shr 16) and 0xFF) / 255f,
        ((this shr 8) and 0xFF) / 255f,
        (this and 0xFF) / 255f,
    )

    override suspend fun annotations(pageIndex: Int): List<PageAnnotation> =
        withContext(dispatcher) {
            withPdfPage(pageIndex) { page ->
                val bounds = page.bounds
                val annots = page.annotations ?: emptyArray()
                annots.mapIndexed { index, annot ->
                    PageAnnotation(
                        index = index,
                        typeName = annotTypeName(annot.type),
                        rects = listOf(annot.rect.toNormalizedRect(bounds)),
                        contents = annot.contents ?: "",
                    )
                }
            }
        }

    private fun annotTypeName(type: Int): String = when (type) {
        PDFAnnotation.TYPE_HIGHLIGHT -> "Highlight"
        PDFAnnotation.TYPE_UNDERLINE -> "Underline"
        PDFAnnotation.TYPE_STRIKE_OUT -> "Strikethrough"
        PDFAnnotation.TYPE_TEXT -> "Note"
        PDFAnnotation.TYPE_INK -> "Ink"
        PDFAnnotation.TYPE_FREE_TEXT -> "Text box"
        PDFAnnotation.TYPE_SQUARE -> "Rectangle"
        PDFAnnotation.TYPE_CIRCLE -> "Ellipse"
        PDFAnnotation.TYPE_LINE -> "Line"
        else -> "Annotation"
    }

    private fun Rect.toNormalizedRect(bounds: Rect): NormalizedRect {
        val width = bounds.x1 - bounds.x0
        val height = bounds.y1 - bounds.y0
        return NormalizedRect(
            left = ((x0 - bounds.x0) / width).coerceIn(0f, 1f),
            top = ((y0 - bounds.y0) / height).coerceIn(0f, 1f),
            right = ((x1 - bounds.x0) / width).coerceIn(0f, 1f),
            bottom = ((y1 - bounds.y0) / height).coerceIn(0f, 1f),
        )
    }

    override suspend fun addTextMarkup(
        pageIndex: Int,
        type: MarkupType,
        rects: List<NormalizedRect>,
        argb: Long,
    ) = withContext(dispatcher) {
        withPdfPage(pageIndex) { page ->
            val bounds = page.bounds
            val w = bounds.x1 - bounds.x0
            val h = bounds.y1 - bounds.y0
            val annotType = when (type) {
                MarkupType.HIGHLIGHT -> PDFAnnotation.TYPE_HIGHLIGHT
                MarkupType.UNDERLINE -> PDFAnnotation.TYPE_UNDERLINE
                MarkupType.STRIKEOUT -> PDFAnnotation.TYPE_STRIKE_OUT
            }
            val annot = page.createAnnotation(annotType)
            val quads = rects.map { r ->
                val x0 = bounds.x0 + r.left * w
                val x1 = bounds.x0 + r.right * w
                val y0 = bounds.y0 + r.top * h
                val y1 = bounds.y0 + r.bottom * h
                Quad(x0, y0, x1, y0, x0, y1, x1, y1)
            }.toTypedArray()
            annot.quadPoints = quads
            annot.color = argb.toRgb()
            annot.update()
            page.update()
        }
        markMutated()
    }

    override suspend fun addNote(
        pageIndex: Int,
        x: Float,
        y: Float,
        contents: String,
        argb: Long,
    ) = withContext(dispatcher) {
        withPdfPage(pageIndex) { page ->
            val bounds = page.bounds
            val w = bounds.x1 - bounds.x0
            val h = bounds.y1 - bounds.y0
            val cx = bounds.x0 + x * w
            val cy = bounds.y0 + y * h
            val annot = page.createAnnotation(PDFAnnotation.TYPE_TEXT)
            annot.rect = Rect(cx, cy, cx + 24f, cy + 24f)
            annot.contents = contents
            annot.color = argb.toRgb()
            annot.update()
            page.update()
        }
        markMutated()
    }

    override suspend fun addInk(
        pageIndex: Int,
        strokes: List<InkStroke>,
        argb: Long,
        strokeWidth: Float,
    ) = withContext(dispatcher) {
        withPdfPage(pageIndex) { page ->
            val bounds = page.bounds
            val w = bounds.x1 - bounds.x0
            val h = bounds.y1 - bounds.y0
            val annot = page.createAnnotation(PDFAnnotation.TYPE_INK)
            val inkList = strokes.map { stroke ->
                stroke.points.map { (px, py) ->
                    Point(bounds.x0 + px * w, bounds.y0 + py * h)
                }.toTypedArray()
            }.toTypedArray()
            annot.inkList = inkList
            annot.color = argb.toRgb()
            annot.border = strokeWidth * w
            annot.update()
            page.update()
        }
        markMutated()
    }

    override suspend fun deleteAnnotation(pageIndex: Int, annotIndex: Int) =
        withContext(dispatcher) {
            withPdfPage(pageIndex) { page ->
                val annots = page.annotations ?: emptyArray()
                annots.getOrNull(annotIndex)?.let { annot ->
                    page.deleteAnnotation(annot)
                    page.update()
                }
            }
            markMutated()
        }

    override suspend fun addFreeText(
        pageIndex: Int,
        rect: NormalizedRect,
        text: String,
        fontSize: Float,
        argb: Long,
    ) = withContext(dispatcher) {
        withPdfPage(pageIndex) { page ->
            val bounds = page.bounds
            val w = bounds.x1 - bounds.x0
            val h = bounds.y1 - bounds.y0
            val annot = page.createAnnotation(PDFAnnotation.TYPE_FREE_TEXT)
            annot.rect = Rect(
                bounds.x0 + rect.left * w,
                bounds.y0 + rect.top * h,
                bounds.x0 + rect.right * w,
                bounds.y0 + rect.bottom * h,
            )
            annot.setDefaultAppearance("Helv", fontSize, argb.toRgb())
            annot.contents = text
            annot.update()
            page.update()
        }
        markMutated()
    }

    override suspend fun addShape(
        pageIndex: Int,
        type: ShapeType,
        rect: NormalizedRect,
        argb: Long,
        strokeWidth: Float,
    ) = withContext(dispatcher) {
        withPdfPage(pageIndex) { page ->
            val bounds = page.bounds
            val w = bounds.x1 - bounds.x0
            val h = bounds.y1 - bounds.y0
            val x0 = bounds.x0 + rect.left * w
            val y0 = bounds.y0 + rect.top * h
            val x1 = bounds.x0 + rect.right * w
            val y1 = bounds.y0 + rect.bottom * h
            when (type) {
                ShapeType.RECTANGLE, ShapeType.ELLIPSE -> {
                    val annot = page.createAnnotation(
                        if (type == ShapeType.RECTANGLE) PDFAnnotation.TYPE_SQUARE
                        else PDFAnnotation.TYPE_CIRCLE
                    )
                    annot.rect = Rect(x0, y0, x1, y1)
                    annot.color = argb.toRgb()
                    annot.border = strokeWidth * w
                    annot.update()
                }

                ShapeType.LINE -> {
                    val annot = page.createAnnotation(PDFAnnotation.TYPE_LINE)
                    annot.setLine(Point(x0, y0), Point(x1, y1))
                    annot.color = argb.toRgb()
                    annot.border = strokeWidth * w
                    annot.update()
                }
            }
            page.update()
        }
        markMutated()
    }

    override suspend fun formFields(pageIndex: Int): List<FormField> =
        withContext(dispatcher) {
            withPdfPage(pageIndex) { page ->
                val bounds = page.bounds
                val widgets = page.widgets ?: emptyArray()
                widgets.mapIndexed { index, widget ->
                    FormField(
                        index = index,
                        type = when (widget.fieldType) {
                            PDFWidget.TYPE_TEXT -> FormFieldType.TEXT
                            PDFWidget.TYPE_CHECKBOX -> FormFieldType.CHECKBOX
                            PDFWidget.TYPE_RADIOBUTTON -> FormFieldType.RADIO
                            PDFWidget.TYPE_COMBOBOX -> FormFieldType.COMBO
                            PDFWidget.TYPE_LISTBOX -> FormFieldType.LIST
                            PDFWidget.TYPE_SIGNATURE -> FormFieldType.SIGNATURE
                            PDFWidget.TYPE_BUTTON -> FormFieldType.BUTTON
                            else -> FormFieldType.UNKNOWN
                        },
                        rect = widget.rect.toNormalizedRect(bounds),
                        value = widget.value ?: "",
                        options = widget.options?.toList() ?: emptyList(),
                    )
                }
            }
        }

    override suspend fun setFormFieldValue(
        pageIndex: Int,
        fieldIndex: Int,
        value: String,
    ): Boolean = withContext(dispatcher) {
        val ok = withPdfPage(pageIndex) { page ->
            val widget = (page.widgets ?: emptyArray()).getOrNull(fieldIndex)
                ?: return@withPdfPage false
            val accepted = widget.setValue(value)
            widget.update()
            page.update()
            accepted
        }
        if (ok) markMutated()
        ok
    }

    override suspend fun toggleFormField(pageIndex: Int, fieldIndex: Int): Unit =
        withContext(dispatcher) {
            withPdfPage(pageIndex) { page ->
                val widget = (page.widgets ?: emptyArray()).getOrNull(fieldIndex)
                    ?: return@withPdfPage
                widget.toggle()
                widget.update()
                page.update()
            }
            markMutated()
        }

    override suspend fun saveTo(filePath: String): Unit = withContext(dispatcher) {
        val pdf = document as? PDFDocument ?: error("Document is not editable")
        pdf.save(filePath, "compress")
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
