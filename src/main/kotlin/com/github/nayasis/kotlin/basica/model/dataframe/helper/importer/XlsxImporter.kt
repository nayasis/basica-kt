package com.github.nayasis.kotlin.basica.model.dataframe.helper.importer

import com.github.nayasis.kotlin.basica.core.string.unescapeXml
import com.github.nayasis.kotlin.basica.model.dataframe.DataFrame
import com.github.nayasis.kotlin.basica.xml.XmlReader
import com.github.nayasis.kotlin.basica.xml.attr
import com.github.nayasis.kotlin.basica.xml.childrenByTagName
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.InputStream
import java.nio.charset.Charset
import java.util.zip.ZipInputStream

private const val TAG_ROW_START           = "<row"
private const val TAG_ROW_END             = "</row>"
private const val TAG_CELL_START          = "<c"
private const val TAG_CELL_END            = "</c>"
private const val TAG_VALUE_START         = "<v>"
private const val TAG_VALUE_END           = "</v>"
private const val TAG_SHARED_STRING_START = "<si>"
private const val TAG_SHARED_STRING_END   = "</si>"
private const val TAG_TEXT_START          = "<t>"
private const val TAG_TEXT_END            = "</t>"

/**
 * XLSX importer
 */
class XlsxImporter(
    private val sheetIndex: Int = 0,
    private val useHeader: Boolean = true,
    private val charset: Charset = Charsets.UTF_8,
) : DataFrameImporter() {

    override fun import(inputStream: InputStream): DataFrame {
        var sharedStrings: MutableList<String>? = null
        var firstSheet: Element? = null
        var dateStyleIndexes: Set<Int>? = null

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

        return firstSheet?.let { toDataframe(it, sharedStrings ?: emptyList(), dateStyleIndexes ?: emptySet()) } ?: DataFrame()
    }

    private fun toDataframe(
        sheet: Element,
        sharedStrings: List<String>,
        dateStyleIndexes: Set<Int>,
    ): DataFrame {
        val dataframe = DataFrame()
        val rows = sheet.childrenByTagName("row")
        var headers = ArrayList<String>()
        rows.forEachIndexed { rowIdx, row ->
            val cells  = row.childrenByTagName("c")
            val values = cells.map { cell ->
                val type  = cell.attr("t")
                val sIdx  = cell.attr("s")?.toIntOrNull()
                val vElem = cell.childrenByTagName("v").firstOrNull()
                val value = vElem?.textContent ?: ""
                when {
                    type == null -> value.toDoubleOrNull() ?: value
                    sIdx in dateStyleIndexes -> excelSerialToDate(value) ?: value
                    type == "s" -> sharedStrings.getOrNull(value.toIntOrNull() ?: -1) ?: ""
                    type == "b" -> value == "1"
                    else -> value
                }
            }
            if(rowIdx == 0) {
                values.forEachIndexed { colIdx, value ->
                    if(useHeader) {
                        headers.add("$value")
                    } else {
                        headers.add("$colIdx")
                    }
                }
            }



            if (rowIdx == 0 && useHeader) {

                headers = values.indices.map { it.toString() }
                values.forEachIndexed { col, label ->
                    dataframe.setLabel(col.toString(), label)
                }
            } else {
                if (headers == null) headers = values.indices.map { it.toString() }
                values.forEachIndexed { col, value ->
                    dataframe.setData(
                        if (useHeader) rowIdx - 1 else rowIdx,
                        headers!![col],
                        value
                    )
                }
            }
        }
        return dataframe
    }

    private fun excelSerialToDate(serial: String): Any? {
        val d = serial.toDoubleOrNull() ?: return null
        // 엑셀 기준: 1899-12-30 (1900 date system)
        return java.time.LocalDate.of(1899, 12, 30).plusDays(d.toLong())
    }

    private fun getDateStyleIndexes(doc: Element): Set<Int> {
        val numFmtIdToDate = mutableSetOf<Int>()
        val builtInDateFmtIds = setOf(14, 15, 16, 17, 18, 19, 20, 21, 22, 45, 46, 47, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 180)
        val numFmts = doc.getElementsByTagName("numFmt")
        for (i in 0 until numFmts.length) {
            val node = numFmts.item(i)
            val id = node.attributes?.getNamedItem("numFmtId")?.nodeValue?.toIntOrNull() ?: continue
            val formatCode = node.attributes?.getNamedItem("formatCode")?.nodeValue?.lowercase() ?: ""
            if (formatCode.contains("yy") || formatCode.contains("mm") || formatCode.contains("dd") || formatCode.contains("h") || formatCode.contains("s")) {
                numFmtIdToDate.add(id)
            }
        }
        val dateStyleIndexes = mutableSetOf<Int>()
        val cellXfs = doc.getElementsByTagName("xf")
        for (i in 0 until cellXfs.length) {
            val node = cellXfs.item(i)
            val numFmtId = node.attributes?.getNamedItem("numFmtId")?.nodeValue?.toIntOrNull() ?: continue
            if (numFmtId in builtInDateFmtIds || numFmtId in numFmtIdToDate) {
                dateStyleIndexes.add(i)
            }
        }
        return dateStyleIndexes
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

    private fun ZipInputStream.toDocument(charset: Charset): Element {
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
} 