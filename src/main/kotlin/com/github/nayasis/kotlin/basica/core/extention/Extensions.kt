package com.github.nayasis.kotlin.basica.core.extention

import com.github.nayasis.kotlin.basica.core.validator.isEmpty
import com.github.nayasis.kotlin.basica.core.validator.isNotEmpty
import java.util.*
import kotlin.reflect.KProperty

class FieldProperty<R, T: Any?>(
    val initialValue: (R) -> T
) {

    private val map = WeakHashMap<R,T>()

    operator fun getValue(thisRef: R, property: KProperty<*>): T =
        map[thisRef] ?: setValue(thisRef, property, initialValue(thisRef))

    operator fun setValue(thisRef: R, property: KProperty<*>, value: T): T {
        map[thisRef] = value
        return value
    }

}

fun Any?.isEmpty(): Boolean {
    return isEmpty(this)
}

fun Any?.isNotEmpty(): Boolean {
    return isNotEmpty(this)
}

infix fun <T> Boolean.then(param: T?): T? = if(this) param else null

inline infix fun <T> Boolean.then(fn: () -> T): T? = if(this) fn() else null

inline fun <T> T?.ifEmpty(fn: () -> T): T {
    return if(isEmpty(this) ) fn() else this!!
}

inline fun <T> T?.ifNull(fn: () -> T): T {
    return this ?: fn()
}

inline fun <T,R> T?.ifNotEmpty(fn: (T) -> R): R? {
    return if(isNotEmpty(this) ) fn(this!!) else null
}

inline fun <T,R> T?.ifNotNull(fn: (T) -> R): R? {
    return if(this != null) fn(this) else null
}