package com.example.tripli.features.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.tripli.model.HomePost

class PostDetailViewModel : ViewModel() {

    private val _post = MutableLiveData<HomePost>()
    val post: LiveData<HomePost> = _post

    fun init(post: HomePost) {
        if (_post.value == null) _post.value = post
    }
}
