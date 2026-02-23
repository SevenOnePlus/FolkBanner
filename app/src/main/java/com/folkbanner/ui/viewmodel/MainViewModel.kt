package com.folkbanner.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.folkbanner.data.model.WallpaperApi
import com.folkbanner.data.model.WallpaperItem
import com.folkbanner.data.repository.WallpaperRepository
import com.folkbanner.utils.AppLogger
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
        if (settingsManager.useUpstreamApi) {
            loadUpstreamApis()
        } else {
            loadDownstreamWallpaper()
        }
    }

    fun loadRandomWallpaper(apiIndex: Int) {
        currentApiIndex = apiIndex
        if (!settingsManager.useUpstreamApi) {
            loadApis()
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

    private fun logApiMode() {
        val isUpstream = settingsManager.useUpstreamApi
        AppLogger.clear()
        AppLogger.log("API模式: ${if (isUpstream) "上游API" else "下游API"}")
        if (!isUpstream) {
            AppLogger.log("R18模式: ${if (settingsManager.r18Enabled) "已启用" else "已关闭"}")
        }
    }

    private fun loadUpstreamApis() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            logApiMode()
            
            try {
                val apiList = repository.loadApis(Constants.API_URL)
                _apis.value = apiList
                AppLogger.log("加载到 ${apiList.size} 个API")
                
                if (apiList.isNotEmpty()) {
                    loadRandomWallpaper(0)
                }
            } catch (e: Exception) {
                AppLogger.log("错误: ${e.message}")
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadDownstreamWallpaper() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            logApiMode()
            _apis.value = emptyList()
            
            AppLogger.log("开始加载下游API...")
            
            try {
                val item = repository.fetchDownstreamWallpaper(settingsManager.r18Enabled)
                if (item != null) {
                    AppLogger.log("壁纸加载成功!")
                    _currentWallpaper.value = item
                    AppLogger.toast("加载成功!")
                } else {
                    AppLogger.log("错误: 壁纸加载返回空")
                    throw Exception("Failed to load wallpaper")
                }
            } catch (e: Exception) {
                AppLogger.log("异常: ${e.message}")
                _error.value = e.message
                AppLogger.toast("加载失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
