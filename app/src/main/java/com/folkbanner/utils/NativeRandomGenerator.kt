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
            fallbackDecodeBase64(input)
        } catch (_: Exception) {
            fallbackDecodeBase64(input)
        }
    }

    private fun fallbackDecodeBase64(input: String): ByteArray? {
        return try {
            var cleanInput = input
            val commaIndex = input.indexOf(',')
            if (commaIndex >= 0) {
                cleanInput = input.substring(commaIndex + 1)
            }
            cleanInput = cleanInput.trim()
            Base64.decode(cleanInput, Base64.DEFAULT)
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