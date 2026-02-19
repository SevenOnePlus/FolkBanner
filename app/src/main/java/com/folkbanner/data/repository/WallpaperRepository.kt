package com.folkbanner.data.repository

import com.folkbanner.data.model.WallpaperApi
import com.folkbanner.data.model.WallpaperItem
import com.folkbanner.data.remote.ApiService
import com.folkbanner.utils.DedupHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class WallpaperRepository(
    private val apiService: ApiService = ApiService(),
    private val dedupHelper: DedupHelper = DedupHelper()
) {
    
    private var apis: List<WallpaperApi> = emptyList()
    
    suspend fun loadApis(baseUrl: String): List<WallpaperApi> = withContext(Dispatchers.IO) {
        apis = apiService.fetchApiList(baseUrl)
        apis
    }
    
    suspend fun fetchSingleWallpaper(apiIndex: Int): WallpaperItem? = withContext(Dispatchers.IO) {
        if (apiIndex < 0 || apiIndex >= apis.size) {
            return@withContext null
        }

        val api = apis[apiIndex]
        var attempts = 0
        val maxAttempts = 10

        while (attempts < maxAttempts) {
            attempts++
            try {
                val url = apiService.fetchWallpaperUrl(api.url)
                if (dedupHelper.isDuplicate(url).not()) {
                    dedupHelper.add(url)
                    return@withContext WallpaperItem(
                        id = UUID.randomUUID().toString(),
                        url = url,
                        apiName = api.name
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        null
    }
    
    fun clearCurrentApiCache() {
        dedupHelper.clear()
    }
    
}
