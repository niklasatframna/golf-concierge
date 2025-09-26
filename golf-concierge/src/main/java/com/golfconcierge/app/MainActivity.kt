package com.golfconcierge.app

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.golfconcierge.app.data.Brand
import com.golfconcierge.app.data.InMemoryGolfRepository
import com.golfconcierge.app.data.Model
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class MainActivity : AppCompatActivity() {

    // View Declarations
    private lateinit var handicapSeekBar: SeekBar
    private lateinit var handicapTitle: TextView
    private lateinit var categoryChipGroup: ChipGroup
    private lateinit var brandAutoCompleteTextView: AutoCompleteTextView
    private lateinit var modelAutoCompleteTextView: AutoCompleteTextView
    private lateinit var moreInfoEditText: EditText
    private lateinit var continueButton: Button
    private lateinit var aiProviderAutoCompleteTextView: AutoCompleteTextView


    // Data holders for state
    private var originalCategoryKeys: List<String> = emptyList()
    private var currentSelectedCategoryKey: String? = null
    private var modelsForCurrentBrand: List<Model> = emptyList()
    private var selectedModel: Model? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Views
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        handicapTitle = findViewById(R.id.handicapTitle)
        handicapSeekBar = findViewById(R.id.handicapSeekBar)
        categoryChipGroup = findViewById(R.id.categoryChipGroup)
        brandAutoCompleteTextView = findViewById(R.id.brandAutoCompleteTextView)
        modelAutoCompleteTextView = findViewById(R.id.modelAutoCompleteTextView)
        moreInfoEditText = findViewById(R.id.moreInfoEditText)
        continueButton = findViewById(R.id.continueButton)
        aiProviderAutoCompleteTextView = findViewById(R.id.aiProviderAutoCompleteTextView)


        val initialHandicapProgress = handicapSeekBar.progress
        "Handicap: $initialHandicapProgress".also { handicapTitle.text = it }

        InMemoryGolfRepository.initialize(applicationContext)

        setupHandicapSeekBar()
        setupCategoryChips()
        setupAiProviderSpinner()
        setupContinueButton()
        setupAboutDialog()
    }

    private fun setupHandicapSeekBar() {
        handicapSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                "Handicap: $progress".also { handicapTitle.text = it }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupCategoryChips() {
        originalCategoryKeys = InMemoryGolfRepository.getCategories()
        categoryChipGroup.removeAllViews()

        if (originalCategoryKeys.isEmpty()) {
            updateSpinnersBasedOnCategory(null)
            return
        }

        originalCategoryKeys.forEachIndexed { index, categoryKey ->
            val chip = Chip(this).apply {
                text = InMemoryGolfRepository.displayCategory(categoryKey)
                tag = categoryKey
                isClickable = true
                isCheckable = true
            }

            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    val selectedKey = chip.tag as? String
                    if (currentSelectedCategoryKey != selectedKey) {
                        currentSelectedCategoryKey = selectedKey
                        updateSpinnersBasedOnCategory(currentSelectedCategoryKey)
                    }
                }
            }
            categoryChipGroup.addView(chip)
            if (index == 0) {
                chip.isChecked = true
            }
        }
    }

    private fun updateSpinnersBasedOnCategory(selectedCategoryKey: String?) {
        this.currentSelectedCategoryKey = selectedCategoryKey
        clearBrandAndModelSelection()

        val brandObjects: List<Brand> = InMemoryGolfRepository.getBrands()
        val brandNames = brandObjects.map { it.name }
        val brandAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, brandNames)
        brandAutoCompleteTextView.setAdapter(brandAdapter)

        brandAutoCompleteTextView.setOnItemClickListener { parent, _, position, _ ->
            val selectedBrandName = parent.getItemAtPosition(position) as? String
            updateModelSpinner(selectedBrandName, currentSelectedCategoryKey)
        }
    }

    private fun updateModelSpinner(brandName: String?, categoryKey: String?) {
        clearModelSelection()
        if (brandName == null || categoryKey == null) {
            modelAutoCompleteTextView.setAdapter(null)
            return
        }

        modelsForCurrentBrand = InMemoryGolfRepository.getModelsForBrand(brandName, categoryKey)
        val modelDisplayItems = modelsForCurrentBrand.map {
            val subCatDisplay = it.subCategory.uppercase()
            "${it.name} (${it.year}) [$subCatDisplay]"
        }

        val modelAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, modelDisplayItems)
        modelAutoCompleteTextView.setAdapter(modelAdapter)

        modelAutoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
            selectedModel = if (position < modelsForCurrentBrand.size) modelsForCurrentBrand[position] else null
        }
    }

    private fun clearBrandAndModelSelection() {
        brandAutoCompleteTextView.setText("", false)
        brandAutoCompleteTextView.setAdapter(null)
        clearModelSelection()
    }

    private fun clearModelSelection() {
        modelAutoCompleteTextView.setText("", false)
        modelAutoCompleteTextView.setAdapter(null)
        modelsForCurrentBrand = emptyList()
        selectedModel = null
    }
    private fun setupAiProviderSpinner() {
        val aiProviders = listOf("Gemini", "OpenAI", "Perplexity")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, aiProviders)
        aiProviderAutoCompleteTextView.setAdapter(adapter)
        // Set a default selection
        aiProviderAutoCompleteTextView.setText(aiProviders[0], false)
    }


    private fun setupContinueButton() {
        continueButton.setOnClickListener {
            handleContinueClick()
        }
    }

    private fun handleContinueClick() {
        val handicap = handicapSeekBar.progress
        val brandName = brandAutoCompleteTextView.text.toString()
        val aiProvider = aiProviderAutoCompleteTextView.text.toString()


        if (currentSelectedCategoryKey.isNullOrEmpty()) {
            Toast.makeText(this, "Please select a category.", Toast.LENGTH_SHORT).show()
            return
        }
        if (brandName.isEmpty()) {
            Toast.makeText(this, "Please select a brand.", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedModel == null) {
            Toast.makeText(this, "Please select a model.", Toast.LENGTH_SHORT).show()
            return
        }
        if (aiProvider.isEmpty()) {
            Toast.makeText(this, "Please select an AI Provider.", Toast.LENGTH_SHORT).show()
            return
        }


        val intent = Intent(this, CompareActivity::class.java).apply {
            putExtra("handicap", handicap)
            putExtra("brand1", brandName)
            putExtra("category", currentSelectedCategoryKey)
            putExtra("model1Name", selectedModel!!.name)
            putExtra("model1Year", selectedModel!!.year)
            putExtra("model1SubCategory", selectedModel!!.subCategory)
            putExtra("moreInfo", moreInfoEditText.text.toString().trim())
            putExtra("aiProvider", aiProvider)

        }
        startActivity(intent)
    }

    private fun setupAboutDialog() {
        findViewById<TextView>(R.id.aboutTextView).setOnClickListener {
            val aboutMessage = "Golf Concierge helps you to compare golf clubs when you are set out to buy one. It does not have a full dataset of brands and models but it does what it should."
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("About Golf Concierge")
                .setMessage(aboutMessage)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .create()
                .show()
        }
    }
}
