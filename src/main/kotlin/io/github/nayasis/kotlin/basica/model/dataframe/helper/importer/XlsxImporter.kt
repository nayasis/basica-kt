package io.github.nayasis.kotlin.basica.model.dataframe.helper.importer

import io.github.nayasis.kotlin.basica.core.string.unescapeXml
import io.github.nayasis.kotlin.basica.model.dataframe.DataFrame
import io.github.nayasis.kotlin.basica.model.dataframe.helper.toDocument
import io.github.nayasis.kotlin.basica.xml.attr
import io.github.nayasis.kotlin.basica.xml.childrenByTagName
import io.github.nayasis.kotlin.basica.xml.firstOrNull
import io.github.nayasis.kotlin.basica.xml.iterator
import org.w3c.dom.Element
import org.w3c.dom.Node
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
        val rowCache = mutableMapOf<Int, MutableMap<Int, Any?>>()
        var appliedLastColumnIndex = -1

        fun parseRowValues(row: Node): MutableMap<Int, Any?> {
            val result = mutableMapOf<Int, Any?>()
            var nextSequentialCol = 0
            row.childrenByTagName("c").forEach { cell ->
                val colIdx = cell.attr("r")?.let { parseColumnIndex(it) }?.takeIf { it >= 0 } ?: nextSequentialCol
                nextSequentialCol = colIdx + 1
                val type = cell.attr("t")
                val sIdx = cell.attr("s")?.toIntOrNull()
                val vElem = cell.childrenByTagName("v").firstOrNull()
                val raw = vElem?.textContent ?: ""
                val value = when {
                    vElem == null -> null
                    sIdx in dateStyleIndexes.dateIndexes -> excelSerialToDate(raw) ?: raw
                    sIdx in dateStyleIndexes.dateTimeIndexes -> excelSerialToDateTime(raw) ?: raw
                    type == null -> parseNumber(raw) ?: raw
                    type == "s" -> sharedStrings.getOrNull(raw.toIntOrNull() ?: -1) ?: ""
                    type == "b" -> raw == "1"
                    else -> raw
                }
                result[colIdx] = value
            }
            return result
        }

        fun uniqueKey(base: String): String {
            if (!dataframe.keys.contains(base)) return base
            var i = 1
            while (dataframe.keys.contains("${base}_$i")) i++
            return "${base}_$i"
        }

        if (rows.isNotEmpty()) {
            val firstRowValues = parseRowValues(rows.first())
            val firstRowMaxColumnIndex = firstRowValues.keys.maxOrNull() ?: -1
            appliedLastColumnIndex = when {
                lastColumnIndex < 0 -> firstRowMaxColumnIndex
                firstRowAsHeader -> minOf(firstRowMaxColumnIndex, lastColumnIndex)
                else -> lastColumnIndex
            }
            if (appliedLastColumnIndex >= 0) {
                for (col in 0..appliedLastColumnIndex) {
                    val key = if (firstRowAsHeader) {
                        uniqueKey((firstRowValues[col]?.toString() ?: "$col"))
                    } else {
                        "$col"
                    }
                    dataframe.addKey(key)
                }
            }
        }

        val firstPhysicalRowIndex = rows.firstOrNull()?.attr("r")?.toIntOrNull() ?: 1

        rows.forEachIndexed { index, row ->
            val rowIdx = row.attr("r")?.toIntOrNull() ?: (index + 1)
            if (index == 0 && firstRowAsHeader) return@forEachIndexed

            val dataRow = when {
                firstRowAsHeader -> rowIdx - firstPhysicalRowIndex - 1
                else             -> rowIdx - firstPhysicalRowIndex
            }.takeIf { it >= 0 } ?: return@forEachIndexed

            val rowValues = parseRowValues(row)
            rowCache[dataRow] = rowValues

            if (lastColumnIndex < 0) {
                val maxIndexInRow = rowValues.keys.maxOrNull() ?: -1
                while (maxIndexInRow > appliedLastColumnIndex) {
                    appliedLastColumnIndex += 1
                    dataframe.addKey(uniqueKey("$appliedLastColumnIndex"))
                    for (r in 0..dataRow) {
                        dataframe.setData(r, appliedLastColumnIndex, rowCache[r]?.get(appliedLastColumnIndex))
                    }
                }
            }

            rowValues.forEach { (colIdx, value) ->
                if (colIdx <= appliedLastColumnIndex) {
                    dataframe.setData(dataRow, colIdx, value)
                }
            }
        }

        return dataframe
    }

    private fun parseRowIndex(address: String): Int {
        val rowPart = address.takeLastWhile { it.isDigit() }
        return rowPart.toIntOrNull() ?: 0
    }

    private fun parseColumnIndex(address: String): Int {
        val colPart = address.takeWhile { it.isLetter() }.uppercase()
        if (colPart.isEmpty()) return -1
        var index = 0
        for (ch in colPart) {
            index = index * 26 + (ch - 'A' + 1)
        }
        return index - 1
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
                val value = siNode.childrenByTagName("t")
                    .joinToString("") { it.textContent }
                    .unescapeXml()
                sharedStrings.add(value)
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
