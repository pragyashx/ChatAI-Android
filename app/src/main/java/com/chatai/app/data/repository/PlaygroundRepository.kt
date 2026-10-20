package com.chatai.app.data.repository

import com.chatai.app.data.local.dao.CodeAttemptDao
import com.chatai.app.data.local.entity.CodeAttemptEntity
import com.chatai.app.data.remote.Judge0Api
import com.chatai.app.data.remote.OpenRouterApi
import com.chatai.app.data.remote.dto.MessageDto
import com.chatai.app.domain.model.CodeAttempt
import com.chatai.app.domain.model.CodeExecutionResult
import com.chatai.app.domain.model.CodeTestCase
import com.chatai.app.domain.model.SupportedLanguage
import com.chatai.app.domain.model.TestExecutionResult
import com.chatai.app.domain.model.VerificationResult
import com.chatai.app.domain.model.VerificationStatus
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.UUID

class PlaygroundRepository(
    private val attemptDao: CodeAttemptDao,
    private val judge0Api: Judge0Api,
    private val openRouterApi: OpenRouterApi
) {
    fun observeRecentAttempts(): Flow<List<CodeAttempt>> =
        attemptDao.observeRecentAttempts(10)
            .map { attempts -> attempts.map { it.toDomainModel() } }
            .flowOn(Dispatchers.IO)

    suspend fun runCode(
        language: SupportedLanguage,
        sourceCode: String,
        stdin: String
    ): CodeExecutionResult = judge0Api.execute(language, sourceCode, stdin)

    suspend fun generateTestCases(
        apiKey: String,
        language: SupportedLanguage,
        problemDescription: String,
        sourceCode: String
    ): List<CodeTestCase> {
        val prompt = """Create exactly 5 useful black-box test cases for this programming task.

Language: ${language.displayName}
Task: $problemDescription

Code:
```text
$sourceCode
```

Return only a JSON array. Each item must have string fields "name", "input", and "expectedOutput". The input is the exact stdin and expectedOutput is the exact stdout. Include normal, boundary, and edge cases. Do not include Markdown or explanations."""

        val response = StringBuilder()
        openRouterApi.sendMessageStream(
            apiKey = apiKey,
            messages = listOf(
                MessageDto(
                    role = "system",
                    content = "You generate deterministic executable test cases. Return valid JSON only."
                ),
                MessageDto(role = "user", content = prompt)
            )
        ).collect { response.append(it) }

        return GeneratedTestParser.parse(response.toString())
    }

    suspend fun verify(
        language: SupportedLanguage,
        sourceCode: String,
        tests: List<CodeTestCase>
    ): VerificationResult {
        val selectedTests = tests.take(MAX_TEST_CASES)
        val results = mutableListOf<TestExecutionResult>()
        for (test in selectedTests) {
            val execution = runCode(language, sourceCode, test.input)
            results += TestExecutionResult(
                testCase = test,
                actualOutput = execution.visibleOutput,
                passed = execution.isSuccessful && OutputComparator.matches(
                    expected = test.expectedOutput,
                    actual = execution.stdout
                ),
                executionStatus = execution.status
            )

            if (execution.statusId == COMPILATION_ERROR_STATUS) break
        }

        val status = when {
            results.isEmpty() -> VerificationStatus.ERROR
            results.all { it.passed } && results.size == selectedTests.size ->
                VerificationStatus.VERIFIED
            else -> VerificationStatus.FAILED
        }
        return VerificationResult(status = status, tests = results)
    }

    suspend fun saveAttempt(
        language: SupportedLanguage,
        sourceCode: String,
        problemDescription: String,
        result: VerificationResult,
        details: String = ""
    ) {
        val failure = result.firstFailure
        attemptDao.insertAttempt(
            CodeAttemptEntity(
                id = UUID.randomUUID().toString(),
                language = language.name,
                sourceCode = sourceCode,
                problemDescription = problemDescription,
                status = result.status.name,
                passedTests = result.passedCount,
                totalTests = result.tests.size,
                counterexampleInput = failure?.testCase?.input,
                expectedOutput = failure?.testCase?.expectedOutput,
                actualOutput = failure?.actualOutput,
                details = details,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    private companion object {
        const val MAX_TEST_CASES = 5
        const val COMPILATION_ERROR_STATUS = 6
    }
}

object GeneratedTestParser {
    fun parse(content: String): List<CodeTestCase> {
        val start = content.indexOf('[')
        val end = content.lastIndexOf(']')
        require(start >= 0 && end > start) { "AI did not return a JSON test-case array" }

        val array = JsonParser.parseString(content.substring(start, end + 1)).asJsonArray
        val tests = array.mapIndexed { index, element ->
            val item = element.asJsonObject
            CodeTestCase(
                name = item.get("name")?.asString?.trim().orEmpty().ifBlank { "Test ${index + 1}" },
                input = item.get("input")?.asString.orEmpty(),
                expectedOutput = item.get("expectedOutput")?.asString
                    ?: throw IllegalArgumentException("Test ${index + 1} has no expectedOutput")
            )
        }.take(5)

        require(tests.isNotEmpty()) { "AI returned no test cases" }
        return tests
    }
}

object OutputComparator {
    fun matches(expected: String, actual: String): Boolean = normalize(expected) == normalize(actual)

    internal fun normalize(value: String): String = value
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .lines()
        .joinToString("\n") { it.trimEnd() }
        .trim()
}
