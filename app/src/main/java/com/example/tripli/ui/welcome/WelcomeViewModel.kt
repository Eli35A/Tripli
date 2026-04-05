package com.example.tripli.ui.welcome

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tripli.data.model.AppUser
import com.example.tripli.data.repository.UserRepository
import kotlinx.coroutines.launch

class WelcomeViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _user = MutableLiveData<AppUser?>(null)
    val user: LiveData<AppUser?> = _user

    private val _isSigningOut = MutableLiveData(false)
    val isSigningOut: LiveData<Boolean> = _isSigningOut

    private val _signedOut = MutableLiveData(false)
    val signedOut: LiveData<Boolean> = _signedOut

    private val _message = MutableLiveData<String?>(null)
    val message: LiveData<String?> = _message

    fun loadUser(uid: String) {
        viewModelScope.launch {
            _user.value = userRepository.getCachedUser(uid)
        }
    }

    fun loadCurrentUser() {
        viewModelScope.launch {
            _user.value = userRepository.getCurrentUser()
        }
    }

    fun signOut() {
        if (_isSigningOut.value == true) return

        viewModelScope.launch {
            _isSigningOut.value = true
            runCatching {
                userRepository.signOut()
            }.onSuccess {
                _signedOut.value = true
            }.onFailure {
                _message.value = it.localizedMessage ?: "Failed to sign out."
            }
            _isSigningOut.value = false
        }
    }

    fun onSignedOutNavigated() {
        _signedOut.value = false
    }

    fun onMessageShown() {
        _message.value = null
    }

    class Factory(
        private val userRepository: UserRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WelcomeViewModel::class.java)) {
                return WelcomeViewModel(userRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
