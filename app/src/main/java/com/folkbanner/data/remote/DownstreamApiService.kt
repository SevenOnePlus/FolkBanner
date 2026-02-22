package com.folkbanner.data.remote

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.folkbanner.utils.NativeRandomGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

class DownstreamApiService {

    companion object {
        private const val GITHUB_API_NORMAL = "https://api.github.com/repos/SevenOnePlus/Banner-Down/contents/Normal"
        private const val GITHUB_API_R18 = "https://api.github.com/repos/SevenOnePlus/Banner-Down/contents/R18+"
        private const val GITHUB_RAW = "https://raw.githubusercontent.com/SevenOnePlus/Banner-Down/main"
        private const val CACHE_DURATION_MS = 5 * 60 * 1000L
        private const val DATA_URI_PREFIX_MAX_LENGTH = 100
        
        private val client = OkHttpClient.Builder()
            .followRedirects(true)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
            
        private val base64CleanRegex = Regex("[\\s\\r\\n]")
        
        @Volatile
        private var cachedNormalFiles: List<NormalFile>? = null
        @Volatile
        private var cachedR18Files: List<NormalFile>? = null
        @Volatile
        private var cacheTimestamp: Long = 0
        private val cacheLock = Any()
    }

    data class NormalFile(
        val name: String,
        val downloadUrl: String
    )

    suspend fun fetchRandomNormalImage(r18Enabled: Boolean = false): RandomFileResult = withContext(Dispatchers.IO) {
        val files = fetchFileList(r18Enabled)
        
        if (files.isEmpty()) {
            throw Exception("No files found")
        }
        
        val index = NativeRandomGenerator.generateRandomIndex(files.size)
        val file = files[index - 1]
        
        val base64Content = downloadFileContent(file.downloadUrl)
        val cleanBase64 = cleanBase64String(base64Content)
        
        var imageData: ByteArray? = null
        
        try {
            imageData = NativeRandomGenerator.decodeBase64(cleanBase64)
        } catch (_: Exception) { }
        
        if (imageData == null || imageData.isEmpty()) {
            try {
                imageData = Base64.decode(cleanBase64, Base64.DEFAULT)
            } catch (_: Exception) { }
        }
        
        if (imageData == null || imageData.isEmpty()) {
            throw Exception("Failed to decode base64")
        }
        
        val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
        
        if (bitmap == null) {
            val options = BitmapFactory.Options()
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            val bitmap2 = BitmapFactory.decodeByteArray(imageData, 0, imageData.size, options)
            
            if (bitmap2 != null) {
                return@withContext RandomFileResult(
                    index = index,
                    total = files.size,
                    filename = file.name,
                    url = file.downloadUrl,
                    bitmap = bitmap2
                )
            }
            
            throw Exception("Failed to decode image")
        }
        
        RandomFileResult(
            index = index,
            total = files.size,
            filename = file.name,
            url = file.downloadUrl,
            bitmap = bitmap
        )
    }

    private fun cleanBase64String(input: String): String {
        var result = input.trim()
        
        val base64Marker = ";base64,"
        val markerIndex = result.indexOf(base64Marker)
        if (markerIndex >= 0) {
            result = result.substring(markerIndex + base64Marker.length)
        } else {
            val commaIndex = result.indexOf(',')
            if (commaIndex >= 0 && commaIndex < DATA_URI_PREFIX_MAX_LENGTH) {
                result = result.substring(commaIndex + 1)
            }
        }
        
        return base64CleanRegex.replace(result, "")
    }

    private suspend fun fetchFileList(r18Enabled: Boolean): List<NormalFile> {
        val now = System.currentTimeMillis()
        
        val cachedNormal = synchronized(cacheLock) {
            cachedNormalFiles?.takeIf { now - cacheTimestamp < CACHE_DURATION_MS && it.isNotEmpty() }
        }
        val cachedR18 = synchronized(cacheLock) {
            cachedR18Files?.takeIf { now - cacheTimestamp < CACHE_DURATION_MS && it.isNotEmpty() }
        }
        
        val normalFiles: List<NormalFile>
        val r18Files: List<NormalFile>
        
        if (cachedNormal != null) {
            normalFiles = cachedNormal
        } else {
            normalFiles = fetchFilesFromApi(GITHUB_API_NORMAL, "Normal")
            if (normalFiles.isEmpty()) {
                throw Exception("Normal文件夹为空或获取失败")
            }
            synchronized(cacheLock) {
                cachedNormalFiles = normalFiles
            }
        }
        
        if (r18Enabled) {
            if (cachedR18 != null) {
                r18Files = cachedR18
            } else {
                r18Files = fetchFilesFromApi(GITHUB_API_R18, "R18+")
                synchronized(cacheLock) {
                    cachedR18Files = r18Files
                    cacheTimestamp = now
                }
            }
            
            if (r18Files.isEmpty()) {
                synchronized(cacheLock) {
                    cacheTimestamp = now
                }
                return normalFiles
            }
            
            return normalFiles + r18Files
        }
        
        synchronized(cacheLock) {
            cacheTimestamp = now
        }
        
        return normalFiles
    }
    
    private suspend fun fetchFilesFromApi(apiUrl: String, folderName: String): List<NormalFile> {
        val request = Request.Builder()
            .url(apiUrl)
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return@use emptyList()
            }
            
            val body = response.body?.string() ?: return@use emptyList()
            val jsonArray = JSONArray(body)
            
            (0 until jsonArray.length()).mapNotNull { i ->
                val item = jsonArray.getJSONObject(i)
                if (item.getString("type") == "file") {
                    val name = item.getString("name")
                    val downloadUrl = item.optString("download_url").ifEmpty {
                        "$GITHUB_RAW/${item.getString("path")}"
                    }
                    NormalFile(name, downloadUrl)
                } else null
            }
        }
    }
    
    private fun downloadFileContent(url: String): String {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Failed to download: ${response.code}")
            }
            return response.body?.string() ?: throw Exception("Empty content")
        }
    }

    data class RandomFileResult(
        val index: Int,
        val total: Int,
        val filename: String,
        val url: String,
        val bitmap: Bitmap
    )

    fun clearCache() {
        synchronized(cacheLock) {
            cachedNormalFiles = null
            cachedR18Files = null
            cacheTimestamp = 0
        }
    }
}
