package com.github.nayasis.kotlin.basica.cache.implement

import com.github.nayasis.kotlin.basica.cache.Cache

open class FifoCache<K,V>(
    capacity: Int = 128
): Cache<K,V> {

    protected val map = object: LinkedHashMap<K,V>(capacity) {
        override fun removeEldestEntry(eldest: Map.Entry<K,V?>): Boolean {
            return size > capacity
        }
    }

    override fun contains(key: K) = map.containsKey(key)

    override fun put(key: K, value: V): V {
        map[key] = value
        return value
    }

    override fun putIfAbsent(key: K, value: V): V {
        map.putIfAbsent(key, value)
        return value
    }

    override fun getOrPut(key: K, defaultValue: () -> V): V {
        return if( map.containsKey(key) ) {
            map[key]!!
        } else {
            defaultValue.invoke().also { map[key] = it }
        }
    }

    override fun get(key: K): V? {
        return if( map.containsKey(key) ) {
            map[key]
        } else {
            null
        }
    }

    override fun getOrElse(key: K, defaultValue: () -> V): V {
        return if( map.containsKey(key) ) {
            map[key]!!
        } else {
            defaultValue.invoke()
        }
    }

    override fun getOrDefault(key: K, default: V): V {
        return if( map.containsKey(key) ) {
            map[key]!!
        } else {
            default
        }
    }

    override fun evict(key: K) {
        map.remove(key)
    }

    override val size: Int
        get() = map.size

    override fun evict() {
        map.clear()
    }

    override val keys
        get() = map.keys

    override fun putAll(map: Map<K,V>) =
        map.forEach { (k, v) -> put(k,v) }

    @Suppress("UNCHECKED_CAST")
    override fun putAll(cache: Cache<K,V>) =
        cache.keys.forEach {
            put(it, cache[it] as V)
        }

}