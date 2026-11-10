package com.example.networklibraryanalyzer

import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import java.io.IOException

class KtorHelper {

    interface ResultCallback {
        fun onResult(
            statusCode: Int,
            response: String,
            responseTime: Double,
            error: String?
        )
    }

    companion object {

        @JvmStatic
        fun execute(
            api: String,
            method: String,
            jsonBody: String,
            callback: ResultCallback
        ) {
            val client = HttpClient(Android) {
                expectSuccess = false
            }

            CoroutineScope(Dispatchers.IO).launch {

                val startTime = System.nanoTime()

                try {
                    val response = client.request(api) {

                        this.method = HttpMethod(method.uppercase())

                        if (method.uppercase() in
                            listOf("POST", "PUT", "PATCH")) {

                            contentType(ContentType.Application.Json)
                            setBody(jsonBody)
                        }
                    }

                    val responseText = response.bodyAsText()
                    val statusCode = response.status.value

                    val endTime = System.nanoTime()

                    val responseTime =
                        (endTime - startTime) / 1_000_000.0

                    callback.onResult(
                        statusCode,
                        responseText,
                        responseTime,
                        null
                    )

                } catch (e: Exception) {

                    callback.onResult(
                        0,
                        "",
                        0.0,
                        e.message ?: "Network error"
                    )

                } finally {
                    client.close()
                }
            }
        }
    }
}