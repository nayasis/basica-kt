@file:JvmName("LocalDateTimes")
@file:Suppress("PrivatePropertyName")

package io.github.nayasis.kotlin.basica.core.localdate

import io.github.nayasis.kotlin.basica.core.extension.isEmpty
import io.github.nayasis.kotlin.basica.core.string.capture
import io.github.nayasis.kotlin.basica.core.string.extractDigit
import java.nio.file.attribute.FileTime
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.time.temporal.Temporal
import java.util.*
import kotlin.math.min
import java.sql.Date as SqlDate

private val REGEX_OFFSET  = "(.*)([+-])(\\d{2}):?(\\d{2})(.*)".toRegex()
private val REGEX_REMAINS_DATETIME = "[^yMdHmsSZ]".toRegex()
private val REGEX_REMAINS_TIME     = "[^HmsS]".toRegex()

/**
 * convert string to LocalDateTime
 *
 * @receiver String
 * @param format date format
 * @param native consider input format as Java's LocalDateTime format
 * @return LocalDateTime
 */
@Suppress("DuplicatedCode")
fun String.toLocalDateTime(format: String = "", native: Boolean = false): LocalDateTime {

    if(native && format.isNotEmpty())
        return toLocalDateTime(this, format)

    val fmt = if(format.isEmpty()) "yyyyMMddHHmmssSSS" else format.toJvmTimeFormat(true).replace(REGEX_REMAINS_DATETIME, "")

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

    return toLocalDateTime("$value", "$pattern").let {
        if( offset.isEmpty() ) it else it.withOffset(ZoneOffset.of(offset))
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

    if( native && format.isNotEmpty() )
        return toLocalTime(this, format)

    val fmt  = if(format.isEmpty()) "HHmmssSSS" else format.toJvmTimeFormat(true).replace(REGEX_REMAINS_TIME, "")
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

private fun String.cleansing(): String {
    return this.replace(REGEX_REMAINS_DATETIME, "")
}

private fun toLocalDateTime(value: String, pattern: String): LocalDateTime {
    return value.toLocalDateTime(pattern.toDateTimeFormatter(true))
}

private fun toLocalTime(value: String, pattern: String): LocalTime {
    return LocalTime.parse(value, pattern.toTimeFormatter(true))
}

fun String.toLocalDateTime(): LocalDateTime = toLocalDateTime(native=false)

fun String.toLocalDateTime(pattern: DateTimeFormatter): LocalDateTime {
return runCatching { LocalDateTime.parse(this, pattern) }
    .recoverCatching { LocalDate.parse(this, pattern).atTime(0, 0) }
    .recoverCatching { YearMonth.parse(this, pattern).atDay(1).atTime(0, 0) }
    .recoverCatching { Year.parse(this, pattern).atMonthDay(MonthDay.of(1, 1)).atTime(0, 0) }
    .getOrElse { throw it }
}

fun String.toLocalDate(): LocalDate = toLocalDate(native=false)

fun String.toLocalDate(format: String = "", native: Boolean = false): LocalDate = this.toLocalDateTime(format,native).toLocalDate()

fun String.toLocalDate(format: DateTimeFormatter): LocalDate = this.toLocalDateTime(format).toLocalDate()

fun String.toLocalTime(): LocalTime = toLocalTime(native=false)

fun String.toLocalTime(format: DateTimeFormatter): LocalTime = LocalTime.parse(this, format)

fun String.toZonedDateTime(format: DateTimeFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME, zoneId: ZoneId = ZoneId.systemDefault()): ZonedDateTime {
    return runCatching {
        ZonedDateTime.parse(this, format)
    }.recoverCatching {
        ZonedDateTime.of(this.toLocalDateTime(format), zoneId)
    }.getOrElse {
        ZonedDateTime.of(this.toLocalDateTime(), zoneId)
    }
}

fun String.toDate(format: String = "", native: Boolean = false, zoneId: ZoneId = ZoneId.systemDefault()): Date {
    return this.toLocalDateTime(format, native).toDate(zoneId)
}

fun String.toDate(format: DateTimeFormatter, zoneId: ZoneId = ZoneId.systemDefault()): Date {
    return this.toLocalDateTime(format).toDate(zoneId)
}

fun String.toCalendar(format: String = "", native: Boolean = false, zoneId: ZoneId = ZoneId.systemDefault()): Calendar =
    this.toDate(format, native, zoneId).toCalendar(zoneId)

fun String.toCalendar(format: DateTimeFormatter, zoneId: ZoneId = ZoneId.systemDefault()): Calendar =
    this.toDate(format, zoneId).toCalendar(zoneId)

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

fun Date.toCalendar(zoneId: ZoneId? = null): Calendar {
    val t = this
    return if(zoneId == null) {
        Calendar.getInstance()
    } else {
        Calendar.getInstance(TimeZone.getTimeZone(zoneId))
    }.apply { this.time = t }
}

fun Date.toLocalDateTime(zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime =
    this.toZonedDateTime(zoneId).toLocalDateTime()

fun Date.toLocalDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate =
    this.toZonedDateTime(zoneId).toLocalDate()

fun Date.toZonedDateTime(zoneId: ZoneId = ZoneId.systemDefault()): ZonedDateTime =
    ZonedDateTime.ofInstant(this.toInstant(),zoneId)

fun Date.toLocalTime(zoneId: ZoneId = ZoneId.systemDefault()): LocalTime =
    this.toLocalDateTime(zoneId).toLocalTime()

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

fun Calendar.toDate(): Date = this.time

fun Calendar.toZonedDateTime(zoneId: ZoneId? = null): ZonedDateTime {
    val targetZoneId = zoneId ?: this.timeZone.toZoneId()
    return ZonedDateTime.ofInstant(this.toInstant(), targetZoneId)
}

fun Calendar.toLocalDateTime(zoneId: ZoneId? = null): LocalDateTime =
    this.toZonedDateTime(zoneId).toLocalDateTime()

fun Calendar.toLocalDate(zoneId: ZoneId? = null): LocalDate =
    this.toZonedDateTime(zoneId).toLocalDate()

// ZonedDateTime extension functions for timezone conversion
fun ZonedDateTime.toLocalDateTime(targetZoneId: ZoneId): LocalDateTime =
    this.withZoneSameInstant(targetZoneId).toLocalDateTime()

fun ZonedDateTime.toLocalDate(targetZoneId: ZoneId): LocalDate =
    this.withZoneSameInstant(targetZoneId).toLocalDate()

fun ZonedDateTime.toLocalTime(targetZoneId: ZoneId): LocalTime =
    this.withZoneSameInstant(targetZoneId).toLocalTime()

/**
  DateTimeFormatter extension functions

  - toDateTimeFormat, toTimeFormat
  - toDateTimeFormatter, toTimeFormatter
  - cleansing
**/

fun LocalDateTime.format(format: String = "yyyy-MM-dd'T'HH:mm:ss", native: Boolean = false): String =
    this.format(format.toDateTimeFormatter(native))

fun LocalDate.format(format: String = "yyyy-MM-dd", native: Boolean = false): String =
    this.format(format.toDateTimeFormatter(native))

fun LocalTime.format(format: String = "HH:mm:ss", native: Boolean = false): String =
    this.format(format.toTimeFormatter(native))

fun ZonedDateTime.format(format: String = "yyyy-MM-dd'T'HH:mm:ssXXX", native: Boolean = false): String =
    this.format(format.toDateTimeFormatter(native))

fun Date.format(format: String = "", native: Boolean = false, zoneId: ZoneId = ZoneId.systemDefault()): String =
    this.toLocalDateTime(zoneId).format(format.toDateTimeFormatter(native))

fun SqlDate.format(format: String = "", native: Boolean = false): String =
    this.toLocalDate().format(format.toDateTimeFormatter(native))

fun LocalDateTime.toString(format: String = "yyyy-MM-dd'T'HH:mm:ss"): String = this.format(format)

fun LocalDate.toString(format: String = "yyyy-MM-dd"): String = this.format(format)

fun LocalTime.toString(format: String = "HH:mm:ss"): String = this.format(format)

fun ZonedDateTime.toString(format: String = "yyyy-MM-dd'T'HH:mm:ssXXX", native: Boolean = false): String = this.format(format,native)

fun Date.toString(format: String = "", native: Boolean = false, zoneId: ZoneId = ZoneId.systemDefault()): String = this.format(format,native,zoneId)

fun SqlDate.toString(format: String = ""): String = this.format(format)