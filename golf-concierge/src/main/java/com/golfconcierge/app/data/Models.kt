package com.golfconcierge.app.data

import android.content.Context
import androidx.annotation.RawRes
import com.golfconcierge.app.R
import org.json.JSONArray

data class ProsConsResult(
    val model1Pros: String,
    val model1Cons: String,
    val model2Pros: String,
    val model2Cons: String,
    val summary: String
)
data class Brand(
    val name: String
)

data class Model(
    val brandName: String,
    val category: String,
    val subCategory: String,
    val name: String,
    val year: Int
)

object InMemoryGolfRepository {
    private var brands: List<Brand> = emptyList()
    private var models: List<Model> = emptyList()

    fun initialize(context: Context, @RawRes rawResId: Int = R.raw.golf_data2) {
        val json = context.resources.openRawResource(rawResId).use { it.readBytes().toString(Charsets.UTF_8) }
        parseFromJson(json)
    }

    private fun parseFromJson(json: String) {
        val parsedModels = mutableListOf<Model>()
        val brandNames = mutableSetOf<String>() // Use a Set to automatically handle unique brand names

        // The root of the JSON is now an array
        val rootJsonArray = JSONArray(json)

        for (i in 0 until rootJsonArray.length()) {
            val modelObj = rootJsonArray.getJSONObject(i)

            val brandName = modelObj.getString("brand")
            brandNames.add(brandName) // Add to our set of brand names

            val model = Model(
                brandName = brandName,
                category = modelObj.getString("category"),
                subCategory = modelObj.getString("subcategory"), // Assuming key is "subcategory"
                name = modelObj.getString("name"),
                year = modelObj.getInt("year")
            )
            parsedModels.add(model)
        }

        // Convert the set of unique brand names to a list of Brand objects
        val parsedBrands = brandNames.map { Brand(it) }.sortedBy { it.name }

        // Assign to the repository's properties
        this.models = parsedModels.sortedWith( // Optional: sort models if needed
            compareBy<Model> { it.brandName }
                .thenBy { it.category }
                .thenBy { it.subCategory }
                .thenByDescending { it.year }
                .thenBy { it.name }
        )
        this.brands = parsedBrands
    }

    fun getBrands(): List<Brand> = brands

    fun getCategories(): List<String> = models.map { it.category }.distinct().sorted()

    fun getModelsForBrand(
        brandNameFilter: String, // Parameter name changed for clarity
        mainCategory: String? = null,
        excludeKey: ModelKey? = null
    ): List<Model> {
        return models
            .filter { it.brandName.equals(brandNameFilter, ignoreCase = true) } // Filter by model.brandName
            .filter { mainCategory == null || it.category.equals(mainCategory, ignoreCase = true) }
            .filter { excludeKey == null || ModelKey.from(it) != excludeKey }
            .sortedWith(
                compareBy<Model> { it.category }
                    .thenBy { it.subCategory }
                    .thenByDescending { it.year }
                    .thenBy { it.name }
            )
    }

    fun getAllModels(
        mainCategory: String? = null,
        subCategoryType: String? = null,
        excludeKey: ModelKey? = null
    ): List<Model> {
        return models
            .filter { mainCategory == null || it.category.equals(mainCategory, ignoreCase = true) }
            .filter { subCategoryType == null || it.subCategory.equals(subCategoryType, ignoreCase = true) }
            .filter { excludeKey == null || ModelKey.from(it) != excludeKey }
            .sortedWith(
                compareBy<Model> { it.brandName } // Sort by brandName
                    .thenBy { it.category }
                    .thenBy { it.subCategory }
                    .thenByDescending { it.year }
                    .thenBy { it.name }
            )
    }

    fun displayCategory(category: String): String {
        return category.replace('_', ' ').lowercase().split(' ').joinToString(" ") { part ->
            if (part.isEmpty()) part else part.replaceFirstChar { it.titlecase() }
        }
    }
}

data class ModelKey(
    val brandName: String, // Stays as brandName
    val category: String,
    val subCategory: String,
    val name: String,
    val year: Int
) {
    companion object {
        fun from(model: Model): ModelKey = ModelKey(
            model.brandName, // Use model.brandName
            model.category,
            model.subCategory,
            model.name,
            model.year
        )
    }
}


