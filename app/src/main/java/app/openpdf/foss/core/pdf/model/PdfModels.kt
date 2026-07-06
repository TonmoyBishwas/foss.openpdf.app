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

/** Text markup annotation kinds. */
enum class MarkupType { HIGHLIGHT, UNDERLINE, STRIKEOUT }

/**
 * An existing annotation on a page. [index] is its position in the page's
 * annotation array and is only valid until the page is next mutated.
 */
data class PageAnnotation(
    val index: Int,
    val typeName: String,
    val rects: List<NormalizedRect>,
    val contents: String,
)

/** One freehand stroke in normalized page coordinates. */
data class InkStroke(val points: List<Pair<Float, Float>>)
