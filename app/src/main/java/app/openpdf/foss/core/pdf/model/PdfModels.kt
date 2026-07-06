package app.openpdf.foss.core.pdf.model

/** Page dimensions in PDF points (1/72 inch). */
data class PageSize(val width: Float, val height: Float) {
    val aspectRatio: Float get() = if (height == 0f) 1f else width / height
}

/** One entry of the document outline (table of contents). */
data class OutlineNode(
    val title: String,
    val pageIndex: Int,
    val children: List<OutlineNode> = emptyList(),
)

/** Axis-aligned rectangle in normalized page coordinates (0..1, top-left origin). */
data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

/** One search match on a page. */
data class SearchHit(
    val pageIndex: Int,
    val rects: List<NormalizedRect>,
)

/** A text selection on a page: the text and the rects that cover it. */
data class TextSelection(
    val text: String,
    val rects: List<NormalizedRect>,
)
