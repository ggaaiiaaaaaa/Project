package com.example.recipecookinglog.repositories

import com.example.recipecookinglog.models.User
import com.example.recipecookinglog.utils.FirebaseHelper
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository {

    private val firebaseHelper = FirebaseHelper()

    fun getCurrentUser(): FirebaseUser? {
        return firebaseHelper.getCurrentUser()
    }

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return withContext(Dispatchers.IO) {
            firebaseHelper.signIn(email, password)
        }
    }

    suspend fun signUp(email: String, password: String, displayName: String): Result<FirebaseUser> {
        return withContext(Dispatchers.IO) {
            firebaseHelper.signUp(email, password, displayName)
        }
    }

    fun signOut() {
        firebaseHelper.signOut()
    }
}