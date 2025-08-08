package com.github.nayasis.kotlin.basica.model.dataframe

import com.github.nayasis.kotlin.basica.reflection.Reflector
import java.io.Serializable
import kotlin.reflect.KClass

class DataFrame(
    private val body: Columns = Columns()
): Serializable, Cloneable, Iterable<Map<String, Any?>> {

    val lastIndex: Int?
        get() = body.values.mapNotNull { it.lastIndex }.maxOrNull()

    val firstIndex: Int?
        get() = body.values.mapNotNull { it.firstIndex }.minOrNull()

    val size: Int
        get() = lastIndex?.inc()?.minus( firstIndex ?: 0 ) ?: 0

    fun isEmpty(): Boolean {
        return size == 0
    }

    fun isRowEmpty(row: Int): Boolean {
        return ! body.values.any { it.has(row) }
    }

    fun setLabel(key: String, label: String) {
        body.setLabel(key, label)
    }

    fun getLabel(key: String): String {
        return body.getLabel(key)
    }

    val keys: Set<String>
        get() = body.keys

    val values: Collection<Column>
        get() = body.values

    val labels: List<String>
        get() = body.keys.map { body.getLabel(it) }

    fun addKey(key: String) {
        if (body[key] == null) {
            body[key] = Column()
        }
    }

    fun removeKey(key: String) {
        body.remove(key)
    }

    fun getColumn(key: String): Column {
        return body[key] ?: throw NoSuchElementException("No column found for key $key")
    }

    fun getColumn(index: Int): Column {
        return body.getColumnBy(index) ?: throw NoSuchElementException("No column found for index[$index]")
    }

    fun getData(row: Int, col: Int): Any? {
        val key = body.getKeyBy(col) ?: throw NoSuchElementException("No column found for index[$col]")
        return body[key]?.get(row)
    }

    fun getData(row: Int, key: String): Any? {
        return body[key]?.get(row)
    }

    fun setData(row: Int, col: Int, value: Any?) {
        val key = body.getKeyBy(col) ?: throw NoSuchElementException("No column found for index[$col]")
        body[key]?.set(row, value)
    }

    fun setData(row: Int, key: String, value: Any?) {
        addKey(key)
        body[key]!![row] = value
    }

    fun removeData(row: Int, col: Int) {
        val key = body.getKeyBy(col) ?: throw NoSuchElementException("No column found for index[$col]")
        body[key]?.remove(row)
    }

    fun removeData(row: Int, key: String) {
        (body[key]?: throw NoSuchElementException("No column found for key $key")).remove(row)
    }

    fun setRow(row: Int, value: Any?) {
        when(value) {
            null -> {
                body.forEach { (_, column) ->
                    column.set(row, null)
                }
            }
            is Map<*, *> -> {
                for ((key, v) in value) {
                    setData(row, "$key", v)
                }
            }
            is Collection<*> -> {
                var start = row
                value.forEach { setRow(start++, it) }
            }
            is Array<*> -> {
                var start = row
                value.forEach { setRow(start++, it) }
            }
            is CharSequence -> {
                val json = value.toString()
                try {
                    setRow(row, Reflector.toObject<ArrayList<Map<String, Any?>>>(json))
                } catch (e: Exception) {
                    setRow(row, Reflector.toMap(json))
                }
            }
            else -> setRow(row, Reflector.toMap(value))
        }
    }

    fun addRow(value: Any?) {
        setRow(size, value)
    }

    fun addRows(values: Iterable<Any?>) {
        var index = size
        values.forEach { setRow(index++, it) }
    }

    fun getRow(row: Int): Map<String, Any?> {
        return body.mapValues { (_, column) -> column[row] }
    }

    /**
     * Selects specific rows from DataFrame to create a new DataFrame.
     *
     * @param rowIndices indices of rows to select
     * @return new DataFrame object
     */
    fun selectRows(rowIndices: List<Int>): DataFrame {
        val new = DataFrame()
        rowIndices.sorted().forEach { rowIndex ->
            if (!isRowEmpty(rowIndex)) {
                new.addRow(getRow(rowIndex))
            }
        }
        return new
    }

    /**
     * Selects rows from DataFrame based on specified range.
     *
     * @param from starting index (inclusive)
     * @param to ending index (inclusive)
     * @return new DataFrame object containing selected rows
     */
    fun selectRows(from: Int? = null, to: Int? = null): DataFrame {
        val new = DataFrame()
        for(row in (from ?: firstIndex ?: 0) .. (to ?: lastIndex ?: -1)) {
            if (!isRowEmpty(row)) {
                new.addRow(getRow(row))
            }
        }
        return new
    }

    fun removeRow(row: Int) {
        body.forEach { (_, column) ->
            column.remove(row)
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
        startRow: Int = firstIndex ?: 0,
        endRow: Int = lastIndex ?: 0,
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

    fun head(
        n: Int = 10,
        showHeader: Boolean = true,
        showIndex: Boolean = false,
        showLabel: Boolean = true,
        maxColumnWidth: Int = 50,
    ): String {
        return toString(
            showHeader = showHeader,
            showIndex = showIndex,
            showLabel = showLabel,
            startRow = firstIndex ?: 0,
            endRow = (firstIndex ?: 0) + n - 1,
            maxColumnWidth = maxColumnWidth
        )
   }

    fun tail(
        n: Int = 10,
        showHeader: Boolean = true,
        showIndex: Boolean = false,
        showLabel: Boolean = true,
        maxColumnWidth: Int = 50,
    ): String {
        return toString(
            showHeader = showHeader,
            showIndex = showIndex,
            showLabel = showLabel,
            startRow = (lastIndex ?: 0) - n + 1,
            endRow = lastIndex ?: 0,
            maxColumnWidth = maxColumnWidth
        )
    }

    inline fun <reified T: Any> toList(fromIndex: Int = 0, ignoreError: Boolean = true): List<T?> {
        return toList(T::class,fromIndex,ignoreError)
    }

    fun <T: Any> toList(typeClass: KClass<T>, fromIndex: Int = 0, ignoreError: Boolean = true): List<T?> {
        val list = ArrayList<T?>()
        for( i in fromIndex .. (lastIndex ?: -1)) {
            val row = if(isRowEmpty(i)) null else getRow(i)
            list.add(row?.let { runCatching {
                Reflector.toObject(it, typeClass)
            }.getOrElse { e ->
                if(!ignoreError) null else throw e
            }})
        }
        return list
    }

    public override fun clone(): DataFrame {
        return DataFrame(body = body.clone())
    }

    override fun iterator(): Iterator<Map<String, Any?>> {
        return object: Iterator<Map<String,Any?>> {
            private var i    = firstIndex ?: 0
            private val end  = lastIndex ?: -1
            override fun hasNext(): Boolean = i <= end
            override fun next(): Map<String,Any?> = getRow(i++)
        }
    }

}