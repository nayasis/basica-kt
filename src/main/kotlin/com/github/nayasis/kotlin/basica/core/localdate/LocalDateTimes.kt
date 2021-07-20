@file:JvmName("LocalDateTimes")

package com.github.nayasis.kotlin.basica.core.localdate

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.MonthDay
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import java.time.temporal.ChronoField
import java.util.*
import kotlin.math.min
import java.sql.Date as SqlDate

private val PATTERN_DATE_DIGIT = "[^0-9\\+]".toRegex()

/**
 * convert string to LocalDateTime
 *
 * @receiver String
 * @param format date format
 * @param native use input format itself
 * @return LocalDateTime
 */
fun String.toLocalDateTime(format: String = "", native: Boolean = false): LocalDateTime {

    if( format.isNotEmpty() && native )
        return parse(this, format)

    val fmt    = parseFormat(format)
    val digits = this.replace(PATTERN_DATE_DIGIT, "")

    val pattern = StringBuilder()
    val value   = StringBuilder()

    var d = 0
    var pre: Char? = null

    for( f in 0 until min(fmt.length,digits.length) ) {
        val cur = fmt[f]
        // avoid bug(JDK-8031085) in Java8
        if( cur == 'S' && pre != 'S' ) {
            pattern.append('.')
            value.append('.')
        }
        pattern.append(cur)
        if( cur == 'Z' ) {
            repeat(5) { value.append(digits[d++]) }
        } else {
            value.append( digits[d++] )
        }
        pre = cur
    }

    return parse(value.toString(), pattern.toString())

}

private fun parse(value: String, pattern: String): LocalDateTime {
    val ofPattern = DateTimeFormatter.ofPattern(pattern)
    return try {
        LocalDateTime.parse( value, ofPattern)
    } catch (e0: Exception) {
        try {
            LocalDate.parse(value, ofPattern).atTime(0,0)
        } catch (e1: Exception) {
            try {
                YearMonth.parse(value,ofPattern).atDay(1).atTime(0,0)
            } catch (e2: Exception) {
                try {
                    Year.parse(value,ofPattern).atMonthDay(MonthDay.of(1,1)).atTime(0,0)
                } catch (e3: Exception) {
                    throw e0
                }
            }
        }
    }
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

fun String.toLocalDateTime(): LocalDateTime = toLocalDateTime("",false)

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

fun String.toDate(format: String = "", zoneId: ZoneId = ZoneId.systemDefault()): Date {
    return Date.from( this.toZonedDateTime(format,zoneId).toInstant() )
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

fun LocalDateTime.toFormat(format: String = ""): String {
    return this.format( printFormat(format, ISO_LOCAL_DATE_TIME) )
}

fun LocalDate.toFormat(format: String = ""): String {
    return this.format( printFormat(format, ISO_LOCAL_DATE) )
}

fun Date.toFormat(format: String = "", zoneId: ZoneId = ZoneId.systemDefault()): String {
    return this.toLocalDateTime(zoneId).format( printFormat(format, ISO_LOCAL_DATE_TIME) )
}

fun SqlDate.toFormat(format: String = ""): String {
    return this.toLocalDate().format( printFormat(format, ISO_LOCAL_DATE_TIME) )
}

// [LocalDateTime -> LocalDate] is already exists.

fun LocalDateTime.toDate(zoneId: ZoneId = ZoneId.systemDefault()): Date {
    return Date.from( this.toZonedDateTime(zoneId).toInstant() )
}

fun LocalDateTime.toZonedDateTime(zoneId: ZoneId = ZoneId.systemDefault()): ZonedDateTime {
    return this.atZone(zoneId)
}

fun LocalDateTime.toSqlDate(): SqlDate {
    return SqlDate.valueOf(this.toLocalDate())
}

fun LocalDate.toDate(zoneId: ZoneId = ZoneId.systemDefault()): Date {
    return Date.from( this.atStartOfDay(zoneId).toInstant() )
}

fun LocalDate.toLocalDateTime(): LocalDateTime {
    return this.atStartOfDay()
}

fun LocalDate.toZonedDateTime(zoneId: ZoneId = ZoneId.systemDefault()): ZonedDateTime {
    return this.atStartOfDay(zoneId)
}

fun LocalDate.toSqlDate(): SqlDate {
    return SqlDate.valueOf(this)
}

fun Date.toLocalDateTime(zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime {
    return this.toZonedDateTime(zoneId).toLocalDateTime()
}

fun Date.toLocalDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate {
    return this.toZonedDateTime(zoneId).toLocalDate()
}

fun Date.toZonedDateTime(zoneId: ZoneId = ZoneId.systemDefault()): ZonedDateTime {
    return ZonedDateTime.ofInstant(this.toInstant(),zoneId)
}

fun Date.toSqlDate(zoneId: ZoneId = ZoneId.systemDefault()): SqlDate {
    return SqlDate.valueOf(this.toLocalDate(zoneId))
}

fun LocalDateTime.toLong(): Long {
    return this.toZonedDateTime().toInstant().toEpochMilli()
}

fun LocalDate.toLong(): Long {
    return this.toLocalDateTime().toLong()
}

fun Long.toZonedDateTime(zoneId: ZoneId = ZoneId.systemDefault()): ZonedDateTime {
    return Instant.ofEpochMilli(this).atZone(zoneId)
}

fun Long.toLocalDateTime(): LocalDateTime {
    return this.toZonedDateTime().toLocalDateTime()
}

fun Long.toLocalDate(): LocalDate {
    return this.toLocalDateTime().toLocalDate()
}