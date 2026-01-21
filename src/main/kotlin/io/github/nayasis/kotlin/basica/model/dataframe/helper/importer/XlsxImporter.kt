package io.github.nayasis.kotlin.basica.model.dataframe.helper.importer

import io.github.nayasis.kotlin.basica.core.extension.then
import io.github.nayasis.kotlin.basica.core.string.unescapeXml
import io.github.nayasis.kotlin.basica.model.dataframe.DataFrame
import io.github.nayasis.kotlin.basica.model.dataframe.helper.toDocument
import io.github.nayasis.kotlin.basica.xml.attr
import io.github.nayasis.kotlin.basica.xml.childrenByTagName
import io.github.nayasis.kotlin.basica.xml.firstOrNull
import io.github.nayasis.kotlin.basica.xml.iterator
import org.w3c.dom.Element
import java.io.InputStream
import java.nio.charset.Charset
import java.time.LocalDate
import java.util.zip.ZipInputStream
import kotlin.math.roundToLong

/**
 * XLSX importer
 */
class XlsxImporter private constructor(
    private val firstRowAsHeader: Boolean,
    private val charset: Charset,
    private val sheetIndex: Int?,
    private val sheetName: String?,
) : DataFrameImporter() {

    constructor(
        sheetIndex: Int = 0,
        firstRowAsHeader: Boolean = true,
        charset: Charset = Charsets.UTF_8,
    ) : this(
        firstRowAsHeader = firstRowAsHeader,
        charset = charset,
        sheetIndex = sheetIndex,
        sheetName = null,
    )

    constructor(
        sheetName: String,
        firstRowAsHeader: Boolean = true,
        charset: Charset = Charsets.UTF_8,
    ) : this(
        firstRowAsHeader = firstRowAsHeader,
        charset = charset,
        sheetIndex = null,
        sheetName = sheetName,
    )

    private val REGEX_EXPONENTIAL = "[eE][+-]?\\d+".toRegex()

    override fun import(inputStream: InputStream): DataFrame {
        var sharedStrings: MutableList<String>? = null
        val sheetDocs = mutableMapOf<String, Element>()
        var workbookDoc: Element? = null
        var workbookRelsDoc: Element? = null
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
                    "xl/workbook.xml" -> {
                        workbookDoc = zis.toDocument(charset)
                    }
                    "xl/_rels/workbook.xml.rels" -> {
                        workbookRelsDoc = zis.toDocument(charset)
                    }
                    else -> {}
                }
                entry?.name?.takeIf { it.startsWith("xl/worksheets/") && it.endsWith(".xml") }?.let { path ->
                    sheetDocs[path] = zis.toDocument(charset)
                }
                zis.closeEntry()
            }
        }

        val targetSheetPath = sheetName?.let { resolveSheetPathByName(workbookDoc, workbookRelsDoc, it) }
        val sheetDoc = when {
            targetSheetPath != null -> sheetDocs[targetSheetPath]
            else -> sheetDocs["xl/worksheets/sheet${(sheetIndex ?: 0) + 1}.xml"]
        }

        return sheetDoc?.let { toDataframe(it, sharedStrings ?: emptyList(), dateStyleIndexes ?: DateStyleIndexes()) } ?: DataFrame()
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
                if(firstRowAsHeader) {
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
                    dataframe.setData(firstRowAsHeader then rowIdx.minus(2) ?: rowIdx.minus(1), colIdx, value)
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
        return LocalDate.of(1899, 12, 30).plusDays(d.toLong())
    }

    private fun excelSerialToDateTime(serial: String): Any? {
        val d = serial.toDoubleOrNull() ?: return null
        // Excel datetime: integer part is days, decimal part is time
        val days     = d.toLong()
        val seconds  = ((d - days) * 24 * 60 * 60).roundToLong()
        val baseDate = LocalDate.of(1899, 12, 30).plusDays(days)
        // Excel stores datetime as local time, so we should preserve it as LocalDateTime
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

    private fun resolveSheetPathByName(
        workbookDoc: Element?,
        workbookRelsDoc: Element?,
        sheetName: String
    ): String? {
        if (workbookDoc == null) return null
        val sheet = workbookDoc.getElementsByTagName("sheet").iterator()
            .asSequence()
            .mapNotNull { it as? Element }
            .firstOrNull { it.attr("name") == sheetName } ?: return null

        val relId = sheet.attr("r:id")
        val target = relId?.let { id ->
            workbookRelsDoc?.getElementsByTagName("Relationship")?.iterator()
                ?.asSequence()
                ?.mapNotNull { it as? Element }
                ?.firstOrNull { it.attr("Id") == id }
                ?.attr("Target")
        }

        return when {
            !target.isNullOrBlank() -> normalizeSheetTargetPath(target)
            else -> sheet.attr("sheetId")?.toIntOrNull()?.let { "xl/worksheets/sheet$it.xml" }
        }
    }

    private fun normalizeSheetTargetPath(target: String): String {
        val cleaned = target.trimStart('/')
        return if (cleaned.startsWith("xl/")) cleaned else "xl/$cleaned"
    }

} 