package com.github.nayasis.kotlin.basica.model.dataframe.helper.exporter

import com.github.nayasis.kotlin.basica.model.dataframe.DataFrame
import com.github.nayasis.kotlin.basica.model.dataframe.helper.toExcelDate
import com.github.nayasis.kotlin.basica.model.dataframe.helper.isDateObject
import com.github.nayasis.kotlin.basica.model.dataframe.helper.write
import com.github.nayasis.kotlin.basica.model.dataframe.helper.writeEntry
import com.github.nayasis.kotlin.basica.xml.appendElement
import com.github.nayasis.kotlin.basica.xml.appendTo
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.OutputStream
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * XLSX exporter
 */
class XlsxExporter(
    private val dataframe: DataFrame,
    private val sheetName: String = "Sheet1",
    private val showLabel: Boolean = true,
    startIndex: Int? = null,
) : DataFrameExporter() {

    private val first: Int = startIndex?.takeIf { it >= 0 } ?: dataframe.firstIndex ?: 0
    private val last: Int  = dataframe.lastIndex ?: -1

    private val stringIndexMap = buildSharedStrings()

    override fun export(outputStream: OutputStream) {
        ZipOutputStream(outputStream).use { zos ->
            writeContentTypes(zos)
            writeRels(zos)
            writeWorkbook(zos)
            writeWorkbookRels(zos)
            writeSharedStrings(zos)
            writeStyles(zos)
            writeSheet(zos)
        }
    }

    private fun buildSharedStrings(): Map<String, Int> {
        val uniqueStrings = mutableSetOf<String>()
        // read headers
        for (key in dataframe.keys) {
            val label = if (showLabel) dataframe.getLabel(key) else key
            uniqueStrings.add(label)
        }
        // read data
        for (r in first..last) {
            for (key in dataframe.keys) {
                dataframe.getData(r, key).takeIf { it != null && it !is Number && !isDateObject(it) }?.let { value ->
                    uniqueStrings.add(value.toString())
                }
            }
        }
        return uniqueStrings.mapIndexed { index, value -> value to index }.toMap()
    }

    private fun writeContentTypes(zos: ZipOutputStream) {
        // TODO: Check content_Types.xml for correctness
        zos.writeEntry("[Content_Types].xml", """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
              <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
              <Default Extension="xml" ContentType="application/xml"/>
              <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
              <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
              <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
              <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
            </Types>
        """.trimIndent())
    }

    private fun writeRels(zos: ZipOutputStream) {
        zos.writeEntry("_rels/.rels", """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
            </Relationships>
        """.trimIndent())
    }

    private fun writeWorkbook(zos: ZipOutputStream) {
        zos.writeEntry("xl/workbook.xml", """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
              <sheets>
                <sheet name="${sheetName}" sheetId="1" r:id="rId1"/>
              </sheets>
            </workbook>
        """.trimIndent())
    }

    private fun writeWorkbookRels(zos: ZipOutputStream) {
        zos.writeEntry("xl/_rels/workbook.xml.rels", """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
              <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
              <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
            </Relationships>
        """.trimIndent())
    }

    private fun writeSharedStrings(zos: ZipOutputStream) {

        zos.putNextEntry(ZipEntry("xl/sharedStrings.xml"))

        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
        val sst = doc.appendElement("sst").apply {
            setAttribute("xmlns", "http://schemas.openxmlformats.org/spreadsheetml/2006/main")
            setAttribute("count", stringIndexMap.size.toString())
            setAttribute("uniqueCount", stringIndexMap.size.toString())
        }

        stringIndexMap.forEach {(value, index) ->
            sst.appendElement("si").appendElement("t").textContent = value
        }

        zos.write(doc)
        zos.closeEntry()
    }

    private fun writeStyles(zos: ZipOutputStream) {
        zos.writeEntry("xl/styles.xml", """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <numFmts count="2">
                <numFmt numFmtId="14" formatCode="yyyy-mm-dd"/>
                <numFmt numFmtId="15" formatCode="yyyy-mm-dd hh:mm:ss"/>
              </numFmts>
              <fonts count="1">
                <font>
                  <sz val="11"/>
                  <name val="Calibri"/>
                </font>
              </fonts>
              <fills count="1">
                <fill>
                  <patternFill patternType="none"/>
                </fill>
              </fills>
              <borders count="1">
                <border>
                  <left/>
                  <right/>
                  <top/>
                  <bottom/>
                  <diagonal/>
                </border>
              </borders>
              <cellStyleXfs count="1">
                <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
              </cellStyleXfs>
              <cellXfs count="3">
                <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
                <xf numFmtId="14" fontId="0" fillId="0" borderId="0" xfId="0"/>
                <xf numFmtId="15" fontId="0" fillId="0" borderId="0" xfId="0"/>
              </cellXfs>
            </styleSheet>
        """.trimIndent())
    }

    private fun writeSheet(zos: ZipOutputStream) {

        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()

        val worksheet = doc.appendElement("worksheet").apply {
            setAttribute("xmlns", "http://schemas.openxmlformats.org/spreadsheetml/2006/main")
            setAttribute("xmlns:r", "http://schemas.openxmlformats.org/officeDocument/2006/relationships")
        }

        val sheetData = worksheet.appendElement("sheetData")

        val headerRow = sheetData.appendElement("row").apply {
            setAttribute("r", "1")
        }

        dataframe.keys.forEachIndexed { col, key ->
            val label = if (showLabel) dataframe.getLabel(key) else key
            doc.createCell(label, toCellAddress(1, col) ).appendTo(headerRow)
        }

        for (row in first.. last) {
            val dataRow = sheetData.appendElement("row").apply {
                setAttribute("r", (row + 2).toString())
            }
            dataframe.keys.forEachIndexed { col, key ->
                doc.createCell(
                    dataframe.getData(row, key),
                    toCellAddress(row + 2, col),
                ).appendTo(dataRow)
            }
        }

        zos.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
        zos.write(doc)
        zos.closeEntry()
    }

    private fun toCellAddress(row: Int, col: Int): String {
        val colRef = buildString {
            var colNum = col
            while (colNum >= 0) {
                insert(0, ('A' + colNum % 26))
                colNum = colNum / 26 - 1
            }
        }
        return "$colRef${row}" // row is 1-based in Excel
    }

    private fun Document.createCell(value: Any?, cellRef: String): Element {
        return this.createElement("c").apply {
            setAttribute("r", cellRef)
            when {
                value == null -> {}
                value is Number -> {
                    appendElement("v").textContent = value.toString()
                }
                isDateObject(value) -> {
                    val excelDate = toExcelDate(value)
                    if (excelDate != null) {
                        setAttribute("s", when (value) {
                            is LocalDate -> "1" // date
                            else -> "2"         // datetime
                        })
                        appendElement("v").textContent = excelDate.toString()
                    } else {
                        setAttribute("t", "s")
                        appendElement("v").textContent = stringIndexMap[value.toString()]?.toString() ?: "0"
                    }
                }
                else -> {
                    setAttribute("t", "s")
                    appendElement("v").textContent = stringIndexMap.get(value.toString())?.toString() ?: "0"
                }
            }
        }
    }

}