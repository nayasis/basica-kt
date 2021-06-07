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

infix fun <T> Boolean.then( param: T? ): T? = if(this) param else null

fun <T> T?.ifEmpty( fn: () -> T ): T {
    return if(isEmpty(this) ) fn() else this!!
}

fun <T> T?.ifNotEmpty( fn: (T) -> T? ): T? {
    return if(isNotEmpty(this) ) fn(this!!) else this
}

fun <T> T?.ifNull( fn: () -> T ): T {
    return this ?: fn()
}

fun <T> T?.ifNotNull( fn: (T) -> T? ): T? {
    return if(this != null) fn(this) else this
}

inline fun <T> Iterable<T>.sumByLong(selector: (T) -> Long): Long {
    var sum = 0L
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

