package com.github.nayasis.kotlin.basica.cache

import com.github.nayasis.basica.cache.Cache

/**
 * Interface that defines common cache operations.<br><br>
 *
 * <b>Note:</b> Due to the generic use of caching, it is recommended that
 * implementations allow storage of <tt>null</tt> values (for example to
 * cache methods that return {@code null}).
 *
 */
interface Cache<K,V> {

    fun size(): Int

    fun setCapacity(capacity: Int)

    fun setFlushCycle(seconds: Int)

    operator fun contains(key: K?): Boolean

    fun put(key: K?, value: V?)

    fun putIfAbsent(key: K?, value: V?)

    operator fun get(key: K?): V?

    fun clear(key: K?)

    fun clear()

    fun keySet(): Set<K?>

    fun putAll(map: Map<K,V>)

    fun putAll(cache: Cache<K,V>)

}