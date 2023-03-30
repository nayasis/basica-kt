@file:JvmName("LocalDateTimes")

package com.github.nayasis.kotlin.basica.core.localdate

import com.github.nayasis.kotlin.basica.core.extention.isEmpty
import com.github.nayasis.kotlin.basica.core.string.capture
import com.github.nayasis.kotlin.basica.core.string.extractDigit
import java.nio.file.attribute.FileTime
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.*
import java.time.temporal.ChronoField
import java.time.temporal.Temporal
import java.util.*
import kotlin.math.min
import java.sql.Date as SqlDate

private val REGEX_OFFSET    = "(.*)([+-])(\\d{2}):?(\\d{2})(.*)".toRegex()
private val REGEX_USER_TEXT = "'.*?'".toRegex()
private val REGEX_YEAR      = "YYYY".toRegex()
private val REGEX_MONTH     = "(^|[^M])MM([^M]|$)".toRegex()
private val REGEX_DATE      = "(^|[^D])DD([^D]|$)".toRegex()
private val REGEX_MIN       = "MI".toRegex()
private val REGEX_SEC       = "(^|[^S])SS([^S]|$)".toRegex()
private val REGEX_MILISEC   = "(^|[^F])FFF([^F]|$)".toRegex()
private val REGEX_REMAINS   = "[^yMdHmsSZ]".toRegex()

/**
 * convert string to LocalDateTime
 *
 * @receiver String
 * @param format date format
 * @param native use input format itself
 * @return LocalDateTime
 */
@Suppress("DuplicatedCode")
fun String.toLocalDateTime(format: String = "", native: Boolean = false): LocalDateTime {

    if( format.isNotEmpty() && native )
        return toLocalDateTime(this, format)

    val fmt = toDateTimeFormat(format)

    val (body,offset) = this.capture(REGEX_OFFSET).let {
        if(it.isEmpty()) {
            Pair(this.extractDigit(),"")
        } else {
            Pair("${it[0]}${it[4]}".extractDigit(),"${it[1]}${it[2]}${it[3]}")
        }
    }

    val pattern = StringBuilder()
    val value   = StringBuilder()

    for( i in 0 until min(fmt.length, body.length) ) {
        val curr = fmt[i]
        val next = fmt.getOrNull(i+1)
        pattern.append(curr)
        value.append( body[i] )
        // avoid bug(JDK-8031085) in Java8
        if( curr != 'S' && next == 'S' ) {
            pattern.append('.')
            value.append('.')
        }

    }

    return toLocalDateTime(value.toString(),pattern.toString()).let {
        if( offset.isEmpty() ) it else it.withOffset(ZoneOffset.of(offset))
    }

}

private fun toLocalDateTime(value: String, pattern: String): LocalDateTime {
    val ofPattern = ofPattern(pattern)
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

/**
 * convert string to LocalTime
 *
 * @receiver String
 * @param format date format
 * @param native use input format itself
 * @return LocalDateTime
 */
@Suppress("DuplicatedCode")
fun String.toLocalTime(format: String = "", native: Boolean = false): LocalTime {
    if( format.isNotEmpty() && native )
        return toLocalTime(this, format)

    val fmt  = toTimeFormat(format)
    val body = this.extractDigit()

    val pattern = StringBuilder()
    val value   = StringBuilder()

    for( i in 0 until min(fmt.length, body.length) ) {
        val curr = fmt[i]
        val next = fmt.getOrNull(i+1)
        pattern.append(curr)
        value.append( body[i] )
        // avoid bug(JDK-8031085) in Java8
        if( curr != 'S' && next == 'S' ) {
            pattern.append('.')
            value.append('.')
        }
    }

    return toLocalTime(value.toString(),pattern.toString())
}

private fun toLocalTime(value: String, pattern: String): LocalTime {
    return LocalTime.parse(value, DateTimeFormatter.ofPattern(pattern))
}

private fun toDateTimeFormat(format: String): String {
    if( format.isEmpty() ) return "yyyyMMddHHmmssSSS"
    return try { format
        .replace(REGEX_USER_TEXT, "") // remove user text
        .replace(REGEX_YEAR, "yyyy")
        .replace(REGEX_DATE, "$1dd$2")
        .replace(REGEX_MIN, "mm")
        .replace(REGEX_SEC, "$1ss$2")
        .replace(REGEX_MILISEC, "$1SSS$2")
        .replace(REGEX_REMAINS, "")
    } catch (e : Exception) {
        throw IllegalArgumentException("Invalid pattern : $format", e)
    }
}

private fun toPrintingDateTimeFormat(format: String, native: Boolean = false, default: DateTimeFormatter): DateTimeFormatter {
    if( format.isEmpty() ) return default
    return try { (if(native) format else format
        .replace(REGEX_YEAR, "yyyy")
        .replace(REGEX_DATE, "$1dd$2")
        .replace(REGEX_MIN, "mm")
        .replace(REGEX_SEC, "$1ss$2")
        .replace(REGEX_MILISEC, "$1SSS$2")
        ).let { ofPattern(it)  }
    } catch (e: Exception) {
        throw IllegalArgumentException("Invalid pattern : $format", e)
    }
}

private fun toTimeFormat(format: String): String {
    if(format.isEmpty()) return "HHmmssSSS"
    return try { format
        .replace(REGEX_USER_TEXT, "") // remove user text
        .replace(REGEX_YEAR, "")
        .replace(REGEX_MONTH, "")
        .replace(REGEX_DATE, "")
        .replace(REGEX_MIN, "mm")
        .replace(REGEX_SEC, "$1ss$2")
        .replace(REGEX_MILISEC, "$1SSS$2")
        .replace(REGEX_REMAINS, "")
    } catch (e : Exception) {
        throw IllegalArgumentException("Invalid pattern : $format", e)
    }
}

private fun toPrintingTimeFormat(format: String, native: Boolean = false, default: DateTimeFormatter): DateTimeFormatter {
    if( format.isEmpty() ) return default
    return try { (if(native) format else format
            .replace(REGEX_YEAR, "")
            .replace(REGEX_MONTH, "")
            .replace(REGEX_DATE, "")
            .replace(REGEX_MIN, "mm")
            .replace(REGEX_SEC, "$1ss$2")
            .replace(REGEX_MILISEC, "$1SSS$2")
        ).let { ofPattern(it)  }
    } catch (e: Exception) {
        throw IllegalArgumentException("Invalid pattern : $format", e)
    }
}

fun String.toLocalDateTime(): LocalDateTime = toLocalDateTime("",false)

fun String.toLocalDateTime(format: DateTimeFormatter): LocalDateTime = LocalDateTime.parse(this, format)

fun String.toLocalDate(): LocalDate = toLocalDate("",false)

fun String.toLocalDate(format: String = "", native: Boolean = false): LocalDate = this.toLocalDateTime(format,native).toLocalDate()

fun String.toLocalDate(format: DateTimeFormatter): LocalDate = this.toLocalDateTime(format).toLocalDate()

fun String.toLocalTime(): LocalTime = toLocalTime("",false)

fun String.toLocalTime(format: DateTimeFormatter): LocalTime = LocalTime.parse(this, format)

fun String.toZonedDateTime(format: String = "", native: Boolean = false, zoneId: ZoneId = ZoneId.systemDefault()): ZonedDateTime =
    ZonedDateTime.of( this.toLocalDateTime(format,native), zoneId )

fun String.toZonedDateTime(format: DateTimeFormatter, zoneId: ZoneId = ZoneId.systemDefault()): ZonedDateTime =
    ZonedDateTime.of( this.toLocalDateTime(format), zoneId )

fun String.toDate(format: String = "", native: Boolean = false, zoneId: ZoneId = ZoneId.systemDefault()): Date =
    Date.from( this.toZonedDateTime(format,native,zoneId).toInstant() )

fun String.toDate(format: DateTimeFormatter, zoneId: ZoneId = ZoneId.systemDefault()): Date =
    Date.from( this.toZonedDateTime(format,zoneId).toInstant() )

fun String.toSqlDate(format: String = "", native: Boolean = false): java.sql.Date =
    java.sql.Date.valueOf(this.toLocalDate(format,native))

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

fun LocalDateTime.format(format: String = "", native: Boolean = false): String =
    this.format(toPrintingDateTimeFormat(format, native, ISO_LOCAL_DATE_TIME))

fun LocalDate.format(format: String = "", native: Boolean = false): String =
    this.format(toPrintingDateTimeFormat(format, native, ISO_LOCAL_DATE))

fun LocalTime.format(format: String = "", native: Boolean = false): String =
    this.format(toPrintingTimeFormat(format, native, ISO_LOCAL_TIME))

fun Date.format(format: String = "", native: Boolean = false, zoneId: ZoneId = ZoneId.systemDefault()): String =
    this.toLocalDateTime(zoneId).format(toPrintingDateTimeFormat(format, native, ISO_LOCAL_DATE_TIME))

fun SqlDate.format(format: String = "", native: Boolean = false): String =
    this.toLocalDate().format(toPrintingDateTimeFormat(format, native, ISO_LOCAL_DATE_TIME))

fun LocalDateTime.toString(format: String = ""): String = this.format(format)

fun LocalDate.toString(format: String = ""): String = this.format(format)

//fun LocalTime.toString(format: String = ""): String = this.for

fun Date.toString(format: String = "", native: Boolean = false, zoneId: ZoneId = ZoneId.systemDefault()): String = this.format(format,native,zoneId)

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
