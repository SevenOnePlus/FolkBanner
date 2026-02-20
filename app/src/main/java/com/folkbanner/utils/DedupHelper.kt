package com.folkbanner.utils

import java.util.Collections

class DedupHelper {
    
    private val urlSet = Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<String, Boolean>())
    
    fun isDuplicate(url: String): Boolean = urlSet.contains(url)
    
    fun add(url: String) {
        urlSet.add(url)
    }
    
    fun clear() {
        urlSet.clear()
    }
}