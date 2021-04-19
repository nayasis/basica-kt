package com.github.nayasis.kotlin.basica.model

import com.github.nayasis.kotlin.basica.core.fontwidth

private val REGEX_N = "\n".toRegex()
private val REGEX_R = "\r".toRegex()

private fun toString(value: Any?, maxWidth: Int): String {
    when (value) {
        null -> return ""
        is Map<*, *> -> if (value.isEmpty()) return "{}"
        is Collection<*> -> if (value.isEmpty()) return "[]"
    }
    return value.toString().substring(0,maxWidth).replace(REGEX_N, "\\\\n").replace(REGEX_R, "\\\\r")
}

private fun toDisplayString(value: Any?, maxWidth: Double): String {

    val sb = StringBuilder()
    var len = 0.0

    for( c in toString(value,maxWidth.toInt()) ) {
        len += c.fontwidth()
        if( len > maxWidth )
            return sb.toString()
        sb.append(c)
        if( len == maxWidth ) {
            return sb.toString()
        }
    }

    return sb.toString()

}

private fun getDisplayWidth(value: Any?, max: Int): Double {
    var width = 0.0
    for( c in toString(value,max) ) {
        width += c.fontwidth()
        if( width >= max )
            return width
    }
    return width
}

class NGridPrinter(
    val showCount: Int = 500,
    val maxColumnWidth: Int = 255,
) {


}

class PrintMeta {

    val header = ArrayList<String>()
    val alias  = ArrayList<String>()
    val width  = ArrayList<Double>()

    constructor(ngrid: NGrid) {

    }

}