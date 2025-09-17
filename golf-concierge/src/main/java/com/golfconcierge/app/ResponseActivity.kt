package com.golfconcierge.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.widget.Button
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
        val compareAnotherButton: Button = findViewById(R.id.compareAnotherButton)
        val startOverButton: Button = findViewById(R.id.startOverButton)

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
                // Join the list of pros into a single bulleted string
                val prosString = prosConsItem.pros.joinToString(separator = "\n") { "\t• $it" }
                prosBuilder.append(prosString)
                prosBuilder.append("\n\n")

                // Build Cons section
                consBuilder.bold { append(modelName) }
                consBuilder.append("\n")
                // Join the list of cons into a single bulleted string
                val consString = prosConsItem.cons.joinToString(separator = "\n") { "\t• $it" }
                consBuilder.append(consString)
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

        // --- Setup Button Click Listeners ---

        // "Compare Another Model" button simply finishes this activity to go back to CompareActivity
        compareAnotherButton.setOnClickListener {
            finish()
        }

        // "Start Over" button navigates back to MainActivity, clearing the activities on top
        startOverButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish() // Finish the current activity
        }
    }
}