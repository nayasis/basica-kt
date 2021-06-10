package com.github.nayasis.kotlin.basica.model

import java.io.Serializable
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

interface NGridHeader: Serializable, Cloneable {

    fun add(key: Any)
    fun keys(): List<Any>
    fun aliases(): List<String>
    fun size(key: Any): Int
    fun containsKey(key: Any): Boolean
    fun setAlias(key: Any, alias: String)
    fun getAlias(key: Any): String
    fun isEmpty(): Boolean

}

class Header(
    private val grid: NGrid
): NGridHeader {

    companion object {
        private const val serialVersionUID = 4570402963506233954L
    }

    private val keys    = HashMap<Any, TreeSet<Int>>() // key, size (max row index by key)
    private val indexes = TreeMap<Int,Any>() // column index, key
    private val aliases = HashMap<Any,String>()

    fun init(header: Header) {
        clear()
        keys.putAll( header.keys )
        indexes.putAll( header.indexes )
        aliases.putAll( header.aliases )
    }

    fun merge(header: Header) {
        header.keys.forEach { add(it.key) }
        aliases.putAll( header.aliases )
        grid.printer = null
    }

    override fun add(key: Any) {
        if( ! keys.containsKey(key) ) {
            keys[key] = TreeSet()
            indexes[nextCol()] = key
            grid.printer = null
        }
    }

    fun addAll(header: KClass<*>?) {
        if( header == null ) return
        header.memberProperties.forEach { add(it.name) }
    }

    fun addAll(header: Set<Any>?) {
        if( header == null ) return
        header.forEach { add(it) }
    }

    fun add(key: Any, rowindex: Int) {
        if( rowindex < 0 )
            throw IndexOutOfBoundsException("$rowindex")
        add(key)
        keys[key]!!.add(rowindex)
        grid.printer = null
    }

    fun next(key: Any): Int {
        val next = size(key)
        add(key,next)
        return next
    }

    fun remove(key: Any, rowindex: Int) {
        keys[key]?.let {
            it.remove(rowindex)
            if( it.isEmpty() ) {
                remove(key)
            }
            grid.printer = null
        }
    }

    fun remove(key: Any) {
        if( ! keys.containsKey(key) ) return
        keys.remove(key)
        aliases.remove(key)
        indexes.mapNotNull { if(it.value == key) it.key else null }.firstOrNull().let {
            indexes.remove(it)
        }
        grid.printer = null
    }

    override fun size(key: Any): Int {
        val last = keys[key]?.lastOrNull()
        return if( last == null ) 0 else last + 1
    }

    override fun containsKey(key: Any): Boolean = keys.containsKey(key)

    fun keyByIndex(index: Int): Any? = indexes[index]

    override fun keys(): List<Any> = indexes.map { it.value }

    override fun aliases(): List<String> = keys().map { getAlias(it) }

    override fun setAlias(key: Any, alias: String ) {
        if( keys.containsKey(key) ) {
            aliases[key] = alias
            grid.printer = null
        }
    }

    override fun getAlias(key: Any ): String = aliases[key] ?: "$key"
    override fun isEmpty(): Boolean = keys.isEmpty()

    private fun nextCol(): Int = if(indexes.isEmpty()) 0 else indexes.lastKey() + 1

    fun clear() {
        keys.clear()
        indexes.clear()
        aliases.clear()
        grid.printer = null
    }

}