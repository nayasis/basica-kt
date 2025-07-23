package com.github.nayasis.kotlin.basica.model.dataframe

class Columns: LinkedHashMap<String, Column>() {

    private val labels = HashMap<String, String>()

    private val keyByIndex: Map<Int, String>
        get() = _keyByIndex ?: keys.withIndex().associate { it.index to it.value }.also { _keyByIndex = it }
        private var _keyByIndex: Map<Int, String>? = null

    override fun clear() {
        values.forEach { it.clear() }
    }

    fun getLabel(key: String): String {
        return labels[key] ?: key
    }

    fun setLabel(key: String, label: String) {
        labels[key] = label
    }

    fun removeLabel(key: String) {
        labels.remove(key)
    }

    fun getKeyBy(index: Int): String? {
        return keyByIndex[index]
    }

    fun getColumnBy(index: Int): Column? {
        return keyByIndex[index]?.let { key -> this[key] }
    }

    override fun put(key: String, value: Column): Column? {
        _keyByIndex = null
        return super.put(key, value)
    }

    override fun putAll(m: Map<out String, Column>) {
        _keyByIndex = null
        super.putAll(m)
    }

    override fun putIfAbsent(key: String, value: Column): Column? {
        return super.putIfAbsent(key, value).also{
            if(it == null) _keyByIndex = null
        }
    }

    override fun remove(key: String): Column? {
        _keyByIndex = null
        return super.remove(key)
    }

    override fun removeEldestEntry(eldest: Map.Entry<String, Column>?): Boolean {
        _keyByIndex = null
        return super.removeEldestEntry(eldest)
    }

    override fun clone(): Columns {
        return Columns().also { columns ->
            this.labels.forEach { key, value ->
                columns.labels[key] = value
            }
            this.forEach { key, column ->
                columns[key] = column.clone()
            }
        }
    }

}