package com.chatai.app.data.remote

import com.chatai.app.domain.model.SupportedLanguage
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
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
            sourceCode = "print(input())",
            stdin = "42"
        )

        assertTrue(result.isSuccessful)
        assertEquals("42\n", result.stdout)
        assertEquals("Accepted", result.status)

        assertEquals("/languages/", server.takeRequest().path)
        val submission = server.takeRequest()
        assertTrue(submission.path.orEmpty().startsWith("/submissions?base64_encoded=true&wait=true"))
        val submissionBody = submission.body.readUtf8()
        assertTrue(submissionBody.contains("\"language_id\":109"))
        assertEquals(
            Base64.getEncoder().encodeToString("42".toByteArray()),
            JsonParser.parseString(submissionBody).asJsonObject.get("stdin").asString
        )
        assertTrue(server.takeRequest().path.orEmpty().startsWith("/submissions/submission-token?"))
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
}
