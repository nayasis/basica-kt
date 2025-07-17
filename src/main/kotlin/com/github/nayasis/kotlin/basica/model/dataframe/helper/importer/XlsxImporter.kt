package com.github.nayasis.kotlin.basica.model.dataframe.helper.importer

import com.github.nayasis.kotlin.basica.core.extension.then
import com.github.nayasis.kotlin.basica.core.string.unescapeXml
import com.github.nayasis.kotlin.basica.model.dataframe.DataFrame
import com.github.nayasis.kotlin.basica.model.dataframe.helper.toDocument
import com.github.nayasis.kotlin.basica.xml.XmlReader
import com.github.nayasis.kotlin.basica.xml.attr
import com.github.nayasis.kotlin.basica.xml.childrenByTagName
import com.github.nayasis.kotlin.basica.xml.firstOrNull
import com.github.nayasis.kotlin.basica.xml.iterator
import org.w3c.dom.Element
import java.io.InputStream
import java.nio.charset.Charset
import java.util.zip.ZipInputStream

/**
 * XLSX importer
 */
class XlsxImporter(
    private val sheetIndex: Int = 0,
    private val useHeader: Boolean = true,
    private val charset: Charset = Charsets.UTF_8,
) : DataFrameImporter() {

    private val REGEX_EXPONENTIAL = "[eE][+-]?\\d+".toRegex()

    override fun import(inputStream: InputStream): DataFrame {
        var sharedStrings: MutableList<String>? = null
        var firstSheet: Element? = null
        var dateStyleIndexes: DateStyleIndexes? = null

        ZipInputStream(inputStream).use { zis ->
            var entry: java.util.zip.ZipEntry?
            while (zis.nextEntry.also { entry = it } != null) {
                when (entry?.name) {
                    "xl/styles.xml" -> {
                        dateStyleIndexes = getDateStyleIndexes(zis.toDocument(charset))
                    }
                    "xl/sharedStrings.xml" -> {
                        sharedStrings = getSharedStrings(zis.toDocument(charset))
                    }
                    "xl/worksheets/sheet${sheetIndex + 1}.xml" -> {
                        firstSheet = zis.toDocument(charset)
                    }
                    else -> {}
                }
                zis.closeEntry()
            }
        }

        return firstSheet?.let { toDataframe(it, sharedStrings ?: emptyList(), dateStyleIndexes ?: DateStyleIndexes()) } ?: DataFrame()
    }

    private fun toDataframe(
        sheet: Element,
        sharedStrings: List<String>,
        dateStyleIndexes: DateStyleIndexes,
    ): DataFrame {
        val dataframe = DataFrame()
        val rows = sheet.childrenByTagName("row")
        rows.forEachIndexed { index, row ->
            val cells  = row.childrenByTagName("c")
            val rowIdx = row.attr("r")?.toIntOrNull() ?: (index + 1)
            val values = cells.map { cell ->
                val type  = cell.attr("t")
                val sIdx  = cell.attr("s")?.toIntOrNull()
                val vElem = cell.childrenByTagName("v").firstOrNull()
                val value = vElem?.textContent ?: ""
                when {
                    sIdx in dateStyleIndexes.dateIndexes -> {
                        excelSerialToDate(value) ?: value
                    }
                    sIdx in dateStyleIndexes.dateTimeIndexes -> {
                        excelSerialToDateTime(value) ?: value
                    }
                    type == null -> parseNumber(value) ?: value
                    type == "s" -> sharedStrings.getOrNull(value.toIntOrNull() ?: -1) ?: ""
                    type == "b" -> value == "1"
                    else -> value
                }
            }
            // set header
            if(index == 0) {
                if(useHeader) {
                    values.forEachIndexed { colIdx, value ->
                        dataframe.addKey("$value")
                    }
                } else {
                    values.forEachIndexed { colIdx, value ->
                        dataframe.addKey("$colIdx")
                        dataframe.setData(rowIdx, colIdx, value)
                    }
                }
            // set body
            } else {
                values.forEachIndexed { colIdx, value ->
                    dataframe.setData(useHeader then rowIdx.minus(2) ?: rowIdx.minus(1), colIdx, value)
                }
            }
        }
        return dataframe
    }

    private fun parseRowIndex(address: String): Int {
        val rowPart = address.takeLastWhile { it.isDigit() }
        return rowPart.toIntOrNull() ?: 0
    }

    private fun excelSerialToDate(serial: String): Any? {
        val d = serial.toDoubleOrNull() ?: return null
        // Date on Excel starts from 1899-12-30 (1900 date system)
        return java.time.LocalDate.of(1899, 12, 30).plusDays(d.toLong())
    }

    private fun excelSerialToDateTime(serial: String): Any? {
        val d = serial.toDoubleOrNull() ?: return null
        // Excel datetime: integer part is days, decimal part is time
        val days = d.toLong()
        val seconds = ((d - days) * 24 * 60 * 60).toLong()
        val baseDate = java.time.LocalDate.of(1899, 12, 30).plusDays(days)
        return baseDate.atStartOfDay().plusSeconds(seconds)
    }

    private fun parseNumber(value: String): Number? {
        if (value.isEmpty()) return null
        
        // Check for exponential notation (e.g., 1.23e+10, 1.23E-5)
        if (value.contains(REGEX_EXPONENTIAL)) {
            return value.toDoubleOrNull()
        }
        
        // dot count
        return when (value.count { it == '.' }) {
            0 -> { // to int or long
                val longValue = value.toLongOrNull()
                when {
                    longValue == null -> null
                    longValue <= Int.MAX_VALUE && longValue >= Int.MIN_VALUE -> longValue.toInt()
                    else -> longValue
                }
            }
            1 -> value.toDoubleOrNull()
            // this is to handle cases like "1.2.3" which should not be parsed as a number
            else -> null
        }
    }

    private data class DateStyleIndexes(
        val dateIndexes: MutableSet<Int> = mutableSetOf(),
        val dateTimeIndexes: MutableSet<Int> = mutableSetOf(),
    )

    private fun getDateStyleIndexes(doc: Element): DateStyleIndexes {

        val numFmtIdToDate     = mutableSetOf<Int>()
        val numFmtIdToDateTime = mutableSetOf<Int>()
        val numFmtIdBuiltIn    = setOf(14, 15, 16, 17, 18, 19, 20, 21, 22, 45, 46, 47, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 180)

        doc.getElementsByTagName("numFmt").iterator().forEach { node ->
            val id         = node.attr("numFmtId")?.toIntOrNull() ?: return@forEach
            val formatCode = node.attr("formatCode")?.lowercase() ?: ""
            when {
                formatCode.contains("h") && formatCode.contains("s") -> numFmtIdToDateTime.add(id)
                formatCode.contains("yy") && formatCode.contains("mm") && formatCode.contains("dd") -> numFmtIdToDate.add(id)
            }
        }

        // find date format style in [cellXfs > xf]
        val rs = DateStyleIndexes()
        doc.getElementsByTagName("cellXfs").firstOrNull()?.let { cellXfs ->
            cellXfs.childrenByTagName("xf").forEachIndexed { index, xf ->
                val numFmtId = xf.attr("numFmtId")?.toIntOrNull() ?: 0
                when (numFmtId) {
                    in numFmtIdToDate     -> rs.dateIndexes.add(index)
                    in numFmtIdToDateTime -> rs.dateTimeIndexes.add(index)
                    in numFmtIdBuiltIn    -> rs.dateTimeIndexes.add(index)
                }
            }
        }
        
        return rs
    }

    private fun getSharedStrings(doc: Element): MutableList<String> {
        val sharedStrings = mutableListOf<String>()
        doc.getElementsByTagName("si").let { nodes ->
            for (i in 0 until nodes.length) {
                val siNode = nodes.item(i)
                siNode.childrenByTagName("t").firstOrNull()?.let {
                    sharedStrings.add(it.textContent.unescapeXml())
                }
            }
        }
        return sharedStrings
    }

} 