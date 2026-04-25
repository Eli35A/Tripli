package com.example.tripli.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tripli.data.model.Comment
import com.example.tripli.data.model.HomePost
import com.example.tripli.data.repository.HomePostRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: HomePostRepository) : ViewModel() {

    private val _posts = MutableLiveData<List<HomePost>>(repository.getHomePosts())
    val posts: LiveData<List<HomePost>> = _posts

    private val _isRefreshing = MutableLiveData(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(1000)
            _posts.value = repository.getHomePosts()
            _isRefreshing.value = false
        }
    }

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

    fun addComment(postId: String, text: String) {
        val newComment = Comment(
            id = System.currentTimeMillis().toString(),
            userName = "You",
            userInitial = "Y",
            userAccentHex = "#39C4E7",
            text = text,
            timeAgo = "Just now"
        )
        _posts.value = _posts.value?.map {
            if (it.id == postId) it.copy(
                comments = it.comments + newComment,
                commentCount = it.commentCount + 1
            ) else it
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
