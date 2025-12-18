package com.example.recipecookinglog.utils

object Constants {
    const val DATABASE_NAME = "recipe_database"
    const val COLLECTION_RECIPES = "recipes"
    const val COLLECTION_USERS = "users"

    const val EXTRA_RECIPE_ID = "recipe_id"
    const val EXTRA_RECIPE = "recipe"

    const val PICK_IMAGE_REQUEST = 1001
    const val CAMERA_REQUEST = 1002

    val CUISINES = listOf(
        "All", "Italian", "Chinese", "Mexican", "Indian",
        "Japanese", "French", "Thai", "Mediterranean", "American"
    )

    val MEAL_TYPES = listOf(
        "All", "Breakfast", "Lunch", "Dinner",
        "Snack", "Dessert", "Appetizer"
    )
}