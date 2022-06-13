package com.github.nayasis.kotlin.basica.core.math

import java.time.LocalDateTime

fun max(a: LocalDateTime, b: LocalDateTime): LocalDateTime = if( a > b ) a else b
fun min(a: LocalDateTime, b: LocalDateTime): LocalDateTime = if( a > b ) b else a
