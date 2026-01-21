package io.github.nayasis.kotlin.basica.model.dataframe.helper.exporter

import io.github.nayasis.kotlin.basica.model.dataframe.DataFrame
import io.github.nayasis.kotlin.basica.model.dataframe.helper.isDateObject
import io.github.nayasis.kotlin.basica.model.dataframe.helper.toExcelDate
import io.github.nayasis.kotlin.basica.model.dataframe.helper.write
import io.github.nayasis.kotlin.basica.model.dataframe.helper.writeEntry
import io.github.nayasis.kotlin.basica.xml.appendElement
import io.github.nayasis.kotlin.basica.xml.appendTo
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.OutputStream
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * XLSX exporter for multiple sheets
 */
class XlsxMultiExporter(
    sheets: Map<String, DataFrame>,
    private val showLabel: Boolean = true,
    private val startIndex: Int? = null,
) : DataFrameExporter() {

    private val sheetDefs = sheets.entries.mapIndexed { index, entry ->
        SheetDef(
            name = entry.key,
            dataframe = entry.value,
            sheetIndex = index,
            first = startIndex?.takeIf { it >= 0 } ?: entry.value.firstIndex ?: 0,
            last = entry.value.lastIndex ?: -1,
        )
    }

    private val stringIndexMap = buildSharedStrings()

    override fun export(outputStream: OutputStream) {
        ZipOutputStream(outputStream).use { zos ->
            writeContentTypes(zos)
            writeRels(zos)
            writeWorkbook(zos)
            writeWorkbookRels(zos)
            writeSharedStrings(zos)
            writeStyles(zos)
            sheetDefs.forEach { sheet ->
                writeSheet(zos, sheet)
            }
        }
    }

    private fun buildSharedStrings(): Map<String, Int> {
        val uniqueStrings = mutableSetOf<String>()
        sheetDefs.forEach { sheet ->
            val dataframe = sheet.dataframe
            // read headers
            for (key in dataframe.keys) {
                val label = if (showLabel) dataframe.getLabel(key) else key
                uniqueStrings.add(label)
            }
            // read data
            for (r in sheet.first..sheet.last) {
                if (dataframe.isRowEmpty(r)) continue
                for (key in dataframe.keys) {
                    dataframe.getData(r, key)
                        .takeIf { it != null && it !is Number && !isDateObject(it) }
                        ?.let { value -> uniqueStrings.add(value.toString()) }
                }
            }
        }
        return uniqueStrings.mapIndexed { index, value -> value to index }.toMap()
    }

    private fun writeContentTypes(zos: ZipOutputStream) {
        val sheetOverrides = sheetDefs.joinToString("\n") { sheet ->
            "  <Override PartName=\"/xl/worksheets/sheet${sheet.sheetIndex + 1}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
        }
        zos.writeEntry("[Content_Types].xml", """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
              <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
              <Default Extension="xml" ContentType="application/xml"/>
              <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
${sheetOverrides}
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
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
        val workbook = doc.appendElement("workbook").apply {
            setAttribute("xmlns", "http://schemas.openxmlformats.org/spreadsheetml/2006/main")
            setAttribute("xmlns:r", "http://schemas.openxmlformats.org/officeDocument/2006/relationships")
        }
        val sheets = workbook.appendElement("sheets")
        sheetDefs.forEach { sheet ->
            sheets.appendElement("sheet").apply {
                setAttribute("name", sheet.name)
                setAttribute("sheetId", (sheet.sheetIndex + 1).toString())
                setAttribute("r:id", "rId${sheet.sheetIndex + 1}")
            }
        }
        zos.writeEntry("xl/workbook.xml", doc)
    }

    private fun writeWorkbookRels(zos: ZipOutputStream) {
        val sheetRels = sheetDefs.joinToString("\n") { sheet ->
            "  <Relationship Id=\"rId${sheet.sheetIndex + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet${sheet.sheetIndex + 1}.xml\"/>"
        }
        val sharedStringsId = sheetDefs.size + 1
        val stylesId = sheetDefs.size + 2
        zos.writeEntry("xl/_rels/workbook.xml.rels", """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
${sheetRels}
              <Relationship Id="rId${sharedStringsId}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
              <Relationship Id="rId${stylesId}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
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
        stringIndexMap.forEach { (value, _) ->
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

    private fun writeSheet(zos: ZipOutputStream, sheet: SheetDef) {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
        val worksheet = doc.appendElement("worksheet").apply {
            setAttribute("xmlns", "http://schemas.openxmlformats.org/spreadsheetml/2006/main")
            setAttribute("xmlns:r", "http://schemas.openxmlformats.org/officeDocument/2006/relationships")
        }
        val sheetData = worksheet.appendElement("sheetData")
        val headerRow = sheetData.appendElement("row").apply {
            setAttribute("r", "1")
        }
        sheet.dataframe.keys.forEachIndexed { col, key ->
            val label = if (showLabel) sheet.dataframe.getLabel(key) else key
            doc.createCell(label, toCellAddress(1, col)).appendTo(headerRow)
        }
        for (row in sheet.first..sheet.last) {
            if (sheet.dataframe.isRowEmpty(row)) continue
            val dataRow = sheetData.appendElement("row").apply {
                setAttribute("r", (row + 2).toString())
            }
            sheet.dataframe.keys.forEachIndexed { col, key ->
                doc.createCell(
                    sheet.dataframe.getData(row, key),
                    toCellAddress(row + 2, col),
                ).appendTo(dataRow)
            }
        }
        zos.putNextEntry(ZipEntry("xl/worksheets/sheet${sheet.sheetIndex + 1}.xml"))
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
        return "$colRef${row}"
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
                            is LocalDate -> "1"
                            else -> "2"
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

    private data class SheetDef(
        val name: String,
        val dataframe: DataFrame,
        val sheetIndex: Int,
        val first: Int,
        val last: Int,
    )

}
