package com.github.nayasis.kotlin.basica.core.math

import java.math.BigDecimal
import java.math.RoundingMode

fun round(x: Double, scale: Int, mode: RoundingMode = RoundingMode.HALF_UP): Double {
    return BigDecimal(x).setScale(scale,mode).toDouble()
}