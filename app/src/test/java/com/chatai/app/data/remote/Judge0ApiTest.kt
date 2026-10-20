package com.chatai.app.data.remote

import com.chatai.app.domain.model.SupportedLanguage
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.UnknownHostException
import java.util.Base64

class Judge0ApiTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun execute_discoversRuntimeSubmitsAndReadsAcceptedResult() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""[{"id":109,"name":"Python (3.13.2)"}]""")
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"token":"submission-token"}""")
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{
                        "stdout":"NDIK",
                        "stderr":null,
                        "compile_output":null,
                        "message":null,
                        "time":"0.01",
                        "memory":1024,
                        "status":{"id":3,"description":"Accepted"}
                    }""".trimIndent()
                )
        )
        val api = Judge0Api(baseUrl = server.url("/").toString().removeSuffix("/"))

        val result = api.execute(
            language = SupportedLanguage.PYTHON,
            sourceCode = "print(\"Hello from Judge0\")",
            stdin = "42"
        )

        assertTrue(result.isSuccessful)
        assertEquals("42\n", result.stdout)
        assertEquals("Accepted", result.status)

        val languagesRequest = server.takeRequest()
        assertEquals("/languages/", languagesRequest.path)
        assertEquals("RgVaepgJiP0iJ2", languagesRequest.getHeader("X-Auth-Token"))

        val submissionRequest = server.takeRequest()
        assertEquals("RgVaepgJiP0iJ2", submissionRequest.getHeader("X-Auth-Token"))
        assertTrue(submissionRequest.path.orEmpty().startsWith("/submissions?base64_encoded=true&wait=true"))
        val submissionBody = submissionRequest.body.readUtf8()
        val jsonBody = JsonParser.parseString(submissionBody).asJsonObject
        assertTrue(submissionBody.contains("\"language_id\":109"))
        assertEquals(
            "cHJpbnQoIkhlbGxvIGZyb20gSnVkZ2UwIik=",
            jsonBody.get("source_code").asString
        )
        assertEquals(
            Base64.getEncoder().encodeToString("42".toByteArray()),
            jsonBody.get("stdin").asString
        )

        val resultRequest = server.takeRequest()
        assertEquals("RgVaepgJiP0iJ2", resultRequest.getHeader("X-Auth-Token"))
        assertTrue(resultRequest.path.orEmpty().startsWith("/submissions/submission-token?"))
    }

    @Test
    fun execute_sendsEmptyStdinAsEmptyString() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""[{"id":109,"name":"Python (3.13.2)"}]""")
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{
                        "stdout":"b2sK",
                        "stderr":null,
                        "compile_output":null,
                        "message":null,
                        "time":"0.01",
                        "memory":1024,
                        "status":{"id":3,"description":"Accepted"}
                    }""".trimIndent()
                )
        )
        val api = Judge0Api(baseUrl = server.url("/").toString().removeSuffix("/"))

        val result = api.execute(SupportedLanguage.PYTHON, "print('ok')", "")

        assertTrue(result.isSuccessful)
        server.takeRequest()
        val submission = server.takeRequest()
        val body = JsonParser.parseString(submission.body.readUtf8()).asJsonObject
        assertEquals("", body.get("stdin").asString)
    }

    @Test
    fun execute_retriesWhenResultIsNotAvailableYet() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""[{"id":109,"name":"Python (3.13.2)"}]""")
        )
        server.enqueue(
            MockResponse().setResponseCode(201)
                .setBody("""{"token":"eventual-token"}""")
        )
        server.enqueue(MockResponse().setResponseCode(400).setBody("Result is not available yet"))
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(
                    """{
                        "stdout":"b2sK",
                        "stderr":null,
                        "compile_output":null,
                        "message":null,
                        "time":"0.01",
                        "memory":1024,
                        "status":{"id":3,"description":"Accepted"}
                    }""".trimIndent()
                )
        )
        val api = Judge0Api(baseUrl = server.url("/").toString().removeSuffix("/"))

        val result = api.execute(SupportedLanguage.PYTHON, "print('ok')", "")

        assertTrue(result.isSuccessful)
        assertEquals(4, server.requestCount)
    }

    @Test
    fun execute_usesImmediateResultWhenWaitIsSupported() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""[{"id":109,"name":"Python (3.13.2)"}]""")
        )
        server.enqueue(
            MockResponse().setResponseCode(201)
                .setBody(
                    """{
                        "stdout":"aW1tZWRpYXRlCg==",
                        "stderr":null,
                        "compile_output":null,
                        "message":null,
                        "time":"0.01",
                        "memory":1024,
                        "status":{"id":3,"description":"Accepted"}
                    }""".trimIndent()
                )
        )
        val api = Judge0Api(baseUrl = server.url("/").toString().removeSuffix("/"))

        val result = api.execute(SupportedLanguage.PYTHON, "print('immediate')", "")

        assertEquals("immediate\n", result.stdout)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun execute_decodesCompileOutputAndMessageFields() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""[{"id":109,"name":"Python (3.13.2)"}]""")
        )
        server.enqueue(
            MockResponse().setResponseCode(201)
                .setBody(
                    """{
                        "stdout":null,
                        "stderr":null,
                        "compile_output":"Y29tcGlsYXRpb24gZmFpbGVkCg==",
                        "message":"cnVudGltZSBub3RlCg==",
                        "time":"0.01",
                        "memory":1024,
                        "status":{"id":6,"description":"Compilation Error"}
                    }""".trimIndent()
                )
        )
        val api = Judge0Api(baseUrl = server.url("/").toString().removeSuffix("/"))

        val result = api.execute(SupportedLanguage.PYTHON, "x =", "")

        assertEquals("Compilation Error", result.status)
        assertEquals("compilation failed\n", result.compileOutput)
        assertEquals("runtime note\n", result.stderr)
        assertEquals("compilation failed\n", result.visibleOutput)
    }

    @Test
    fun execute_decodesWrappedBase64Stdout() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""[{"id":109,"name":"Python (3.13.2)"}]""")
        )
        server.enqueue(
            MockResponse().setResponseCode(201)
                .setBody(
                    """{
                        "stdout":"U29tZSByZXN1bHQgd2l0aCB3cmFwcGVk\nIGJhc2U2NCBvdXRwdXQK",
                        "stderr":null,
                        "compile_output":null,
                        "message":null,
                        "time":"0.01",
                        "memory":1024,
                        "status":{"id":3,"description":"Accepted"}
                    }""".trimIndent()
                )
        )
        val api = Judge0Api(baseUrl = server.url("/").toString().removeSuffix("/"))

        val result = api.execute(SupportedLanguage.PYTHON, "print('ok')", "")

        assertEquals("Some result with wrapped base64 output\n", result.stdout)
    }

    @Test
    fun execute_returnsHelpfulAuthError() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""[{"id":109,"name":"Python (3.13.2)"}]""")
        )
        server.enqueue(MockResponse().setResponseCode(401).setBody("Unauthorized"))
        val api = Judge0Api(baseUrl = server.url("/").toString().removeSuffix("/"))

        val error = runCatching {
            api.execute(SupportedLanguage.PYTHON, "print('x')", "")
        }.exceptionOrNull()

        assertTrue(error is java.io.IOException)
        assertEquals(
            "Judge0 authentication failed. Check the configured execution token.",
            error?.message
        )
    }

    @Test
    fun execute_returnsHelpfulNetworkErrorWhenServerIsUnreachable() = runBlocking {
        val failingClient = OkHttpClient.Builder()
            .addInterceptor { throw UnknownHostException("tailscale down") }
            .build()
        val api = Judge0Api(
            baseUrl = "http://100.92.1.2:2358",
            client = failingClient
        )

        val error = runCatching {
            api.execute(SupportedLanguage.PYTHON, "print('x')", "")
        }.exceptionOrNull()

        assertTrue(error is java.io.IOException)
        assertEquals(
            "Cannot reach Judge0 server at 100.92.1.2. Check Tailscale connectivity.",
            error?.message
        )
        assertFalse(error?.message.orEmpty().contains("RgVaepgJiP0iJ2"))
    }
}
