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

class RecipeRepository(private val context: Context) {

    private val firebaseHelper = FirebaseHelper()
    private val recipeDao = RecipeDatabase.getDatabase(context).recipeDao()
    private val TAG = "RecipeRepository"

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
                Log.d(TAG, "Synced ${recipes.size} recipes from Firebase")
            }
            result
        }
    }

    // Add recipe (both Firebase and local)
    suspend fun addRecipe(recipe: Recipe, imageUri: Uri?): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "=== ADD RECIPE START ===")

                // Handle image upload to Firebase Storage if provided
                if (imageUri != null) {
                    Log.d(TAG, "Image URI provided: $imageUri")
                    Log.d(TAG, "URI scheme: ${imageUri.scheme}")

                    // Read and compress image from URI
                    val byteArray = readAndCompressImage(imageUri)

                    if (byteArray != null) {
                        Log.d(TAG, "Image compressed: ${byteArray.size / 1024}KB")

                        // Upload to Firebase Storage
                        val uploadResult = firebaseHelper.uploadImageBytes(byteArray)

                        if (uploadResult.isSuccess) {
                            val firebaseUrl = uploadResult.getOrNull()
                            recipe.imageUrl = firebaseUrl ?: ""
                            Log.d(TAG, "✓ Image uploaded to Firebase Storage")
                            Log.d(TAG, "✓ Firebase Storage URL: ${recipe.imageUrl}")
                        } else {
                            Log.e(TAG, "✗ Image upload failed: ${uploadResult.exceptionOrNull()?.message}")
                            recipe.imageUrl = ""
                        }
                    } else {
                        Log.e(TAG, "✗ Failed to compress image")
                        recipe.imageUrl = ""
                    }
                } else {
                    Log.d(TAG, "No image provided")
                    recipe.imageUrl = ""
                }

                // Add recipe to Firestore
                Log.d(TAG, "Saving recipe to Firestore...")
                val result = firebaseHelper.addRecipe(recipe)

                if (result.isSuccess) {
                    // Save to local database
                    recipe.isSynced = true
                    recipeDao.insertRecipe(recipe)
                    Log.d(TAG, "=== ADD RECIPE SUCCESS ===")
                    Log.d(TAG, "Recipe ID: ${recipe.id}")
                    Log.d(TAG, "Image URL in Firestore: ${recipe.imageUrl}")
                } else {
                    Log.e(TAG, "=== ADD RECIPE FAILED ===")
                    Log.e(TAG, "Error: ${result.exceptionOrNull()?.message}")
                }

                result

            } catch (e: Exception) {
                Log.e(TAG, "=== ADD RECIPE EXCEPTION ===")
                Log.e(TAG, "Error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    // Update recipe
    suspend fun updateRecipe(recipe: Recipe, imageUri: Uri?): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "=== UPDATE RECIPE START ===")
                Log.d(TAG, "Recipe ID: ${recipe.id}")
                Log.d(TAG, "Current image URL: ${recipe.imageUrl}")

                // Handle new image upload if provided
                if (imageUri != null) {
                    Log.d(TAG, "New image URI provided: $imageUri")

                    // Read and compress image
                    val byteArray = readAndCompressImage(imageUri)

                    if (byteArray != null) {
                        Log.d(TAG, "New image compressed: ${byteArray.size / 1024}KB")

                        // Upload new image to Firebase Storage
                        val uploadResult = firebaseHelper.uploadImageBytes(byteArray)

                        if (uploadResult.isSuccess) {
                            val oldImageUrl = recipe.imageUrl
                            val newImageUrl = uploadResult.getOrNull() ?: ""

                            // Delete old image from Firebase Storage
                            if (oldImageUrl.isNotEmpty() && oldImageUrl.contains("firebasestorage")) {
                                Log.d(TAG, "Deleting old image from Firebase Storage")
                                firebaseHelper.deleteImageFromUrl(oldImageUrl)
                            }

                            recipe.imageUrl = newImageUrl
                            Log.d(TAG, "✓ New image uploaded to Firebase Storage")
                            Log.d(TAG, "✓ New Firebase Storage URL: ${recipe.imageUrl}")
                        } else {
                            Log.e(TAG, "✗ New image upload failed: ${uploadResult.exceptionOrNull()?.message}")
                        }
                    } else {
                        Log.e(TAG, "✗ Failed to compress new image")
                    }
                } else {
                    Log.d(TAG, "No new image provided, keeping existing image URL")
                }

                // Update in Firestore
                Log.d(TAG, "Updating recipe in Firestore...")
                val result = firebaseHelper.updateRecipe(recipe)

                if (result.isSuccess) {
                    recipe.isSynced = true
                    recipeDao.updateRecipe(recipe)
                    Log.d(TAG, "=== UPDATE RECIPE SUCCESS ===")
                }

                result

            } catch (e: Exception) {
                Log.e(TAG, "=== UPDATE RECIPE EXCEPTION ===")
                Log.e(TAG, "Error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    // Delete recipe
    suspend fun deleteRecipe(recipe: Recipe): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "=== DELETE RECIPE START ===")
                Log.d(TAG, "Recipe ID: ${recipe.id}")

                // Delete image from Firebase Storage if exists
                if (recipe.imageUrl.isNotEmpty() && recipe.imageUrl.contains("firebasestorage")) {
                    Log.d(TAG, "Deleting image from Firebase Storage")
                    firebaseHelper.deleteImageFromUrl(recipe.imageUrl)
                }

                // Delete from Firestore
                val result = firebaseHelper.deleteRecipe(recipe.id)
                if (result.isSuccess) {
                    recipeDao.deleteRecipe(recipe)
                    Log.d(TAG, "=== DELETE RECIPE SUCCESS ===")
                }
                result
            } catch (e: Exception) {
                Log.e(TAG, "=== DELETE RECIPE EXCEPTION ===")
                Log.e(TAG, "Error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    // Get recipe by ID
    suspend fun getRecipeById(recipeId: String): Recipe? {
        return withContext(Dispatchers.IO) {
            recipeDao.getRecipeById(recipeId)
        }
    }

    /**
     * Read image from URI and compress to byte array
     * This works for both content:// URIs (from gallery) and file:// URIs
     */
    private fun readAndCompressImage(uri: Uri): ByteArray? {
        try {
            Log.d(TAG, "Reading image from URI: $uri")
            Log.d(TAG, "URI scheme: ${uri.scheme}")

            // Open input stream from content resolver
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream for URI")
                return null
            }

            // Decode bitmap from stream
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from stream")
                return null
            }

            Log.d(TAG, "Original image size: ${bitmap.width}x${bitmap.height}")

            // Calculate scaling to max 1024x1024
            val maxSize = 1024
            val scale = minOf(
                maxSize.toFloat() / bitmap.width,
                maxSize.toFloat() / bitmap.height,
                1.0f // Don't upscale
            )

            val newWidth = (bitmap.width * scale).toInt()
            val newHeight = (bitmap.height * scale).toInt()

            Log.d(TAG, "Resizing to: ${newWidth}x${newHeight}")

            // Create scaled bitmap
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

            // Compress to JPEG
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val byteArray = outputStream.toByteArray()

            Log.d(TAG, "Compressed to: ${byteArray.size / 1024}KB")

            // Clean up
            bitmap.recycle()
            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }

            return byteArray

        } catch (e: Exception) {
            Log.e(TAG, "Error reading/compressing image: ${e.message}", e)
            return null
        }
    }
}