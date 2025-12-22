package com.example.recipecookinglog.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.recipecookinglog.R
import com.example.recipecookinglog.databinding.ActivityAddEditRecipeBinding
import com.example.recipecookinglog.models.Recipe
import com.example.recipecookinglog.utils.Constants
import com.example.recipecookinglog.viewmodels.RecipeViewModel

class AddEditRecipeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditRecipeBinding
    private val recipeViewModel: RecipeViewModel by viewModels()
    private var selectedImageUri: Uri? = null
    private var currentRecipe: Recipe? = null
    private var isEditMode = false

    // Use GetContent for simpler, more reliable image picking
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { originalUri ->
            android.util.Log.d("AddEditRecipe", "Image selected: $originalUri")
            
            // Copy the image to app's internal storage to ensure reliable access
            try {
                val copiedUri = copyImageToInternalStorage(originalUri)
                if (copiedUri != null) {
                    selectedImageUri = copiedUri
                    android.util.Log.d("AddEditRecipe", "Image copied to: $copiedUri")
                } else {
                    selectedImageUri = originalUri
                    android.util.Log.d("AddEditRecipe", "Using original URI: $originalUri")
                }
                
                Glide.with(this)
                    .load(selectedImageUri)
                    .centerCrop()
                    .into(binding.ivRecipeImage)
                binding.btnSelectImage.text = "Change Image"
            } catch (e: Exception) {
                android.util.Log.e("AddEditRecipe", "Error handling image: ${e.message}", e)
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun copyImageToInternalStorage(sourceUri: Uri): Uri? {
        return try {
            val inputStream = contentResolver.openInputStream(sourceUri) ?: return null
            val fileName = "recipe_image_${System.currentTimeMillis()}.jpg"
            val outputFile = java.io.File(filesDir, fileName)
            
            inputStream.use { input ->
                java.io.FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            android.net.Uri.fromFile(outputFile)
        } catch (e: Exception) {
            android.util.Log.e("AddEditRecipe", "Failed to copy image: ${e.message}", e)
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditRecipeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupSpinners()
        setupObservers()
        setupClickListeners()

        // Check if editing existing recipe
        intent.getSerializableExtra(Constants.EXTRA_RECIPE)?.let { recipe ->
            currentRecipe = recipe as Recipe
            isEditMode = true
            displayRecipe(currentRecipe!!)
            supportActionBar?.title = "Edit Recipe"
            binding.btnSave.text = "Update Recipe"
        }
    }

    private fun setupSpinners() {
        // Cuisine spinner
        val cuisineAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            Constants.CUISINES.filter { it != "All" }
        )
        cuisineAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCuisine.adapter = cuisineAdapter

        // Meal type spinner
        val mealTypeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            Constants.MEAL_TYPES.filter { it != "All" }
        )
        mealTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMealType.adapter = mealTypeAdapter
    }

    private fun setupObservers() {
        recipeViewModel.loading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSave.isEnabled = !isLoading
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
                finish()
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnSelectImage.setOnClickListener {
            // GetContent launcher only needs the MIME type
            imagePickerLauncher.launch("image/*")
        }

        binding.btnSave.setOnClickListener {
            saveRecipe()
        }
    }

    private fun displayRecipe(recipe: Recipe) {
        binding.apply {
            etRecipeName.setText(recipe.name)
            etIngredients.setText(recipe.ingredients)
            etSteps.setText(recipe.steps)
            ratingBar.rating = recipe.rating

            // Set spinner selections
            val cuisinePosition = Constants.CUISINES.indexOf(recipe.cuisine) - 1
            if (cuisinePosition >= 0) {
                spinnerCuisine.setSelection(cuisinePosition)
            }

            val mealTypePosition = Constants.MEAL_TYPES.indexOf(recipe.mealType) - 1
            if (mealTypePosition >= 0) {
                spinnerMealType.setSelection(mealTypePosition)
            }

            if (recipe.imageUrl.isNotEmpty()) {
                Glide.with(this@AddEditRecipeActivity)
                    .load(recipe.imageUrl)
                    .centerCrop()
                    .into(ivRecipeImage)
                btnSelectImage.text = "Change Image"
            }
        }
    }

    private fun saveRecipe() {
        binding.apply {
            val name = etRecipeName.text.toString().trim()
            val ingredients = etIngredients.text.toString().trim()
            val steps = etSteps.text.toString().trim()
            val rating = ratingBar.rating
            val cuisine = spinnerCuisine.selectedItem.toString()
            val mealType = spinnerMealType.selectedItem.toString()

            if (!validateInput(name, ingredients, steps)) {
                return
            }

            if (isEditMode && currentRecipe != null) {
                currentRecipe!!.apply {
                    this.name = name
                    this.ingredients = ingredients
                    this.steps = steps
                    this.rating = rating
                    this.cuisine = cuisine
                    this.mealType = mealType
                }
                recipeViewModel.updateRecipe(currentRecipe!!, selectedImageUri)
            } else {
                val newRecipe = Recipe(
                    name = name,
                    ingredients = ingredients,
                    steps = steps,
                    rating = rating,
                    cuisine = cuisine,
                    mealType = mealType
                )
                recipeViewModel.addRecipe(newRecipe, selectedImageUri)
            }
        }
    }

    private fun validateInput(name: String, ingredients: String, steps: String): Boolean {
        if (name.isEmpty()) {
            binding.tilRecipeName.error = "Recipe name is required"
            return false
        }
        if (ingredients.isEmpty()) {
            binding.tilIngredients.error = "Ingredients are required"
            return false
        }
        if (steps.isEmpty()) {
            binding.tilSteps.error = "Steps are required"
            return false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}