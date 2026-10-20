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
import java.net.ConnectException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Base64
import java.util.concurrent.TimeUnit

class Judge0Api(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val authToken: String = DEFAULT_AUTH_TOKEN,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
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
        try {
            val submission = Judge0SubmissionRequest(
                languageId = resolveLanguageId(language),
                sourceCode = encode(sourceCode),
                stdin = if (stdin.isEmpty()) "" else encode(stdin)
            )
            val createRequest = requestBuilder(
                "$baseUrl/submissions?base64_encoded=true&wait=true&fields=$RESULT_FIELDS"
            )
                .post(gson.toJson(submission).toRequestBody(jsonMediaType))
                .build()

            val token = client.newCall(createRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    throw toHttpFailure("submission", response.code, response.body?.string().orEmpty())
                }
                val responseBody = response.body?.string().orEmpty()
                val json = runCatching { JsonParser.parseString(responseBody).asJsonObject }
                    .getOrElse {
                        throw IOException("Judge0 returned an invalid submission response")
                    }
                if (json.has("status")) {
                    val immediateResult = gson.fromJson(json, Judge0Result::class.java)
                        ?: throw IOException("Judge0 returned an empty execution result")
                    return@withContext immediateResult.toDomainModel()
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
        } catch (error: Exception) {
            throw mapExecutionException(error)
        }
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
        val request = requestBuilder("$baseUrl/languages/")
            .get()
            .build()

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw toHttpFailure("languages", response.code, response.body?.string().orEmpty())
            }
            val type = object : TypeToken<List<Judge0Language>>() {}.type
            runCatching {
                gson.fromJson<List<Judge0Language>>(response.body?.string(), type).orEmpty()
            }.getOrElse {
                throw IOException("Judge0 returned an invalid languages response")
            }
        }
    }

    private fun fetchResult(token: String): Judge0Result {
        val request = requestBuilder("$baseUrl/submissions/$token?base64_encoded=true&fields=$RESULT_FIELDS")
            .get()
            .build()

        return client.newCall(request).execute().use { response ->
            if (response.code in TRANSIENT_RESULT_CODES) {
                throw ResultNotAvailableException(
                    "HTTP ${response.code} ${response.body?.string().orEmpty()}".trim()
                )
            }
            if (!response.isSuccessful) {
                throw toHttpFailure("result", response.code, response.body?.string().orEmpty())
            }
            runCatching {
                gson.fromJson(response.body?.string(), Judge0Result::class.java)
            }.getOrNull() ?: throw IOException("Judge0 returned an invalid execution result")
        }
    }

    private fun requestBuilder(url: String): Request.Builder {
        val builder = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/json")
        if (authToken.isNotBlank()) {
            builder.addHeader("X-Auth-Token", authToken)
        }
        return builder
    }

    private fun toHttpFailure(endpoint: String, code: Int, body: String): IOException {
        val lowerBody = body.lowercase()
        val message = when {
            code == 401 || code == 403 ->
                "Judge0 authentication failed. Check the configured execution token."
            code == 408 ->
                "Judge0 request timed out. Please try again."
            code == 429 ->
                "Judge0 is rate-limiting requests. Please wait and try again."
            code in 500..599 ->
                "Judge0 server is unavailable right now. Please try again shortly."
            endpoint == "languages" && code == 404 ->
                "Judge0 languages endpoint was not found on the configured server."
            endpoint == "submission" && ("language" in lowerBody || "language_id" in lowerBody) ->
                "The selected language is not supported by this Judge0 server."
            else ->
                "Judge0 request failed (HTTP $code)."
        }
        return IOException(message)
    }

    private fun mapExecutionException(error: Exception): IOException {
        val message = when (error) {
            is SocketTimeoutException ->
                "Code execution timed out while waiting for Judge0."
            is UnknownHostException, is ConnectException ->
                "Cannot reach Judge0 server at 100.92.1.2. Check Tailscale connectivity."
            is IOException ->
                return error
            else ->
                error.message ?: "Code execution failed due to an unexpected Judge0 error."
        }
        return IOException(message, error)
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
        if (value.isNullOrBlank()) return ""
        val normalized = value.trim()
        val decoders = listOf(
            Base64.getDecoder(),
            Base64.getMimeDecoder(),
            Base64.getUrlDecoder()
        )

        for (decoder in decoders) {
            val decoded = runCatching {
                String(decoder.decode(normalized), Charsets.UTF_8)
            }.getOrNull()
            if (decoded != null) return decoded
        }

        return value
    }

    private companion object {
        val PROCESSING_STATUS_IDS = setOf(1, 2)
        val TRANSIENT_RESULT_CODES = setOf(400, 404, 503)
        const val MAX_POLL_ATTEMPTS = 24
        const val POLL_DELAY_MS = 500L
        const val RESULT_FIELDS = "stdout,stderr,compile_output,message,status,time,memory"
        const val DEFAULT_BASE_URL = "http://100.92.1.2:2358"
        const val DEFAULT_AUTH_TOKEN = "RgVaepgJiP0iJ2"
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
