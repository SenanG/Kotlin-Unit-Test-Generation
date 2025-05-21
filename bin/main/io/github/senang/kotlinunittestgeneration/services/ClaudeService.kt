package io.github.senang.kotlinunittestgeneration.services

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

// Data classes for serialization/deserialization (must match FastAPI backend)
@Serializable
data class GenerateTestApiRequest(val code: String)

@Serializable
data class GenerateTestApiResponse(
    val generated_test_code: String,
    val model_used: String
)

object ClaudeService {
    // Configure your backend URL here. For dev, it's usually localhost.
    private const val BACKEND_URL = "http://localhost:8000/generate"
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    private val json = Json { ignoreUnknownKeys = true } // Lenient JSON parsing

    /**
     * Sends the Kotlin function code to the backend and retrieves generated tests.
     *
     * @param functionCode The raw Kotlin code of the function.
     * @return The generated test code as a string, or null if an error occurs.
     * @throws Exception if network or parsing errors occur.
     */
    fun generateTestsForFunction(functionCode: String): String? {
        if (functionCode.isBlank()) {
            println("Function code is blank, not sending to backend.")
            return null
        }

        val requestBody = try {
            json.encodeToString(GenerateTestApiRequest.serializer(), GenerateTestApiRequest(code = functionCode))
        } catch (e: Exception) {
            println("Error serializing request to JSON: ${e.message}")
            throw e // Re-throw to be caught by the calling AnAction and displayed to user
        }

        val request = HttpRequest.newBuilder()
            .uri(URI.create(BACKEND_URL))
            .timeout(Duration.ofMinutes(2)) // Timeout for the entire request-response cycle
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        println("Sending request to backend: $BACKEND_URL with code snippet: ${functionCode.take(100)}...")

        try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            println("Received response from backend. Status: ${response.statusCode()}")

            if (response.statusCode() == 200) {
                val responseBody = response.body()
                if (responseBody.isNullOrBlank()) {
                    println("Backend returned an empty response body.")
                    throw Exception("Backend returned empty response.")
                }
                return try {
                    val apiResponse = json.decodeFromString(GenerateTestApiResponse.serializer(), responseBody)
                    println("Successfully parsed response from backend. Model used: ${apiResponse.model_used}")
                    apiResponse.generated_test_code
                } catch (e: Exception) {
                    println("Error parsing JSON response from backend: ${e.message}")
                    println("Raw response: $responseBody")
                    throw Exception("Error parsing backend response: ${e.message}. Raw: ${responseBody.take(500)}")
                }
            } else {
                val errorBody = response.body().take(500) // Limit error body size in message
                println("Error from backend: ${response.statusCode()} - Body: $errorBody")
                throw Exception("Backend error: ${response.statusCode()}. Response: $errorBody")
            }
        } catch (e: Exception) {
            println("Exception during HTTP call or processing: ${e.message}")
            // Log e.cause if needed for more details
            throw e // Re-throw to be handled by the action
        }
    }
} 