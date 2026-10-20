package com.chatai.app.ui.screens.playground

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chatai.app.BuildConfig
import com.chatai.app.ChatApplication
import com.chatai.app.data.local.preferences.DailyUsage
import com.chatai.app.data.local.preferences.DailyUsagePolicy
import com.chatai.app.domain.model.CodeAttempt
import com.chatai.app.domain.model.CodeExecutionResult
import com.chatai.app.domain.model.CodeTestCase
import com.chatai.app.domain.model.SupportedLanguage
import com.chatai.app.domain.model.VerificationResult
import com.chatai.app.domain.model.VerificationStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

data class PlaygroundUiState(
    val language: SupportedLanguage = SupportedLanguage.PYTHON,
    val problemDescription: String = "Read a name from standard input and print Hello, followed by the name and an exclamation mark.",
    val sourceCode: String = SupportedLanguage.PYTHON.starterCode,
    val stdin: String = "Ada",
    val isWorking: Boolean = false,
    val activityLabel: String = "",
    val executionResult: CodeExecutionResult? = null,
    val generatedTests: List<CodeTestCase> = emptyList(),
    val verificationResult: VerificationResult? = null,
    val error: String? = null
)

class CodePlaygroundViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as ChatApplication).container
    private val repository = container.playgroundRepository
    private val usageLimitRepository = container.usageLimitRepository
    private val apiKey = BuildConfig.OPENROUTER_API_KEY

    private val _uiState = MutableStateFlow(PlaygroundUiState())
    val uiState: StateFlow<PlaygroundUiState> = _uiState.asStateFlow()

    val recentAttempts: StateFlow<List<CodeAttempt>> = repository.observeRecentAttempts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dailyUsage: StateFlow<DailyUsage> = usageLimitRepository.usage
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DailyUsage(used = 0)
        )

    fun updateLanguage(language: SupportedLanguage) {
        val current = _uiState.value
        val currentIsStarter = SupportedLanguage.entries.any { it.starterCode == current.sourceCode }
        _uiState.value = current.copy(
            language = language,
            sourceCode = if (current.sourceCode.isBlank() || currentIsStarter) {
                language.starterCode
            } else {
                current.sourceCode
            },
            executionResult = null,
            verificationResult = null
        )
    }

    fun updateProblemDescription(value: String) {
        _uiState.value = _uiState.value.copy(
            problemDescription = value.take(MAX_PROBLEM_LENGTH),
            verificationResult = null
        )
    }

    fun updateSourceCode(value: String) {
        _uiState.value = _uiState.value.copy(
            sourceCode = value.take(MAX_SOURCE_LENGTH),
            executionResult = null,
            verificationResult = null
        )
    }

    fun updateStdin(value: String) {
        _uiState.value = _uiState.value.copy(stdin = value.take(MAX_STDIN_LENGTH))
    }

    fun runCode() {
        val snapshot = _uiState.value
        if (!validateSource(snapshot.sourceCode)) return

        viewModelScope.launch {
            _uiState.value = snapshot.copy(
                isWorking = true,
                activityLabel = "Running in Judge0 sandbox...",
                executionResult = null,
                verificationResult = null,
                error = null
            )
            try {
                val result = repository.runCode(
                    language = snapshot.language,
                    sourceCode = snapshot.sourceCode,
                    stdin = snapshot.stdin
                )
                _uiState.value = _uiState.value.copy(executionResult = result)
                repository.saveAttempt(
                    language = snapshot.language,
                    sourceCode = snapshot.sourceCode,
                    problemDescription = snapshot.problemDescription,
                    result = VerificationResult(VerificationStatus.NOT_RUN, emptyList()),
                    details = result.status
                )
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = error.message ?: "Code execution failed"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isWorking = false, activityLabel = "")
            }
        }
    }

    fun generateTestsAndVerify() {
        val snapshot = _uiState.value
        if (!validateSource(snapshot.sourceCode)) return
        if (snapshot.problemDescription.isBlank()) {
            _uiState.value = snapshot.copy(error = "Describe what the program should do before verification")
            return
        }
        if (apiKey.isBlank()) {
            _uiState.value = snapshot.copy(error = "Set OPENROUTER_API_KEY to generate tests")
            return
        }

        viewModelScope.launch {
            if (!usageLimitRepository.tryConsume()) {
                _uiState.value = snapshot.copy(
                    error = "Daily AI limit reached. Your 10 requests reset tomorrow."
                )
                return@launch
            }
            _uiState.value = snapshot.copy(
                isWorking = true,
                activityLabel = "AI is designing edge cases...",
                executionResult = null,
                generatedTests = emptyList(),
                verificationResult = null,
                error = null
            )
            try {
                val tests = repository.generateTestCases(
                    apiKey = apiKey,
                    language = snapshot.language,
                    problemDescription = snapshot.problemDescription,
                    sourceCode = snapshot.sourceCode
                )
                _uiState.value = _uiState.value.copy(
                    generatedTests = tests,
                    activityLabel = "Executing ${tests.size} generated tests..."
                )
                val verification = repository.verify(
                    language = snapshot.language,
                    sourceCode = snapshot.sourceCode,
                    tests = tests
                )
                _uiState.value = _uiState.value.copy(verificationResult = verification)
                repository.saveAttempt(
                    language = snapshot.language,
                    sourceCode = snapshot.sourceCode,
                    problemDescription = snapshot.problemDescription,
                    result = verification
                )
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = error.message ?: "Verification failed"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isWorking = false, activityLabel = "")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun validateSource(sourceCode: String): Boolean {
        if (sourceCode.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Enter some code first")
            return false
        }
        return true
    }

    private companion object {
        const val MAX_SOURCE_LENGTH = DailyUsagePolicy.MAX_INPUT_CHARACTERS
        const val MAX_PROBLEM_LENGTH = 2_000
        const val MAX_STDIN_LENGTH = 4_000
    }
}
