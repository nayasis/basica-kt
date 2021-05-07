package com.github.nayasis.kotlin.basica.core

import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
fun <T:Number> Number.cast(type: KClass<T>): T {
    return when(type) {
        this::class -> this
        Short::class -> this.toShort()
        Byte::class -> this.toByte()
        Int::class -> this.toInt()
        Long::class -> this.toLong()
        Float::class -> this.toFloat()
        Double::class -> this.toDouble()
        BigDecimal::class -> BigDecimal(this.toString())
        BigInteger::class -> BigInteger(this.toString())
        else -> this
    } as T
}

inline fun <reified T:Number> Number.cast(): T {
    return cast(T::class)
}