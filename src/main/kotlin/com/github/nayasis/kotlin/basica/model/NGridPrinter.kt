package com.github.nayasis.kotlin.basica.model

import com.github.nayasis.kotlin.basica.core.dpadEnd
import com.github.nayasis.kotlin.basica.core.dpadStart
import com.github.nayasis.kotlin.basica.core.fontwidth
import com.github.nayasis.kotlin.basica.core.repeat
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.round

private const val INDEX_COLUMN_NAME = "index"
private const val NO_DATA = "NO DATA"

private fun toString(value: Any?, maxWidth: Int): String {
    when (value) {
        null -> return ""
        is Map<*, *> -> if (value.isEmpty()) return "{}"
        is Collection<*> -> if (value.isEmpty()) return "[]"
    }
    return value.toString().let {
        it.substring(0,min(maxWidth,it.length))
    }
}

private fun toDisplayString(value: Any?, maxWidth: Int): String {

    val sb = StringBuilder()
    var size = 0.0

    for( c in toString(value,maxWidth) ) {

        val w: Double = when(c) {
            '\n' -> 2.0
            '\r' -> 2.0
            else -> c.fontwidth()
        }

        if( size + w > maxWidth )
            return "${sb.substring(0, maxWidth.toInt()-(4-round(w).toInt()))}..."

        size += w
        sb.append( when(c) {
            '\n' -> "\\n"
            '\r' -> "\\r"
            else -> c
        })
        if( size == maxWidth.toDouble() )
            return sb.toString()

    }

    return sb.toString()

}

private fun getDisplayWidth(value: Any?, max: Double): Double {
    var width = 0.0
    for( c in toString(value,max.toInt()) ) {
        val w = c.fontwidth()
        when {
            width + w >  max -> return width
            width     == max -> return max
            else             -> width += w
        }
    }
    return width
}

class NGridPrinter(
    private val grid: NGrid,
    val maxColumnWidth: Int = 100,
) {

    private val meta = PrintMeta(grid, maxColumnWidth)

    fun toString(showHeader: Boolean, useAlias: Boolean, rowcount:Int, showIndexColumn: Boolean): String {

        val line  = meta.line('-','+',showIndexColumn)
        val empty = meta.line(' ','|',showIndexColumn)
        val body  = StringBuilder().append(line)

        if( showHeader && ! grid.header().isEmpty() ) {
            if( useAlias ) {
                body.append('\n').append(toString(meta.alias,showIndexColumn))
            } else {
                body.append('\n').append(toString(meta.header,showIndexColumn))
            }
            body.append('\n').append(line)
        }

        if( grid.body().isEmpty() ) {
            if( grid.header().isEmpty() ) {
                body.append('\n').append(empty)
            }
        } else {
            for( (i,row) in grid.body() ) {
                when {
                    i > rowcount -> break
                    row.isEmpty() -> body.append('\n').append(empty)
                    else -> body.append('\n').append(toString(row,if(showIndexColumn) i else null))
                }
            }
        }

        return body.append('\n').append(line).toString()

    }

    private fun toString(header : ArrayList<*>, showIndexColumn: Boolean): String {

        var line = StringBuilder().append('|')

        if( showIndexColumn )
            line.append(INDEX_COLUMN_NAME.dpadStart(meta.indexWidth,' ')).append('|')

        for( i in 0 until meta.header.size ) {
            val value  = header[i]
            val width  = meta.width[i]
            line.append(toDisplayString(value,maxColumnWidth).dpadEnd(width, ' ')).append('|')
        }

        return line.toString()

    }

    private fun toString(row : Map<Any,Any?>, index: Int? = null): String {

        var line = StringBuilder().append('|')

        if( index != null )
            line.append(index.toString().dpadStart(meta.indexWidth,' ')).append('|')

        for( i in 0 until meta.header.size ) {
            val key    = meta.header[i]
            val value  = row[key]
            val width  = meta.width[i]
            line.append(toString(value,maxColumnWidth).let {
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

private class PrintMeta {

    val header     = ArrayList<Any>()
    val alias      = ArrayList<String>()
    val width      = ArrayList<Int>()
    var indexWidth = INDEX_COLUMN_NAME.length

    constructor(grid: NGrid, maxColumnWidth: Int) {
        for( key in grid.header().keys() ) {
            header.add(key)
            alias.add(grid.header().getAlias(key))
            width.add(maxwidth(grid,key,maxColumnWidth.toDouble()))
        }
        indexWidth = log10(grid.size().toDouble()).toInt()
    }

    private fun maxwidth(grid: NGrid, key: Any, maxColumnWidth: Double): Int {
        var width = listOf(
            getDisplayWidth(key, maxColumnWidth),
            getDisplayWidth(grid.header().getAlias(key), maxColumnWidth),
            grid.body().values.map { row ->
                row[key]?.let { getDisplayWidth(it,maxColumnWidth) } ?: 0.0
            }.maxOrNull() ?: 0.0
        ).maxOrNull() ?: 0.0
        return round(width).toInt()
    }

    fun line(lineCh: Char, delimiter: Char, indexColumn: Boolean): String {
        var line = StringBuilder().append(delimiter)
        if( indexColumn )
            line.append( lineCh.repeat(indexWidth) ).append(delimiter)
        if( width.isEmpty() ) {
            line.append( lineCh.repeat(3) ).append(delimiter)
        } else {
            for( w in width ) {
                line.append( lineCh.repeat(w) ).append(delimiter)
            }
        }
        return line.toString()
    }

}