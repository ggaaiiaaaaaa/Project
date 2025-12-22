package com.example.recipecookinglog.utils

import android.net.Uri
import com.example.recipecookinglog.models.Recipe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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

    // Authentication methods
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            withTimeout(TIMEOUT_MS) {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                Result.success(result.user!!)
            }
        } catch (e: Exception) {
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
                val docRef = firestore.collection(Constants.COLLECTION_RECIPES).document()
                recipe.id = docRef.id
                recipe.userId = getCurrentUser()?.uid ?: ""
                recipe.createdAt = System.currentTimeMillis()
                recipe.updatedAt = System.currentTimeMillis()
                docRef.set(recipe.toMap()).await()
                Result.success(recipe.id)
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to save recipe: ${e.message}"))
        }
    }

    suspend fun updateRecipe(recipe: Recipe): Result<Unit> {
        return try {
            withTimeout(TIMEOUT_MS) {
                recipe.updatedAt = System.currentTimeMillis()
                firestore.collection(Constants.COLLECTION_RECIPES)
                    .document(recipe.id)
                    .update(recipe.toMap())
                    .await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update recipe: ${e.message}"))
        }
    }

    suspend fun deleteRecipe(recipeId: String): Result<Unit> {
        return try {
            withTimeout(TIMEOUT_MS) {
                firestore.collection(Constants.COLLECTION_RECIPES)
                    .document(recipeId)
                    .delete()
                    .await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to delete recipe: ${e.message}"))
        }
    }

    suspend fun getRecipes(cuisine: String? = null, mealType: String? = null): Result<List<Recipe>> {
        return try {
            withTimeout(TIMEOUT_MS) {
                val userId = getCurrentUser()?.uid ?: return@withTimeout Result.failure(
                    Exception("User not logged in")
                )

                // Start with base query - only filter by userId
                var query: Query = firestore.collection(Constants.COLLECTION_RECIPES)
                    .whereEqualTo("userId", userId)
                    .orderBy("updatedAt", Query.Direction.DESCENDING)

                val snapshot = query.get().await()
                var recipes = snapshot.documents.mapNotNull { it.toObject(Recipe::class.java) }

                // Apply filters in memory to avoid composite index requirements
                if (cuisine != null && cuisine != "All") {
                    recipes = recipes.filter { it.cuisine == cuisine }
                }

                if (mealType != null && mealType != "All") {
                    recipes = recipes.filter { it.mealType == mealType }
                }

                Result.success(recipes)
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to fetch recipes: ${e.message}"))
        }
    }

    // Image upload with progress
    suspend fun uploadImage(uri: Uri): Result<String> {
        return try {
            withTimeout(TIMEOUT_MS) {
                val fileName = "recipe_images/${UUID.randomUUID()}.jpg"
                val ref = storage.reference.child(fileName)

                // Upload file
                ref.putFile(uri).await()

                // Get download URL
                val url = ref.downloadUrl.await().toString()
                Result.success(url)
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to upload image: ${e.message}"))
        }
    }
}