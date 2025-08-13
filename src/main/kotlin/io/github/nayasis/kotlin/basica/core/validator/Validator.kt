@file:JvmName("Validator")

package io.github.nayasis.kotlin.basica.core.validator

import io.github.nayasis.kotlin.basica.model.NGrid
import io.github.nayasis.kotlin.basica.model.dataframe.DataFrame
import java.io.InputStream

fun isEmpty(value: Any?): Boolean {
    return when (value) {
        null -> true
        is CharSequence -> value.isEmpty()
        is Collection<*> -> value.isEmpty()
        is Map<*,*> -> value.isEmpty()
        is Array<*> -> value.isEmpty()
        is ByteArray -> value.isEmpty()
        is CharArray -> value.isEmpty()
        is ShortArray -> value.isEmpty()
        is IntArray -> value.isEmpty()
        is LongArray -> value.isEmpty()
        is FloatArray -> value.isEmpty()
        is DoubleArray -> value.isEmpty()
        is BooleanArray -> value.isEmpty()
        is NGrid -> value.size == 0
        is DataFrame -> value.size == 0
        is InputStream -> value.available() == 0
        else -> false
    }
}

fun isNotEmpty(value: Any?): Boolean = ! isEmpty(value)

fun <T> nvl(value: T?, other: T ): T {
    if( isNotEmpty(value) ) return value!!
    return other
}

fun <T> nvl(value: T?, other: T?, another: T ): T {
    if (isNotEmpty(value)) return value!!
    if (isNotEmpty(other)) return other!!
    return another
}

fun nvl(value: Any?): String {
    return value?.toString() ?: ""
}

@JvmOverloads
fun toYn(value: Any?, emptyToY: Boolean = false): String {
    if( isEmpty(value) )
        return if(emptyToY) "Y" else "N"
    if( value is Boolean )
        return if(value) "Y" else "N"
    val v = value.toString().trim()
    return when {
        v.equals("y",true) -> "Y"
        v.equals("yes",true) -> "Y"
        v.equals("t",true) -> "Y"
        v.equals("true",true) -> "Y"
        else -> "N"
    }
}

@JvmOverloads
fun toBoolean(value: Any?, emptyToTrue: Boolean = false): Boolean {
    if( isEmpty(value) )
        return emptyToTrue
    if( value is Boolean )
        return value
    return "Y" == toYn(value)
}