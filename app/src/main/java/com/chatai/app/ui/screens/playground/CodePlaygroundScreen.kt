package com.chatai.app.ui.screens.playground

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chatai.app.domain.model.CodeAttempt
import com.chatai.app.domain.model.CodeExecutionResult
import com.chatai.app.domain.model.SupportedLanguage
import com.chatai.app.domain.model.TestExecutionResult
import com.chatai.app.domain.model.VerificationResult
import com.chatai.app.domain.model.VerificationStatus
import com.chatai.app.ui.theme.ChatColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodePlaygroundScreen(
    onBack: () -> Unit,
    viewModel: CodePlaygroundViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val recentAttempts by viewModel.recentAttempts.collectAsState()
    val dailyUsage by viewModel.dailyUsage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        containerColor = ChatColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Verification Lab", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Run it. Test it. Trust the result.",
                            color = ChatColors.TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ChatColors.SidebarBackground,
                    titleContentColor = ChatColors.TextPrimary,
                    navigationIconContentColor = ChatColors.TextPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                IntroCard(state.verificationResult)
            }
            item {
                LanguageSelector(
                    selected = state.language,
                    enabled = !state.isWorking,
                    onSelected = viewModel::updateLanguage
                )
            }
            item {
                LabeledEditor(
                    label = "What should this program do?",
                    value = state.problemDescription,
                    onValueChange = viewModel::updateProblemDescription,
                    minLines = 2,
                    maxLines = 5,
                    enabled = !state.isWorking
                )
            }
            item {
                LabeledEditor(
                    label = "Source code  ${state.sourceCode.length}/8000",
                    value = state.sourceCode,
                    onValueChange = viewModel::updateSourceCode,
                    minLines = 11,
                    maxLines = 20,
                    enabled = !state.isWorking,
                    monospace = true
                )
            }
            item {
                LabeledEditor(
                    label = "Standard input",
                    value = state.stdin,
                    onValueChange = viewModel::updateStdin,
                    minLines = 2,
                    maxLines = 5,
                    enabled = !state.isWorking,
                    monospace = true
                )
            }
            item {
                ActionPanel(
                    isWorking = state.isWorking,
                    activityLabel = state.activityLabel,
                    remainingAiRequests = dailyUsage.remaining,
                    onRun = viewModel::runCode,
                    onVerify = viewModel::generateTestsAndVerify
                )
            }
            state.error?.let { error ->
                item {
                    PersistentErrorCard(error = error, onDismiss = viewModel::clearError)
                }
            }
            state.executionResult?.let { result ->
                item { ExecutionCard(result) }
            }
            state.verificationResult?.let { result ->
                item { VerificationSummary(result) }
                items(result.tests) { testResult ->
                    TestResultCard(testResult)
                }
            }
            if (recentAttempts.isNotEmpty()) {
                item {
                    Text(
                        "Recent attempts",
                        color = ChatColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp
                    )
                }
                items(recentAttempts.take(5), key = { it.id }) { attempt ->
                    AttemptRow(attempt)
                }
            }
        }
    }
}

@Composable
private fun PersistentErrorCard(error: String, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ChatColors.Error.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = ChatColors.Error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.size(10.dp))
            Text(
                error,
                color = ChatColors.TextPrimary,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = ChatColors.Error)
            }
        }
    }
}

@Composable
private fun IntroCard(result: VerificationResult?) {
    val status = result?.status ?: VerificationStatus.NOT_RUN
    val accent = when (status) {
        VerificationStatus.VERIFIED -> ChatColors.Accent
        VerificationStatus.FAILED, VerificationStatus.ERROR -> ChatColors.Error
        VerificationStatus.NOT_RUN -> Color(0xFF6D5DFB)
    }
    val title = when (status) {
        VerificationStatus.VERIFIED -> "Verified by execution"
        VerificationStatus.FAILED -> "Counterexample found"
        VerificationStatus.ERROR -> "Verification error"
        VerificationStatus.NOT_RUN -> "Evidence, not guesses"
    }
    val subtitle = when (status) {
        VerificationStatus.VERIFIED -> "All ${result?.tests?.size.orZero()} generated tests passed in Judge0."
        VerificationStatus.FAILED -> "${result?.passedCount ?: 0}/${result?.tests?.size.orZero()} tests passed. Review the first failure below."
        VerificationStatus.ERROR -> "The test run could not produce a reliable result."
        VerificationStatus.NOT_RUN -> "AI designs the tests; an isolated sandbox decides whether the code passes."
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = ChatColors.Surface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(accent.copy(alpha = 0.18f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (status == VerificationStatus.VERIFIED) {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.Default.Science
                    },
                    contentDescription = null,
                    tint = accent
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = ChatColors.TextPrimary, fontWeight = FontWeight.Bold)
                Text(subtitle, color = ChatColors.TextSecondary, fontSize = 13.sp)
            }
        }
    }
}

private fun Int?.orZero(): Int = this ?: 0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSelector(
    selected: SupportedLanguage,
    enabled: Boolean,
    onSelected: (SupportedLanguage) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text("Language") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            colors = playgroundTextFieldColors(),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(ChatColors.Surface)
        ) {
            SupportedLanguage.entries.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language.displayName, color = ChatColors.TextPrimary) },
                    onClick = {
                        expanded = false
                        onSelected(language)
                    }
                )
            }
        }
    }
}

@Composable
private fun LabeledEditor(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    minLines: Int,
    maxLines: Int,
    enabled: Boolean,
    monospace: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        minLines = minLines,
        maxLines = maxLines,
        enabled = enabled,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = ChatColors.CodeText,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            lineHeight = 20.sp
        ),
        colors = playgroundTextFieldColors(),
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
private fun playgroundTextFieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedTextColor = ChatColors.TextPrimary,
    unfocusedTextColor = ChatColors.TextPrimary,
    disabledTextColor = ChatColors.TextSecondary,
    focusedContainerColor = ChatColors.CodeBackground,
    unfocusedContainerColor = ChatColors.CodeBackground,
    disabledContainerColor = ChatColors.CodeBackground,
    focusedBorderColor = ChatColors.Accent,
    unfocusedBorderColor = ChatColors.Border,
    focusedLabelColor = ChatColors.Accent,
    unfocusedLabelColor = ChatColors.TextSecondary,
    cursorColor = ChatColors.Accent
)

@Composable
private fun ActionPanel(
    isWorking: Boolean,
    activityLabel: String,
    remainingAiRequests: Int,
    onRun: () -> Unit,
    onVerify: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onRun,
                enabled = !isWorking,
                colors = ButtonDefaults.buttonColors(containerColor = ChatColors.Surface),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text("Run once")
            }
            Button(
                onClick = onVerify,
                enabled = !isWorking && remainingAiRequests > 0,
                colors = ButtonDefaults.buttonColors(containerColor = ChatColors.Accent),
                modifier = Modifier.weight(1.35f)
            ) {
                Icon(Icons.Default.Science, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text("Generate & verify")
            }
        }
        if (isWorking) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = ChatColors.Accent,
                    strokeWidth = 2.dp
                )
                Text(activityLabel, color = ChatColors.TextSecondary, fontSize = 13.sp)
            }
        } else {
            Text(
                if (remainingAiRequests > 0) {
                    "$remainingAiRequests/10 AI requests remaining. Sandbox runs do not use this allowance."
                } else {
                    "Daily AI limit reached. You can still run code in the sandbox."
                },
                color = ChatColors.TextTertiary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ExecutionCard(result: CodeExecutionResult) {
    ResultContainer(title = "Execution: ${result.status}", successful = result.isSuccessful) {
        CodeBlock(result.visibleOutput)
        if (result.timeSeconds != null || result.memoryKb != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Time ${result.timeSeconds ?: "-"}s  |  Memory ${result.memoryKb ?: 0} KB",
                color = ChatColors.TextTertiary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun VerificationSummary(result: VerificationResult) {
    val successful = result.status == VerificationStatus.VERIFIED
    val title = if (successful) {
        "Verified: ${result.passedCount}/${result.tests.size} passed"
    } else {
        "Not verified: ${result.passedCount}/${result.tests.size} passed"
    }
    ResultContainer(title = title, successful = successful) {
        result.firstFailure?.let { failure ->
            Text(
                "First failing counterexample",
                color = ChatColors.Warning,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(6.dp))
            Text("Input", color = ChatColors.TextTertiary, fontSize = 11.sp)
            CodeBlock(failure.testCase.input.ifEmpty { "<empty input>" })
            Spacer(Modifier.height(6.dp))
            Text("Expected", color = ChatColors.TextTertiary, fontSize = 11.sp)
            CodeBlock(failure.testCase.expectedOutput)
            Spacer(Modifier.height(6.dp))
            Text("Actual", color = ChatColors.TextTertiary, fontSize = 11.sp)
            CodeBlock(failure.actualOutput)
        }
    }
}

@Composable
private fun TestResultCard(result: TestExecutionResult) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ChatColors.Surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (result.passed) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (result.passed) ChatColors.Accent else ChatColors.Error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    result.testCase.name,
                    color = ChatColors.TextPrimary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (result.passed) "PASS" else "FAIL",
                    color = if (result.passed) ChatColors.Accent else ChatColors.Error,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (!result.passed) {
                Text(result.executionStatus, color = ChatColors.TextTertiary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ResultContainer(
    title: String,
    successful: Boolean,
    content: @Composable () -> Unit
) {
    val accent = if (successful) ChatColors.Accent else ChatColors.Error
    Card(
        colors = CardDefaults.cardColors(containerColor = ChatColors.Surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(title, color = accent, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun CodeBlock(value: String) {
    Surface(
        color = ChatColors.CodeBackground,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            value.ifBlank { "<no output>" },
            color = ChatColors.CodeText,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.padding(10.dp)
        )
    }
}

@Composable
private fun AttemptRow(attempt: CodeAttempt) {
    val color = when (attempt.status) {
        VerificationStatus.VERIFIED -> ChatColors.Accent
        VerificationStatus.FAILED, VerificationStatus.ERROR -> ChatColors.Error
        VerificationStatus.NOT_RUN -> ChatColors.TextTertiary
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = ChatColors.Surface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(attempt.language.displayName, color = ChatColors.TextPrimary, fontWeight = FontWeight.Medium)
                Text(
                    SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(attempt.createdAt)),
                    color = ChatColors.TextTertiary,
                    fontSize = 11.sp
                )
            }
            Text(
                if (attempt.status == VerificationStatus.NOT_RUN) {
                    "RUN"
                } else {
                    "${attempt.passedTests}/${attempt.totalTests} ${attempt.status.name}"
                },
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}
