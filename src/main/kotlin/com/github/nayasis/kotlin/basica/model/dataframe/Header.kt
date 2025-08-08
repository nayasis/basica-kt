package com.github.nayasis.kotlin.basica.model.dataframe

class Header: Cloneable, Iterable<Any> {

    private val _keys       = ArrayList<Any>()
    private val _indexByKey = HashMap<Any,Int>()
    private val _keyByIndex = HashMap<Int,Any>()
    private val _labels     = HashMap<Any,String>()

    val size: Int
        get() = _keys.size

    val keys: Set<Any>
        get() = this._keys.toSet()

    fun isEmpty(): Boolean = _keys.isEmpty()

    fun contains(key: Any): Boolean {
        return _indexByKey.containsKey(key)
    }

    fun clear() {
        _keys.clear()
        _indexByKey.clear()
        _keyByIndex.clear()
        _labels.clear()
    }

    fun getKeyBy(index: Int): Any? {
        return _keyByIndex[index]
    }

    fun getIndexBy(key: Any): Int? {
        return _indexByKey[key]
    }

    fun setLabel(key: Any, label: String) {
        if( ! _indexByKey.containsKey(key) )
            throw IllegalArgumentException("Key '$key' does not exist in the header.")
        _labels[key] = label
    }

    fun getLabel(key: Any): String? {
        if( ! _indexByKey.containsKey(key) )
            throw IllegalArgumentException("Key '$key' does not exist in the header.")
        return _labels[key] ?: "$key"
    }

    fun removeLabel(key: Any) {
        if( ! _indexByKey.containsKey(key) )
            throw IllegalArgumentException("Key '$key' does not exist in the header.")
        _labels.remove(key)
    }

    fun add(key: Any) {
        if( ! _indexByKey.containsKey(key) ) {
            val index = _keys.size
            _keys.add(key)
            _indexByKey[key] = index
            _keyByIndex[index] = key
        }
    }

    fun remove(key: Any) {
        if( _indexByKey.containsKey(key) ) {
            val index = _indexByKey[key]!!
            _keys.removeAt(index)
            _indexByKey.remove(key)
            _keyByIndex.remove(index)
            for(i in index until _keys.size) {
                val k = _keys[i]
                _indexByKey[k] = i
                _keyByIndex[i] = k
            }
        }
    }

    override fun clone(): Header {
        return Header().apply {
            _keys.addAll(this@Header._keys)
            _indexByKey.putAll(this@Header._indexByKey)
            _keyByIndex.putAll(this@Header._keyByIndex)
            _labels.putAll(this@Header._labels)
        }
    }

    override fun iterator(): Iterator<Any> {
        return _keys.iterator()
    }

}