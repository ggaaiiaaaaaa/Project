package com.example.recipecookinglog.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
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

                // Set rating text (for the badge)
                val ratingText = if (recipe.rating > 0) String.format("%.1f", recipe.rating) else "N/A"
                tvRating.text = ratingText

                // Keep compatibility with rating bar if it exists
                ratingBar.rating = recipe.rating

                // Load image
                Log.d("RecipeAdapter", "Loading image for ${recipe.name}: ${recipe.imageUrl}")

                if (recipe.imageUrl.isNotEmpty()) {
                    Glide.with(itemView.context)
                        .load(recipe.imageUrl)
                        .centerCrop()
                        .placeholder(R.drawable.ic_recipe_placeholder)
                        .error(R.drawable.ic_recipe_placeholder)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(ivRecipeImage)
                } else {
                    Log.d("RecipeAdapter", "No image URL for ${recipe.name}, using placeholder")
                    ivRecipeImage.setImageResource(R.drawable.ic_recipe_placeholder)
                }

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