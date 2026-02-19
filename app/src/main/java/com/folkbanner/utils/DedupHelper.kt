package com.folkbanner.utils

class DedupHelper {
    
    private val urlSet = HashSet<String>()
    
    @Synchronized
    fun isDuplicate(url: String): Boolean {
        val hash = url.hashCode().toString()
        return urlSet.contains(hash)
    }
    
    @Synchronized
    fun add(url: String) {
        val hash = url.hashCode().toString()
        urlSet.add(hash)
    }
    
    @Synchronized
    fun clear() {
        urlSet.clear()
    }
    
    @Synchronized
    fun size(): Int = urlSet.size
    
}
