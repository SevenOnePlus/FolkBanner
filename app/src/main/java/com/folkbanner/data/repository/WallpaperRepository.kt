package com.folkbanner.data.repository

import android.graphics.Bitmap
import com.folkbanner.data.model.WallpaperApi
import com.folkbanner.data.model.WallpaperItem
import com.folkbanner.data.remote.ApiService
import com.folkbanner.data.remote.DownstreamApiService
import com.folkbanner.utils.DedupHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class WallpaperRepository(
    private val apiService: ApiService = ApiService(),
    private val downstreamApiService: DownstreamApiService = DownstreamApiService(),
    private val dedupHelper: DedupHelper = DedupHelper()
) {

    private var apis: List<WallpaperApi> = emptyList()
    private var currentBitmap: Bitmap? = null

    suspend fun loadApis(baseUrl: String): List<WallpaperApi> = withContext(Dispatchers.IO) {
        apis = apiService.fetchApiList(baseUrl)
        apis
    }

    suspend fun fetchSingleWallpaper(apiIndex: Int): WallpaperItem? = withContext(Dispatchers.IO) {
        if (apiIndex !in apis.indices) return@withContext null

        val api = apis[apiIndex]
        repeat(10) {
            try {
                val url = apiService.fetchWallpaperUrl(api.url)
                if (!dedupHelper.isDuplicate(url)) {
                    dedupHelper.add(url)
                    return@withContext WallpaperItem(
                        id = UUID.randomUUID().toString(),
                        url = url,
                        apiName = api.name
                    )
                }
            } catch (_: Exception) { }
        }
        null
    }

    suspend fun fetchDownstreamWallpaper(r18Enabled: Boolean = false): WallpaperItem? = withContext(Dispatchers.IO) {
        try {
            val result = downstreamApiService.fetchRandomNormalImage(r18Enabled)
            currentBitmap = result.bitmap
            WallpaperItem(
                id = UUID.randomUUID().toString(),
                url = "downstream://github/${result.filename}?index=${result.index}&total=${result.total}",
                apiName = "GitHub [${result.index}/${result.total}]",
                bitmap = result.bitmap
            )
        } catch (_: Exception) {
            null
        }
    }

    fun getCurrentBitmap(): Bitmap? = currentBitmap

    fun clearCurrentApiCache() {
        dedupHelper.clear()
        currentBitmap = null
        downstreamApiService.clearCache()
    }
}