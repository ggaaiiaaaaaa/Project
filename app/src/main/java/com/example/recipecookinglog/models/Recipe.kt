package com.example.recipecookinglog.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId
import java.io.Serializable

@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey
    @DocumentId
    var id: String = "",
    var name: String = "",
    var ingredients: String = "",
    var steps: String = "",
    var imageUrl: String = "",
    var rating: Float = 0f,
    var cuisine: String = "",
    var mealType: String = "",
    var userId: String = "",
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    var isSynced: Boolean = false
) : Serializable {

    constructor() : this("", "", "", "", "", 0f, "", "", "", 0L, 0L, false)

    fun toMap(): Map<String, Any> {
        return hashMapOf(
            "id" to id,
            "name" to name,
            "ingredients" to ingredients,
            "steps" to steps,
            "imageUrl" to imageUrl,
            "rating" to rating,
            "cuisine" to cuisine,
            "mealType" to mealType,
            "userId" to userId,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }
}