package com.folkbanner.utils

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

object AppLogger {

    private val _logs = MutableLiveData<String>()
    val logs: LiveData<String> = _logs

    private val _toast = MutableLiveData<String?>()
    val toast: LiveData<String?> = _toast
    
    // Debug模式开关，生产环境可设为false
    var isDebugMode = true

    fun log(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        val logEntry = "[$timestamp] $message"
        _logs.postValue((_logs.value ?: "") + logEntry + "\n")
    }
    
    /**
     * 记录调试信息（仅在debug模式下输出）
     * 用于敏感信息如Base64内容片段等
     */
    fun logDebug(message: String) {
        if (isDebugMode) {
            log(message)
        }
    }

    fun toast(message: String) {
        _toast.postValue(message)
    }

    fun clearToast() {
        _toast.postValue(null)
    }

    fun clear() {
        _logs.postValue("")
    }
}