package com.example.tripli.ui.map

import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tripli.data.model.HomePost
import com.example.tripli.data.repository.HomePostRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MapViewModel(
    private val repository: HomePostRepository,
    application: Application
) : AndroidViewModel(application) {

    data class PostWithLocation(val post: HomePost, val latLng: LatLng)

    private val _postLocations = MutableLiveData<List<PostWithLocation>>(emptyList())
    val postLocations: LiveData<List<PostWithLocation>> = _postLocations

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val geocodeCache = mutableMapOf<String, LatLng?>()

    init { loadPosts() }

    fun refresh() { loadPosts() }

    private fun loadPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val posts = repository.getAllCachedOrFirstPage()
                val items = withContext(Dispatchers.IO) {
                    posts.mapNotNull { post ->
                        geocodeLocation(post.location)?.let { PostWithLocation(post, it) }
                    }
                }
                _postLocations.value = items
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load posts"
            } finally {
                _isLoading.value = false
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun geocodeLocation(location: String): LatLng? {
        if (location.isBlank()) return null
        geocodeCache[location]?.let { return it }
        return try {
            val geocoder = Geocoder(getApplication(), Locale.getDefault())
            val results = geocoder.getFromLocationName(location, 1)
            val ll = results?.firstOrNull()?.let { LatLng(it.latitude, it.longitude) }
            geocodeCache[location] = ll
            ll
        } catch (e: Exception) {
            geocodeCache[location] = null
            null
        }
    }

    fun onErrorShown() { _errorMessage.value = null }

    class Factory(
        private val repository: HomePostRepository,
        private val application: Application
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MapViewModel(repository, application) as T
    }
}
