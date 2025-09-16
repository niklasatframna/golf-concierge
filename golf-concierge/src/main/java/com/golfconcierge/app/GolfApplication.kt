package com.golfconcierge.app

import android.app.Application
import com.golfconcierge.app.data.GeminiRepository

class GolfApplication : Application() {

    // Lazy-initialize the repository so it's only created when first needed
    val geminiRepository: GeminiRepository by lazy {
        GeminiRepository()
    }
}
