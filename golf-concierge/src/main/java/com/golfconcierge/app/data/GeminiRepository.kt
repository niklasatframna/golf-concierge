package com.golfconcierge.app.data

import com.golfconcierge.app.BuildConfig
import com.google.genai.Client
import com.google.genai.types.Content
import com.google.genai.types.GenerateContentResponse
import com.google.genai.types.Part

class GeminiRepository : GenerativeAiRepository {

    private val genaiClient = Client.builder().apiKey(BuildConfig.GEMINI_API_KEY).build()

    override suspend fun generateContent(prompt: String): String {
        return try {
            val response: GenerateContentResponse = genaiClient.models.generateContent(
                "gemini-1.5-pro",
                listOf(
                    Content.builder().parts(Part.builder().text(prompt)).build()
                ),
                null
            )

            response.text() ?: "Error: Response was empty"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
