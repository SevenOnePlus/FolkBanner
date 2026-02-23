package com.folkbanner.utils

class DedupHelper {
    
    private val urlSet = HashSet<String>()
    
    fun isDuplicate(url: String): Boolean = urlSet.contains(url)
    
    fun add(url: String) {
        urlSet.add(url)
    }
    
    fun clear() {
        urlSet.clear()
    }
}