package com.github.nayasis.kotlin.basica.model

import com.github.nayasis.kotlin.basica.reflection.Reflector
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.reflect.KClass

//@Suppress("MayBeConstant")
class NGrid: Serializable, Cloneable, Iterable<Map<Any,Any?>> {

    companion object {
        private const val serialVersionUID = 4570402963506233953L
    }

    private val header  = Header(this)
    private val body    = TreeMap<Int,HashMap<Any,Any?>>()

    internal var printer: NGridPrinter? = null

    constructor()
    constructor( grid: NGrid ) {
        header.init(grid.header)
        body.putAll(grid.body)
    }

    fun header(): NGridHeader = header
    fun body() : Map<Int,Map<Any,Any?>> = body

    fun setRow( index: Int, value: Any?, modifyHeader: Boolean = true ) {
        if( index < 0 )
            throw IndexOutOfBoundsException(index)
        when (value) {
            null -> body[index] = HashMap()
            is Map<*,*> -> setMap(index, value, modifyHeader)
            is NGrid -> {
                header.merge(value.header)
                value.body.forEach{ body[maxindex()] = it.value }
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
        printer = null
    }

    private fun setMap( index: Int, map: Map<*,*>, modifyHeader: Boolean ) {
        val e = HashMap<Any, Any?>(map)
        body[index] = e
        if( modifyHeader ) {
            e.keys.forEach { header.add(it,index) }
        }
    }

    fun addRow( value: Any?, modifyHeader: Boolean = true ) = setRow( maxindex(), value, modifyHeader )

    fun addData(key: Any, value: Any?) = setData(header.size(key), key, value)

    fun removeRow(index: Int) {
        if( ! body.containsKey(index) ) return
        body[index]!!.keys.forEach{ header.remove(it,index) }
        body.remove(index)
    }

    fun removeKey( key: Any ) {
        header.remove(key)
        for( index in ArrayList(body.keys) ) {
            val row = body[index]!!
            row.remove(key)
            if( row.isEmpty() ) {
                body.remove(index)
            }
        }
    }

    private fun maxindex(): Int = if(body.isEmpty()) 0 else body.lastKey() + 1

    fun size(): Int = maxindex()

    fun size(key: Any): Int = header.size(key)

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
        printer = null
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
        for( i in 0 until maxindex()) {
            list.add( body[i] ?: empty )
        }
        return list
    }

    fun <T:Any> toList(typeClass: KClass<T>, ignoreError: Boolean = true): List<T?> {
        val list = ArrayList<T?>()
        for( i in 0 until maxindex()) {
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
        for( i in 0 until size() ) {
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

    fun sort(comparator: Comparator<Map<Any,Any?>>) {

        val incies = ArrayList(body.keys)
        val rows   = ArrayList(body.values)

        Collections.sort(rows,comparator)

        body.clear()
        for( i in 0 until incies.size ) {
            body[incies[i]] = rows[i]
        }

    }

    override fun toString(): String {
        return toString(true)
    }

    fun toString(showHeader: Boolean = true, rowcount:Int = 500, showIndexColumn: Boolean = false, useAlias: Boolean = true, maxColumnWidth: Int = 100): String {
        if( printer == null || printer!!.maxColumnWidth != maxColumnWidth ) {
            printer = NGridPrinter(this,maxColumnWidth)
        }
        return printer!!.toString(showHeader,useAlias,rowcount,showIndexColumn)
    }

    override fun iterator(): Iterator<Map<Any,Any?>> {
        return object: Iterator<Map<Any,Any?>> {
            var size  = size()
            var index = 0
            override fun hasNext(): Boolean = index < size
            override fun next(): Map<Any,Any?> = getRow(index++)
        }
    }

}

class Header(
    private val grid: NGrid
): NGridHeader {

    companion object {
        private const val serialVersionUID = 4570402963506233954L
    }

    private val keys    = HashMap<Any,TreeSet<Int>>() // key, size (max row index by key)
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

    fun add(key: Any, rowindex: Int) {
        if( rowindex < 0 )
            throw IndexOutOfBoundsException(rowindex)
        add(key)
        keys[key]!!.add(rowindex)
        grid.printer = null
    }

    fun remove(key: Any, rowindex: Int ) {
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

    fun size(key: Any): Int {
        val last = keys[key]?.lastOrNull()
        return if( last == null ) 0 else last + 1
    }

    override fun containsKey(key: Any): Boolean = keys.containsKey(key)

    fun keyByIndex(index: Int): Any? = indexes[index]

    override fun keys(): List<Any> = indexes.map { it.value }

    override fun aliases(): List<String> = keys().map { getAlias(it) }

    override fun size(): Int = keys.size

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