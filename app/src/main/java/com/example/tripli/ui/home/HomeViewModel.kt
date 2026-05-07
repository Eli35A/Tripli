package com.example.tripli.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tripli.data.model.Comment
import com.example.tripli.data.model.HomePost
import com.example.tripli.data.repository.HomePostRepository
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: HomePostRepository) : ViewModel() {

    private val allPosts = mutableListOf<HomePost>()

    private val _posts = MutableLiveData<List<HomePost>>(emptyList())
    val posts: LiveData<List<HomePost>> = _posts

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isRefreshing = MutableLiveData(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _isLoadingMore = MutableLiveData(false)
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    private val _canLoadMore = MutableLiveData(false)
    val canLoadMore: LiveData<Boolean> = _canLoadMore

    private val _comments = MutableLiveData<List<Comment>>(emptyList())
    val comments: LiveData<List<Comment>> = _comments

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        // Show cached posts immediately while network loads
        viewModelScope.launch {
            val cached = repository.getCachedPosts()
            if (cached.isNotEmpty()) {
                allPosts.addAll(cached)
                _posts.value = allPosts.toList()
            }
        }
        loadFirstPage(showLoader = true)
    }

    private fun loadFirstPage(showLoader: Boolean = false) {
        viewModelScope.launch {
            if (showLoader) _isLoading.value = true
            runCatching { repository.getFirstPage() }
                .onSuccess { fresh ->
                    allPosts.clear()
                    allPosts.addAll(fresh)
                    _posts.value = allPosts.toList()
                    _canLoadMore.value = repository.hasMorePages()
                }
                .onFailure { e ->
                    // Keep cached data visible; only show error if nothing is displayed
                    if (allPosts.isEmpty()) _errorMessage.value = e.localizedMessage
                }
            if (showLoader) _isLoading.value = false
        }
    }

    fun loadNextPage() {
        if (_isLoadingMore.value == true || !repository.hasMorePages()) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            runCatching { repository.getNextPage() }
                .onSuccess { newPosts ->
                    allPosts.addAll(newPosts)
                    _posts.value = allPosts.toList()
                    _canLoadMore.value = repository.hasMorePages()
                }
                .onFailure { _errorMessage.value = it.localizedMessage }
            _isLoadingMore.value = false
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            runCatching { repository.getFirstPage() }
                .onSuccess { fresh ->
                    allPosts.clear()
                    allPosts.addAll(fresh)
                    _posts.value = allPosts.toList()
                    _canLoadMore.value = repository.hasMorePages()
                }
                .onFailure { _errorMessage.value = it.localizedMessage }
            _isRefreshing.value = false
        }
    }

    fun onLikeToggled(post: HomePost) {
        updatePost(post.copy(
            isLiked = !post.isLiked,
            likeCount = if (post.isLiked) post.likeCount - 1 else post.likeCount + 1
        ))
        viewModelScope.launch {
            runCatching { repository.toggleLike(post) }
                .onFailure { updatePost(post) }
        }
    }

    fun onSaveToggled(post: HomePost) {
        updatePost(post.copy(isSaved = !post.isSaved))
        viewModelScope.launch {
            runCatching { repository.toggleSave(post) }
                .onFailure { updatePost(post) }
        }
    }

    fun loadComments(postId: String) {
        _comments.value = emptyList()
        viewModelScope.launch {
            runCatching { repository.getComments(postId) }
                .onSuccess { _comments.value = it }
                .onFailure { _errorMessage.value = it.localizedMessage }
        }
    }

    fun addComment(postId: String, text: String) {
        viewModelScope.launch {
            runCatching { repository.addComment(postId, text) }
                .onSuccess { comment ->
                    _comments.value = (_comments.value ?: emptyList()) + comment
                    val current = _posts.value?.find { it.id == postId } ?: return@onSuccess
                    updatePost(current.copy(commentCount = current.commentCount + 1))
                }
                .onFailure { _errorMessage.value = it.localizedMessage }
        }
    }

    fun onErrorShown() { _errorMessage.value = null }

    private fun updatePost(post: HomePost) {
        val idx = allPosts.indexOfFirst { it.id == post.id }
        if (idx != -1) allPosts[idx] = post
        _posts.value = allPosts.toList()
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
