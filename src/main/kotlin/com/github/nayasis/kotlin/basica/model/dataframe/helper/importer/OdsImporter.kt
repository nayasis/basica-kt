package com.github.nayasis.kotlin.basica.model.dataframe.helper.importer

import com.github.nayasis.kotlin.basica.model.dataframe.DataFrame
import com.github.nayasis.kotlin.basica.model.dataframe.helper.toDocument
import com.github.nayasis.kotlin.basica.xml.attr
import com.github.nayasis.kotlin.basica.xml.children
import com.github.nayasis.kotlin.basica.xml.toList
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
class OdsImporter(
    private val sheetIndex: Int = 0,
    private val useHeader: Boolean = true,
    private val charset: Charset = Charsets.UTF_8,
) : DataFrameImporter() {

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

        val table = doc.getElementsByTagName("table:table").toList().getOrNull(sheetIndex) ?: return dataframe
        val rows  = table.children().filter { it.nodeName == "table:table-row" }

        var headerDone = false

        var rowIdx = 0
        for (row in rows) {
            row.attr("table:number-rows-repeated")?.toIntOrNull()?.run {
                rowIdx += this
                continue
            }
            val cells = row.children().filter { it.nodeName == "table:table-cell" }.map { cell ->
                val valueType = cell.attr("office:value-type")
                val label = cell.children().firstOrNull { it.nodeName == "text:p" }?.textContent ?: ""
                val value = when (valueType) {
                    "float"   -> parseNumber(cell.attr("office:value"))
                    "date"    -> parseOdsDate(cell.attr("office:date-value"))
                    "boolean" -> cell.attr("office:boolean-value") == "true"
                    else      -> label
                }
                OdsCell(label, value)
            }
            if( ! headerDone ) {
                if(useHeader) {
                    cells.forEach { dataframe.addKey(it.label) }
                } else {
                    cells.forEachIndexed { colIdx, cell ->
                        dataframe.addKey("col$colIdx")
                        dataframe.setData(rowIdx, colIdx, cell.value)
                }
                    rowIdx++
                }
                headerDone = true
            } else {
                cells.forEachIndexed { colIdx, cell ->
                    dataframe.setData(rowIdx, colIdx, cell.value)
                }
                rowIdx++
            }
        }
        return dataframe
    }

    private data class OdsCell(val label: String, val value: Any?)

    private fun parseOdsCellValue(cell: Node, valueType: String?, label: String): Any? {
        return when (valueType) {
            "float" -> parseNumber(cell.attr("office:value"))
            "date"  -> parseOdsDate(cell.attr("office:date-value"))
            "boolean" -> cell.attr("office:boolean-value") == "true"
            else -> label
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