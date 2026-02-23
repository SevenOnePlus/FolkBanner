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
        
        private val client = OkHttpClient.Builder()
            .followRedirects(true)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
            
        private val base64CleanRegex = Regex("[\\s\\r\\n]")
        
        @Volatile private var cachedNormalFiles: List<NormalFile>? = null
        @Volatile private var cachedR18Files: List<NormalFile>? = null
        @Volatile private var cacheTimestamp: Long = 0
        private val cacheLock = Any()
    }

    data class NormalFile(val name: String, val downloadUrl: String)

    data class RandomFileResult(
        val index: Int,
        val total: Int,
        val filename: String,
        val url: String,
        val bitmap: Bitmap
    )

    suspend fun fetchRandomNormalImage(r18Enabled: Boolean = false): RandomFileResult = withContext(Dispatchers.IO) {
        val files = fetchFileList(r18Enabled)
        if (files.isEmpty()) throw Exception("No files found")
        
        val index = NativeRandomGenerator.generateRandomIndex(files.size)
        val file = files[index - 1]
        
        val base64Content = downloadFileContent(file.downloadUrl)
        val bitmap = decodeBase64ToBitmap(base64Content)
            ?: throw Exception("Failed to decode image")
        
        RandomFileResult(index, files.size, file.name, file.downloadUrl, bitmap)
    }

    private fun decodeBase64ToBitmap(base64Content: String): Bitmap? {
        val cleanBase64 = cleanBase64String(base64Content)
        
        // 优先使用 Native 解码（性能更好）
        val imageData = NativeRandomGenerator.decodeBase64(cleanBase64)
            ?: Base64.decode(cleanBase64, Base64.DEFAULT)
        
        if (imageData.isEmpty()) return null
        
        return BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
            ?: BitmapFactory.decodeByteArray(imageData, 0, imageData.size, BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            })
    }

    private suspend fun fetchFileList(r18Enabled: Boolean): List<NormalFile> {
        val now = System.currentTimeMillis()
        val cacheValid = now - cacheTimestamp < CACHE_DURATION_MS
        
        // 获取或刷新 Normal 文件列表
        val normalFiles = cachedNormalFiles?.takeIf { cacheValid && it.isNotEmpty() }
            ?: fetchFilesFromApi(GITHUB_API_NORMAL).also { 
                synchronized(cacheLock) { cachedNormalFiles = it } 
            }
        
        if (normalFiles.isEmpty()) throw Exception("Normal folder empty or fetch failed")
        
        // 如果未启用 R18，直接返回
        if (!r18Enabled) {
            synchronized(cacheLock) { cacheTimestamp = now }
            return normalFiles
        }
        
        // 获取或刷新 R18 文件列表
        val r18Files = cachedR18Files?.takeIf { cacheValid && it.isNotEmpty() }
            ?: fetchFilesFromApi(GITHUB_API_R18).also { 
                synchronized(cacheLock) { cachedR18Files = it; cacheTimestamp = now } 
            }
        
        return if (r18Files.isEmpty()) normalFiles else normalFiles + r18Files
    }
    
    private fun fetchFilesFromApi(apiUrl: String): List<NormalFile> {
        val request = Request.Builder()
            .url(apiUrl)
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            
            val body = response.body?.string() ?: return emptyList()
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
    
    private fun cleanBase64String(input: String): String {
        var result = input.trim()
        
        // 移除 data URI 前缀
        val base64Marker = ";base64,"
        val markerIndex = result.indexOf(base64Marker)
        if (markerIndex >= 0) {
            result = result.substring(markerIndex + base64Marker.length)
        } else {
            val commaIndex = result.indexOf(',')
            if (commaIndex >= 0 && commaIndex < 100) {
                result = result.substring(commaIndex + 1)
            }
        }
        
        return base64CleanRegex.replace(result, "")
    }
    
    private fun downloadFileContent(url: String): String {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Failed to download: ${response.code}")
            return response.body?.string() ?: throw Exception("Empty content")
        }
    }

    fun clearCache() {
        synchronized(cacheLock) {
            cachedNormalFiles = null
            cachedR18Files = null
            cacheTimestamp = 0
        }
    }
}
