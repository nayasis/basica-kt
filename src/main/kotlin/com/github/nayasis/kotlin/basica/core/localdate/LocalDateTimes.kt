@file:JvmName("LocalDateTimes")

package com.github.nayasis.kotlin.basica.core.localdate

import com.github.nayasis.kotlin.basica.core.string.capture
import com.github.nayasis.kotlin.basica.core.string.extractDigit
import java.nio.file.attribute.FileTime
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import java.time.temporal.ChronoField
import java.time.temporal.Temporal
import java.util.*
import kotlin.math.min
import java.sql.Date as SqlDate

private val PATTERN_OFFSET = "(.*)([+-])(\\d{2}):?(\\d{2})(.*)".toRegex()

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

    val fmt = parseFormat(format)

    val (body,offset) = this.capture(PATTERN_OFFSET).let {
        if(it.isEmpty()) {
            Pair(this.extractDigit(),"")
        } else {
            Pair("${it[0]}${it[4]}".extractDigit(),"${it[1]}${it[2]}${it[3]}")
        }
    }

    val pattern = StringBuilder()
    val value   = StringBuilder()

    var d = 0
    var pre: Char? = null

    for( f in 0 until min(fmt.length,body.length) ) {
        val cur = fmt[f]
        // avoid bug(JDK-8031085) in Java8
        if( cur == 'S' && pre != 'S' ) {
            pattern.append('.')
            value.append('.')
        }
        pattern.append(cur)
        value.append( body[d++] )
        pre = cur
    }

    return parse(value.toString(),pattern.toString()).let {
        if( offset.isEmpty() ) it else it.withOffset(ZoneOffset.of(offset))
    }

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
    if( format.isEmpty() ) return "yyyyMMddHHmmssSSS"
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

fun String.toLocalDateTime(format: DateTimeFormatter): LocalDateTime = LocalDateTime.parse(this, format)

fun String.toLocalDate(format: String = ""): LocalDate = this.toLocalDateTime(format).toLocalDate()

fun String.toLocalDate(format: DateTimeFormatter): LocalDate = this.toLocalDateTime(format).toLocalDate()

fun String.toZonedDateTime(format: String = "", zoneId: ZoneId = ZoneId.systemDefault()): ZonedDateTime =
    ZonedDateTime.of( this.toLocalDateTime(format), zoneId )

fun String.toZonedDateTime(format: DateTimeFormatter, zoneId: ZoneId = ZoneId.systemDefault()): ZonedDateTime =
    ZonedDateTime.of( this.toLocalDateTime(format), zoneId )

fun String.toDate(format: String = "", zoneId: ZoneId = ZoneId.systemDefault()): Date =
    Date.from( this.toZonedDateTime(format,zoneId).toInstant() )

fun String.toDate(format: DateTimeFormatter, zoneId: ZoneId = ZoneId.systemDefault()): Date =
    Date.from( this.toZonedDateTime(format,zoneId).toInstant() )

fun String.toSqlDate(format: String = ""): java.sql.Date =
    java.sql.Date.valueOf(this.toLocalDate(format))

fun String.toSqlDate(format: DateTimeFormatter): java.sql.Date =
    java.sql.Date.valueOf(this.toLocalDate(format))

fun LocalDateTime.atStartOfMonth(): LocalDateTime = this.withDayOfMonth(1)

fun LocalDateTime.atEndOfMonth(): LocalDateTime = this.withDayOfMonth(this.toLocalDate().lengthOfMonth())

fun LocalDate.atStartOfMonth(): LocalDate = this.withDayOfMonth(1)

fun LocalDate.atEndOfMonth(): LocalDate = this.withDayOfMonth(this.lengthOfMonth())

fun LocalDateTime.atStartOfDay(): LocalDateTime =
    this.with(ChronoField.NANO_OF_DAY, LocalTime.MIN.toNanoOfDay())

fun LocalDateTime.atEndOfDay(): LocalDateTime =
    this.with(ChronoField.NANO_OF_DAY, LocalTime.MAX.toNanoOfDay())

// [LocalDate.atStartOfDay()] is already exists.

fun LocalDate.atEndOfDay(): LocalDateTime = LocalDateTime.of(this, LocalTime.MAX)

// format, toString

fun LocalDateTime.format(format: String = ""): String =
    this.format( printFormat(format, ISO_LOCAL_DATE_TIME) )

fun LocalDate.format(format: String = ""): String =
    this.format( printFormat(format, ISO_LOCAL_DATE) )

fun Date.format(format: String = "", zoneId: ZoneId = ZoneId.systemDefault()): String =
    this.toLocalDateTime(zoneId).format( printFormat(format, ISO_LOCAL_DATE_TIME) )

fun SqlDate.format(format: String = ""): String =
    this.toLocalDate().format( printFormat(format, ISO_LOCAL_DATE_TIME) )

fun LocalDateTime.toString(format: String = ""): String = this.format(format)

fun LocalDate.toString(format: String = ""): String = this.format(format)

fun Date.toString(format: String = "", zoneId: ZoneId = ZoneId.systemDefault()): String = this.format(format,zoneId)

fun SqlDate.toString(format: String = ""): String = this.format(format)

// [LocalDateTime -> LocalDate] is already exists.

fun LocalDateTime.toDate(zoneId: ZoneId = ZoneId.systemDefault()): Date =
    Date.from( this.toZonedDateTime(zoneId).toInstant() )

fun LocalDateTime.toZonedDateTime(zoneId: ZoneId = ZoneId.systemDefault()): ZonedDateTime =
    this.atZone(zoneId)

fun LocalDateTime.toSqlDate(): SqlDate = SqlDate.valueOf(this.toLocalDate())

fun LocalDateTime.between(other: Temporal): Duration = Duration.between(this,other)

fun LocalDate.toDate(zoneId: ZoneId = ZoneId.systemDefault()): Date =
    Date.from( this.atStartOfDay(zoneId).toInstant() )

fun LocalDate.toLocalDateTime(): LocalDateTime = this.atStartOfDay()

fun LocalDate.toZonedDateTime(zoneId: ZoneId = ZoneId.systemDefault()): ZonedDateTime =
    this.atStartOfDay(zoneId)

fun LocalDate.toSqlDate(): SqlDate = SqlDate.valueOf(this)

fun LocalDate.between(other: Temporal): Duration = Duration.between(this,other)

fun Date.toLocalDateTime(zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime =
    this.toZonedDateTime(zoneId).toLocalDateTime()

fun Date.toLocalDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate =
    this.toZonedDateTime(zoneId).toLocalDate()

fun Date.toZonedDateTime(zoneId: ZoneId = ZoneId.systemDefault()): ZonedDateTime =
    ZonedDateTime.ofInstant(this.toInstant(),zoneId)

fun Date.toSqlDate(zoneId: ZoneId = ZoneId.systemDefault()): SqlDate =
    SqlDate.valueOf(this.toLocalDate(zoneId))

fun LocalDateTime.toLong(): Long = this.toZonedDateTime().toInstant().toEpochMilli()

fun LocalDateTime.toFileTime(): FileTime = FileTime.fromMillis(this.toLong())

fun LocalDate.toLong(): Long = this.toLocalDateTime().toLong()

fun Long.toZonedDateTime(zoneId: ZoneId = ZoneId.systemDefault()): ZonedDateTime =
    Instant.ofEpochMilli(this).atZone(zoneId)

fun Long.toLocalDateTime(): LocalDateTime = this.toZonedDateTime().toLocalDateTime()

fun Long.toLocalDate(): LocalDate = this.toLocalDateTime().toLocalDate()

fun LocalDateTime.withOffset(offset: ZoneOffset): LocalDateTime =
    this.atOffset(ZoneOffset.UTC).withOffsetSameInstant(offset).toLocalDateTime()
