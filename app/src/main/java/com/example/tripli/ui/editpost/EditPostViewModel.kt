package com.example.tripli.ui.editpost

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tripli.data.repository.HomePostRepository
import kotlinx.coroutines.launch

class EditPostViewModel(private val repository: HomePostRepository) : ViewModel() {

    private val _isSaving = MutableLiveData(false)
    val isSaving: LiveData<Boolean> = _isSaving

    private val _saveSuccess = MutableLiveData(false)
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    fun savePost(postId: String, imageBytes: ByteArray?, existingImageUrl: String, location: String, rating: Float, caption: String, hashtags: List<String>) {
        if (location.isBlank()) { _errorMessage.value = "Please enter a destination"; return }
        if (rating == 0f) { _errorMessage.value = "Please rate your experience"; return }
        if (caption.isBlank()) { _errorMessage.value = "Please write a review"; return }

        viewModelScope.launch {
            _isSaving.value = true
            runCatching {
                repository.updatePost(postId, imageBytes, existingImageUrl, location, rating, caption, hashtags)
            }.onSuccess {
                _saveSuccess.value = true
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Failed to save post"
            }
            _isSaving.value = false
        }
    }

    fun onErrorShown() { _errorMessage.value = null }
    fun resetSaveSuccess() { _saveSuccess.value = false }

    class Factory(private val repository: HomePostRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EditPostViewModel::class.java)) return EditPostViewModel(repository) as T
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
