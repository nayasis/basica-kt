package com.github.nayasis.kotlin.basica.model.dataframe.helper

import com.github.nayasis.kotlin.basica.core.localdate.toString
import com.github.nayasis.kotlin.basica.model.dataframe.toDisplayString
import org.w3c.dom.Document
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

fun ZipOutputStream.write(doc: Document) {
    TransformerFactory.newInstance().newTransformer().apply {
        setOutputProperty(OutputKeys.INDENT, "yes")
        setOutputProperty(OutputKeys.ENCODING, "UTF-8")
        setOutputProperty(OutputKeys.STANDALONE, "yes")
    }.transform(DOMSource(doc), StreamResult(this))
}

fun ZipOutputStream.writeEntry(name: String, content: String) {
    putNextEntry(ZipEntry(name))
    write(content.toByteArray())
    closeEntry()
}

fun ZipOutputStream.writeEntry(entry: ZipEntry, content: String) {
    putNextEntry(entry)
    write(content.toByteArray())
    closeEntry()
}

fun ZipOutputStream.writeEntry(name: String, content: Document) {
    putNextEntry(ZipEntry(name))
    write(content)
    closeEntry()
}

fun isDateObject(value: Any?): Boolean {
    return value is LocalDate || value is LocalDateTime || value is ZonedDateTime || value is Date || value is Calendar
}

fun toOdsDate(value: Any?): String? {
    return when (value) {
        is LocalDate -> {
            // ODS date format: YYYY-MM-DD
            value.toString()
        }
        is LocalDateTime -> {
            // ODS datetime format: YYYY-MM-DDTHH:MM:SS
            value.toString()
        }
        is ZonedDateTime -> {
            // ODS datetime format: YYYY-MM-DDTHH:MM:SS+HH:MM
            value.toDisplayString()
        }
        is Date -> {
            value.toString()
            // Convert Date to ISO format
            val calendar = Calendar.getInstance()
            calendar.time = value
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val second = calendar.get(Calendar.SECOND)
            
            if (hour == 0 && minute == 0 && second == 0) {
                // Date only
                String.format("%04d-%02d-%02d", year, month, day)
            } else {
                // Date and time
                String.format("%04d-%02d-%02dT%02d:%02d:%02d", year, month, day, hour, minute, second)
            }
        }
        is Calendar -> {
            // Convert Calendar to ISO format
            val year = value.get(Calendar.YEAR)
            val month = value.get(Calendar.MONTH) + 1
            val day = value.get(Calendar.DAY_OF_MONTH)
            val hour = value.get(Calendar.HOUR_OF_DAY)
            val minute = value.get(Calendar.MINUTE)
            val second = value.get(Calendar.SECOND)
            
            if (hour == 0 && minute == 0 && second == 0) {
                // Date only
                String.format("%04d-%02d-%02d", year, month, day)
            } else {
                // Date and time
                String.format("%04d-%02d-%02dT%02d:%02d:%02d", year, month, day, hour, minute, second)
            }
        }
        else -> null
    }
}

fun toExcelDate(value: Any?): Double? {
    return when (value) {
        is LocalDate -> {
            // Excel date is the number of days since 1990-01-01
            val epochDay = value.toEpochDay()
            // Excel counts 1990-01-01 as 1, so add 25569
            epochDay + 25569.0
        }
        is LocalDateTime -> {
            val epochSecond = value.toEpochSecond(java.time.ZoneOffset.UTC)
            // Excel uses days, not seconds, so divide by 86400 (seconds in a day)
            epochSecond / 86400.0 + 25569.0
        }
        is ZonedDateTime -> {
            val epochSecond = value.toEpochSecond()
            epochSecond / 86400.0 + 25569.0
        }
        is Date -> {
            // Convert Date to milliseconds and calculate Excel date
            value.time / (1000.0 * 86400.0) + 25569.0
        }
        is Calendar -> {
            // Convert Calendar to milliseconds and calculate Excel date
            value.timeInMillis / (1000.0 * 86400.0) + 25569.0
        }
        else -> null
    }
}