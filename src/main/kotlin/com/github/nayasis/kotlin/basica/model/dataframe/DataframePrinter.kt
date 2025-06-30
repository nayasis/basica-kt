package com.github.nayasis.kotlin.basica.model.dataframe

import com.github.nayasis.kotlin.basica.core.character.Characters
import com.github.nayasis.kotlin.basica.core.character.fontWidth
import com.github.nayasis.kotlin.basica.core.character.isCJK
import com.github.nayasis.kotlin.basica.core.character.isFullWidth
import com.github.nayasis.kotlin.basica.core.character.isHalfWidth
import com.github.nayasis.kotlin.basica.core.character.repeat
import com.github.nayasis.kotlin.basica.core.extension.isEmpty
import com.github.nayasis.kotlin.basica.core.extension.isNotEmpty
import com.github.nayasis.kotlin.basica.core.string.dpadEnd
import com.github.nayasis.kotlin.basica.core.string.dpadStart
import kotlin.math.ceil
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.text.iterator

class DataframePrinter(
    private val dataframe: DataFrame,
    private val showHeader: Boolean = true,
    private val showIndex: Boolean = false,
    private val showLabel: Boolean = true,
    private val startRow: Int,
    private val endRow: Int,
    private val maxColumnWidth: Double,
) {
    private val meta = PrintHelper(dataframe, maxColumnWidth, showHeader, showLabel, startRow, endRow)

    override fun toString(): String {

        val line  = meta.makeLine(showIndex,'-', '+')
        val body  = StringBuilder().append(line)

        if (showHeader && ! dataframe.keys.isEmpty()) {
            body.append('\n').append(makeHeader())
            body.append('\n').append(line)
        }

        if (dataframe.isNotEmpty()) {
            for (i in max(startRow, dataframe.firstIndex ?: 0) .. min(endRow, dataframe.lastIndex ?: 0)) {
                body.append('\n').append(toString(dataframe, i, showIndex))
            }
        }

        return body.append('\n').append(line).toString()
    }

    private fun makeHeader(): String {
        val line = StringBuilder().append('|')
        if (showIndex) {
            line.append(INDEX_COLUMN_NAME.dpadStart(meta.indexColumnWidth, ' ')).append('|')
        }
        for (key in dataframe.keys) {
            val value = if (showLabel) dataframe.getLabel(key) else key
            val width = meta.getWidth(key)
            line.append(value.toDisplayString(maxColumnWidth).dpadEnd(width, ' ')).append('|')
        }
        return line.toString()
    }

    private fun toString(dataframe: DataFrame, row: Int, indexColumn: Boolean = false): String {
        val line = StringBuilder().append('|')
        if (indexColumn) {
            line.append("$row".dpadStart(meta.indexColumnWidth, ' ')).append('|')
        }
        meta.columnWidths.forEach { key, width ->
            val value = dataframe.getData(row, key)
            line.append(value.toDisplayString(maxColumnWidth).let {
                if (value is Number) {
                    it.dpadStart(width, ' ')
                } else {
                    it.dpadEnd(width, ' ')
                }
            }).append('|')
        }
        return line.toString()
    }

}

private const val INDEX_COLUMN_NAME = "index"

private class PrintHelper(
    dataframe: DataFrame,
    maxColumnWidth: Double,
    showHeader: Boolean,
    showAlias: Boolean,
    start: Int,
    end: Int?,
) {

    val columnWidths = LinkedHashMap<String,Int>()

    var indexColumnWidth = max(ceil(log10(dataframe.size.toDouble())).toInt(), INDEX_COLUMN_NAME.length)

    init {
        if (showHeader) {
            dataframe.keys.forEach { key ->
                columnWidths[key] = if(showAlias) {
                    dataframe.getLabel(key)
                } else {
                    key
                }.getDisplayWidth(maxColumnWidth).roundToInt()
            }
        }
        dataframe.keys.forEach { key ->
            columnWidths[key] = maxOf(
                columnWidths[key] ?: 0,
                dataframe.getColumn(key).getWidth(start, end ?: dataframe.size, maxColumnWidth).roundToInt()
            )
        }
    }

    fun getWidth(key: String): Int {
        return columnWidths[key] ?: 0
    }

    fun makeLine(showIndex: Boolean, base: Char, delimiter: Char): String {
        val sb = StringBuilder().append(delimiter)
        if( columnWidths.isEmpty() ) {
            sb.append( base.repeat(3) ).append(delimiter)
        } else {
            if(showIndex) {
                sb.append( base.repeat(indexColumnWidth) ).append(delimiter)
            }
            for( w in columnWidths.values ) {
                sb.append( base.repeat(w) ).append(delimiter)
            }
        }
        return sb.toString()
    }

}

fun Any?.toDisplayString(maxWidth: Double): String {

    val value = when {
        this == null                            -> return ""
        this is Map<*, *>     && this.isEmpty() -> return "{}"
        this is Collection<*> && this.isEmpty() -> return "[]"
        this is Array<*>      && this.isEmpty() -> return "[]"
        else                                    -> this.toString()
    }

    val sb = ArrayList<Any>()
    var size = 0.0

    val onFullwidth = Characters.fullwidth > Characters.halfwidth

    value.forEachIndexed { i, c ->
        val w = c.displayWidth
        if(onFullwidth) {
            if(size + w == maxWidth && i < value.lastIndex) {
                return when {
                    c in setOf('\n', '\r', '\b', '\t') -> sb
                    c.isFullWidth() -> sb
                    else -> sb.subList(0, sb.size - 1)
                }.joinToString("", postfix = "..")
            } else if(size + w > maxWidth) {
                return sb.subList(0, sb.size - 1).joinToString("", postfix = "..")
            }
        } else {
            if(size + w == maxWidth && i < value.lastIndex) {
                return when {
                    c in setOf('\n', '\r', '\b', '\t') -> sb
                    else -> sb.subList(0, sb.size - 1)
                }.joinToString("", postfix = "..")
            } else if(size + w > maxWidth) {
                return sb.subList(0, sb.size - 1).joinToString("", postfix = "..")
            }
        }
        sb.add(when (c) {
            '\n' -> "\\n"
            '\r' -> "\\r"
            '\b' -> "\\b"
            '\t' -> "\\t"
            else -> c
        })
        size += w
    }

    return sb.joinToString("")
}

private fun Column.getWidth(startRow: Int, endRow: Int, maxWidth: Double): Double {
    return this.values.filter { it.key in startRow..endRow }.maxOfOrNull { it.value.getDisplayWidth(maxWidth) } ?: 0.0
}

private fun Any?.getDisplayWidth(max: Double): Double {
    when {
        this == null -> return 0.0
        this is Map<*, *>     && this.isEmpty() -> return 2.0
        this is Collection<*> && this.isEmpty() -> return 2.0
        this is Array<*>      && this.isEmpty() -> return 2.0
        else -> {
            var width  = 0.0
            val string = this.toString().also { if (it.length >= max) return max }
            for (c in string) {
                val w = c.displayWidth
                when {
                    width + w > max -> return width
                    width == max    -> return max
                    else            -> width += w
                }
            }
            return width
        }
    }
}

private val Char.displayWidth: Double
    get() {
        return when (this) {
            '\n' -> 2.0
            '\r' -> 2.0
            '\b' -> 2.0
            '\t' -> 2.0
            else -> this.fontWidth
        }
    }