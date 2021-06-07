package com.github.nayasis.kotlin.basica.core.math

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

fun round(x: Double, scale: Int, mode: RoundingMode = RoundingMode.HALF_UP): Double {
    return BigDecimal(x).setScale(scale,mode).toDouble()
}

fun max(a: LocalDateTime, b: LocalDateTime): LocalDateTime = if( a > b ) a else b
fun min(a: LocalDateTime, b: LocalDateTime): LocalDateTime = if( a > b ) b else a
