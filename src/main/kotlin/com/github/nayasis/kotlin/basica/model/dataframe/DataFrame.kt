package com.github.nayasis.kotlin.basica.model.dataframe

import java.util.*

class DataFrame {

    private val columns = LinkedHashMap<Any, Column>()
    private val header  = Header()

    private val indices = TreeSet<Int>()

    val firstIndex: Int?
        get() = indices.firstOrNull()

    val lastIndex: Int?
        get() = indices.lastOrNull()

    fun setLabel(key: Any, label: String) {
        header.setLabel(key, label)
    }

    fun getLabel(key: Any): String? {
        return header.getLabel(key)
    }

    val keys: Set<Any>
        get() = columns.keys

    fun removeKey(key: String) {
        columns.remove(key)
        header.remove(key)
    }

    fun getColumn(key: String): Column? {
        return columns[key]
    }

    fun getColumn(index: Int): Column? {
        return header.getKeyBy(index)?.let { key -> columns[key] }
    }

    fun setRow(r: Int, c: Int, value: Any?) {
        val key = header.getKeyBy(c) ?: throw IllegalArgumentException("No column found for index $c")
        columns[key]?.set(r, value)
        indices.add(r)
    }

    fun setRow(r: Int, key: String, value: Any?) {
        if( ! header.contains(key) ) {
            header.add(key)
            columns[key] = Column()
        }
        columns[key]?.set(r, value)
        indices.add(r)
    }

    fun setRow(r: Int, map: Map<Any, Any?>) {
        for ((key, value) in map) {
            setRow(r, key, value)
        }
    }

    fun setRow(r: Int, value: Any?) {
        if( value is Map<*, *>) {
            setRow(r, value as Map<Any, Any?>)
        } else {
            throw IllegalArgumentException("Value must be a Map<String, Any?>")
        }
    }


}