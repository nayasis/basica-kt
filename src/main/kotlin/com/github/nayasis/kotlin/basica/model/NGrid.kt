package com.github.nayasis.kotlin.basica.model

import com.fasterxml.jackson.core.type.TypeReference
import com.github.nayasis.kotlin.basica.core.character.Characters
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

        var fullFontWidth: Double
            get() = Characters.fullwidth
            set(value) {
                Characters.fullwidth = value
            }

        var halfFontWidth: Double
            get() = Characters.halfwidth
            set(value) {
                Characters.halfwidth = value
            }

    }

    val header: NGridHeader
        get() = _header

    val body: Map<Int,Map<Any,Any?>>
        get() = _body

    private val _header = Header(this)
    private val _body   = TreeMap<Int,HashMap<Any,Any?>>()

    internal var printer: NGridPrinter? = null

    constructor(header: KClass<*>? = null) {
        this._header.addAll(header)
    }

    constructor(grid: NGrid) {
        _header.init(grid._header)
        _body.putAll(grid._body)
    }

    constructor(collection: Collection<*>, header: KClass<*>? = null) {
        if( collection.isEmpty())
            this._header.addAll(header)
        for( row in collection )
            addRow(row)
    }

    constructor(array: Array<*>, header: KClass<*>? = null) {
        if( array.isEmpty())
            this._header.addAll(header)
        for( row in array ) {
            addRow(row)
        }
    }

    fun setRow( index: Int, value: Any? ) {
        if( index < 0 )
            throw IndexOutOfBoundsException("$index")
        when (value) {
            null -> _body[index] = HashMap()
            is Map<*,*> -> setMap(index, value)
            is NGrid -> {
                _header.merge(value._header)
                value._body.forEach{ _body[maxindex()] = it.value }
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

    private fun setMap(index: Int, map: Map<*,*>) {
        val e = HashMap<Any,Any?>(map)
        _body[index] = e
        e.keys.forEach { _header.add(it,index) }
    }

    fun addRow(value: Any?) = setRow(maxindex(), value)

    fun addData(key: Any, value: Any?) = setData(_header.next(key), key, value)

    fun removeRow(index: Int) {
        if( ! _body.containsKey(index) ) return
        _body[index]!!.keys.forEach{ _header.remove(it,index) }
        _body.remove(index)
    }

    fun removeKey( key: Any ) {
        _header.remove(key)
        for( index in ArrayList(_body.keys) ) {
            val row = _body[index]!!
            row.remove(key)
            if( row.isEmpty() ) {
                _body.remove(index)
            }
        }
    }

    fun removeData(row: Int, key: Any) {
        _header.remove(key,row)
        _body[row]?.let {
            it.remove(key)
            if( it.isEmpty() ) {
                _body.remove(row)
            }
        }
    }

    private fun maxindex(): Int = if(_body.isEmpty()) 0 else _body.lastKey() + 1

    val size: Int
        get() = maxindex()

    fun size(key: Any): Int = _header.size(key)

    fun getRow(index: Int): Map<Any,Any?> = _body[index] ?: emptyMap()

    fun setCell(row: Int, col: Int, value: Any?) {
        if( _header.keyByIndex(col) == null )
            _header.add(col,col)
        setData(row, _header.keyByIndex(col)!!, value)
    }

    fun setData(row: Int, key: Any, value: Any?) {
        _header.add(key)
        if( _body[row] == null )
            _body[row] = HashMap()
        _body[row]!![key] = value
        printer = null
    }

    fun getCell(row: Int, col: Int): Any? {
        val key = _header.keyByIndex(col) ?: return null
        return getData(row,key)
    }

    fun getData(row: Int, key: Any): Any? {
        return if (_body[row] == null) null else _body[row]!![key]
    }

    inline fun <reified T: Any> toList(ignoreError: Boolean = true): List<T?> {
        return toList(T::class,ignoreError)
    }

    fun <T:Any> toList(typeClass: KClass<T>, ignoreError: Boolean = true): List<T?> {
        val list = ArrayList<T?>()
        for( i in 0 until maxindex()) {
            try {
                list.add( _body[i]?.let { Reflector.toObject(it,typeClass) } )
            } catch (e: Exception) {
                if( !ignoreError ) {
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
        for( (_,row) in _body) {
            val v = row[key]
            list.add(cast(v,typeClass,ignoreError))
        }
        return list
    }

    fun <T:Any> toListFrom(key: Any, typeRef: TypeReference<T>, ignoreError: Boolean = true): List<T?> {
        val list = ArrayList<T?>()
        for( (_,row) in _body) {
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

    fun clear(bodyOnly: Boolean = false) {
        _body.clear()
        if( ! bodyOnly )
            _header.clear()
    }

    fun sort(comparator: Comparator<Map<Any,Any?>>) {

        val indies = ArrayList(_body.keys)
        val rows   = ArrayList(_body.values)

        Collections.sort(rows,comparator)

        _body.clear()
        for( i in 0 until indies.size )
            _body[indies[i]] = rows[i]

    }

    public override fun clone(): NGrid = NGrid(this)

    override fun toString(): String = toString(true)

    fun toString(showHeader: Boolean = true, rowcount:Int = 500, showIndexColumn: Boolean = false, useAlias: Boolean = true, maxColumnWidth: Int = 100): String {
        if( printer == null || printer!!.maxColumnWidth != maxColumnWidth ) {
            printer = NGridPrinter(this,maxColumnWidth)
        }
        return printer!!.toString(showHeader,useAlias,rowcount,showIndexColumn)
    }

    override fun iterator(): Iterator<Map<Any,Any?>> {
        return object: Iterator<Map<Any,Any?>> {
            var size  = maxindex()
            var index = 0
            override fun hasNext(): Boolean = index < size
            override fun next(): Map<Any,Any?> = getRow(index++)
        }
    }

}