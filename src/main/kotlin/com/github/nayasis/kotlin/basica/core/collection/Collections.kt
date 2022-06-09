package com.github.nayasis.kotlin.basica.core.collection

import com.github.nayasis.kotlin.basica.model.NGrid

fun <T> Iterator<T>.toList(): List<T> {
    return ArrayList<T>().apply {
        while ( hasNext() )
            this += next()
    }
}

inline fun <T> Iterable<T>.sumByLong(selector: (T) -> Long): Long {
    var sum = 0L
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

inline fun <reified T> Collection<T>.toNGrid(): NGrid {
    return NGrid(this,T::class)
}