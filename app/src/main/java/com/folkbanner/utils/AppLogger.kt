package com.folkbanner.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object AppLogger {

    private val _logs = MutableLiveData<String>()
    val logs: LiveData<String> = _logs

    private val _toast = MutableLiveData<String?>()
    val toast: LiveData<String?> = _toast
    
    var isDebugMode = true

    fun log(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        val logEntry = "[$timestamp] $message"
        _logs.postValue((_logs.value ?: "") + logEntry + "\n")
    }
    
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