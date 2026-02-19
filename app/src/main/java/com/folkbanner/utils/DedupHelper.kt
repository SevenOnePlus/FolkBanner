package com.folkbanner.utils

class DedupHelper {
    
    private val urlSet = HashSet<String>()
    private val urlHashList = mutableListOf<String>()
    
    @Synchronized
    fun isDuplicate(url: String): Boolean {
        val hash = url.hashCode().toString()
        return urlSet.contains(hash)
    }
    
    @Synchronized
    fun add(url: String) {
        val hash = url.hashCode().toString()
        urlSet.add(hash)
        urlHashList.add(hash)
    }
    
    @Synchronized
    fun clear() {
        urlSet.clear()
        urlHashList.clear()
    }
    
    @Synchronized
    fun size(): Int = urlSet.size
    
}
