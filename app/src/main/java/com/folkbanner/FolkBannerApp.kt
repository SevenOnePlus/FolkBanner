package com.folkbanner

import android.app.Application
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.OkHttpClient

class FolkBannerApp : Application() {
    
    companion object {
        lateinit var imageLoader: ImageLoader
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        
        val okHttpClient = OkHttpClient.Builder()
            .build()
        
        imageLoader = ImageLoader.Builder(this)
            .okHttpClient { okHttpClient }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(512L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
    }
    
}
