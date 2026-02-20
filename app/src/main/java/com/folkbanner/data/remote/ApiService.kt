package com.folkbanner.data.remote

import com.folkbanner.data.model.WallpaperApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ApiService {

    companion object {
        private val client = OkHttpClient.Builder().followRedirects(true).build()
        private val gson = Gson()
    }

    suspend fun fetchApiList(apiUrl: String): List<WallpaperApi> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(apiUrl).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Failed to fetch API list: ${response.code}")
            val body = response.body?.string() ?: throw Exception("Empty response")
            gson.fromJson(body, object : TypeToken<List<WallpaperApi>>() {}.type)
        }
    }

    suspend fun fetchWallpaperUrl(apiUrl: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(apiUrl).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Failed to fetch wallpaper: ${response.code}")
            response.request.url.toString()
        }
    }
}