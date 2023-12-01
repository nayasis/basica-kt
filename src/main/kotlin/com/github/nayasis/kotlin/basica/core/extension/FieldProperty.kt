package com.github.nayasis.kotlin.basica.core.extension

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