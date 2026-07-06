package app.openpdf.foss.core.util

/**
 * Parses user page-range input like "1-3, 7, 9-12" into zero-based page
 * indices, clamped to [pageCount]. Returns null when nothing valid remains.
 */
fun parsePageRanges(input: String, pageCount: Int): List<Int>? {
    val result = mutableListOf<Int>()
    input.split(',').forEach { raw ->
        val part = raw.trim()
        if (part.isEmpty()) return@forEach
        val bounds = part.split('-').map { it.trim() }
        when (bounds.size) {
            1 -> bounds[0].toIntOrNull()?.let { page ->
                if (page in 1..pageCount) result.add(page - 1)
            }

            2 -> {
                val start = bounds[0].toIntOrNull()
                val end = bounds[1].toIntOrNull()
                if (start != null && end != null && start <= end) {
                    (start.coerceAtLeast(1)..end.coerceAtMost(pageCount)).forEach {
                        result.add(it - 1)
                    }
                }
            }
        }
    }
    return result.ifEmpty { null }
}
