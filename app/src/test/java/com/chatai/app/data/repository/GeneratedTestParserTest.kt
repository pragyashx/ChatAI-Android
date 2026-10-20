package com.chatai.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GeneratedTestParserTest {
    @Test
    fun parse_extractsJsonArrayFromMarkdownResponse() {
        val response = """Here are the tests:
            |```json
            |[
            |  {"name":"Normal","input":"Ada\n","expectedOutput":"Hello, Ada!\n"},
            |  {"name":"Empty","input":"\n","expectedOutput":"Hello, !\n"}
            |]
            |```
        """.trimMargin()

        val tests = GeneratedTestParser.parse(response)

        assertEquals(2, tests.size)
        assertEquals("Normal", tests.first().name)
        assertEquals("Ada\n", tests.first().input)
    }

    @Test
    fun parse_capsGeneratedTestsAtFive() {
        val items = (1..7).joinToString(",") {
            "{\"name\":\"Test $it\",\"input\":\"$it\",\"expectedOutput\":\"$it\"}"
        }

        assertEquals(5, GeneratedTestParser.parse("[$items]").size)
    }

    @Test
    fun parse_rejectsMissingExpectedOutput() {
        assertThrows(IllegalArgumentException::class.java) {
            GeneratedTestParser.parse("[{\"name\":\"Bad\",\"input\":\"1\"}]")
        }
    }
}
