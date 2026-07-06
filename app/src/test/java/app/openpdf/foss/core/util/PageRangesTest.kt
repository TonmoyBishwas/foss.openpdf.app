package app.openpdf.foss.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PageRangesTest {

    @Test
    fun `single pages parse to zero-based indices`() {
        assertEquals(listOf(0, 2, 4), parsePageRanges("1, 3, 5", 10))
    }

    @Test
    fun `ranges expand inclusively`() {
        assertEquals(listOf(1, 2, 3), parsePageRanges("2-4", 10))
    }

    @Test
    fun `mixed input with spaces`() {
        assertEquals(listOf(0, 1, 6), parsePageRanges(" 1-2 , 7 ", 10))
    }

    @Test
    fun `out of bounds pages are dropped or clamped`() {
        assertEquals(listOf(8, 9), parsePageRanges("9-15", 10))
        assertNull(parsePageRanges("42", 10))
    }

    @Test
    fun `garbage returns null`() {
        assertNull(parsePageRanges("abc", 10))
        assertNull(parsePageRanges("", 10))
        assertNull(parsePageRanges("5-2", 10))
    }

    @Test
    fun `duplicates are preserved`() {
        assertEquals(listOf(0, 0, 1), parsePageRanges("1,1,2", 10))
    }
}
