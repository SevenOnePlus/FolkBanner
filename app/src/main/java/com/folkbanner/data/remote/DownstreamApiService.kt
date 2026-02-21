package com.folkbanner.data.remote

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.folkbanner.utils.AppLogger
import com.folkbanner.utils.NativeRandomGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

class DownstreamApiService {

    companion object {
        private const val GITHUB_API = "https://api.github.com/repos/SevenOnePlus/Banner-Down/contents/Normal"
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
        private var cachedFiles: List<NormalFile>? = null
        @Volatile
        private var cacheTimestamp: Long = 0
        private val cacheLock = Any()
    }

    data class NormalFile(
        val name: String,
        val downloadUrl: String
    )

    suspend fun fetchRandomNormalImage(): RandomFileResult = withContext(Dispatchers.IO) {
        AppLogger.log("开始获取文件列表...")
        
        val files = fetchFileList()
        AppLogger.log("获取到 ${files.size} 个文件")
        
        if (files.isEmpty()) {
            AppLogger.log("错误: 文件列表为空")
            throw Exception("No files found")
        }
        
        val index = NativeRandomGenerator.generateRandomIndex(files.size)
        AppLogger.log("随机选择: 第 $index 个文件")
        
        val file = files[index - 1]
        AppLogger.log("文件名: ${file.name}")
        
        AppLogger.log("开始下载文件内容...")
        val base64Content = downloadFileContent(file.downloadUrl)
        AppLogger.log("下载完成, 大小: ${base64Content.length} 字符")
        
        AppLogger.logDebug("原始内容前100字符: ${base64Content.take(100)}")
        
        val cleanBase64 = cleanBase64String(base64Content)
        AppLogger.log("清理后大小: ${cleanBase64.length} 字符")
        
        var imageData: ByteArray? = null
        var decodeMethod = ""
        
        try {
            AppLogger.log("尝试Native Base64解码...")
            imageData = NativeRandomGenerator.decodeBase64(cleanBase64)
            if (imageData != null && imageData.isNotEmpty()) {
                decodeMethod = "Native"
                AppLogger.log("Native解码成功: ${imageData.size} 字节")
            }
        } catch (e: Exception) {
            AppLogger.log("Native解码异常: ${e.message}")
        }
        
        if (imageData == null || imageData.isEmpty()) {
            try {
                AppLogger.log("尝试Android Base64解码...")
                imageData = Base64.decode(cleanBase64, Base64.DEFAULT)
                decodeMethod = "Android"
                AppLogger.log("Android解码成功: ${imageData.size} 字节")
            } catch (e: Exception) {
                AppLogger.log("Android解码异常: ${e.message}")
            }
        }
        
        if (imageData == null || imageData.isEmpty()) {
            AppLogger.log("错误: 所有Base64解码方式都失败")
            throw Exception("Failed to decode base64")
        }
        
        AppLogger.log("解码完成(${decodeMethod}): ${imageData.size} 字节")
        
        val firstBytes = imageData.take(16).toByteArray()
        val hexString = firstBytes.joinToString(" ") { "%02X".format(it) }
        AppLogger.log("数据头部(hex): $hexString")
        
        val mimeType = detectMimeType(imageData)
        AppLogger.log("检测到格式: $mimeType")
        
        AppLogger.log("开始生成Bitmap...")
        val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
        
        if (bitmap == null) {
            AppLogger.log("BitmapFactory返回null，尝试其他方式...")
            
            val options = BitmapFactory.Options()
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            val bitmap2 = BitmapFactory.decodeByteArray(imageData, 0, imageData.size, options)
            
            if (bitmap2 != null) {
                AppLogger.log("成功! 图片尺寸: ${bitmap2.width}x${bitmap2.height}")
                return@withContext RandomFileResult(
                    index = index,
                    total = files.size,
                    filename = file.name,
                    url = file.downloadUrl,
                    bitmap = bitmap2
                )
            }
            
            AppLogger.log("错误: Bitmap生成失败，图片数据可能损坏或格式不支持")
            throw Exception("Failed to decode image")
        }
        
        AppLogger.log("成功! 图片尺寸: ${bitmap.width}x${bitmap.height}")
        
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

    private fun detectMimeType(data: ByteArray): String {
        if (data.size < 4) return "unknown"
        
        return when {
            data[0] == 0x89.toByte() && data[1] == 0x50.toByte() -> "PNG"
            data[0] == 0xFF.toByte() && data[1] == 0xD8.toByte() -> "JPEG"
            data[0] == 0x47.toByte() && data[1] == 0x49.toByte() -> "GIF"
            data[0] == 0x42.toByte() && data[1] == 0x4D.toByte() -> "BMP"
            data[0] == 0x52.toByte() && data[1] == 0x49.toByte() && 
            data[2] == 0x46.toByte() && data[3] == 0x46.toByte() -> "WEBP"
            else -> "unknown"
        }
    }

    private suspend fun fetchFileList(): List<NormalFile> {
        val now = System.currentTimeMillis()
        
        // 检查缓存 - 使用 synchronized 返回值避免在块内 return
        val cached = synchronized(cacheLock) {
            cachedFiles?.takeIf { now - cacheTimestamp < CACHE_DURATION_MS && it.isNotEmpty() }
        }
        
        if (cached != null) {
            AppLogger.log("使用缓存的文件列表(${cached.size}个)")
            return cached
        }
        
        AppLogger.log("请求GitHub API...")
        val request = Request.Builder()
            .url(GITHUB_API)
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                AppLogger.log("API请求失败: ${response.code}")
                // 尝试使用过期缓存作为后备
                val fallback = synchronized(cacheLock) { cachedFiles }
                if (!fallback.isNullOrEmpty()) {
                    return@use fallback
                }
                throw Exception("Failed to fetch file list: ${response.code}")
            }
            
            val body = response.body?.string() ?: throw Exception("Empty response")
            
            response.header("X-RateLimit-Remaining")?.let {
                AppLogger.log("API剩余次数: $it")
            }
            
            AppLogger.log("API响应成功")
            
            val jsonArray = JSONArray(body)
            
            val files = (0 until jsonArray.length()).mapNotNull { i ->
                val item = jsonArray.getJSONObject(i)
                if (item.getString("type") == "file") {
                    val name = item.getString("name")
                    val downloadUrl = item.optString("download_url").ifEmpty {
                        "$GITHUB_RAW/${item.getString("path")}"
                    }
                    NormalFile(name, downloadUrl)
                } else null
            }
            
            synchronized(cacheLock) {
                cachedFiles = files
                cacheTimestamp = now
            }
            
            return@use files
        }
    }
    private fun downloadFileContent(url: String): String {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                AppLogger.log("下载失败: ${response.code}")
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

    /**
     * 清除文件列表缓存
     * 当用户刷新或切换API模式时应调用此方法
     */
    fun clearCache() {
        synchronized(cacheLock) {
            cachedFiles = null
            cacheTimestamp = 0
        }
    }
}
