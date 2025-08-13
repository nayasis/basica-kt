package io.github.nayasis.kotlin.basica.core.math

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.Date

fun maxOf(a: LocalDateTime, b: LocalDateTime): LocalDateTime = if( a > b ) a else b
fun minOf(a: LocalDateTime, b: LocalDateTime): LocalDateTime = if( a > b ) b else a

fun maxOf(a: LocalDate, b: LocalDate): LocalDate = if( a > b ) a else b
fun minOf(a: LocalDate, b: LocalDate): LocalDate = if( a > b ) b else a

fun maxOf(a: ZonedDateTime, b: ZonedDateTime): ZonedDateTime = if( a > b ) a else b
fun minOf(a: ZonedDateTime, b: ZonedDateTime): ZonedDateTime = if( a > b ) b else a

fun maxOf(a: Date, b: Date): Date = if( a > b ) a else b
fun minOf(a: Date, b: Date): Date = if( a > b ) b else a