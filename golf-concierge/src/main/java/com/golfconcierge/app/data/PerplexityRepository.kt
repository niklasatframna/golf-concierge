package com.golfconcierge.app.data

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import com.aallam.openai.client.OpenAIHost
import com.golfconcierge.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

class PerplexityRepository : GenerativeAiRepository {

    private val perplexityAI: OpenAI

    init {
        val config = OpenAIConfig(
            token = BuildConfig.PERPLEXITY_API_KEY,
            host = OpenAIHost("https://api.perplexity.ai"),
            timeout = Timeout(socket = 60.seconds),
        )
        perplexityAI = OpenAI(config)
    }

    override suspend fun generateContent(prompt: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val chatCompletionRequest = ChatCompletionRequest(
                    model = ModelId("sonar-pro"), // Use a Perplexity model
                    messages = listOf(
                        ChatMessage(
                            role = ChatRole.System,
                            content = "You are a helpful assistant that responds only in valid JSON format. Do not add any conversational text or markdown formatting."
                        ),
                        ChatMessage(
                            role = ChatRole.User,
                            content = prompt
                        )
                    )
                )
                val completion = perplexityAI.chatCompletion(chatCompletionRequest)
                completion.choices.first().message.content ?: ""
            } catch (e: Exception) {
                // In a real app, you'd want to handle this more gracefully
                // For example, by logging the error and returning a user-friendly message
                e.printStackTrace()
                "Error: Could not get a response from Perplexity. ${e.message}"
            }
        }
    }
}
