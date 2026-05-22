package com.example.tripli.ui.profile

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tripli.data.model.AppUser
import com.example.tripli.data.model.HomePost
import com.example.tripli.di.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepo = ServiceLocator.provideUserRepository(application)
    private val postRepo = ServiceLocator.provideHomePostRepository(application)

    private val _user = MutableLiveData<AppUser?>()
    val user: LiveData<AppUser?> = _user

    private val _myPosts = MutableLiveData<List<HomePost>>(emptyList())
    val myPosts: LiveData<List<HomePost>> = _myPosts

    private val _likedPosts = MutableLiveData<List<HomePost>>(emptyList())
    val likedPosts: LiveData<List<HomePost>> = _likedPosts

    private val _isLoadingProfile = MutableLiveData(false)
    val isLoadingProfile: LiveData<Boolean> = _isLoadingProfile

    private val _isLoadingMyPosts = MutableLiveData(false)
    val isLoadingMyPosts: LiveData<Boolean> = _isLoadingMyPosts

    private val _isLoadingLikedPosts = MutableLiveData(false)
    val isLoadingLikedPosts: LiveData<Boolean> = _isLoadingLikedPosts

    private val _isSaving = MutableLiveData(false)
    val isSaving: LiveData<Boolean> = _isSaving

    private val _loggedOut = MutableLiveData(false)
    val loggedOut: LiveData<Boolean> = _loggedOut

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _isLoadingProfile.value = true
            val cached = userRepo.getCachedCurrentUser()
            if (cached != null) _user.value = cached

            val uid = userRepo.getCurrentUserId() ?: run {
                _isLoadingProfile.value = false
                return@launch
            }

            val fresh = userRepo.fetchAndCacheUser(uid)
            if (fresh != null) _user.value = fresh
            _isLoadingProfile.value = false

            refreshMyPosts(uid)
            refreshLikedPosts(uid)
        }
    }

    fun refreshMyPosts() {
        val uid = userRepo.getCurrentUserId() ?: return
        refreshMyPosts(uid)
    }

    fun refreshLikedPosts() {
        val uid = userRepo.getCurrentUserId() ?: return
        refreshLikedPosts(uid)
    }

    fun refreshAll() {
        val uid = userRepo.getCurrentUserId() ?: return
        refreshMyPosts(uid)
        refreshLikedPosts(uid)
    }

    private fun refreshMyPosts(uid: String) {
        viewModelScope.launch {
            _isLoadingMyPosts.value = true
            val cached = postRepo.getCachedUserPosts(uid)
            if (cached.isNotEmpty()) _myPosts.value = cached
            _myPosts.value = postRepo.getUserPosts(uid)
            _isLoadingMyPosts.value = false
        }
    }

    private fun refreshLikedPosts(uid: String) {
        viewModelScope.launch {
            _isLoadingLikedPosts.value = true
            val cached = postRepo.getCachedLikedPosts()
            if (cached.isNotEmpty()) _likedPosts.value = cached
            _likedPosts.value = postRepo.getLikedPosts(uid)
            _isLoadingLikedPosts.value = false
        }
    }

    fun signOut() {
        viewModelScope.launch {
            userRepo.signOut()
            _loggedOut.value = true
        }
    }

    fun updateProfile(name: String, bio: String, photoUri: Uri?) {
        val uid = userRepo.getCurrentUserId() ?: return
        viewModelScope.launch {
            _isSaving.value = true
            val localPhotoPath = photoUri?.let { savePhotoLocally(it) }
            userRepo.updateProfile(uid, name, bio, localPhotoPath)
            _user.value = userRepo.getCachedCurrentUser()
            _isSaving.value = false
        }
    }

    private suspend fun savePhotoLocally(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val ctx: Context = getApplication()
            val file = File(ctx.filesDir, "profile_photo.jpg")
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { input.copyTo(it) }
            }
            file.absolutePath
        }.getOrNull()
    }
}
