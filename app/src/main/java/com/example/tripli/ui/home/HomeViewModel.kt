package com.example.tripli.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.tripli.data.model.HomePost
import com.example.tripli.data.repository.HomePostRepository

class HomeViewModel(private val repository: HomePostRepository) : ViewModel() {

    private val _posts = MutableLiveData<List<HomePost>>(repository.getHomePosts())
    val posts: LiveData<List<HomePost>> = _posts

    fun onLikeToggled(post: HomePost) {
        _posts.value = _posts.value?.map {
            if (it.id == post.id) it.copy(
                isLiked = !it.isLiked,
                likeCount = if (it.isLiked) it.likeCount - 1 else it.likeCount + 1
            ) else it
        }
    }

    fun onSaveToggled(post: HomePost) {
        _posts.value = _posts.value?.map {
            if (it.id == post.id) it.copy(isSaved = !it.isSaved) else it
        }
    }

    class Factory(private val repository: HomePostRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                return HomeViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
