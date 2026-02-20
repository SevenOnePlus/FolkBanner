package com.folkbanner.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.folkbanner.data.model.WallpaperApi
import com.folkbanner.data.model.WallpaperItem
import com.folkbanner.data.repository.WallpaperRepository
import com.folkbanner.utils.Constants
import com.folkbanner.utils.SettingsManager
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WallpaperRepository()
    private val settingsManager = SettingsManager.getInstance(application)

    private val _apis = MutableLiveData<List<WallpaperApi>>()
    val apis: LiveData<List<WallpaperApi>> = _apis

    private val _currentWallpaper = MutableLiveData<WallpaperItem?>()
    val currentWallpaper: LiveData<WallpaperItem?> = _currentWallpaper

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var currentApiIndex = 0

    init {
        loadApis()
    }

    fun loadApis() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            if (settingsManager.useUpstreamApi) {
                try {
                    val apiList = repository.loadApis(Constants.UPSTREAM_API_URL)
                    _apis.value = apiList
                    if (apiList.isNotEmpty()) loadRandomWallpaper(0)
                } catch (e: Exception) {
                    _error.value = e.message
                } finally {
                    _isLoading.value = false
                }
            } else {
                _apis.value = emptyList()
                loadDownstreamWallpaper()
            }
        }
    }

    private suspend fun loadDownstreamWallpaper() {
        _isLoading.value = true
        _error.value = null
        try {
            val item = repository.fetchDownstreamWallpaper()
            _currentWallpaper.value = item ?: throw Exception("Failed to load wallpaper")
        } catch (e: Exception) {
            _error.value = e.message
        } finally {
            _isLoading.value = false
        }
    }

    fun loadRandomWallpaper(apiIndex: Int) {
        currentApiIndex = apiIndex
        
        if (!settingsManager.useUpstreamApi) {
            viewModelScope.launch { loadDownstreamWallpaper() }
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val item = repository.fetchSingleWallpaper(apiIndex)
                _currentWallpaper.value = item ?: throw Exception("Failed to load wallpaper")
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshWallpaper() {
        repository.clearCurrentApiCache()
        loadRandomWallpaper(currentApiIndex)
    }
}
