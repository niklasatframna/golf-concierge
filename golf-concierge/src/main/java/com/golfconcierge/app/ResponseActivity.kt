package com.golfconcierge.app

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView

class ResponseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_response)

        // Setup Toolbar
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Find Views
        val summaryTextView: TextView = findViewById(R.id.summaryTextView)
        val comparisonContainer: LinearLayout = findViewById(R.id.comparisonContainer)
        val compareAnotherButton: Button = findViewById(R.id.compareAnotherButton)
        val startOverButton: Button = findViewById(R.id.startOverButton)

        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("comparisonResult", ComparisonResult::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("comparisonResult") as? ComparisonResult
        }

        if (result?.prosCons != null) {
            val prosConsHolder = result.prosCons
            summaryTextView.text = prosConsHolder.summary

            // Clear any previous views and dynamically add new ones
            comparisonContainer.removeAllViews()
            prosConsHolder.modelComparisons.forEach { (modelName, prosConsItem) ->
                val modelCard = createModelCard(this, modelName, prosConsItem)
                comparisonContainer.addView(modelCard)
            }
        } else {
            summaryTextView.text = "Error: Could not retrieve comparison data."
        }

        // Setup Button Click Listeners
        compareAnotherButton.setOnClickListener {
            finish()
        }

        startOverButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun createModelCard(context: Context, modelName: String, item: ProsConsItem): MaterialCardView {
        val inflater = LayoutInflater.from(context)
        val cardView = inflater.inflate(R.layout.item_comparison_card, null) as MaterialCardView

        val modelNameTextView: TextView = cardView.findViewById(R.id.modelNameTextView)
        val prosContainer: LinearLayout = cardView.findViewById(R.id.prosContainer)
        val consContainer: LinearLayout = cardView.findViewById(R.id.consContainer)

        modelNameTextView.text = modelName

        // Populate Pros
        item.pros.forEach { proText ->
            val proTextView = inflater.inflate(R.layout.item_pro_con, prosContainer, false) as TextView
            proTextView.text = proText
            proTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check, 0, 0, 0) // Pro icon
            prosContainer.addView(proTextView)
        }

        // Populate Cons
        item.cons.forEach { conText ->
            val conTextView = inflater.inflate(R.layout.item_pro_con, consContainer, false) as TextView
            conTextView.text = conText
            conTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_clear, 0, 0, 0) // Con icon
            consContainer.addView(conTextView)
        }

        return cardView
    }
}
