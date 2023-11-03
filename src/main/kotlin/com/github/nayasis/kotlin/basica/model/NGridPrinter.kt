package com.github.nayasis.kotlin.basica.model

import com.github.nayasis.kotlin.basica.core.character.fontWidth
import com.github.nayasis.kotlin.basica.core.character.repeat
import com.github.nayasis.kotlin.basica.core.string.dpadEnd
import com.github.nayasis.kotlin.basica.core.string.dpadStart
import kotlin.math.ceil
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val INDEX_COLUMN_NAME = "index"

class NGridPrinter(
    val grid: NGrid,
    val maxColumnWidth: Double,
    val showHeader: Boolean,
    val showIndexColumn: Boolean,
    val showAlias: Boolean,
    val maxRowCount: Int,
) {

    private val meta = HeaderMeta(grid, maxColumnWidth, showIndexColumn, showAlias, maxRowCount)

    override fun toString(): String {

        val line  = meta.makeLine('-','+')
        val empty = meta.makeLine(' ','|')
        val body  = StringBuilder().append(line)

        if( showHeader && ! grid.header.isEmpty() ) {
            body.append('\n').append(makeHeader())
            body.append('\n').append(line)
        }

        if( grid.body.isEmpty() ) {
            if( grid.header.isEmpty() ) {
                body.append('\n').append(empty)
            }
        } else {
            for( (i,row) in grid.body ) {
                when {
                    i > meta.rowCount -> break
                    row.isEmpty() -> body.append('\n').append(empty)
                    else -> body.append('\n').append(toString(row, if(showIndexColumn) i else null))
                }
            }
        }

        return body.append('\n').append(line).toString()

    }

    private fun makeHeader(): String {
        val line = StringBuilder().append('|')
        if( showIndexColumn )
            line.append(INDEX_COLUMN_NAME.dpadStart(meta.indexColumnWidth,' ')).append('|')
        for(key in grid.header.keys()) {
            val value = if(showAlias) grid.header.getAlias(key) else key
            val width = meta.columnWidths[key] ?: 0
            line.append(value.toDisplayString(maxColumnWidth).dpadEnd(width, ' ')).append('|')
        }
        return line.toString()
    }

    private fun toString(row : Map<Any,Any?>, index: Int? = null): String {
        val line = StringBuilder().append('|')
        if( index != null )
            line.append(index.toString().dpadStart(meta.indexColumnWidth,' ')).append('|')
        meta.columnWidths.forEach { key, width ->
            val value  = row[key]
            line.append(value.toDisplayString(maxColumnWidth).let {
                if( value is Number ) {
                    it.dpadStart(width,' ')
                } else {
                    it.dpadEnd(width, ' ')
                }
            }).append('|')
        }
        return line.toString()
    }

}

private class HeaderMeta(
    grid: NGrid,
    maxColumnWidth: Double,
    val showIndexColumn: Boolean,
    showAlias: Boolean,
    maxRowCount: Int,
) {

    val rowCount = maxOf(grid.size, maxRowCount)
    val indexColumnWidth = max(ceil(log10(rowCount.toDouble())).toInt(), INDEX_COLUMN_NAME.length)
    val columnWidths = LinkedHashMap<Any,Int>()

    init {
        for( key in grid.header.keys() ) {
            columnWidths[key] = (if(showAlias) grid.header.getAlias(key) else key).getDisplayWidth(maxColumnWidth).roundToInt()
        }
        grid.body.forEach { i, row ->
            if(i > rowCount) return@forEach
            row.forEach{ key, value ->
                columnWidths[key] = maxOf(columnWidths[key] ?: 0, value.getDisplayWidth(maxColumnWidth).roundToInt())
            }
        }
    }

    fun makeLine(base: Char, delimiter: Char): String {
        val sb = StringBuilder().append(delimiter)
        if( showIndexColumn )
            sb.append( base.repeat(indexColumnWidth) ).append(delimiter)
        if( columnWidths.isEmpty() ) {
            sb.append( base.repeat(3) ).append(delimiter)
        } else {
            for( w in columnWidths.values ) {
                sb.append( base.repeat(w) ).append(delimiter)
            }
        }
        return sb.toString()
    }

}

private fun Any?.toDisplayString(maxWidth: Double): String {

    val value = when {
        this == null -> ""
        this is Map<*,*> && this.isEmpty() -> "{}"
        this is Collection<*> && this.isEmpty() -> "[]"
        this is Array<*> && this.isEmpty() -> "[]"
        else -> this.toString()
    }

    val sb = StringBuilder()
    var size = 0.0

    value.forEachIndexed { i, c ->
        val w = c.displayWidth
        if(size + w > maxWidth) {
            val remainSpace = maxWidth - sb.getDisplayWidth(maxWidth)
            return if( remainSpace <= 1.0) {
                "${sb.substring(0,sb.length - 1)}.."
            } else {
                "${sb}.."
            }
        } else if(size + w == maxWidth && i < value.length - 1) {
            return if(w >= 2.0) {
                "${sb}.."
            } else {
                "${sb.substring(0,sb.length - 1)}.."
            }
        }
        sb.append( when(c) {
            '\n' -> "\\n"
            '\r' -> "\\r"
            '\b' -> "\\b"
            else -> c
        })
        size += c.displayWidth
    }

    return sb.toString()

}

private fun Any?.getDisplayWidth(max: Double): Double {
    when {
        this == null -> return 0.0
        this is Map<*,*> && this.isEmpty() -> return min(2.0, max)
        this is Collection<*> && this.isEmpty() -> return min(2.0, max)
        this is Array<*> && this.isEmpty() -> return min(2.0, max)
        else -> {
            var width = 0.0
            val stringVal = this.toString().also { if (it.length >= max) return max }
            for (c in stringVal) {
                val w = c.displayWidth
                when {
                    width + w > max -> return width
                    width == max -> return max
                    else -> width += w
                }
            }
            return width
        }
    }
}


private val Char.displayWidth: Double
    get() {
        return when(this) {
            '\n' -> 2.0
            '\r' -> 2.0
            '\b' -> 2.0
            else -> this.fontWidth
        }
    }