package com.example.recipecookinglog.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.recipecookinglog.R
import com.example.recipecookinglog.databinding.ActivityRecipeDetailBinding
import com.example.recipecookinglog.models.Recipe
import com.example.recipecookinglog.utils.Constants
import com.example.recipecookinglog.viewmodels.RecipeViewModel
import kotlinx.coroutines.launch

class RecipeDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecipeDetailBinding
    private val recipeViewModel: RecipeViewModel by viewModels()
    private var currentRecipe: Recipe? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupObservers()
        loadRecipe()
    }

    private fun setupObservers() {
        recipeViewModel.loading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        recipeViewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                recipeViewModel.clearMessages()
            }
        }

        recipeViewModel.success.observe(this) { success ->
            success?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                recipeViewModel.clearMessages()
                if (it.contains("deleted")) {
                    finish()
                }
            }
        }
    }

    private fun loadRecipe() {
        val recipeId = intent.getStringExtra(Constants.EXTRA_RECIPE_ID)
        if (recipeId != null) {
            lifecycleScope.launch {
                currentRecipe = recipeViewModel.getRecipeById(recipeId)
                currentRecipe?.let { displayRecipe(it) }
            }
        }
    }

    private fun displayRecipe(recipe: Recipe) {
        binding.apply {
            tvRecipeName.text = recipe.name
            tvCuisine.text = "Cuisine: ${recipe.cuisine}"
            tvMealType.text = "Meal Type: ${recipe.mealType}"
            ratingBar.rating = recipe.rating
            tvIngredients.text = recipe.ingredients
            tvSteps.text = recipe.steps

            if (recipe.imageUrl.isNotEmpty()) {
                Glide.with(this@RecipeDetailActivity)
                    .load(recipe.imageUrl)
                    .centerCrop()
                    .into(ivRecipeImage)
            } else {
                ivRecipeImage.setImageResource(R.drawable.ic_recipe_placeholder)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_recipe_options, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_edit -> {
                currentRecipe?.let {
                    val intent = Intent(this, AddEditRecipeActivity::class.java)
                    intent.putExtra(Constants.EXTRA_RECIPE, it)
                    startActivity(intent)
                }
                true
            }
            R.id.action_delete -> {
                showDeleteDialog()
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDeleteDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Recipe")
            .setMessage("Are you sure you want to delete this recipe?")
            .setPositiveButton("Delete") { _, _ ->
                currentRecipe?.let { recipeViewModel.deleteRecipe(it) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
