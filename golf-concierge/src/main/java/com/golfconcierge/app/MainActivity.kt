package com.golfconcierge.app // Or your actual package name

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.golfconcierge.app.data.Brand
import com.golfconcierge.app.data.InMemoryGolfRepository
import com.golfconcierge.app.data.Model
import com.golfconcierge.app.ui.adapters.BrandSpinnerAdapter // **Ensure this path is correct**
import kotlin.text.uppercase

class MainActivity : AppCompatActivity() {

    // View Declarations
    private lateinit var handicapSeekBar: SeekBar
    private lateinit var handicapTitle: TextView
    private lateinit var categoryChipGroup: ChipGroup // Changed from Spinner
    private lateinit var brandSpinner: Spinner
    private lateinit var modelSpinner: Spinner
    private lateinit var moreInfoEditText: EditText
    private lateinit var continueButton: Button

    // Data holders for state
    private var originalCategoryKeys: List<String> = emptyList()
    private var currentSelectedCategoryKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ensure R.layout.activity_main has the ChipGroup instead of categorySpinner
        setContentView(R.layout.activity_main)

        // Initialize Views
        handicapTitle = findViewById(R.id.handicapTitle)
        handicapSeekBar = findViewById(R.id.handicapSeekBar)
        categoryChipGroup = findViewById(R.id.categoryChipGroup) // This ID must match your XML
        brandSpinner = findViewById(R.id.brandSpinner)
        modelSpinner = findViewById(R.id.modelSpinner)
        moreInfoEditText = findViewById(R.id.moreInfoEditText)
        continueButton = findViewById(R.id.continueButton)

        val initialHandicapProgress = handicapSeekBar.progress
        "Handicap: $initialHandicapProgress".also { handicapTitle.text = it }

        // Initialize Data Repository
        InMemoryGolfRepository.initialize(applicationContext)

        // Setup Handicap SeekBar
        handicapSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                handicapTitle.text = "Handicap: $progress"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Setup Category Chips (replaces categorySpinner setup)
        setupCategoryChips()

        // Setup Continue Button
        continueButton.setOnClickListener {
            handleContinueClick()
        }

        findViewById<TextView>(R.id.aboutTextView).setOnClickListener {
            // This is the code that runs when the user clicks the TextView

            // The message you want to display in the dialog
            val aboutMessage = "Golf Concierge helps you to compare golf clubs when you are set out to buy one. It does not have a full dataset of brands and models but it does what it should."

            // Use the standard AlertDialog.Builder from AppCompat
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("About Golf Concierge")
                .setMessage(aboutMessage)
                .setPositiveButton("OK") { dialog, _ ->
                    // This code runs when the user clicks the "OK" button.
                    // We just want to close the dialog.
                    dialog.dismiss()
                }
                .create() // Creates the dialog
                .show()   // Shows it to the user
        }
    }

    private fun setupCategoryChips() {
        originalCategoryKeys = InMemoryGolfRepository.getCategories()
        categoryChipGroup.removeAllViews() // Clear any existing chips if re-populating

        if (originalCategoryKeys.isEmpty()) {
            Log.w("MainActivity", "No categories found to create chips.")
            updateSpinnersBasedOnCategory(null) // Pass null if no category
            return
        }

        originalCategoryKeys.forEachIndexed { index, categoryKey ->
            val chip = Chip(this) // Create a new Chip for each category
            chip.text = InMemoryGolfRepository.displayCategory(categoryKey) // User-friendly display name
            chip.tag = categoryKey // Store the original key (e.g., "DRIVER")
            chip.isClickable = true
            chip.isCheckable = true // Required for ChipGroup singleSelection
            // chip.isCheckedIconVisible = true // Optional: show check icon

            // Optional: Style your chip here or via XML styles/themes
            // e.g., chip.setChipStrokeWidthResource(R.dimen.chip_stroke_width)

            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    val selectedKey = chip.tag as? String
                    // Only update if the selection has actually changed
                    if (currentSelectedCategoryKey != selectedKey) {
                        currentSelectedCategoryKey = selectedKey
                        Log.d("MainActivity", "Category Chip selected: ${chip.text}, Key: $currentSelectedCategoryKey")
                        updateSpinnersBasedOnCategory(currentSelectedCategoryKey)
                    }
                }
            }
            categoryChipGroup.addView(chip)

            // Automatically select the first chip by default
            if (index == 0) {
                chip.isChecked = true
                // The onCheckedChangeListener will be triggered, calling updateSpinnersBasedOnCategory
            }
        }
    }

    private fun updateSpinnersBasedOnCategory(selectedCategoryKey: String?) {
        this.currentSelectedCategoryKey = selectedCategoryKey // Update the class member

        Log.d("MainActivity", "Updating spinners for category key: $selectedCategoryKey")

        // 1. Update Brand Spinner using BrandSpinnerAdapter
        val brandObjects: List<Brand> = InMemoryGolfRepository.getBrands()
        if (brandObjects.isNotEmpty()) {
            val customBrandAdapter = BrandSpinnerAdapter(this, brandObjects)
            brandSpinner.adapter = customBrandAdapter
        } else {
            brandSpinner.adapter = null // Clear adapter if no brands
            Log.w("MainActivity", "No brands found. Clearing brand spinner.")
        }

        // 2. Setup Brand Spinner Listener
        // This listener will react to brand selections and update the model spinner.
        brandSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedBrand = parent?.getItemAtPosition(position) as? Brand
                if (selectedBrand != null) {
                    Log.d("MainActivity", "Brand selected: ${selectedBrand.name}")
                    // Pass both selected brand name and current category key
                    updateModelSpinner(selectedBrand.name, currentSelectedCategoryKey)
                } else {
                    // Fallback or error handling if selected item is not a Brand object
                    if (brandObjects.isNotEmpty() && position < brandObjects.size) {
                        // This case should ideally not happen with BrandSpinnerAdapter
                        updateModelSpinner(brandObjects[position].name, currentSelectedCategoryKey)
                    } else {
                        Log.w("MainActivity", "Selected brand is null or not a Brand object at position $position.")
                        updateModelSpinner(null, null) // Clear model spinner
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                Log.d("MainActivity", "No brand selected.")
                updateModelSpinner(null, null) // Clear model spinner
            }
        }

        // 3. Initial state for Model Spinner
        // If brands were populated, the brandSpinner's onItemSelectedListener for position 0
        // will automatically call updateModelSpinner.
        // If no brands or no category, ensure model spinner is cleared.
        if (brandObjects.isEmpty() || selectedCategoryKey == null) {
            updateModelSpinner(null, null)
        }
        // Note: The first call to updateModelSpinner is now primarily driven by the
        // brandSpinner's onItemSelectedListener being triggered when its adapter is set.
    }

    private fun updateModelSpinner(brandName: String?, categoryKey: String?) {
        if (brandName == null || categoryKey == null) {
            // Clear the model spinner if brand or category is not selected
            modelSpinner.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, emptyList())
            modelSpinner.tag = emptyList<Model>() // Clear stored models
            Log.d("MainActivity", "Clearing model spinner (no brand or category specified).")
            return
        }

        Log.d("MainActivity", "Updating models for Brand: '$brandName', Category: '$categoryKey'")
        val models: List<Model> = InMemoryGolfRepository.getModelsForBrand(
            brandNameFilter = brandName,
            mainCategory = categoryKey
            // Add subCategoryType filter here if you implement UI for it
        )

        val modelDisplayItems = models.map {
            // Example display: "G440 Max (2025) [GI]" - ensure subCategoryType is not null in Model
            val subCatDisplay = it.subCategory.uppercase()
            "${it.name} (${it.year}) [$subCatDisplay]"
        }
        Log.d("MainActivity", "Models found for $brandName / $categoryKey: ${models.size}")

        val modelAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item, // Layout for the selected item view
            modelDisplayItems
        )
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) // Layout for dropdown
        modelSpinner.adapter = modelAdapter
        modelSpinner.tag = models // Store the actual List<Model> objects for use in continueButton
    }

    private fun handleContinueClick() {
        val handicap = handicapSeekBar.progress

        // Get selected category key from the class member
        val categoryToUse = this.currentSelectedCategoryKey
        if (categoryToUse.isNullOrEmpty()) {
            Toast.makeText(this, "Please select a category.", Toast.LENGTH_SHORT).show()
            Log.w("MainActivity", "Continue attempt with no category selected.")
            return
        }

        // Get selected brand name from brandSpinner
        val selectedBrandObject = brandSpinner.selectedItem as? Brand
        val brandNameToUse = selectedBrandObject?.name
        if (brandNameToUse.isNullOrEmpty()) {
            Toast.makeText(this, "Please select a brand.", Toast.LENGTH_SHORT).show()
            Log.w("MainActivity", "Continue attempt with no brand selected.")
            return
        }

        // Get selected model from modelSpinner's tag
        val modelsFromTag = modelSpinner.tag as? List<Model> ?: emptyList()
        val selectedModelFromSpinner: Model? = if (modelsFromTag.isNotEmpty() &&
            modelSpinner.selectedItemPosition >= 0 &&
            modelSpinner.selectedItemPosition < modelsFromTag.size) {
            modelsFromTag[modelSpinner.selectedItemPosition]
        } else {
            null
        }

        if (selectedModelFromSpinner == null) {
            Toast.makeText(this, "Please select a model.", Toast.LENGTH_SHORT).show()
            Log.w("MainActivity", "Continue attempt with no model selected.")
            return
        }

        val moreInfo = moreInfoEditText.text?.toString().orEmpty()

        Log.i("MainActivity", "Continue Button Clicked. Data to send:" +
                " Handicap=$handicap," +
                " Category='$categoryToUse'," +
                " Brand='$brandNameToUse'," +
                " ModelName='${selectedModelFromSpinner.name}'," +
                " ModelYear=${selectedModelFromSpinner.year}," +
                " ModelSubCat='${selectedModelFromSpinner.subCategory}'," +
                " MoreInfo='$moreInfo'")

        val intent = Intent(this, CompareActivity::class.java).apply {
            putExtra("handicap", handicap)
            putExtra("brand1", brandNameToUse)
            putExtra("category", categoryToUse) // This is the main category key like "DRIVER"
            putExtra("model1Name", selectedModelFromSpinner.name)
            putExtra("model1Year", selectedModelFromSpinner.year)
            // Send subCategoryType from your Model data class
            putExtra("model1SubCategory", selectedModelFromSpinner.subCategory)
            // The old "model1Category" which was model.category is now handled by the main "category" extra.
            // If CompareActivity still expects model.category from the Model object itself, adjust as needed.
            putExtra("moreInfo", moreInfo)
        }
        startActivity(intent)
    }
}
