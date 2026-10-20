package com.chatai.app.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportedLanguageTest {
    @Test
    fun starterCode_doesNotContainEscapedQuoteLiterals() {
        SupportedLanguage.entries.forEach { language ->
            assertFalse(
                "${language.displayName} starter contains an escaped quote literal",
                language.starterCode.contains("\\\"")
            )
        }
    }

    @Test
    fun pythonStarter_readsInputAndPrintsGreeting() {
        assertTrue(SupportedLanguage.PYTHON.starterCode.contains("input()"))
        assertTrue(SupportedLanguage.PYTHON.starterCode.contains("print(f\"Hello, {name}!\")"))
    }
}
