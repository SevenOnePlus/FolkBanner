package com.folkbanner.data.remote

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

    suspend fun fetchRandomImage(): Bitmap = withContext(Dispatchers.IO) {
        val files = fetchFileList()
        if (files.isEmpty()) throw Exception("No files found")
        
        val index = NativeRandomGenerator.generateRandomIndex(files.size)
        val file = files[index - 1]
        
        val base64Content = downloadFileContent(file.downloadUrl)
        val imageData = NativeRandomGenerator.decodeBase64(base64Content)
            ?: throw Exception("Failed to decode base64")
        
        BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
            ?: throw Exception("Failed to decode image")
    }

    suspend fun fetchRandomNormalImage(): RandomFileResult = withContext(Dispatchers.IO) {
        val files = fetchFileList()
        if (files.isEmpty()) throw Exception("No files found")
        
        val index = NativeRandomGenerator.generateRandomIndex(files.size)
        val file = files[index - 1]
        
        val base64Content = downloadFileContent(file.downloadUrl)
        val imageData = NativeRandomGenerator.decodeBase64(base64Content)
            ?: throw Exception("Failed to decode base64")
        
        val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
            ?: throw Exception("Failed to decode image")
        
        RandomFileResult(
            index = index,
            total = files.size,
            filename = file.name,
            url = file.downloadUrl,
            bitmap = bitmap
        )
    }

    private suspend fun fetchFileList(): List<NormalFile> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(GITHUB_API)
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Failed to fetch file list: ${response.code}")
            
            val body = response.body?.string() ?: throw Exception("Empty response")
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
            if (!response.isSuccessful) throw Exception("Failed to download: ${response.code}")
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