package com.folkbanner.utils

class DedupHelper {

    private val urlSet = HashSet<String>()

    @Synchronized
    fun isDuplicate(url: String): Boolean = urlSet.contains(url)

    @Synchronized
    fun add(url: String) {
        urlSet.add(url)
    }

    @Synchronized
    fun clear() {
        urlSet.clear()
    }

    @Synchronized
    fun size(): Int = urlSet.size

}
