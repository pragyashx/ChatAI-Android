package com.chatai.app.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OutputComparatorTest {
    @Test
    fun matches_ignoresLineEndingsAndTrailingWhitespace() {
        assertTrue(OutputComparator.matches("hello  \r\nworld\r\n", "hello\nworld"))
    }

    @Test
    fun matches_preservesMeaningfulInternalWhitespace() {
        assertFalse(OutputComparator.matches("hello world", "helloworld"))
    }

    @Test
    fun matches_ignoresBlankEdges() {
        assertTrue(OutputComparator.matches("\n42\n", "42"))
    }
}
