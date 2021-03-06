package com.github.nayasis.kotlin.basica.model

import com.fasterxml.jackson.core.type.TypeReference
import com.github.nayasis.kotlin.basica.core.number.cast
import com.github.nayasis.kotlin.basica.core.string.toNumber
import com.github.nayasis.kotlin.basica.reflection.Reflector
import java.io.Serializable
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.cast
import kotlin.reflect.full.isSubclassOf

@Suppress("UNCHECKED_CAST")
class NGrid: Serializable, Cloneable, Iterable<Map<Any,Any?>> {

    companion object {
        private const val serialVersionUID = 4570402963506233953L
    }

    private val header  = Header(this)
    private val body    = TreeMap<Int,HashMap<Any,Any?>>()

    internal var printer: NGridPrinter? = null

    constructor(header: KClass<*>? = null) {
        this.header.addAll(header)
    }

    constructor(grid: NGrid) {
        header.init(grid.header)
        body.putAll(grid.body)
    }

    constructor(collection: Collection<*>, header: KClass<*>? = null) {
        if( collection.isEmpty())
            this.header.addAll(header)
        for( row in collection )
            addRow(row)
    }

    constructor(array: Array<*>, header: KClass<*>? = null) {
        if( array.isEmpty())
            this.header.addAll(header)
        for( row in array ) {
            addRow(row)
        }
    }

    fun header(): NGridHeader = header
    fun body() : Map<Int,Map<Any,Any?>> = body

    fun setRow( index: Int, value: Any? ) {
        if( index < 0 )
            throw IndexOutOfBoundsException("$index")
        when (value) {
            null -> body[index] = HashMap()
            is Map<*,*> -> setMap(index, value)
            is NGrid -> {
                header.merge(value.header)
                value.body.forEach{ body[maxindex()] = it.value }
            }
            is Collection<*> -> {
                var idx = index
                value.forEach { setRow(idx++,it) }
            }
            is Array<*> -> {
                var idx = index
                value.forEach { setRow(idx++,it) }
            }
            is CharSequence -> {
                val json = value.toString()
                try {
                    setRow( index, Reflector.toObject<ArrayList<Map<String,Any?>>>(json) )
                } catch (e: Exception) {
                    setRow( index, Reflector.toMap(json) )
                }
            }
            else -> setRow( index, Reflector.toMap(value) )
        }
        printer = null
    }

    private fun setMap( index: Int, map: Map<*,*> ) {
        val e = HashMap<Any,Any?>(map)
        body[index] = e
        e.keys.forEach { header.add(it,index) }
    }

    fun addRow( value: Any? ) = setRow( maxindex(), value )

    fun addData(key: Any, value: Any?) = setData(header.next(key), key, value)

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

    fun removeData(row: Int, key: Any) {
        header.remove(key,row)
        body[row]?.let {
            it.remove(key)
            if( it.isEmpty() ) {
                body.remove(row)
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

    private fun <T:Any> cast(value: Any?, typeClass: KClass<T>, ignoreError: Boolean): T? {
        return if( value == null ) {
            null
        } else if( typeClass == String::class ) {
            value.toString() as T
        } else if( (value is CharSequence || value is Char) && typeClass.isSubclassOf(Number::class) ) {
            value.toString().toNumber(typeClass as KClass<Number>) as T
        } else if( value is Number ) {
            value.cast(typeClass as KClass<Number>) as T
        } else {
            try {
                typeClass.cast(value)
            } catch (e: Exception) {
                try {
                    Reflector.toObject(value, typeClass)
                } catch (e1: Exception) {
                    if( ignoreError ) {
                        null
                    } else {
                        throw e1
                    }
                }
            }
        }
    }

    fun <T:Any> toListFrom(key: Any, typeClass: KClass<T>, ignoreError: Boolean = true): List<T?> {
        val list = ArrayList<T?>()
        for( (_,row) in body) {
            val v = row[key]
            list.add(cast(v,typeClass,ignoreError))
        }
        return list
    }

    fun <T:Any> toListFrom(key: Any, typeRef: TypeReference<T>, ignoreError: Boolean = true): List<T?> {
        val list = ArrayList<T?>()
        for( (_,row) in body) {
            list.add( row[key].let {
                if(it == null) null else {
                    try {
                        Reflector.toObject(it, typeRef)
                    } catch (e: Exception) {
                        if( ignoreError ) {
                            null
                        } else {
                            throw e
                        }
                    }
                }
            })
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

        val indies = ArrayList(body.keys)
        val rows   = ArrayList(body.values)

        Collections.sort(rows,comparator)

        body.clear()
        for( i in 0 until indies.size )
            body[indies[i]] = rows[i]

    }

    override fun toString(): String = toString(true)

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