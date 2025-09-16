package com.golfconcierge.app

import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.SpannableStringBuilder
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.bold

class ResponseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_response)

        // Find TextViews from the layout
        val prosTextView: TextView = findViewById(R.id.prosTextView)
        val consTextView: TextView = findViewById(R.id.consTextView)
        val summaryTextView: TextView = findViewById(R.id.summaryTextView)

        // Get the data object passed from CompareActivity
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("comparisonResult", ComparisonResult::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("comparisonResult") as? ComparisonResult
        }

        if (result?.prosCons != null) {
            val prosConsHolder = result.prosCons

            val prosBuilder = SpannableStringBuilder()
            val consBuilder = SpannableStringBuilder()

            // Loop through each model in the comparison map
            prosConsHolder.modelComparisons.forEach { (modelName, prosConsItem) ->
                // Build Pros section
                prosBuilder.bold { append(modelName) }
                prosBuilder.append("\n")
                // Format the bullet points
                prosBuilder.append(prosConsItem.pros.replace("- ", "\t• ").trim())
                prosBuilder.append("\n\n")

                // Build Cons section
                consBuilder.bold { append(modelName) }
                consBuilder.append("\n")
                // Format the bullet points
                consBuilder.append(prosConsItem.cons.replace("- ", "\t• ").trim())
                consBuilder.append("\n\n")
            }

            // Set the text for each view
            prosTextView.text = prosBuilder
            consTextView.text = consBuilder
            summaryTextView.text = prosConsHolder.summary

        } else {
            // Handle cases where data is missing or there was a parsing error
            summaryTextView.text = "Error: Could not retrieve comparison data."
        }
    }
}