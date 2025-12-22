package com.example.recipecookinglog.repositories

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import com.example.recipecookinglog.models.Recipe
import com.example.recipecookinglog.utils.FirebaseHelper
import com.example.recipecookinglog.utils.RecipeDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

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
                // Handle image upload if provided
                if (imageUri != null) {
                    Log.d("RecipeRepository", "Starting image upload for URI: $imageUri")

                    // Compress the image
                    val compressedFile = compressImage(imageUri)

                    if (compressedFile != null) {
                        Log.d("RecipeRepository", "Image compressed successfully: ${compressedFile.absolutePath}")

                        // Upload to Firebase Storage
                        val imageResult = firebaseHelper.uploadImage(Uri.fromFile(compressedFile))

                        if (imageResult.isSuccess) {
                            val downloadUrl = imageResult.getOrNull()
                            recipe.imageUrl = downloadUrl ?: ""
                            Log.d("RecipeRepository", "Image uploaded successfully: ${recipe.imageUrl}")

                            // Clean up temporary file
                            compressedFile.delete()
                        } else {
                            Log.e("RecipeRepository", "Image upload failed: ${imageResult.exceptionOrNull()?.message}")
                            // Continue without image
                            recipe.imageUrl = ""
                        }
                    } else {
                        Log.e("RecipeRepository", "Image compression failed")
                        recipe.imageUrl = ""
                    }
                } else {
                    Log.d("RecipeRepository", "No image selected")
                    recipe.imageUrl = ""
                }

                // Add to Firebase Firestore
                val result = firebaseHelper.addRecipe(recipe)

                if (result.isSuccess) {
                    // Save to local database
                    recipe.isSynced = true
                    recipeDao.insertRecipe(recipe)
                    Log.d("RecipeRepository", "Recipe saved successfully with image URL: ${recipe.imageUrl}")
                } else {
                    Log.e("RecipeRepository", "Failed to save recipe to Firestore: ${result.exceptionOrNull()?.message}")
                }

                result

            } catch (e: Exception) {
                Log.e("RecipeRepository", "Error adding recipe: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    // Update recipe
    suspend fun updateRecipe(recipe: Recipe, imageUri: Uri?): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Handle new image upload if provided
                if (imageUri != null) {
                    Log.d("RecipeRepository", "Updating image for URI: $imageUri")

                    val compressedFile = compressImage(imageUri)

                    if (compressedFile != null) {
                        val imageResult = firebaseHelper.uploadImage(Uri.fromFile(compressedFile))

                        if (imageResult.isSuccess) {
                            recipe.imageUrl = imageResult.getOrNull() ?: recipe.imageUrl
                            Log.d("RecipeRepository", "Image updated successfully: ${recipe.imageUrl}")
                            compressedFile.delete()
                        } else {
                            Log.e("RecipeRepository", "Image update failed: ${imageResult.exceptionOrNull()?.message}")
                        }
                    }
                }

                // Update in Firebase
                val result = firebaseHelper.updateRecipe(recipe)

                if (result.isSuccess) {
                    recipe.isSynced = true
                    recipeDao.updateRecipe(recipe)
                    Log.d("RecipeRepository", "Recipe updated successfully")
                }

                result

            } catch (e: Exception) {
                Log.e("RecipeRepository", "Error updating recipe: ${e.message}", e)
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

    /**
     * Compress and save image to a temporary file
     * Returns the File object if successful, null otherwise
     */
    private fun compressImage(uri: Uri): File? {
        try {
            Log.d("RecipeRepository", "Compressing image from URI: $uri")

            // Read the image from URI
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) {
                Log.e("RecipeRepository", "Failed to decode bitmap from URI")
                return null
            }

            Log.d("RecipeRepository", "Original bitmap size: ${bitmap.width}x${bitmap.height}")

            // Calculate new dimensions (max 1024x1024)
            val maxSize = 1024
            val ratio = minOf(
                maxSize.toFloat() / bitmap.width,
                maxSize.toFloat() / bitmap.height
            )

            val newWidth = (ratio * bitmap.width).toInt()
            val newHeight = (ratio * bitmap.height).toInt()

            Log.d("RecipeRepository", "Resizing to: ${newWidth}x${newHeight}")

            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

            // Compress to JPEG with 80% quality
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val byteArray = outputStream.toByteArray()

            Log.d("RecipeRepository", "Compressed size: ${byteArray.size / 1024}KB")

            // Save to cache directory with unique name
            val cacheDir = context.cacheDir
            val imageFile = File(cacheDir, "recipe_${System.currentTimeMillis()}.jpg")
            val fileOutputStream = FileOutputStream(imageFile)
            fileOutputStream.write(byteArray)
            fileOutputStream.close()

            // Clean up bitmaps
            bitmap.recycle()
            resizedBitmap.recycle()

            Log.d("RecipeRepository", "Compressed image saved to: ${imageFile.absolutePath}")
            return imageFile

        } catch (e: Exception) {
            Log.e("RecipeRepository", "Error compressing image: ${e.message}", e)
            return null
        }
    }
}