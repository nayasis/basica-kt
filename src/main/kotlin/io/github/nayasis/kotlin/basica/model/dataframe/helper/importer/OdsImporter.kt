package io.github.nayasis.kotlin.basica.model.dataframe.helper.importer

import io.github.nayasis.kotlin.basica.model.dataframe.DataFrame
import io.github.nayasis.kotlin.basica.model.dataframe.helper.toDocument
import io.github.nayasis.kotlin.basica.xml.attr
import io.github.nayasis.kotlin.basica.xml.children
import io.github.nayasis.kotlin.basica.xml.toList
import io.github.oshai.kotlinlogging.KotlinLogging
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.InputStream
import java.nio.charset.Charset
import java.util.zip.ZipInputStream

private val logger = KotlinLogging.logger {}

/**
 * ODS importer
 */
class OdsImporter private constructor(
    private val firstRowAsHeader: Boolean,
    private val charset: Charset,
    private val sheetIndex: Int?,
    private val sheetName: String?,
    private val lastColumnIndex: Int,
) : DataFrameImporter() {

    constructor(
        sheetIndex: Int = 0,
        firstRowAsHeader: Boolean = true,
        lastColumnIndex: Int = -1,
        charset: Charset = Charsets.UTF_8,
    ) : this(
        firstRowAsHeader = firstRowAsHeader,
        charset = charset,
        sheetIndex = sheetIndex,
        sheetName = null,
        lastColumnIndex = lastColumnIndex,
    )

    constructor(
        sheetName: String,
        firstRowAsHeader: Boolean = true,
        lastColumnIndex: Int = -1,
        charset: Charset = Charsets.UTF_8,
    ) : this(
        firstRowAsHeader = firstRowAsHeader,
        charset = charset,
        sheetIndex = null,
        sheetName = sheetName,
        lastColumnIndex = lastColumnIndex,
    )

    override fun import(inputStream: InputStream): DataFrame {
        var contentDoc: Element? = null
        ZipInputStream(inputStream).use { zis ->
            var entry: java.util.zip.ZipEntry?
            while (zis.nextEntry.also { entry = it } != null) {
                if (entry?.name == "content.xml") {
                    contentDoc = zis.toDocument(charset)
                }
                zis.closeEntry()
            }
        }
        return contentDoc?.let { toDataframe(it) } ?: DataFrame()
    }

    private fun toDataframe(doc: Element): DataFrame {
        val dataframe = DataFrame()
        val tables = doc.getElementsByTagName("table:table").toList()
        val table = when {
            sheetName != null -> tables.firstOrNull { it.attr("table:name") == sheetName }
            else -> tables.getOrNull(sheetIndex ?: 0)
        } ?: return dataframe
        val rows  = table.children().filter { it.nodeName == "table:table-row" }
        if (rows.isEmpty()) return dataframe

        fun parseRowValues(row: Node): MutableMap<Int, Any?> {
            val values = mutableMapOf<Int, Any?>()
            var colIdx = 0
            row.children().filter { it.nodeName == "table:table-cell" }.forEach { cell ->
                val repeat = cell.attr("table:number-columns-repeated")?.toIntOrNull() ?: 1
                val valueType = cell.attr("office:value-type")
                val label = cell.children().firstOrNull { it.nodeName == "text:p" }?.textContent ?: ""
                val value = parseOdsCellValue(cell, valueType, label)
                if (value != null || !valueType.isNullOrBlank() || label.isNotBlank()) {
                    for (i in 0 until repeat) {
                        values[colIdx + i] = value
                    }
                }
                colIdx += repeat
            }
            return values
        }

        val rowCache = mutableMapOf<Int, MutableMap<Int, Any?>>()
        val firstRowValues = parseRowValues(rows.first())
        var appliedLastColumnIndex = when {
            lastColumnIndex < 0 -> (firstRowValues.keys.maxOrNull() ?: -1)
            firstRowAsHeader -> minOf(firstRowValues.keys.maxOrNull() ?: -1, lastColumnIndex)
            else -> lastColumnIndex
        }

        if (appliedLastColumnIndex >= 0) {
            for (col in 0..appliedLastColumnIndex) {
                val key = if (firstRowAsHeader) {
                    (firstRowValues[col]?.toString() ?: "$col")
                } else {
                    "$col"
                }
                if (!dataframe.keys.contains(key)) {
                    dataframe.addKey(key)
                } else {
                    dataframe.addKey("${key}_$col")
                }
            }
        }

        var rowIdx = 0
        rows.forEachIndexed { index, row ->
            val repeatRows = row.attr("table:number-rows-repeated")?.toIntOrNull() ?: 1
            if (index == 0 && firstRowAsHeader) {
                return@forEachIndexed
            }

            val rowValues = parseRowValues(row)
            val repeatCount = if (rowValues.isEmpty() && repeatRows > 1000) 1 else repeatRows

            repeat(repeatCount) {
                rowCache[rowIdx] = rowValues
                if (lastColumnIndex < 0) {
                    val maxIndexInRow = rowValues.keys.maxOrNull() ?: -1
                    while (maxIndexInRow > appliedLastColumnIndex) {
                        appliedLastColumnIndex += 1
                        dataframe.addKey("$appliedLastColumnIndex")
                        for (r in 0..rowIdx) {
                            dataframe.setData(r, appliedLastColumnIndex, rowCache[r]?.get(appliedLastColumnIndex))
                        }
                    }
                }
                rowValues.forEach { (colIdx, value) ->
                    if (colIdx <= appliedLastColumnIndex) {
                        dataframe.setData(rowIdx, colIdx, value)
                    }
                }
                rowIdx++
            }
        }
        return dataframe
    }

    private fun parseOdsCellValue(cell: Node, valueType: String?, label: String): Any? {
        return when (valueType) {
            "float" -> parseNumber(cell.attr("office:value"))
            "date"  -> parseOdsDate(cell.attr("office:date-value"))
            "boolean" -> cell.attr("office:boolean-value") == "true"
            else -> label.ifEmpty { null }
        }
    }

    private fun parseOdsDate(str: String?): Any? {
        if (str.isNullOrBlank()) return null
        return try {
            when {
                str.length == 10 -> java.time.LocalDate.parse(str)
                str.contains('+') -> java.time.ZonedDateTime.parse(str)
                else -> java.time.LocalDateTime.parse(str)
            }
        } catch (e: Exception) {
            logger.error(e) { "Error parsing OdsDate" }
            str
        }
    }

    private fun parseNumber(value: String?): Number? {
        if (value.isNullOrBlank()) return null
        return value.toDoubleOrNull()?.let {
            if (it % 1 == 0.0) {
                if (it <= Int.MAX_VALUE && it >= Int.MIN_VALUE) it.toInt() else it.toLong()
            } else it
        }
    }

} 
