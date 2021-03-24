package com.github.nayasis.kotlin.basica.core

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import java.time.temporal.ChronoField
import java.util.*
import java.sql.Date as SqlDate

/**
 * convert string to Date
 *
 * @receiver String
 * @param format date format
 * @param native use input format itself
 * @return Date
 */
@Suppress("NAME_SHADOWING")
fun String.toDate(format: String = "", native: Boolean = false): Date {

    if( format.isNotEmpty() && native )
        return SimpleDateFormat(format).parse(this)

    val format = parseFormat(format)
    val digits = this.replace("[^0-9\\+]".toRegex(), "")

    val size = kotlin.math.min( format.length, digits.replace("+","").length )

    val pattern = StringBuilder()
    val value   = StringBuilder()

    var k = 0
    for( i in 0 until size) {
        val c = format[i]
        pattern.append(c)
        if( c == 'Z' ) {
            repeat(5) { value.append(digits[k++]) }
        } else {
            value.append( digits[k++] )
        }
    }

    return SimpleDateFormat(pattern.toString()).parse(value.toString())

}

/**
 * convert string to LocalDateTime
 *
 * @receiver String
 * @param format date format
 * @param native use input format itself
 * @return LocalDateTime
 */
fun String.toLocalDateTime(format: String = "", native: Boolean = false): LocalDateTime {
    return this.toDate(format,native).toLocalDateTime()
}

private fun parseFormat(format: String): String {
    if( format.isEmpty() ) return "yyyyMMddHHmmssSSSZ"
    return format
        .replace("'.*?'".toRegex(), "") // remove user text
        .replace("YYYY".toRegex(), "yyyy")
        .replace("(^|[^D])DD([^D]|$)".toRegex(), "$1dd$2")
        .replace("MI".toRegex(), "mm")
        .replace("(^|[^S])SS([^S]|$)".toRegex(), "$1ss$2")
        .replace("(^|[^F])FFF([^F]|$)".toRegex(), "$1SSS$2")
        .replace("[^yMdHmsSZ]".toRegex(), "")
}

private fun printFormat(format: String, default: DateTimeFormatter): DateTimeFormatter {
    if( format.isEmpty() ) return default
    return DateTimeFormatter.ofPattern( format
        .replace("YYYY".toRegex(), "yyyy")
        .replace("(^|[^D])DD([^D]|$)".toRegex(), "$1dd$2")
        .replace("MI".toRegex(), "mm")
        .replace("(^|[^S])SS([^S]|$)".toRegex(), "$1ss$2")
        .replace("(^|[^F])FFF([^F]|$)".toRegex(), "$1SSS$2")
    )
}

fun String.toLocalDateTime(format: DateTimeFormatter): LocalDateTime {
    return LocalDateTime.parse(this, format)
}

fun String.toLocalDate(format: String = ""): LocalDate {
    return this.toLocalDateTime(format).toLocalDate()
}

fun String.toLocalDate(format: DateTimeFormatter): LocalDate {
    return this.toLocalDateTime(format).toLocalDate()
}

fun String.toZonedDateTime(format: String = "", zoneId: ZoneId = ZoneId.systemDefault()): ZonedDateTime {
    return ZonedDateTime.of( this.toLocalDateTime(format), zoneId )
}

fun String.toZonedDateTime(format: DateTimeFormatter, zoneId: ZoneId = ZoneId.systemDefault()): ZonedDateTime {
    return ZonedDateTime.of( this.toLocalDateTime(format), zoneId )
}

fun String.toDate(format: DateTimeFormatter, zoneId: ZoneId = ZoneId.systemDefault()): Date {
    return Date.from( this.toZonedDateTime(format,zoneId).toInstant() )
}

fun String.toSqlDate(format: String = ""): java.sql.Date {
    return java.sql.Date.valueOf(this.toLocalDate(format))
}

fun String.toSqlDate(format: DateTimeFormatter): java.sql.Date {
    return java.sql.Date.valueOf(this.toLocalDate(format))
}

fun LocalDateTime.atStartOfMonth(): LocalDateTime {
    return this.withDayOfMonth(1)
}

fun LocalDateTime.atEndOfMonth(): LocalDateTime {
    return this.withDayOfMonth(this.toLocalDate().lengthOfMonth())
}

fun LocalDate.atStartOfMonth(): LocalDate {
    this.atStartOfDay()
    return this.withDayOfMonth(1)
}

fun LocalDate.atEndOfMonth(): LocalDate {
    return this.withDayOfMonth(this.lengthOfMonth())
}

fun LocalDateTime.atStartOfDay(): LocalDateTime {
    return this.with(ChronoField.NANO_OF_DAY, LocalTime.MIN.toNanoOfDay())
}

fun LocalDateTime.atEndOfDay(): LocalDateTime {
    return this.with(ChronoField.NANO_OF_DAY, LocalTime.MAX.toNanoOfDay())
}

// [LocalDate.atStartOfDay()] is already exists.

fun LocalDate.atEndOfDay(): LocalDateTime {
    return LocalDateTime.of(this, LocalTime.MAX)
}

fun LocalDateTime.toString(format: String = ""): String {
    return this.format( printFormat(format, ISO_LOCAL_DATE_TIME) )
}

fun LocalDate.toString(format: String = ""): String {
    return this.format( printFormat(format, ISO_LOCAL_DATE) )
}

fun Date.toString(format: String = "", zoneId: ZoneId = ZoneId.systemDefault()): String {
    return this.toLocalDateTime(zoneId).format( printFormat(format, ISO_LOCAL_DATE_TIME) )
}

fun SqlDate.toString(format: String = ""): String {
    return this.toLocalDate().format( printFormat(format, ISO_LOCAL_DATE_TIME) )
}

