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
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun log(message: String) {
        val timestamp: String
        val logEntry: String
        val currentLogs: String
        
        synchronized(logLock) {
            timestamp = dateFormat.format(Date())
            logEntry = "[$timestamp] $message"
            currentLogs = _logs.value ?: ""
        }
        
        _logs.postValue(currentLogs + logEntry + "\n")
    }

    fun toast(message: String) {
        _toast.postValue(message)
    }

    fun clearToast() {
        _toast.postValue(null)
    }

    fun clear() {
        synchronized(logLock) {
            _logs.postValue("")
        }
    }
}