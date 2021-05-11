package com.github.nayasis.kotlin.basica.cache.implement

class FifoCache<K,V>(capacity: Int = 128): LruCache<K, V>(capacity) {
    override fun setCapacity(capacity: Int) {
        map = object: LinkedHashMap<K, V>(capacity, .75f, false) {
            override fun removeEldestEntry(eldest: Map.Entry<K, V>): Boolean {
                return size > capacity
            }
        }
    }
}