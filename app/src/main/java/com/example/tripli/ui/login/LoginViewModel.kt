package com.example.tripli.ui.login

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tripli.data.model.AppUser
import com.example.tripli.data.model.TravelPost
import com.example.tripli.data.repository.FeaturedPostsRepository
import com.example.tripli.data.repository.UserRepository
import kotlinx.coroutines.launch

class LoginViewModel(
    private val userRepository: UserRepository,
    featuredPostsRepository: FeaturedPostsRepository
) : ViewModel() {

    private val _posts = MutableLiveData(featuredPostsRepository.getFeaturedPosts())
    val posts: LiveData<List<TravelPost>> = _posts

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _message = MutableLiveData<String?>(null)
    val message: LiveData<String?> = _message

    private val _navigationUser = MutableLiveData<AppUser?>(null)
    val navigationUser: LiveData<AppUser?> = _navigationUser

    init {
        checkExistingSession()
    }

    fun signInWithGoogleIdToken(idToken: String) {
        if (_isLoading.value == true) {
            Log.d(TAG, "signInWithGoogleIdToken: already loading, ignoring request")
            return
        }

        Log.d(TAG, "signInWithGoogleIdToken: starting sign-in process")
        viewModelScope.launch {
            _isLoading.value = true
            Log.d(TAG, "signInWithGoogleIdToken: calling userRepository.signInWithGoogle")
            userRepository.signInWithGoogle(idToken)
                .onSuccess { user ->
                    Log.d(TAG, "signInWithGoogleIdToken: success! user=${user.uid}")
                    _navigationUser.value = user
                }
                .onFailure { throwable ->
                    Log.e(TAG, "signInWithGoogleIdToken: failure", throwable)
                    _message.value = throwable.localizedMessage ?: "Google sign-in failed."
                }
            Log.d(TAG, "signInWithGoogleIdToken: setting isLoading to false")
            _isLoading.value = false
        }
    }

    fun onGoogleFlowFailed(message: String) {
        Log.d(TAG, "onGoogleFlowFailed: message=$message")
        _message.value = message
    }

    fun onNavigationHandled() {
        _navigationUser.value = null
    }

    fun onMessageShown() {
        _message.value = null
    }

    private fun checkExistingSession() {
        Log.d(TAG, "checkExistingSession: checking...")
        viewModelScope.launch {
            userRepository.syncSignedInUser()?.let { user ->
                Log.d(TAG, "checkExistingSession: found existing session for user=${user.uid}")
                _navigationUser.value = user
            } ?: Log.d(TAG, "checkExistingSession: no existing session")
        }
    }

    class Factory(
        private val userRepository: UserRepository,
        private val featuredPostsRepository: FeaturedPostsRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                return LoginViewModel(userRepository, featuredPostsRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    companion object {
        private const val TAG = "LoginViewModel"
    }
}
