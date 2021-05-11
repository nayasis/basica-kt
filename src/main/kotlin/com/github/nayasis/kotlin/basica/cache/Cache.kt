package com.github.nayasis.kotlin.basica.cache

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

    operator fun contains(key: K): Boolean

    fun put(key: K, value: V): V

    fun putIfAbsent(key: K, value: V): V

    operator fun get(key: K): V?

    fun getOrElse(key: K): V?

    fun getOrDefault(key: K, default: V): V

    fun evict(key: K)

    fun evict()

    fun keySet(): Set<K>

    fun putAll(map: Map<K,V>)

    fun putAll(cache: Cache<K,V>)

}