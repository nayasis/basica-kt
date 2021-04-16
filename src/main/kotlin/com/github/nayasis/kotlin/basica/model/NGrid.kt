package com.github.nayasis.kotlin.basica.model

import com.github.nayasis.kotlin.basica.reflection.Reflector
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.reflect.KClass

@Suppress("MayBeConstant")
class NGrid: Serializable, Cloneable {

    companion object {
        private const val serialVersionUID = 4570402963506233953L
    }

    private val header = Header()
    private val body   = TreeMap<Int,HashMap<Any,Any?>>()

    constructor()
    constructor( grid: NGrid ) {
        header.init(grid.header)
        body.putAll(grid.body)
    }

    fun setRow( index: Int, value: Any?, modifyHeader: Boolean = true ) {
        if( index < 0 )
            throw IndexOutOfBoundsException(index)
        when (value) {
            null -> body[index] = HashMap()
            is Map<*,*> -> setMap(index, value, modifyHeader)
            is NGrid -> {
                header.merge(value.header)
                value.body.forEach{ body[size()] = it.value }
            }
            is Collection<*> -> {
                var idx = index
                value.forEach { setRow(idx++,it,modifyHeader) }
            }
            is Array<*> -> {
                var idx = index
                value.forEach { setRow(idx++,it,modifyHeader) }
            }
            is CharSequence -> {
                val json = value.toString()
                try {
                    setRow( index, Reflector.toObject<ArrayList<Map<String,Any?>>>(json), modifyHeader )
                } catch (e: Exception) {
                    setRow( index, Reflector.toMap(json), modifyHeader )
                }
            }
            else -> setRow( index, Reflector.toMap(value), modifyHeader )
        }
    }

    private fun setMap( index: Int, map: Map<*,*>, modifyHeader: Boolean ) {
        val e = HashMap<Any, Any?>(map)
        body[index] = e
        if( modifyHeader ) {
            e.keys.forEach { header.add(it) }
        }
    }

    fun addRow( value: Any?, modifyHeader: Boolean = true ) = setRow( size(), value, modifyHeader )

    fun removeRow(index: Int) = body.remove(index)

    fun removeKey( key: Any ) {
        header.remove(key)
        body.forEach{ it.value.remove(key) }
    }

    fun size(): Int = if(body.isEmpty()) 0 else body.lastKey() + 1
    fun headerSize(): Int = header.size()

    fun getRow(index: Int): Map<Any,Any?> = body[index] ?: emptyMap()

    fun setCell(row: Int, col: Int, value: Any?) {
        if( header.keyByIndex(col) == null )
            header.add(col,col)
        setData(row, header.keyByIndex(col)!!, value)
    }

    fun setData(row: Int, key: Any, value: Any?) {
        header.add(key)
        if( body[row] == null )
            body[row] = HashMap()
        body[row]!![key] = value
    }

    fun getCell(row: Int, col: Int): Any? {
        val key = header.keyByIndex(col) ?: return null
        return getData(row,key)
    }

    fun getData(row: Int, key: Any): Any? {
        return if (body[row] == null) null else body[row]!![key]
    }

    fun toList(): List<Map<Any,Any?>> {
        val empty = emptyMap<Any,Any?>()
        val list = ArrayList<Map<Any,Any?>>()
        for( i in 0 until size()) {
            list.add( body[i] ?: empty )
        }
        return list
    }

    fun <T:Any> toList(typeClass: KClass<T>, ignoreError: Boolean = true): List<T?> {
        val list = ArrayList<T?>()
        for( i in 0 until size()) {
            try {
                list.add( body[i]?.let { Reflector.toObject(it,typeClass) } )
            } catch (e: Exception) {
                if( ignoreError ) {
                    list.add(null)
                } else {
                    throw e
                }
            }
        }
        return list
    }

    fun <T:Any> toListColumn(key: Any, typeClass: KClass<T>): List<T?> {
        val list = ArrayList<T?>()
        for( i in 0 until size()) {
            list.add(getData(i,key) as T?)
        }
        return list
    }

    fun copy(): NGrid = NGrid(this)

    fun clear(bodyOnly: Boolean = false) {
        body.clear()
        if( ! bodyOnly )
            header.clear()
    }

    fun containsKey(key: Any): Boolean = header.containsKey(key)
    fun keyByIndex(index: Int): Any? = header.keyByIndex(index)
    fun keys(): Set<Any> = header.keys()
    fun setAlias( key: Any, alias: String ) = header.setAlias(key,alias)
    fun getAlias( key: Any ): String = header.getAlias(key)

}

class Header: Serializable, Cloneable {

    companion object {
        private const val serialVersionUID = 4570402963506233954L
    }

    private val keys    = HashMap<Any,Int>()
    private val indexes = TreeMap<Int,Any>()
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
    }

    fun add(key: Any) {
        if( keys.containsKey(key) ) return
        var index = size()
        keys[key] = index
        indexes[index] = key
    }

    fun add(key: Any, index: Int) {
        if( index < 0 )
            throw IndexOutOfBoundsException(index)
        if( keys.containsKey(key) ) return
        keys[key] = index
        indexes[index] = key
    }

    fun remove(key: Any) {
        if( ! keys.containsKey(key) ) return
        indexes.remove(keys[key])
        keys.remove(key)
        aliases.remove(key)
    }

    fun containsKey(key: Any): Boolean {
        return keys.containsKey(key)
    }

    fun keyByIndex(index: Int): Any? {
        return indexes[index] ?: null
    }

    fun keys(): Set<Any> {
        return indexes.map { it.value }.toSet()
    }

    fun setAlias( key: Any, alias: String ) {
        if( ! keys.containsKey(key) ) return
        aliases[key] = alias
    }

    fun getAlias( key: Any ): String {
        return aliases[key] ?: "$key"
    }

    fun size(): Int {
        return if(indexes.isEmpty()) 0 else indexes.lastKey() + 1
    }

    fun clear() {
        keys.clear()
        indexes.clear()
        aliases.clear()
    }

}