package com.example.recipecookinglog.repositories

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.LiveData
import com.example.recipecookinglog.models.Recipe
import com.example.recipecookinglog.utils.FirebaseHelper
import com.example.recipecookinglog.utils.RecipeDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class RecipeRepository(private val context: Context) {

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
                // Upload and compress image if provided
                if (imageUri != null) {
                    val compressedUri = compressImage(imageUri)
                    val imageResult = firebaseHelper.uploadImage(compressedUri)
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
                // Upload new compressed image if provided
                if (imageUri != null) {
                    val compressedUri = compressImage(imageUri)
                    val imageResult = firebaseHelper.uploadImage(compressedUri)
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

    // Compress image before uploading
    private fun compressImage(uri: Uri): Uri {
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)

            // Calculate new dimensions (max 1024x1024)
            val maxSize = 1024
            val ratio = Math.min(
                maxSize.toFloat() / bitmap.width,
                maxSize.toFloat() / bitmap.height
            )

            val width = (ratio * bitmap.width).toInt()
            val height = (ratio * bitmap.height).toInt()

            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)

            // Compress to JPEG with 75% quality
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)

            // Save to cache and return URI
            val path = MediaStore.Images.Media.insertImage(
                context.contentResolver,
                resizedBitmap,
                "compressed_${System.currentTimeMillis()}",
                null
            )

            bitmap.recycle()
            resizedBitmap.recycle()

            return Uri.parse(path) ?: uri
        } catch (e: Exception) {
            e.printStackTrace()
            return uri // Return original if compression fails
        }
    }
}