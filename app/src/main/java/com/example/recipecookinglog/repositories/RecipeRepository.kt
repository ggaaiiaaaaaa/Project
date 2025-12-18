package com.example.recipecookinglog.repositories

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.recipecookinglog.models.Recipe
import com.example.recipecookinglog.utils.DatabaseHelper
import com.example.recipecookinglog.utils.FirebaseHelper
import com.example.recipecookinglog.utils.RecipeDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecipeRepository(context: Context) {

    private val firebaseHelper = FirebaseHelper()
    private val recipeDao = RecipeDatabase.getDatabase(context).recipeDao()

    // Get all recipes from Room (local)
    fun getAllRecipesLocal(): LiveData<List<Recipe>> {
        return recipeDao.getAllRecipes()
    }

    // Sync recipes from Firebase to local database
    suspend fun syncRecipes(cuisine: String? = null, mealType: String? = null): Result<List<Recipe>> {
        return withContext(Dispatchers.IO) {
            val result = firebaseHelper.getRecipes(cuisine, mealType)
            if (result.isSuccess) {
                val recipes = result.getOrNull() ?: emptyList()
                // Clear and insert all recipes
                recipeDao.deleteAllRecipes()
                recipes.forEach { recipe ->
                    recipe.isSynced = true
                    recipeDao.insertRecipe(recipe)
                }
            }
            result
        }
    }

    // Add recipe (both Firebase and local)
    suspend fun addRecipe(recipe: Recipe, imageUri: Uri?): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Upload image if provided
                if (imageUri != null) {
                    val imageResult = firebaseHelper.uploadImage(imageUri)
                    if (imageResult.isSuccess) {
                        recipe.imageUrl = imageResult.getOrNull() ?: ""
                    }
                }

                // Add to Firebase
                val result = firebaseHelper.addRecipe(recipe)
                if (result.isSuccess) {
                    recipe.isSynced = true
                    recipeDao.insertRecipe(recipe)
                }
                result
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Update recipe
    suspend fun updateRecipe(recipe: Recipe, imageUri: Uri?): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Upload new image if provided
                if (imageUri != null) {
                    val imageResult = firebaseHelper.uploadImage(imageUri)
                    if (imageResult.isSuccess) {
                        recipe.imageUrl = imageResult.getOrNull() ?: recipe.imageUrl
                    }
                }

                // Update in Firebase
                val result = firebaseHelper.updateRecipe(recipe)
                if (result.isSuccess) {
                    recipe.isSynced = true
                    recipeDao.updateRecipe(recipe)
                }
                result
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Delete recipe
    suspend fun deleteRecipe(recipe: Recipe): Result<Unit> {
        return withContext(Dispatchers.IO) {
            val result = firebaseHelper.deleteRecipe(recipe.id)
            if (result.isSuccess) {
                recipeDao.deleteRecipe(recipe)
            }
            result
        }
    }

    // Get recipe by ID
    suspend fun getRecipeById(recipeId: String): Recipe? {
        return withContext(Dispatchers.IO) {
            recipeDao.getRecipeById(recipeId)
        }
    }
}