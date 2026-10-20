package com.chatai.app.domain.model

enum class SupportedLanguage(
    val displayName: String,
    val runtimePrefix: String,
    val starterCode: String
) {
    PYTHON(
        displayName = "Python",
        runtimePrefix = "Python (3",
        starterCode = """name = input().strip()
print(f"Hello, {name}!")"""
    ),
    JAVA(
        displayName = "Java",
        runtimePrefix = "Java (",
        starterCode = """import java.util.Scanner;

class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String name = scanner.nextLine().trim();
        System.out.println("Hello, " + name + "!");
    }
}"""
    ),
    C(
        displayName = "C",
        runtimePrefix = "C (",
        starterCode = """#include <stdio.h>

int main(void) {
    char name[100];
    scanf("%99s", name);
    printf("Hello, %s!\n", name);
    return 0;
}"""
    ),
    CPP(
        displayName = "C++",
        runtimePrefix = "C++ (",
        starterCode = """#include <iostream>
#include <string>
using namespace std;

int main() {
    string name;
    getline(cin, name);
    cout << "Hello, " << name << "!\n";
    return 0;
}"""
    ),
    JAVASCRIPT(
        displayName = "JavaScript",
        runtimePrefix = "JavaScript (",
        starterCode = """const fs = require('fs');
const name = fs.readFileSync(0, 'utf8').trim();
console.log(`Hello, ${'$'}{name}!`);"""
    ),
    KOTLIN(
        displayName = "Kotlin",
        runtimePrefix = "Kotlin (",
        starterCode = """fun main() {
    val name = readln().trim()
    println("Hello, ${'$'}name!")
}"""
    )
}

data class CodeTestCase(
    val name: String,
    val input: String,
    val expectedOutput: String
)

data class CodeExecutionResult(
    val statusId: Int,
    val status: String,
    val stdout: String,
    val stderr: String,
    val compileOutput: String,
    val timeSeconds: String?,
    val memoryKb: Long?
) {
    val isSuccessful: Boolean get() = statusId == 3

    val visibleOutput: String
        get() = stdout.ifBlank {
            compileOutput.ifBlank {
                stderr.ifBlank { status }
            }
        }
}

data class TestExecutionResult(
    val testCase: CodeTestCase,
    val actualOutput: String,
    val passed: Boolean,
    val executionStatus: String
)

enum class VerificationStatus {
    NOT_RUN,
    VERIFIED,
    FAILED,
    ERROR
}

data class VerificationResult(
    val status: VerificationStatus,
    val tests: List<TestExecutionResult>
) {
    val passedCount: Int get() = tests.count { it.passed }
    val firstFailure: TestExecutionResult? get() = tests.firstOrNull { !it.passed }
}

data class CodeAttempt(
    val id: String,
    val language: SupportedLanguage,
    val status: VerificationStatus,
    val passedTests: Int,
    val totalTests: Int,
    val createdAt: Long
)
