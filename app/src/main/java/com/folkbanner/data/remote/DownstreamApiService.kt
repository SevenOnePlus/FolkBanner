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

class DownstreamApiService {

    companion object {
        private const val GITHUB_API = "https://api.github.com/repos/SevenOnePlus/Banner-Down/contents/Normal"
        private const val GITHUB_RAW = "https://raw.githubusercontent.com/SevenOnePlus/Banner-Down/main"
        
        private val client = OkHttpClient.Builder()
            .followRedirects(true)
            .build()
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
        
        AppLogger.log("开始Base64解码...")
        AppLogger.log("内容前50字符: ${base64Content.take(50)}")
        
        val cleanBase64 = cleanBase64String(base64Content)
        AppLogger.log("清理后大小: ${cleanBase64.length} 字符")
        
        val imageData = try {
            NativeRandomGenerator.decodeBase64(cleanBase64).also {
                AppLogger.log("Native解码结果: ${if (it != null) "${it.size}字节" else "null"}")
            }
        } catch (e: Exception) {
            AppLogger.log("Native解码异常: ${e.message}")
            null
        } ?: run {
            AppLogger.log("尝试Android Base64解码...")
            try {
                Base64.decode(cleanBase64, Base64.DEFAULT).also {
                    AppLogger.log("Android解码成功: ${it.size}字节")
                }
            } catch (e: Exception) {
                AppLogger.log("Android解码异常: ${e.message}")
                null
            }
        }
        
        if (imageData == null || imageData.isEmpty()) {
            AppLogger.log("错误: Base64解码失败")
            throw Exception("Failed to decode base64")
        }
        AppLogger.log("解码完成, 图片大小: ${imageData.size} 字节")
        
        AppLogger.log("开始生成Bitmap...")
        val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
        if (bitmap == null) {
            AppLogger.log("错误: Bitmap生成失败，图片数据可能损坏")
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
        
        val commaIndex = result.indexOf(',')
        if (commaIndex >= 0 && commaIndex < 50) {
            result = result.substring(commaIndex + 1)
        }
        
        result = result.replace(Regex("[\\s\\r\\n]"), "")
        
        return result
    }

    private suspend fun fetchFileList(): List<NormalFile> = withContext(Dispatchers.IO) {
        AppLogger.log("请求GitHub API...")
        val request = Request.Builder()
            .url(GITHUB_API)
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                AppLogger.log("API请求失败: ${response.code}")
                throw Exception("Failed to fetch file list: ${response.code}")
            }
            
            val body = response.body?.string() ?: throw Exception("Empty response")
            AppLogger.log("API响应成功")
            
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
}