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
        val cleanInput = cleanBase64Input(input)
        return try {
            nativeDecodeBase64(cleanInput)
        } catch (_: Exception) {
            // 回退到 Android Base64
            tryDecodeBase64(cleanInput)
        }
    }

    /**
     * 清理 Base64 输入，移除 data URI 前缀和空白字符
     */
    fun cleanBase64Input(input: String): String {
        var result = input.trim()
        
        // 移除 data URI 前缀 (如 "data:image/png;base64,")
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
        
        return result.replace(Regex("[\\s\\r\\n]"), "")
    }

    private fun tryDecodeBase64(input: String): ByteArray? {
        return try {
            Base64.decode(input, Base64.DEFAULT)
        } catch (_: Exception) {
            null
        }
    }

    @FastNative
    private external fun nativeGenerateRandomIndex(count: Int): Int
    
    @FastNative
    private external fun nativeGenerateRandomInRange(min: Int, max: Int): Int
    
    private external fun nativeDecodeBase64(input: String): ByteArray?
}