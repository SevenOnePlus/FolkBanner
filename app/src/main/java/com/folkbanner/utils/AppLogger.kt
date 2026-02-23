package com.folkbanner.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {

    private val _logs = MutableLiveData<String>()
    val logs: LiveData<String> = _logs

    private val _toast = MutableLiveData<String?>()
    val toast: LiveData<String?> = _toast
    
    private val logLock = Any()
    private val dateFormat by lazy { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    
    var debugMode = false

    fun log(message: String) {
        if (!debugMode) return
        
        val logEntry = synchronized(logLock) {
            "[${dateFormat.format(Date())}] $message"
        }
        
        _logs.postValue((_logs.value ?: "") + logEntry + "\n")
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