package com.example.recipecookinglog.utils

import android.net.Uri
import android.util.Log
import com.example.recipecookinglog.models.Recipe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.UUID

class FirebaseHelper {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    // Timeout for operations (30 seconds)
    private val TIMEOUT_MS = 30000L
    private val TAG = "FirebaseHelper"

    // Authentication methods
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            withTimeout(TIMEOUT_MS) {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                Result.success(result.user!!)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign in error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, password: String, displayName: String): Result<FirebaseUser> {
        return try {
            withTimeout(TIMEOUT_MS) {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user!!

                // Update profile
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                user.updateProfile(profileUpdates).await()

                // Create user document in Firestore
                val userMap = hashMapOf(
                    "uid" to user.uid,
                    "email" to email,
                    "displayName" to displayName
                )
                firestore.collection(Constants.COLLECTION_USERS)
                    .document(user.uid)
                    .set(userMap)
                    .await()

                Result.success(user)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign up error: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    // Recipe CRUD operations
    suspend fun addRecipe(recipe: Recipe): Result<String> {
        return try {
            withTimeout(TIMEOUT_MS) {
                val currentUser = getCurrentUser()
                if (currentUser == null) {
                    Log.e(TAG, "User not logged in")
                    return@withTimeout Result.failure(Exception("User not logged in"))
                }

                val docRef = firestore.collection(Constants.COLLECTION_RECIPES).document()
                recipe.id = docRef.id
                recipe.userId = currentUser.uid
                recipe.createdAt = System.currentTimeMillis()
                recipe.updatedAt = System.currentTimeMillis()

                Log.d(TAG, "Adding recipe: ${recipe.name} for user: ${currentUser.uid}")
                Log.d(TAG, "Recipe image URL: ${recipe.imageUrl}")

                docRef.set(recipe.toMap()).await()

                Log.d(TAG, "Recipe added successfully: ${recipe.id}")
                Result.success(recipe.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save recipe: ${e.message}", e)
            Result.failure(Exception("Failed to save recipe: ${e.message}"))
        }
    }

    suspend fun updateRecipe(recipe: Recipe): Result<Unit> {
        return try {
            withTimeout(TIMEOUT_MS) {
                recipe.updatedAt = System.currentTimeMillis()

                Log.d(TAG, "Updating recipe: ${recipe.id}")
                Log.d(TAG, "Recipe image URL: ${recipe.imageUrl}")

                firestore.collection(Constants.COLLECTION_RECIPES)
                    .document(recipe.id)
                    .update(recipe.toMap())
                    .await()

                Log.d(TAG, "Recipe updated successfully")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update recipe: ${e.message}", e)
            Result.failure(Exception("Failed to update recipe: ${e.message}"))
        }
    }

    suspend fun deleteRecipe(recipeId: String): Result<Unit> {
        return try {
            withTimeout(TIMEOUT_MS) {
                Log.d(TAG, "Deleting recipe: $recipeId")

                firestore.collection(Constants.COLLECTION_RECIPES)
                    .document(recipeId)
                    .delete()
                    .await()

                Log.d(TAG, "Recipe deleted successfully")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete recipe: ${e.message}", e)
            Result.failure(Exception("Failed to delete recipe: ${e.message}"))
        }
    }

    suspend fun getRecipes(cuisine: String? = null, mealType: String? = null): Result<List<Recipe>> {
        return try {
            withTimeout(TIMEOUT_MS) {
                val currentUser = getCurrentUser()
                if (currentUser == null) {
                    Log.e(TAG, "User not logged in")
                    return@withTimeout Result.failure(Exception("User not logged in"))
                }

                Log.d(TAG, "Fetching recipes for user: ${currentUser.uid}, cuisine: $cuisine, mealType: $mealType")

                // Simple query - just filter by userId
                val snapshot = firestore.collection(Constants.COLLECTION_RECIPES)
                    .whereEqualTo("userId", currentUser.uid)
                    .get()
                    .await()

                Log.d(TAG, "Fetched ${snapshot.documents.size} documents from Firestore")

                // Manually parse documents to handle missing fields gracefully
                var recipes = snapshot.documents.mapNotNull { doc ->
                    try {
                        val data = doc.data
                        if (data == null) {
                            Log.w(TAG, "Document ${doc.id} has no data")
                            return@mapNotNull null
                        }

                        // Create Recipe with safe defaults for missing fields
                        val recipe = Recipe(
                            id = doc.id,
                            name = data["name"] as? String ?: "",
                            ingredients = data["ingredients"] as? String ?: "",
                            steps = data["steps"] as? String ?: "",
                            imageUrl = data["imageUrl"] as? String ?: "",
                            rating = (data["rating"] as? Number)?.toFloat() ?: 0f,
                            cuisine = data["cuisine"] as? String ?: "",
                            mealType = data["mealType"] as? String ?: "",
                            userId = data["userId"] as? String ?: "",
                            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                            isSynced = false
                        )

                        Log.d(TAG, "Parsed recipe: ${recipe.name}, id: ${recipe.id}, imageUrl: ${recipe.imageUrl}")
                        recipe
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing recipe from document ${doc.id}: ${e.message}", e)
                        null
                    }
                }

                Log.d(TAG, "Successfully parsed ${recipes.size} recipes")

                // Apply filters in memory
                if (cuisine != null && cuisine != "All") {
                    recipes = recipes.filter { it.cuisine == cuisine }
                    Log.d(TAG, "After cuisine filter: ${recipes.size} recipes")
                }

                if (mealType != null && mealType != "All") {
                    recipes = recipes.filter { it.mealType == mealType }
                    Log.d(TAG, "After mealType filter: ${recipes.size} recipes")
                }

                // Sort by updatedAt in descending order (newest first)
                recipes = recipes.sortedByDescending { it.updatedAt }

                Log.d(TAG, "Returning ${recipes.size} recipes after filtering and sorting")

                Result.success(recipes)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch recipes: ${e.message}", e)
            e.printStackTrace()
            Result.failure(Exception("Failed to fetch recipes: ${e.message}"))
        }
    }

    // Image upload with byte array (NEW METHOD)
    suspend fun uploadImageBytes(byteArray: ByteArray): Result<String> {
        return try {
            withTimeout(TIMEOUT_MS) {
                val fileName = "recipe_images/${UUID.randomUUID()}.jpg"
                val ref = storage.reference.child(fileName)

                Log.d(TAG, "=== FIREBASE UPLOAD START ===")
                Log.d(TAG, "Uploading byte array, size: ${byteArray.size / 1024}KB")
                Log.d(TAG, "Target path: $fileName")

                // Upload byte array
                Log.d(TAG, "Starting putBytes...")
                val uploadTask = ref.putBytes(byteArray).await()
                Log.d(TAG, "putBytes completed, bytes transferred: ${uploadTask.bytesTransferred}")

                // Get download URL
                Log.d(TAG, "Getting download URL...")
                val url = ref.downloadUrl.await().toString()

                Log.d(TAG, "=== FIREBASE UPLOAD SUCCESS ===")
                Log.d(TAG, "Download URL: $url")

                Result.success(url)
            }
        } catch (e: Exception) {
            Log.e(TAG, "=== FIREBASE UPLOAD FAILED ===")
            Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Error message: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Failed to upload image: ${e.message}"))
        }
    }

    // Image upload with URI (KEPT FOR COMPATIBILITY)
    suspend fun uploadImage(uri: Uri): Result<String> {
        return try {
            withTimeout(TIMEOUT_MS) {
                val fileName = "recipe_images/${UUID.randomUUID()}.jpg"
                val ref = storage.reference.child(fileName)

                Log.d(TAG, "=== FIREBASE UPLOAD START ===")
                Log.d(TAG, "Input URI: $uri")
                Log.d(TAG, "URI scheme: ${uri.scheme}")
                Log.d(TAG, "Target path: $fileName")

                // Upload file
                Log.d(TAG, "Starting putFile...")
                val uploadTask = ref.putFile(uri).await()
                Log.d(TAG, "putFile completed, bytes transferred: ${uploadTask.bytesTransferred}")

                // Get download URL
                Log.d(TAG, "Getting download URL...")
                val url = ref.downloadUrl.await().toString()

                Log.d(TAG, "=== FIREBASE UPLOAD SUCCESS ===")
                Log.d(TAG, "Download URL: $url")

                Result.success(url)
            }
        } catch (e: Exception) {
            Log.e(TAG, "=== FIREBASE UPLOAD FAILED ===")
            Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Error message: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Failed to upload image: ${e.message}"))
        }
    }
}