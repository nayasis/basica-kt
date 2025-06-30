package com.github.nayasis.kotlin.basica.model.dataframe

import com.github.nayasis.kotlin.basica.reflection.Reflector
import java.io.Serializable

class DataFrame: Serializable, Cloneable, Iterable<Map<String, Any?>> {

    private val body = Columns()

    val lastIndex: Int?
        get() = body.values.mapNotNull { it.lastIndex }.maxOrNull()

    val firstIndex: Int?
        get() = body.values.mapNotNull { it.firstIndex }.minOrNull()

    val size: Int
        get() = lastIndex?.inc()?.minus( firstIndex ?: 0 ) ?: 0

    fun isEmpty(): Boolean {
        return size == 0
    }

    fun setLabel(key: String, label: String) {
        body.setLabel(key, label)
    }

    fun getLabel(key: String): String? {
        return body.getLabel(key)
    }

    val keys: Set<String>
        get() = body.keys

    val values: Collection<Column>
        get() = body.values

    fun removeKey(key: String) {
        body.remove(key)
    }

    fun getColumn(key: String): Column {
        return body[key] ?: throw NoSuchElementException("No column found for key $key")
    }

    fun getColumn(index: Int): Column {
        return body.getColumnBy(index) ?: throw NoSuchElementException("No column found for index[$index]")
    }

    fun getData(r: Int, c: Int): Any? {
        val key = body.getKeyBy(c) ?: throw NoSuchElementException("No column found for index[$c]")
        return body[key]?.get(r)
    }

    fun getData(r: Int, key: String): Any? {
        return body[key]?.get(r)
    }

    fun setData(r: Int, c: Int, value: Any?) {
        val key = body.getKeyBy(c) ?: throw NoSuchElementException("No column found for index[$c]")
        body[key]?.set(r, value)
    }

    fun setData(r: Int, key: String, value: Any?) {
        if (body[key] == null) {
            body[key] = Column()
        }
        body[key]!!.set(r, value)
    }

    fun removeData(r: Int, c: Int) {
        val key = body.getKeyBy(c) ?: throw NoSuchElementException("No column found for index[$c]")
        body[key]?.remove(r)
    }

    fun removeData(r: Int, key: String) {
        body[key]?.remove(r) ?: throw NoSuchElementException("No column found for key $key")
    }

    fun setRow(r: Int, value: Any?) {
        when(value) {
            null -> {
                body.forEach { (_, column) ->
                    column.set(r, null)
                }
            }
            is Map<*, *> -> {
                for ((key, value) in value) {
                    setData(r, "$key", value)
                }
            }
            is Collection<*> -> {
                var start = r
                value.forEach { setRow(start++, it) }
            }
            is Array<*> -> {
                var start = r
                value.forEach { setRow(start++, it) }
            }
            is CharSequence -> {
                val json = value.toString()
                try {
                    setRow(r, Reflector.toObject<ArrayList<Map<String, Any?>>>(json))
                } catch (e: Exception) {
                    setRow(r, Reflector.toMap(json))
                }
            }
            else -> setRow(r, Reflector.toMap(value))
        }
    }

    fun addRow(value: Any?) {
        setRow(size, value)
    }

    fun getRow(r: Int): Map<String, Any?> {
        return body.mapValues { (_, column) -> column.get(r) }
    }

    fun removeRow(r: Int) {
        body.forEach { (_, column) ->
            column.remove(r)
        }
    }

    fun clear() {
        body.clear()
    }

    override fun toString(): String {
        return toString(showHeader = true)
    }

    fun toString(
        showHeader: Boolean = true,
        showIndex: Boolean = false,
        showLabel: Boolean = true,
        startRow: Int = 0,
        endRow: Int = Int.MAX_VALUE,
        maxColumnWidth: Int = 50,
    ): String {
        return DataframePrinter(
            this,
            showHeader,
            showIndex,
            showLabel,
            startRow,
            endRow,
            maxColumnWidth.toDouble(),
        ).toString()
    }

    override fun iterator(): Iterator<Map<String, Any?>> {
        return object: Iterator<Map<String,Any?>> {
            private val size = this@DataFrame.size
            private var index = firstIndex ?: 0
            override fun hasNext(): Boolean = index < size
            override fun next(): Map<String,Any?> = getRow(index++)
        }
    }

}