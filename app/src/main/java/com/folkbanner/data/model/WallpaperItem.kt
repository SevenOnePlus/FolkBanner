package com.folkbanner.data.model

import android.graphics.Bitmap

data class WallpaperItem(
    val id: String,
    val url: String,
    val apiName: String,
    val bitmap: Bitmap? = null
)