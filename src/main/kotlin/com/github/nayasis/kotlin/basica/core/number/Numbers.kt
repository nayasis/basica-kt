package com.github.nayasis.kotlin.basica.core.number

import java.math.BigDecimal
import java.math.BigInteger
import java.text.DecimalFormat
import java.time.Duration
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

fun Number.format(fractionDigits: Int): String {
    return DecimalFormat().apply { maximumFractionDigits = fractionDigits }.format(this)
}

val Number.millis: Duration
    get() = Duration.ofMillis(this.toLong())

val Number.seconds: Duration
    get() = Duration.ofSeconds(this.toLong())

val Number.minutes: Duration
    get() = Duration.ofMinutes(this.toLong())

val Number.hours: Duration
    get() = Duration.ofHours(this.toLong())

operator fun Duration.plus(duration: Duration): Duration
    = this.plus(duration)

operator fun Duration.minus(duration: Duration): Duration
    = this.minus(duration)