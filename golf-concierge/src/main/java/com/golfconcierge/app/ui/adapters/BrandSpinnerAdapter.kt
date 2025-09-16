package com.golfconcierge.app.ui.adapters // Replace with your actual package structure

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.golfconcierge.app.R
import com.golfconcierge.app.data.Brand

class BrandSpinnerAdapter(
    context: Context,
    private val brands: List<Brand> // Your list of Brand objects from InMemoryGolfRepository
) : ArrayAdapter<Brand>(context, 0, brands) { // Pass 0 as layout ID, we inflate manually

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    // This method is called to get the view for the selected item (when spinner is closed)
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createCustomView(position, convertView, parent)
    }

    // This method is called to get the view for each item in the dropdown list
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createCustomView(position, convertView, parent)
    }

    private fun createCustomView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val viewHolder: ViewHolder

        if (convertView == null) {
            view = inflater.inflate(R.layout.list_item_brand_with_icon, parent, false)
            viewHolder = ViewHolder(
                view.findViewById(R.id.brand_icon_imageview),
                view.findViewById(R.id.brand_name_textview)
            )
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as ViewHolder
        }

        val brand = getItem(position) // getItem() is from ArrayAdapter

        if (brand != null) {
            // Display the brand name (optionally title-cased)
            viewHolder.nameTextView.text = brand.name.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }

            // Construct the drawable resource name based on the brand name
            // e.g., "TaylorMade" -> "taylormade"
            // This assumes your drawable files are named like "ping.png", "taylormade.png" (all lowercase)
            val resourceName = brand.name.lowercase()
            // .replace(" ", "_") // Uncomment and use if brand names could have spaces and files use underscores

            val resourceId = getDrawableResourceIdByName(context, resourceName)

            if (resourceId != 0) { // 0 means resource not found
                viewHolder.iconImageView.setImageResource(resourceId)
                viewHolder.iconImageView.visibility = View.VISIBLE
            } else {
                // If no specific icon is found:
                // Option 1: Hide the ImageView
                viewHolder.iconImageView.visibility = View.GONE
                // Option 2: Set a default placeholder icon
                // viewHolder.iconImageView.setImageResource(R.drawable.default_brand_icon)
                // viewHolder.iconImageView.visibility = View.VISIBLE
                Log.w("BrandSpinnerAdapter", "Drawable not found for brand: ${brand.name} (tried: $resourceName)")
            }
        } else {
            // Handle null brand object if that's possible in your data
            viewHolder.nameTextView.text = ""
            viewHolder.iconImageView.visibility = View.GONE
        }

        return view
    }

    private fun getDrawableResourceIdByName(context: Context, resourceName: String): Int {
        // This helper function dynamically finds a drawable resource ID by its name string.
        // The type is "drawable", and it searches in the app's package.
        return context.resources.getIdentifier(
            resourceName,
            "drawable",
            context.packageName
        )
    }

    // ViewHolder pattern for better performance by avoiding repeated findViewById calls
    private data class ViewHolder(val iconImageView: ImageView, val nameTextView: TextView)
}
