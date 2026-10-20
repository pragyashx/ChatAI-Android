package com.chatai.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.chatai.app.domain.model.CodeAttempt
import com.chatai.app.domain.model.SupportedLanguage
import com.chatai.app.domain.model.VerificationStatus

@Entity(tableName = "code_attempts", indices = [Index("createdAt")])
data class CodeAttemptEntity(
    @PrimaryKey val id: String,
    val language: String,
    val sourceCode: String,
    val problemDescription: String,
    val status: String,
    val passedTests: Int,
    val totalTests: Int,
    val counterexampleInput: String?,
    val expectedOutput: String?,
    val actualOutput: String?,
    val details: String,
    val createdAt: Long
) {
    fun toDomainModel(): CodeAttempt = CodeAttempt(
        id = id,
        language = runCatching { SupportedLanguage.valueOf(language) }
            .getOrDefault(SupportedLanguage.PYTHON),
        status = runCatching { VerificationStatus.valueOf(status) }
            .getOrDefault(VerificationStatus.ERROR),
        passedTests = passedTests,
        totalTests = totalTests,
        createdAt = createdAt
    )
}
