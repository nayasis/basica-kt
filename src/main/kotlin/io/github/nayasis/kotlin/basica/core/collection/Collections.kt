package io.github.nayasis.kotlin.basica.core.collection

import io.github.nayasis.kotlin.basica.model.NGrid
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.time.Duration

fun <T> Iterator<T>.toList(): List<T> {
    return ArrayList<T>().apply {
        while ( hasNext() )
            this += next()
    }
}

inline fun <T> Iterable<T>.sumByDuration(selector: (T) -> Duration): Duration {
    var sum = Duration.ZERO
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

inline fun <T> Iterable<T>.sumByLong(selector: (T) -> Long): Long {
    var sum = 0L
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

inline fun <T> Iterable<T>.sumByBigDecimal(selector: (T) -> BigDecimal): BigDecimal {
    var sum = BigDecimal.ZERO
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

inline fun <T> Iterable<T>.sumByBigInteger(selector: (T) -> BigInteger): BigInteger {
    var sum = BigInteger.ZERO
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

inline fun <reified T> Collection<T>.toNGrid(): NGrid {
    return NGrid(this,T::class)
}