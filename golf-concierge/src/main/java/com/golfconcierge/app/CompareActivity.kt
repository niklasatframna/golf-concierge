package com.golfconcierge.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.golfconcierge.app.data.Brand
import com.golfconcierge.app.data.InMemoryGolfRepository
import com.golfconcierge.app.data.Model
import com.google.android.material.appbar.MaterialToolbar
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CompareActivity : AppCompatActivity() {

    // Views
    private lateinit var brandAutoCompleteTextView2: AutoCompleteTextView
    private lateinit var modelAutoCompleteTextView2: AutoCompleteTextView
    private lateinit var recommendButton: Button
    private lateinit var summaryTextView: TextView
    private lateinit var loadingIndicator: ProgressBar

    // Data from Intent
    private var handicap: Int = 0
    private lateinit var brand1Name: String
    private lateinit var category: String
    private lateinit var model1Name: String
    private var model1Year: Int = 0
    private lateinit var model1SubCategory: String
    private lateinit var moreInfo: String

    // State holders
    private var modelsForCurrentBrand2: List<Model> = emptyList()
    private var selectedModel2: Model? = null
    private var selectedBrand2: Brand? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compare)

        // Initialize Views
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        brandAutoCompleteTextView2 = findViewById(R.id.brandAutoCompleteTextView2)
        modelAutoCompleteTextView2 = findViewById(R.id.modelAutoCompleteTextView2)
        recommendButton = findViewById(R.id.recommendButton)
        summaryTextView = findViewById(R.id.summaryTextView)
        loadingIndicator = findViewById(R.id.loadingIndicator)

        // Get data from MainActivity Intent
        handicap = intent.getIntExtra("handicap", 0)
        brand1Name = intent.getStringExtra("brand1") ?: ""
        category = intent.getStringExtra("category") ?: ""
        model1Name = intent.getStringExtra("model1Name") ?: ""
        model1Year = intent.getIntExtra("model1Year", 0)
        model1SubCategory = intent.getStringExtra("model1SubCategory") ?: ""
        moreInfo = intent.getStringExtra("moreInfo") ?: ""

        val summaryText = "Comparing against: $brand1Name $model1Name ($model1Year) [$model1SubCategory]"
        summaryTextView.text = summaryText

        InMemoryGolfRepository.initialize(applicationContext)

        setupBrandDropdown()
        setupRecommendButton()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle arrow click here
        if (item.itemId == android.R.id.home) {
            finish() // close this activity and return to preview activity (if there is any)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupBrandDropdown() {
        val brands = InMemoryGolfRepository.getBrands()
        val brandNames = brands.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, brandNames)
        brandAutoCompleteTextView2.setAdapter(adapter)

        brandAutoCompleteTextView2.setOnItemClickListener { parent, _, position, _ ->
            val selectedBrandName = parent.getItemAtPosition(position) as? String
            selectedBrand2 = brands.find { it.name == selectedBrandName }
            setupModelDropdown(selectedBrand2?.name)
        }
    }

    private fun setupModelDropdown(brandName: String?) {
        clearModelSelection()
        if (brandName == null) {
            modelAutoCompleteTextView2.setAdapter(null)
            return
        }

        val allModelsForBrand = InMemoryGolfRepository.getModelsForBrand(brandName, category)
        modelsForCurrentBrand2 = allModelsForBrand.filterNot { model ->
            model.name == model1Name &&
                    model.year == model1Year &&
                    model.subCategory.equals(model1SubCategory, ignoreCase = true)
        }

        val modelDisplayItems = modelsForCurrentBrand2.map {
            val subCatDisplay = it.subCategory.uppercase()
            "${it.name} (${it.year}) [$subCatDisplay]"
        }
        val modelAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, modelDisplayItems)
        modelAutoCompleteTextView2.setAdapter(modelAdapter)

        modelAutoCompleteTextView2.setOnItemClickListener { _, _, position, _ ->
            selectedModel2 = if (position < modelsForCurrentBrand2.size) modelsForCurrentBrand2[position] else null
        }
    }

    private fun clearModelSelection() {
        modelAutoCompleteTextView2.setText("", false)
        modelAutoCompleteTextView2.setAdapter(null)
        modelsForCurrentBrand2 = emptyList()
        selectedModel2 = null
    }

    private fun setupRecommendButton() {
        recommendButton.setOnClickListener {
            if (selectedBrand2 == null || selectedModel2 == null) {
                Toast.makeText(this, "Please select a brand and model to compare.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val prompt = constructPrompt(
                brand1Name, model1Name,
                model1Year,
                selectedBrand2!!.name, selectedModel2!!.name, selectedModel2!!.year,
                handicap, category, moreInfo
            )

            lifecycleScope.launch {
                recommendButton.isEnabled = false
                loadingIndicator.visibility = View.VISIBLE
                try {
                    val result = withContext(Dispatchers.IO) {
                        val geminiRepo = (application as GolfApplication).geminiRepository
                        geminiRepo.generateContent(prompt)
                    }

                    val comparisonResult = parseResponse(result)
                    val intent = Intent(this@CompareActivity, ResponseActivity::class.java).apply {
                        putExtra("comparisonResult", comparisonResult)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("CompareActivity", "Error during Gemini call", e)
                    Toast.makeText(this@CompareActivity, "An error occurred. Please try again.", Toast.LENGTH_LONG).show()
                } finally {
                    recommendButton.isEnabled = true
                    loadingIndicator.visibility = View.GONE
                }
            }
        }
    }


    private fun constructPrompt(
        brand1: String, model1: String, year1: Int,
        brand2: String, model2: String, year2: Int,
        handicap: Int, category: String, moreInfo: String
    ): String {
        val model1Full = "$brand1 $model1 ($year1)"
        val model2Full = "$brand2 $model2 ($year2)"

        return """
        Compare the ${category} "${model1Full}" to "${model2Full}" for a player with a ${handicap} handicap. High focus on reviews but also use product description. Also consider the following: ${moreInfo}.
        Format the response as JSON. Use the full model names as keys. The JSON should have this structure, and keep the text short and concise. If one of the clubs or both are not good for the handicap, then state so in the summary field and propose other ${category} from same brand and model range :
        {
          "proscons": {
            "model1": {
              "name": "$model1Full",
              "pros": [
                "...",
                "..."
              ],
              "cons": [
                "...",
                "..."
              ]
            },
            "model2": {
              "name": "$model2Full",
              "pros": [
                "...",
                "..."
              ],
              "cons": [
                "...",
                "..."
              ]
            },
            "summary": "A recommendation summary..."
          }
        }
        """.trimIndent()
    }

    private fun parseResponse(jsonString: String): ComparisonResult {
        val gson = Gson()
        try {
            val cleanJson = jsonString.substringAfter("```json").substringBefore("```").trim()
            val jsonObject = gson.fromJson(cleanJson, JsonObject::class.java)
            val prosConsObject = jsonObject.getAsJsonObject("proscons")
            val summary = prosConsObject.get("summary").asString
            val modelComparisonsMap = mutableMapOf<String, ProsConsItem>()
            prosConsObject.entrySet().forEach { (key, value) ->
                if (key != "summary" && value.isJsonObject) {
                    val modelObject = value.asJsonObject
                    val modelName = modelObject.get("name").asString
                    val prosConsItem = gson.fromJson(modelObject, ProsConsItem::class.java)
                    modelComparisonsMap[modelName] = prosConsItem
                }
            }
            val prosConsHolder = ProsConsHolder(modelComparisons = modelComparisonsMap, summary = summary)
            return ComparisonResult(prosCons = prosConsHolder)
        } catch (e: Exception) {
            Log.e("CompareActivity", "Error parsing JSON response", e)
            val errorHolder = ProsConsHolder(summary = "Failed to parse AI response. Raw output:\n\n$jsonString")
            return ComparisonResult(prosCons = errorHolder)
        }
    }
}
