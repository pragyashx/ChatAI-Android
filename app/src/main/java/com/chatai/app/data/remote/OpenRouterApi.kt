package com.chatai.app.data.remote

import android.util.Log
import com.chatai.app.data.remote.dto.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class OpenRouterApi() {

    companion object {
        private const val TAG = "OpenRouterApi"
        private const val BASE_URL = "https://openrouter.ai/api/v1"
        private const val CHAT_ENDPOINT = "$BASE_URL/chat/completions"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun sendMessageStream(
        apiKey: String,
        messages: List<MessageDto>
    ): Flow<String> = flow {
        val request = ChatRequest(
            model = AiModels.LUNA_MODEL_ID,
            messages = messages,
            stream = true
        )

        val jsonBody = gson.toJson(request)
        Log.d(TAG, "Request model: ${AiModels.LUNA_MODEL_ID}, messages: ${messages.size}")

        val requestBody = jsonBody.toRequestBody(jsonMediaType)

        val httpRequest = Request.Builder()
            .url(CHAT_ENDPOINT)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("HTTP-Referer", "https://chatai.app")
            .addHeader("X-Title", "Chiti Code Android")
            .build()

        // Use synchronous streaming for reliable Flow emission
        val response = client.newCall(httpRequest).execute()

        if (!response.isSuccessful) {
            val errorBody = response.body?.string()
            Log.e(TAG, "API Error ${response.code}: $errorBody")
            throw IOException(mapApiErrorMessage(response.code, errorBody))
        }

        val reader = response.body?.byteStream()?.bufferedReader()
            ?: throw IOException("Empty response body")

        reader.use { br ->
            var line: String?
            while (br.readLine().also { line = it } != null) {
                val currentLine = line ?: continue

                if (currentLine.startsWith("data: ")) {
                    val data = currentLine.removePrefix("data: ").trim()
                    if (data == "[DONE]") {
                        break
                    }

                    try {
                        val chunk = gson.fromJson(data, StreamChunk::class.java)
                        val content = chunk.choices?.firstOrNull()?.delta?.content
                        if (content != null) {
                            emit(content)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing chunk: ${e.message}, data: $data")
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun mapApiErrorMessage(statusCode: Int, errorBody: String?): String {
        val parsedMessage = runCatching {
            gson.fromJson(errorBody, ErrorResponse::class.java)?.error?.message
        }.getOrNull().orEmpty()
        val combined = "$parsedMessage ${errorBody.orEmpty()}".lowercase()

        return when {
            statusCode == 401 || "unauthorized" in combined || "invalid api key" in combined ->
                "Invalid API key. Please update your key and try again."

            statusCode == 429 || "rate limit" in combined || "too many requests" in combined ->
                "Rate limit reached. Please wait a moment and try again."

            "unsupported" in combined || "file" in combined || "attachment" in combined ||
                "image_url" in combined || "content part" in combined ->
                "This chat model currently supports image attachments only (JPG, JPEG, PNG)."

            statusCode in 500..599 ->
                "AI service is temporarily unavailable. Please try again shortly."

            else -> "Unable to process your request right now. Please try again."
        }
    }

}
