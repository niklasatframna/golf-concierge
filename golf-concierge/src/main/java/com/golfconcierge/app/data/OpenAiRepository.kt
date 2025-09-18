package com.golfconcierge.app.data

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.golfconcierge.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OpenAiRepository : GenerativeAiRepository {

    private val openAI = OpenAI(token = BuildConfig.OPENAI_API_KEY)

    override suspend fun generateContent(prompt: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val chatCompletionRequest = ChatCompletionRequest(
                    model = ModelId("gpt-3.5-turbo"),
                    messages = listOf(
                        ChatMessage(
                            role = ChatRole.User,
                            content = prompt
                        )
                    )
                )
                val completion = openAI.chatCompletion(chatCompletionRequest)
                completion.choices.first().message.content ?: ""
            } catch (e: Exception) {
                // In a real app, you'd want to handle this more gracefully
                // For example, by logging the error and returning a user-friendly message
                e.printStackTrace()
                "Error: Could not get a response from OpenAI. ${e.message}"
            }
        }
    }
}
