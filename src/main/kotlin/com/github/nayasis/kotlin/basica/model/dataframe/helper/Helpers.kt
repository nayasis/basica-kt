package com.github.nayasis.kotlin.basica.model.dataframe.helper

import com.github.nayasis.kotlin.basica.core.localdate.format
import com.github.nayasis.kotlin.basica.core.localdate.toDate
import com.github.nayasis.kotlin.basica.core.localdate.toString
import com.github.nayasis.kotlin.basica.model.dataframe.toDisplayString
import com.github.nayasis.kotlin.basica.xml.XmlReader
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.InputStream
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
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

fun ZipInputStream.toDocument(charset: Charset = Charsets.UTF_8): Element {
    return XmlReader.read(UnclosableInputStream(this), charset)
}

private class UnclosableInputStream(
    private val wrapped: InputStream
) : InputStream() {
    override fun read(): Int = wrapped.read()
    override fun read(b: ByteArray): Int = wrapped.read(b)
    override fun read(b: ByteArray, off: Int, len: Int): Int = wrapped.read(b, off, len)
    override fun close() { /* do nothing */ }
}

fun isDateObject(value: Any?): Boolean {
    return value is LocalDate || value is LocalDateTime || value is ZonedDateTime || value is Date || value is Calendar
}

fun toOdsDate(value: Any?): String? {
    return when (value) {
        is LocalDate -> value.format()
        is LocalDateTime -> value.format()
        is ZonedDateTime -> value.format()
        is Date -> value.format()
        is Calendar -> value.toDate().format()
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
            // Excel uses days, not seconds, so divide by 86400 (seconds in a day)
            val epochSecond = value.toEpochSecond(java.time.ZoneOffset.UTC)
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