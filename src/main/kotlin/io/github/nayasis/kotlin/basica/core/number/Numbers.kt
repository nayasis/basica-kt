package io.github.nayasis.kotlin.basica.core.number

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.reflect.KClass
import kotlin.time.Duration

@Suppress("UNCHECKED_CAST")
fun <T: Number> Number.cast(type: KClass<T>): T {
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

inline fun <reified T: Number> Number.cast(): T {
    return cast(T::class)
}

fun Number.format(fractionDigits: Int): String {
    return DecimalFormat().apply { maximumFractionDigits = fractionDigits }.format(this)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
operator fun Duration.plus(duration: Duration): Duration = this.plus(duration)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
operator fun Duration.minus(duration: Duration): Duration = this.minus(duration)

fun Double.round(scale: Int = 0, mode: RoundingMode = RoundingMode.HALF_UP): Double {
    return BigDecimal(this).setScale(scale,mode).toDouble()
}

fun Double.floor(scale: Int = 0): Double {
    return BigDecimal(this).setScale(scale,RoundingMode.FLOOR).toDouble()
}

fun Double.ceil(scale: Int = 0): Double {
    return BigDecimal(this).setScale(scale,RoundingMode.CEILING).toDouble()
}

fun Float.round(scale: Int = 0, mode: RoundingMode = RoundingMode.HALF_UP): Float {
    return BigDecimal(this.toDouble()).setScale(scale,mode).toFloat()
}

fun Float.floor(scale: Int = 0): Float {
    return BigDecimal(this.toDouble()).setScale(scale,RoundingMode.FLOOR).toFloat()
}

fun Float.ceil(scale: Int = 0): Float {
    return BigDecimal(this.toDouble()).setScale(scale,RoundingMode.CEILING).toFloat()
}

fun Number.toBigDecimal(): BigDecimal {
    return when(this) {
        is BigDecimal -> this.add(BigDecimal.ZERO)
        is BigInteger -> BigDecimal(this)
        is Short -> BigDecimal(this.toInt())
        is Byte -> BigDecimal(this.toInt())
        is Int -> BigDecimal(this)
        is Long -> BigDecimal(this)
        is Float -> BigDecimal(this.toDouble())
        is Double -> BigDecimal(this)
        else -> throw TypeCastException("Can not convert to BigDecimal (type:${this.javaClass}, value:$this)")
    }
}

fun BigDecimal.isEqual(other: BigDecimal?): Boolean {
    return this.compareTo(other) == 0
}

fun BigDecimal.isNotEqual(other: BigDecimal?): Boolean {
    return this.compareTo(other) != 0
}

fun BigDecimal.div(other: BigDecimal, scale: Int = 6): BigDecimal {
    return this.divide(other,scale,RoundingMode.DOWN)
}