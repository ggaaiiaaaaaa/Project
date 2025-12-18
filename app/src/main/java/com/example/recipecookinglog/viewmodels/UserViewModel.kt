package com.example.recipecookinglog.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipecookinglog.repositories.UserRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

class UserViewModel : ViewModel() {

    private val repository = UserRepository()

    private val _currentUser = MutableLiveData<FirebaseUser?>()
    val currentUser: LiveData<FirebaseUser?> = _currentUser

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _success = MutableLiveData<Boolean>()
    val success: LiveData<Boolean> = _success

    init {
        _currentUser.value = repository.getCurrentUser()
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _loading.value = true
            val result = repository.signIn(email, password)
            _loading.value = false

            if (result.isSuccess) {
                _currentUser.value = result.getOrNull()
                _success.value = true
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Login failed"
            }
        }
    }

    fun signUp(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _loading.value = true
            val result = repository.signUp(email, password, displayName)
            _loading.value = false

            if (result.isSuccess) {
                _currentUser.value = result.getOrNull()
                _success.value = true
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Registration failed"
            }
        }
    }

    fun signOut() {
        repository.signOut()
        _currentUser.value = null
    }

    fun clearError() {
        _error.value = null
    }
}