@file:JvmName("Validator")

package com.github.nayasis.kotlin.basica.core.validator

import com.github.nayasis.kotlin.basica.model.NGrid
import java.io.InputStream

fun isEmpty(value: Any?): Boolean {
    return when (value) {
        null -> true
        is CharSequence -> value.isEmpty()
        is Collection<*> -> value.isEmpty()
        is Array<*> -> value.isEmpty()
        is NGrid -> value.size() == 0
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

fun toYn(value: Any?): String = toYn(value,false)

fun toBoolean(value: Any?, emptyToTrue: Boolean = false): Boolean {
    if( isEmpty(value) )
        return emptyToTrue
    if( value is Boolean )
        return value
    return "Y" == toYn(value)
}

fun toBoolean(value: Any?): Boolean = toBoolean(value,false)