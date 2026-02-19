package com.folkbanner.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import coil.ImageLoader
import coil.request.ImageRequest
import com.folkbanner.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

object DownloadManager {
    
    suspend fun downloadImage(
        context: Context,
        imageUrl: String,
        imageLoader: ImageLoader
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false)
                .build()
            
            val result = imageLoader.execute(request)
            val drawable = result.drawable ?: return@withContext false
            
            val bitmap = drawableToBitmap(drawable)
            saveToGallery(context, bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }
        
        val width = drawable.intrinsicWidth.coerceAtLeast(1)
        val height = drawable.intrinsicHeight.coerceAtLeast(1)
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        
        return bitmap
    }
    
    private fun saveToGallery(context: Context, bitmap: Bitmap): Boolean {
        val fileName = "FolkBanner_${System.currentTimeMillis()}.jpg"
        
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FolkBanner")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        
        val contentResolver = context.contentResolver
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: return false
        
        var outputStream: OutputStream? = null
        try {
            outputStream = contentResolver.openOutputStream(uri)
            outputStream?.let { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(uri, contentValues, null, null)
            }
            
            return true
        } catch (e: Exception) {
            contentResolver.delete(uri, null, null)
            return false
        } finally {
            outputStream?.close()
        }
    }
    
}
