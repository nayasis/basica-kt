package io.github.nayasis.kotlin.basica.cache.implement

open class LruCache<K,V>(
    capacity: Int = 128
): FifoCache<K,V>(capacity) {

    override fun put(key: K, value: V): V {
        tick(key)
        return super.put(key, value)
    }

    override fun getOrPut(key: K, defaultValue: () -> V): V {
        tick(key)
        return super.getOrPut(key, defaultValue)
    }

    override fun get(key: K): V? {
        tick(key)
        return super.get(key)
    }

    override fun getOrElse(key: K, defaultValue: () -> V): V {
        tick(key)
        return super.getOrElse(key, defaultValue)
    }

    override fun getOrDefault(key: K, default: V): V {
        tick(key)
        return super.getOrDefault(key, default)
    }

    private fun tick(key: K) {
        if(map.containsKey(key)) {
            val value = map.remove(key)!!
            map[key] = value
        }
    }

}