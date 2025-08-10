package com.example.wedsnap20.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wedsnap20.services.AuthService
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val _user = MutableStateFlow<FirebaseUser?>(null)
    private val _nameOverride = MutableStateFlow<String?>(null)

    private val authService = AuthService(application, this)

    val user = _user.asStateFlow()
    val nameOverride = _nameOverride.asStateFlow()

    fun setUser(user: FirebaseUser?) {
        _user.value = user
    }

    fun getCurrentUser(): FirebaseUser? {
        return _user.value
    }
    fun setNameOverride(name: String?) {
        _nameOverride.value = name
    }
    fun getNameOverride(): String? {
        return _nameOverride.value
    }
    fun getCurrentUserName(onResult: (String?) -> Unit) {
        val uid = _user.value?.uid
        if (uid != null) {
            authService.getCurrentUserNameFromDb(uid, onResult)
        } else {
            onResult(null)
        }
    }

}

