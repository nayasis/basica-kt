package com.github.nayasis.kotlin.basica.cache.implement

import com.github.nayasis.kotlin.basica.cache.Cache

open class LruCache<K,V>: Cache<K,V> {

    protected lateinit var map: HashMap<K,V?>
    private val creationTimes = HashMap<K,Long>()

    var flushMiliseconds = Int.MAX_VALUE

    constructor( capacity: Int = 128 ) {
        setCapacity(capacity)
    }

    override fun size(): Int {
        return map.size
    }

    override fun setCapacity(capacity: Int) {
        map = object: LinkedHashMap<K,V?>(capacity, .75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<K,V?>): Boolean {
                return size > capacity
            }
        }
    }

    private fun noFlush() = flushMiliseconds == Int.MAX_VALUE

    private fun flush(key: K) {
        if (noFlush()) return
        if (elapsedMillis(key) >= flushMiliseconds) evict(key)
    }

    private fun resetAccessTime(key: K) {
        if (noFlush()) return
        creationTimes[key] = System.nanoTime()
    }

    private fun elapsedMillis(key: K): Long {
        if( ! creationTimes.containsKey(key) ) {
            creationTimes[key] = System.nanoTime()
            return 0
        }
        return (System.nanoTime() - creationTimes[key]!!) / 1_000_000
    }

    override fun contains(key: K): Boolean {
        flush(key)
        return map.containsKey(key)
    }

    override fun put(key: K, value: V): V {
        map[key] = value
        resetAccessTime(key)
        return value
    }

    override fun putIfAbsent(key: K, value: V): V {
        map.putIfAbsent(key, value)
        resetAccessTime(key)
        return value
    }

    override fun get(key: K): V? {
        flush(key)
        return if( map.containsKey(key) ) {
            resetAccessTime(key)
            map[key]
        } else {
            null
        }
    }

    override fun getOrElse(key: K): V? {
        return get(key)
    }

    override fun getOrDefault(key: K, default: V): V {
        return get(key) ?: default
    }

    override fun evict(key: K) {
        map.remove(key)
        creationTimes.remove(key)
    }

    override fun evict() {
        map.clear()
        creationTimes.clear()
    }

    override fun keySet(): Set<K> = map.keys

    override fun putAll(map: Map<K,V>) =
        map.forEach { (k, v) -> put(k,v) }

    override fun putAll(cache: Cache<K,V>) =
        cache.keySet().forEach {
            map[it] = cache[it]
            resetAccessTime(it)
        }



}