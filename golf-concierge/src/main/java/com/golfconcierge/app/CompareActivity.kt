package com.golfconcierge.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.golfconcierge.app.data.Brand
import com.golfconcierge.app.data.InMemoryGolfRepository
import com.golfconcierge.app.data.Model
import com.golfconcierge.app.ui.adapters.BrandSpinnerAdapter
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CompareActivity : AppCompatActivity() {

    // Views
    private lateinit var brandSpinner2: Spinner
    private lateinit var modelSpinner2: Spinner
    private lateinit var recommendButton: Button
    private lateinit var summaryTextView: TextView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var backButton: Button

    // Data from Intent
    private var handicap: Int = 0
    private lateinit var brand1Name: String
    private lateinit var category: String
    private lateinit var model1Name: String
    private var model1Year: Int = 0
    private lateinit var model1SubCategory: String
    private lateinit var moreInfo: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compare)

        // Initialize Views
        brandSpinner2 = findViewById(R.id.brandSpinner2)
        modelSpinner2 = findViewById(R.id.modelSpinner2)
        recommendButton = findViewById(R.id.recommendButton)
        summaryTextView = findViewById(R.id.summaryTextView)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        backButton = findViewById(R.id.backButton)

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

        setupBrandSpinner2()

        backButton.setOnClickListener {
            finish()
        }

        recommendButton.setOnClickListener {
            val selectedModel2 = getSelectedModel2()
            if (selectedModel2 != null) {
                val prompt = constructPrompt(
                    brand1Name, model1Name, model1SubCategory, model1Year,
                    (brandSpinner2.selectedItem as Brand).name, selectedModel2.name, selectedModel2.subCategory, selectedModel2.year,
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

                        Log.d("JSON_DEBUG", "Raw JSON: $result")
                        val comparisonResult = parseResponse(result)
                        Log.d("JSON_DEBUG", "Parsed Result: $comparisonResult")

                        val intent = Intent(this@CompareActivity, ResponseActivity::class.java).apply {
                            putExtra("comparisonResult", comparisonResult)
                        }
                        startActivity(intent)
                    } finally {
                        recommendButton.isEnabled = true
                        loadingIndicator.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun setupBrandSpinner2() {
        val brands = InMemoryGolfRepository.getBrands()
        val adapter = BrandSpinnerAdapter(this, brands)
        brandSpinner2.adapter = adapter

        brandSpinner2.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedBrand = brands[position]
                setupModelSpinner2(selectedBrand.name)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                modelSpinner2.adapter = null
            }
        }
    }

    private fun setupModelSpinner2(brandName: String) {
        val allModelsForBrand = InMemoryGolfRepository.getModelsForBrand(brandName, category)
        val filteredModels = allModelsForBrand.filterNot { model ->
            model.name == model1Name &&
                    model.year == model1Year &&
                    model.subCategory.equals(model1SubCategory, ignoreCase = true)
        }
        val modelDisplayItems = filteredModels.map {
            val subCatDisplay = it.subCategory.uppercase()
            "${it.name} (${it.year}) [$subCatDisplay]"
        }
        val modelAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modelDisplayItems)
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        modelSpinner2.adapter = modelAdapter
        modelSpinner2.tag = filteredModels
    }

    private fun getSelectedModel2(): Model? {
        val modelsFromTag = modelSpinner2.tag as? List<*> ?: return null
        val selectedPosition = modelSpinner2.selectedItemPosition
        if (selectedPosition < 0 || selectedPosition >= modelsFromTag.size) {
            return null
        }
        return modelsFromTag[selectedPosition] as Model?
    }

    private fun constructPrompt(
        brand1: String, model1: String, subCat1: String, year1: Int,
        brand2: String, model2: String, subCat2: String, year2: Int,
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