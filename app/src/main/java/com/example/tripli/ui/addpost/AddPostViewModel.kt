package com.example.tripli.ui.addpost

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tripli.data.repository.HomePostRepository
import kotlinx.coroutines.launch

class AddPostViewModel(private val repository: HomePostRepository) : ViewModel() {

    private val _isSubmitting = MutableLiveData(false)
    val isSubmitting: LiveData<Boolean> = _isSubmitting

    private val _submitSuccess = MutableLiveData(false)
    val submitSuccess: LiveData<Boolean> = _submitSuccess

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    fun submitPost(imageBytes: ByteArray?, location: String, rating: Float, caption: String, hashtags: List<String>) {
        if (location.isBlank()) { _errorMessage.value = "Please enter a destination"; return }
        if (rating == 0f) { _errorMessage.value = "Please rate your experience"; return }
        if (caption.isBlank()) { _errorMessage.value = "Please write a review"; return }

        viewModelScope.launch {
            _isSubmitting.value = true
            runCatching {
                repository.createPost(imageBytes, location, rating, caption, hashtags)
            }.onSuccess {
                _submitSuccess.value = true
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Failed to share post"
            }
            _isSubmitting.value = false
        }
    }

    fun onErrorShown() { _errorMessage.value = null }
    fun resetSubmitSuccess() { _submitSuccess.value = false }

    class Factory(private val repository: HomePostRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AddPostViewModel::class.java)) return AddPostViewModel(repository) as T
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
