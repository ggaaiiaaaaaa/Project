package com.example.recipecookinglog.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestListener
import com.example.recipecookinglog.R
import com.example.recipecookinglog.databinding.ItemRecipeBinding
import com.example.recipecookinglog.models.Recipe

class RecipeAdapter(
    private val onRecipeClick: (Recipe) -> Unit
) : ListAdapter<Recipe, RecipeAdapter.RecipeViewHolder>(RecipeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val binding = ItemRecipeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecipeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecipeViewHolder(
        private val binding: ItemRecipeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(recipe: Recipe) {
            binding.apply {
                tvRecipeName.text = recipe.name
                tvCuisine.text = recipe.cuisine
                tvMealType.text = recipe.mealType

                // Set rating text
                val ratingText = if (recipe.rating > 0) {
                    String.format("%.1f", recipe.rating)
                } else {
                    "N/A"
                }
                tvRating.text = ratingText
                ratingBar.rating = recipe.rating

                // Load image with detailed logging
                Log.d("RecipeAdapter", "=" .repeat(50))
                Log.d("RecipeAdapter", "Loading image for recipe: ${recipe.name}")
                Log.d("RecipeAdapter", "Recipe ID: ${recipe.id}")
                Log.d("RecipeAdapter", "Image URL: '${recipe.imageUrl}'")
                Log.d("RecipeAdapter", "Image URL is empty: ${recipe.imageUrl.isEmpty()}")
                Log.d("RecipeAdapter", "Image URL is blank: ${recipe.imageUrl.isBlank()}")

                when {
                    recipe.imageUrl.isNotEmpty() && recipe.imageUrl.isNotBlank() -> {
                        Log.d("RecipeAdapter", "Attempting to load image from URL: ${recipe.imageUrl}")

                        Glide.with(itemView.context)
                            .load(recipe.imageUrl)
                            .centerCrop()
                            .placeholder(R.drawable.ic_recipe_placeholder)
                            .error(R.drawable.ic_recipe_placeholder)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                                override fun onLoadFailed(
                                    e: com.bumptech.glide.load.engine.GlideException?,
                                    model: Any?,
                                    target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    Log.e("RecipeAdapter", "Failed to load image for ${recipe.name}")
                                    Log.e("RecipeAdapter", "URL was: ${recipe.imageUrl}")
                                    Log.e("RecipeAdapter", "Error: ${e?.message}")
                                    e?.logRootCauses("RecipeAdapter")
                                    return false
                                }

                                override fun onResourceReady(
                                    resource: android.graphics.drawable.Drawable,
                                    model: Any,
                                    target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                                    dataSource: com.bumptech.glide.load.DataSource,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    Log.d("RecipeAdapter", "Successfully loaded image for ${recipe.name}")
                                    Log.d("RecipeAdapter", "Data source: $dataSource")
                                    return false
                                }
                            })
                            .into(ivRecipeImage)
                    }
                    else -> {
                        Log.w("RecipeAdapter", "No image URL for ${recipe.name}, using placeholder")
                        ivRecipeImage.setImageResource(R.drawable.ic_recipe_placeholder)
                    }
                }

                Log.d("RecipeAdapter", "=" .repeat(50))

                // Set click listener
                root.setOnClickListener {
                    onRecipeClick(recipe)
                }
            }
        }
    }

    class RecipeDiffCallback : DiffUtil.ItemCallback<Recipe>() {
        override fun areItemsTheSame(oldItem: Recipe, newItem: Recipe): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Recipe, newItem: Recipe): Boolean {
            return oldItem == newItem
        }
    }
}