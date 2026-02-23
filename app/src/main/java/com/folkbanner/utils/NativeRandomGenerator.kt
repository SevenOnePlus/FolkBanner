package com.folkbanner.utils

import android.util.Base64
import dalvik.annotation.optimization.FastNative

object NativeRandomGenerator {
    
    var isNativeLoaded = false
        private set

    init {
        try {
            System.loadLibrary("folkrandom")
            isNativeLoaded = true
        } catch (_: UnsatisfiedLinkError) {
            isNativeLoaded = false
        }
    }

    fun generateRandomIndex(count: Int): Int {
        if (count <= 0) return 0
        return try {
            nativeGenerateRandomIndex(count)
        } catch (_: UnsatisfiedLinkError) {
            (1..count).random()
        }
    }

    fun generateRandomInRange(min: Int, max: Int): Int {
        if (min > max) return min
        return try {
            nativeGenerateRandomInRange(min, max)
        } catch (_: UnsatisfiedLinkError) {
            (min..max).random()
        }
    }

    fun decodeBase64(input: String): ByteArray? {
        return try {
            nativeDecodeBase64(input)
        } catch (_: UnsatisfiedLinkError) {
            decodeBase64Fallback(input)
        } catch (_: Exception) {
            decodeBase64Fallback(input)
        }
    }

    private fun decodeBase64Fallback(input: String): ByteArray? {
        return try {
            val cleanInput = cleanBase64Input(input)
            Base64.decode(cleanInput, Base64.DEFAULT)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 移除 data URI 前缀，只保留 Base64 数据部分
     */
    fun cleanBase64Input(input: String): String {
        val trimmed = input.trim()
        val base64Marker = ";base64,"
        val markerIndex = trimmed.indexOf(base64Marker)
        
        return if (markerIndex >= 0) {
            trimmed.substring(markerIndex + base64Marker.length)
        } else {
            val commaIndex = trimmed.indexOf(',')
            if (commaIndex >= 0 && commaIndex < 100) {
                trimmed.substring(commaIndex + 1)
            } else {
                trimmed
            }
        }
    }

    @FastNative
    private external fun nativeGenerateRandomIndex(count: Int): Int
    
    @FastNative
    private external fun nativeGenerateRandomInRange(min: Int, max: Int): Int
    
    private external fun nativeDecodeBase64(input: String): ByteArray?
}
