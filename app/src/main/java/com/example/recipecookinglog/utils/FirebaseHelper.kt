package com.example.recipecookinglog.utils

import android.net.Uri
import com.example.recipecookinglog.models.Recipe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseHelper {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    // Authentication methods
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, password: String, displayName: String): Result<FirebaseUser> {
        return try {
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
            val docRef = firestore.collection(Constants.COLLECTION_RECIPES).document()
            recipe.id = docRef.id
            recipe.userId = getCurrentUser()?.uid ?: ""
            docRef.set(recipe.toMap()).await()
            Result.success(recipe.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRecipe(recipe: Recipe): Result<Unit> {
        return try {
            recipe.updatedAt = System.currentTimeMillis()
            firestore.collection(Constants.COLLECTION_RECIPES)
                .document(recipe.id)
                .update(recipe.toMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRecipe(recipeId: String): Result<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_RECIPES)
                .document(recipeId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRecipes(cuisine: String? = null, mealType: String? = null): Result<List<Recipe>> {
        return try {
            val userId = getCurrentUser()?.uid ?: return Result.failure(Exception("User not logged in"))

            var query: Query = firestore.collection(Constants.COLLECTION_RECIPES)
                .whereEqualTo("userId", userId)

            if (cuisine != null && cuisine != "All") {
                query = query.whereEqualTo("cuisine", cuisine)
            }

            if (mealType != null && mealType != "All") {
                query = query.whereEqualTo("mealType", mealType)
            }

            val snapshot = query.get().await()
            val recipes = snapshot.documents.mapNotNull { it.toObject(Recipe::class.java) }
            Result.success(recipes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Image upload
    suspend fun uploadImage(uri: Uri): Result<String> {
        return try {
            val fileName = "recipe_images/${UUID.randomUUID()}.jpg"
            val ref = storage.reference.child(fileName)
            ref.putFile(uri).await()
            val url = ref.downloadUrl.await().toString()
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}