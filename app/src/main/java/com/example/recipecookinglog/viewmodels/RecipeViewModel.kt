package com.example.recipecookinglog.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.recipecookinglog.models.Recipe
import com.example.recipecookinglog.repositories.RecipeRepository
import kotlinx.coroutines.launch

class RecipeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RecipeRepository(application)

    val recipes: LiveData<List<Recipe>> = repository.getAllRecipesLocal()

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _success = MutableLiveData<String?>()
    val success: LiveData<String?> = _success

    // Sync recipes from Firebase
    fun syncRecipes(cuisine: String? = null, mealType: String? = null) {
        viewModelScope.launch {
            _loading.value = true
            val result = repository.syncRecipes(cuisine, mealType)
            _loading.value = false

            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to sync recipes"
            }
        }
    }

    // Add new recipe
    fun addRecipe(recipe: Recipe, imageUri: Uri?) {
        viewModelScope.launch {
            _loading.value = true
            val result = repository.addRecipe(recipe, imageUri)
            _loading.value = false

            if (result.isSuccess) {
                _success.value = "Recipe added successfully"
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to add recipe"
            }
        }
    }

    // Update recipe
    fun updateRecipe(recipe: Recipe, imageUri: Uri?) {
        viewModelScope.launch {
            _loading.value = true
            val result = repository.updateRecipe(recipe, imageUri)
            _loading.value = false

            if (result.isSuccess) {
                _success.value = "Recipe updated successfully"
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to update recipe"
            }
        }
    }

    // Delete recipe
    fun deleteRecipe(recipe: Recipe) {
        viewModelScope.launch {
            _loading.value = true
            val result = repository.deleteRecipe(recipe)
            _loading.value = false

            if (result.isSuccess) {
                _success.value = "Recipe deleted successfully"
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to delete recipe"
            }
        }
    }

    // Get recipe by ID
    suspend fun getRecipeById(recipeId: String): Recipe? {
        return repository.getRecipeById(recipeId)
    }

    fun clearMessages() {
        _error.value = null
        _success.value = null
    }
}
