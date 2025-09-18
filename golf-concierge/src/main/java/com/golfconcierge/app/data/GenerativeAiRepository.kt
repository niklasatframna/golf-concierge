package com.golfconcierge.app.data

interface GenerativeAiRepository {
    suspend fun generateContent(prompt: String): String
}
