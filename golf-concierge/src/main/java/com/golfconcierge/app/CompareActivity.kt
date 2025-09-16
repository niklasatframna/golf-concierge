package com.golfconcierge.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.golfconcierge.app.data.Brand
import com.golfconcierge.app.data.InMemoryGolfRepository
import com.golfconcierge.app.data.Model
import com.golfconcierge.app.ui.adapters.BrandSpinnerAdapter
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CompareActivity : AppCompatActivity() {

    // Views
    private lateinit var brandSpinner2: Spinner
    private lateinit var modelSpinner2: Spinner
    private lateinit var recommendButton: Button
    private lateinit var summaryTextView: TextView // To display model1 details

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

        // Get data from MainActivity Intent
        handicap = intent.getIntExtra("handicap", 0)
        brand1Name = intent.getStringExtra("brand1") ?: ""
        category = intent.getStringExtra("category") ?: ""
        model1Name = intent.getStringExtra("model1Name") ?: ""
        model1Year = intent.getIntExtra("model1Year", 0)
        model1SubCategory = intent.getStringExtra("model1SubCategory") ?: ""
        moreInfo = intent.getStringExtra("moreInfo") ?: ""

        // Display summary of the first selected model
        val summaryText = "Comparing against: $brand1Name $model1Name ($model1Year) [$model1SubCategory]"
        summaryTextView.text = summaryText

        // Initialize repository if needed (assuming it's already initialized in MainActivity)
        InMemoryGolfRepository.initialize(applicationContext)

        // Setup Spinners for the second model selection
        setupBrandSpinner2()

        // Setup button click listener
        // Inside your onCreate method...
        recommendButton.setOnClickListener {
            val selectedModel2 = getSelectedModel2()
            if (selectedModel2 != null) {
                val prompt = constructPrompt(
                    brand1Name, model1Name, model1SubCategory, model1Year,
                    (brandSpinner2.selectedItem as Brand).name, selectedModel2.name, selectedModel2.subCategory, selectedModel2.year,
                    handicap, category, moreInfo
                )
                // ... (logging, button disabling logic) ...

                CoroutineScope(Dispatchers.IO).launch {
                    val geminiRepo = (application as GolfApplication).geminiRepository
                    val result = geminiRepo.generateContent(prompt) // This is the JSON string
                    withContext(Dispatchers.Main) {
                        // ... (button re-enabling logic) ...

                        // Parse the JSON string into our data object
                        val comparisonResult = parseResponse(result)

                        // Pass the single, serializable object to the next activity
                        val intent = Intent(this@CompareActivity, ResponseActivity::class.java).apply {
                            putExtra("comparisonResult", comparisonResult)
                        }
                        startActivity(intent)
                    }
                }
            }
            // ...
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
        // 1. Get all models from the repository for the selected brand and category
        val allModelsForBrand = InMemoryGolfRepository.getModelsForBrand(brandName, category)

        // 2. Filter out the model that was already selected in MainActivity (model1)
        val filteredModels = allModelsForBrand.filterNot { model ->
            model.name == model1Name &&
                    model.year == model1Year &&
                    model.subCategory.equals(model1SubCategory, ignoreCase = true)
        }

        // 3. Create the display strings from the new filtered list
        val modelDisplayItems = filteredModels.map {
            val subCatDisplay = it.subCategory.uppercase()
            "${it.name} (${it.year}) [$subCatDisplay]"
        }

        // 4. Set up the adapter with the filtered items
        val modelAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modelDisplayItems)
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        modelSpinner2.adapter = modelAdapter

        // 5. CRITICAL: Store the filtered list in the tag so getSelectedModel2() works correctly
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
        val model1Full = "$brand1 $model1 ($year1) [$subCat1]"
        val model2Full = "$brand2 $model2 ($year2) [$subCat2]"

        return """
        Compare the ${category} "${model1Full}" to "${model2Full}" for a player with a ${handicap} handicap. High focus on reviews but also use product description. Also consider the following: ${moreInfo}.
        Format the response as a JSON. Use the full model names as keys. The JSON should have this structure, and keep the text short and concise. If one of the clubs or both are not good for the handicap, then state so in the summary field and propose other ${category} from same brand and model range :
        {
          "proscons": {
            "$model1Full": {
              "pros": "...",
              "cons": "..."
            },
            "$model2Full": {
              "pros": "...",
              "cons": "..."
            },
            "summary": "A recommendation summary..."
          }
        }
        """.trimIndent()
    }

    private fun parseResponse(jsonString: String): ComparisonResult {
        val gson = Gson()
        try {
            // Attempt to extract the clean JSON part if the AI wrapped it in markdown
            val cleanJson = jsonString.substringAfter("```json").substringBefore("```").trim()
            val jsonObject = gson.fromJson(cleanJson, JsonObject::class.java)

            val prosConsObject = jsonObject.getAsJsonObject("proscons")
            val summary = prosConsObject.get("summary").asString

            // Create a map to hold only the model comparison entries
            val modelComparisonsMap = mutableMapOf<String, ProsConsItem>()
            prosConsObject.entrySet().forEach { entry ->
                if (entry.key != "summary") {
                    val prosConsItem = gson.fromJson(entry.value, ProsConsItem::class.java)
                    modelComparisonsMap[entry.key] = prosConsItem
                }
            }

            val prosConsHolder = ProsConsHolder(modelComparisons = modelComparisonsMap, summary = summary)
            return ComparisonResult(prosCons = prosConsHolder)

        } catch (e: Exception) {
            Log.e("CompareActivity", "Error parsing JSON response", e)
            // Return an error object if parsing fails
            val errorHolder = ProsConsHolder(summary = "Failed to parse AI response. Raw output:\n\n$jsonString")
            return ComparisonResult(prosCons = errorHolder)
        }
    }
}
