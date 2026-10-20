package com.chatai.app.data.remote

import com.chatai.app.domain.model.CodeExecutionResult
import com.chatai.app.domain.model.SupportedLanguage
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.Base64
import java.util.concurrent.TimeUnit

class Judge0Api(
    private val baseUrl: String = "https://ce.judge0.com",
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build(),
    private val gson: Gson = Gson()
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    @Volatile
    private var runtimes: List<Judge0Language>? = null

    suspend fun execute(
        language: SupportedLanguage,
        sourceCode: String,
        stdin: String
    ): CodeExecutionResult = withContext(Dispatchers.IO) {
        val submission = Judge0SubmissionRequest(
            languageId = resolveLanguageId(language),
            sourceCode = encode(sourceCode),
            stdin = encode(stdin)
        )
        val createRequest = Request.Builder()
            .url(
                "$baseUrl/submissions?base64_encoded=true&wait=true&fields=$RESULT_FIELDS"
            )
            .post(gson.toJson(submission).toRequestBody(jsonMediaType))
            .addHeader("Accept", "application/json")
            .build()

        val token = client.newCall(createRequest).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException(
                    "Judge0 submission failed (${response.code}): ${response.body?.string().orEmpty()}"
                )
            }
            val responseBody = response.body?.string().orEmpty()
            val json = JsonParser.parseString(responseBody).asJsonObject
            if (json.has("status")) {
                return@withContext gson.fromJson(json, Judge0Result::class.java).toDomainModel()
            }
            gson.fromJson(json, Judge0Token::class.java)?.token
                ?: throw IOException("Judge0 did not return a submission token")
        }

        var lastUnavailableReason: String? = null
        repeat(MAX_POLL_ATTEMPTS) {
            try {
                val result = fetchResult(token)
                if (result.status.id !in PROCESSING_STATUS_IDS) {
                    return@withContext result.toDomainModel()
                }
            } catch (error: ResultNotAvailableException) {
                lastUnavailableReason = error.message
            }
            delay(POLL_DELAY_MS)
        }

        val reason = lastUnavailableReason?.let { ": $it" }.orEmpty()
        throw IOException("Code execution timed out while waiting for Judge0$reason")
    }

    private fun resolveLanguageId(language: SupportedLanguage): Int {
        val available = runtimes ?: fetchLanguages().also { runtimes = it }
        return available
            .filter { it.name.startsWith(language.runtimePrefix, ignoreCase = true) }
            .maxByOrNull { it.id }
            ?.id
            ?: throw IOException("${language.displayName} is not available on the execution server")
    }

    private fun fetchLanguages(): List<Judge0Language> {
        val request = Request.Builder()
            .url("$baseUrl/languages/")
            .get()
            .addHeader("Accept", "application/json")
            .build()

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Could not load Judge0 languages (${response.code})")
            }
            val type = object : TypeToken<List<Judge0Language>>() {}.type
            gson.fromJson<List<Judge0Language>>(response.body?.string(), type).orEmpty()
        }
    }

    private fun fetchResult(token: String): Judge0Result {
        val request = Request.Builder()
            .url("$baseUrl/submissions/$token?base64_encoded=true&fields=$RESULT_FIELDS")
            .get()
            .addHeader("Accept", "application/json")
            .build()

        return client.newCall(request).execute().use { response ->
            if (response.code in TRANSIENT_RESULT_CODES) {
                throw ResultNotAvailableException(
                    "HTTP ${response.code} ${response.body?.string().orEmpty()}".trim()
                )
            }
            if (!response.isSuccessful) {
                throw IOException(
                    "Could not read Judge0 result (${response.code}): ${response.body?.string().orEmpty()}"
                )
            }
            gson.fromJson(response.body?.string(), Judge0Result::class.java)
                ?: throw IOException("Judge0 returned an empty result")
        }
    }

    private fun Judge0Result.toDomainModel() = CodeExecutionResult(
        statusId = status.id,
        status = status.description,
        stdout = decode(stdout),
        stderr = decode(stderr).ifBlank { decode(message) },
        compileOutput = decode(compileOutput),
        timeSeconds = time,
        memoryKb = memory
    )

    private fun encode(value: String): String =
        Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))

    private fun decode(value: String?): String {
        if (value.isNullOrEmpty()) return ""
        return runCatching {
            String(Base64.getDecoder().decode(value), Charsets.UTF_8)
        }.getOrDefault(value)
    }

    private companion object {
        val PROCESSING_STATUS_IDS = setOf(1, 2)
        val TRANSIENT_RESULT_CODES = setOf(400, 404, 503)
        const val MAX_POLL_ATTEMPTS = 24
        const val POLL_DELAY_MS = 500L
        const val RESULT_FIELDS = "stdout,stderr,compile_output,message,status,time,memory"
    }
}

private data class Judge0Language(val id: Int, val name: String)

private data class Judge0SubmissionRequest(
    @SerializedName("language_id") val languageId: Int,
    @SerializedName("source_code") val sourceCode: String,
    val stdin: String,
    @SerializedName("cpu_time_limit") val cpuTimeLimit: Int = 2,
    @SerializedName("wall_time_limit") val wallTimeLimit: Int = 5
)

private data class Judge0Token(val token: String)

private data class Judge0Result(
    val stdout: String?,
    val stderr: String?,
    @SerializedName("compile_output") val compileOutput: String?,
    val message: String?,
    val status: Judge0Status,
    val time: String?,
    val memory: Long?
)

private data class Judge0Status(val id: Int, val description: String)

private class ResultNotAvailableException(message: String) : IOException(message)
