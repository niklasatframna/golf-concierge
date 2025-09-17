package com.golfconcierge.app

import com.google.gson.annotations.SerializedName
import java.io.Serializable

// This is the main object that will hold the entire parsed response.
// We make it Serializable so we can pass it between Activities in an Intent.
data class ComparisonResult(
    @SerializedName("proscons")
    val prosCons: ProsConsHolder? = null
) : Serializable

// This class represents the object that contains the summary and the dynamic model keys.
data class ProsConsHolder(
    // We use a Map to handle the dynamic keys (the full model names).
    // The @SerializedName is not strictly needed here but good practice.
    // Note: The summary is handled separately during custom parsing.
    var modelComparisons: Map<String, ProsConsItem> = emptyMap(),
    var summary: String = ""
) : Serializable

// This class holds the specific pros and cons for a single model.
data class ProsConsItem(
    @SerializedName("pros")
    val pros: List<String> = emptyList(),
    @SerializedName("cons")
    val cons: List<String> = emptyList()
) : Serializable
