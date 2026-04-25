package com.example.tripli.ui.addpost

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tripli.data.repository.HomePostRepository
import kotlinx.coroutines.launch

class AddPostViewModel(private val repository: HomePostRepository) : ViewModel() {

    private val _selectedImages = MutableLiveData<List<Uri>>(emptyList())
    val selectedImages: LiveData<List<Uri>> = _selectedImages

    private val _isSubmitting = MutableLiveData(false)
    val isSubmitting: LiveData<Boolean> = _isSubmitting

    private val _submitSuccess = MutableLiveData(false)
    val submitSuccess: LiveData<Boolean> = _submitSuccess

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    fun onImagesSelected(uris: List<Uri>) {
        _selectedImages.value = uris
    }

    fun submitPost(
        location: String,
        rating: Float,
        caption: String,
        hashtags: List<String>
    ) {
        if (location.isBlank()) {
            _errorMessage.value = "Please enter a location."
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            runCatching {
                val imageUrl = _selectedImages.value?.firstOrNull()
                    ?.let { repository.uploadImage(it) }
                    ?: ""
                repository.createPost(
                    location = location.trim(),
                    rating = rating,
                    caption = caption.trim(),
                    hashtags = hashtags,
                    imageUrl = imageUrl
                )
            }.onSuccess {
                _submitSuccess.value = true
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Failed to share post."
            }
            _isSubmitting.value = false
        }
    }

    fun onErrorShown() { _errorMessage.value = null }

    class Factory(private val repository: HomePostRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AddPostViewModel::class.java)) {
                return AddPostViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
