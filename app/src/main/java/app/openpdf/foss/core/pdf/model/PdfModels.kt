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
