package io.github.nayasis.kotlin.basica.model.dataframe.helper.exporter

import io.github.nayasis.kotlin.basica.core.string.getCrc32
import io.github.nayasis.kotlin.basica.model.dataframe.DataFrame
import io.github.nayasis.kotlin.basica.model.dataframe.helper.isDateObject
import io.github.nayasis.kotlin.basica.model.dataframe.helper.toOdsDate
import io.github.nayasis.kotlin.basica.model.dataframe.helper.writeEntry
import io.github.nayasis.kotlin.basica.xml.appendElement
import org.w3c.dom.Element
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory


/**
 * ODS exporter
 */
class OdsExporter(
    private val dataframe: DataFrame,
    private val sheetName: String = "Sheet1",
    private val showLabel: Boolean = true,
    startIndex: Int? = null,
): DataFrameExporter() {

    private val first: Int = startIndex?.takeIf { it >= 0 } ?: 0
    private val last: Int  = dataframe.lastIndex ?: -1

    override fun export(outputStream: OutputStream) {
        ZipOutputStream(outputStream).use { zos ->
            writeMimeType(zos)
            writeManifest(zos)
            writeMeta(zos)
            writeSettings(zos)
            writeStyles(zos)
            writeContent(zos)
        }
    }

    private fun writeContent(zos: ZipOutputStream) {

        val doc  = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
        val root = doc.appendElement("office:document-content").apply {
            setAttribute("xmlns:office", "urn:oasis:names:tc:opendocument:xmlns:office:1.0")
            setAttribute("xmlns:table", "urn:oasis:names:tc:opendocument:xmlns:table:1.0")
            setAttribute("xmlns:text", "urn:oasis:names:tc:opendocument:xmlns:text:1.0")
            setAttribute("xmlns:style", "urn:oasis:names:tc:opendocument:xmlns:style:1.0")
            setAttribute("xmlns:number", "urn:oasis:names:tc:opendocument:xmlns:datastyle:1.0")
            setAttribute("office:version", "1.2")
            // 날짜 스타일 추가
            appendElement("office:automatic-styles").apply {
                appendElement("number:date-style").apply {
                    setAttribute("style:name", "styleDate")
                    appendElement("number:year").setAttribute("number:style", "long")
                    appendElement("number:text").textContent = "-"
                    appendElement("number:month").setAttribute("number:style", "long")
                    appendElement("number:text").textContent = "-"
                    appendElement("number:day").setAttribute("number:style", "long")
                }
                appendElement("number:date-style").apply {
                    setAttribute("style:name", "styleDatetime")
                    appendElement("number:year").setAttribute("number:style", "long")
                    appendElement("number:text").textContent = "-"
                    appendElement("number:month").setAttribute("number:style", "long")
                    appendElement("number:text").textContent = "-"
                    appendElement("number:day").setAttribute("number:style", "long")
                    appendElement("number:text").textContent = " "
                    appendElement("number:hours").setAttribute("number:style", "long")
                    appendElement("number:text").textContent = ":"
                    appendElement("number:minutes").setAttribute("number:style", "long")
                    appendElement("number:text").textContent = ":"
                    appendElement("number:seconds").setAttribute("number:style", "long")
                }
                appendElement("style:style").apply {
                    setAttribute("style:name", "cellstyleDate")
                    setAttribute("style:family", "table-cell")
                    setAttribute("style:parent-style-name", "Default")
                    setAttribute("style:data-style-name", "styleDate")
                }
                appendElement("style:style").apply {
                    setAttribute("style:name", "cellstyleDatetime")
                    setAttribute("style:family", "table-cell")
                    setAttribute("style:parent-style-name", "Default")
                    setAttribute("style:data-style-name", "styleDatetime")
                }
            }
        }
        val body   = root.appendElement("office:body")
        val spread = body.appendElement("office:spreadsheet")
        val sheet  = spread.appendElement("table:table").apply {
            setAttribute("table:name", sheetName)
        }
        val header = sheet.appendElement("table:table-row")

        for(label in if(showLabel) dataframe.labels else dataframe.keys) {
            header.appendCell(label)
        }

        var emptyRowCount = 0
        for (i in first..last) {
            val isEmptyRow = dataframe.isRowEmpty(i)
            if (isEmptyRow) {
                emptyRowCount++
            } else {
                if (emptyRowCount > 0) {
                    // print empty rows
                    sheet.appendElement("table:table-row").apply {
                        setAttribute("table:number-rows-repeated", emptyRowCount.toString())
                        appendElement("table:table-cell").apply {
                            setAttribute("table:number-columns-repeated", dataframe.keys.size.toString())
                        }
                    }
                    emptyRowCount = 0
                }
                // print row
                val row = sheet.appendElement("table:table-row")
                for (key in dataframe.keys) {
                    row.appendCell(dataframe.getData(i, key))
                }
            }
        }
        // print empty rows remained
        if (emptyRowCount > 0) {
            sheet.appendElement("table:table-row").apply {
                setAttribute("table:number-rows-repeated", emptyRowCount.toString())
                appendElement("table:table-cell").apply {
                    setAttribute("table:number-columns-repeated", dataframe.keys.size.toString())
                }
            }
        }

        zos.writeEntry("content.xml",doc)
    }

    private fun Element.appendCell(value: Any?): Element {
        return this.appendElement("table:table-cell").apply {
            when {
                value == null -> {
                    setAttribute("office:value-type", "string")
                }
                value is Number -> {
                    setAttribute("office:value-type", "float")
                    setAttribute("office:value", value.toString())
                }
                isDateObject(value) -> {
                    val odsDate = toOdsDate(value)
                    if (odsDate != null) {
                        setAttribute("office:value-type", "date")
                        setAttribute("office:date-value", odsDate)
                        when (value) {
                            is LocalDateTime, is Date, is ZonedDateTime
                                 -> setAttribute("table:style-name", "cellstyleDatetime")
                            else -> setAttribute("table:style-name", "cellstyleDate")
                        }
                    } else {
                        setAttribute("office:value-type", "string")
                    }
                }
                else -> {
                    setAttribute("office:value-type", "string")
                    value.toString().takeIf { it.isNotEmpty() }?.let { text ->
                        appendElement("text:p").textContent = text
                    }
                }
            }
        }
    }

    private fun writeMimeType(zos: ZipOutputStream) {
        val mimeType = "application/vnd.oasis.opendocument.spreadsheet"
        zos.writeEntry(ZipEntry("mimetype").apply {
            setMethod(ZipEntry.STORED)
            setSize(mimeType.length.toLong())
            setCrc(mimeType.getCrc32())
        }, mimeType)
    }

    private fun writeManifest(zos: ZipOutputStream) {
        zos.writeEntry("META-INF/manifest.xml", """
            <?xml version="1.0" encoding="UTF-8"?>
            <manifest:manifest xmlns:manifest="urn:oasis:names:tc:opendocument:xmlns:manifest:1.0" manifest:version="1.2">
              <manifest:file-entry manifest:full-path="/" manifest:version="1.2" manifest:media-type="application/vnd.oasis.opendocument.spreadsheet"/>
              <manifest:file-entry manifest:full-path="content.xml" manifest:media-type="text/xml"/>
              <manifest:file-entry manifest:full-path="styles.xml" manifest:media-type="text/xml"/>
              <manifest:file-entry manifest:full-path="meta.xml" manifest:media-type="text/xml"/>
              <manifest:file-entry manifest:full-path="settings.xml" manifest:media-type="text/xml"/>
            </manifest:manifest>
        """.trimIndent())
    }

    private fun writeMeta(zos: ZipOutputStream) {
        zos.writeEntry("meta.xml", """
            <?xml version="1.0" encoding="UTF-8"?>
            <office:document-meta xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:meta="urn:oasis:names:tc:opendocument:xmlns:meta:1.0" xmlns:dc="http://purl.org/dc/elements/1.1/" office:version="1.2">
              <office:meta>
                <meta:generator>basica</meta:generator>
                <dc:title>Basica Export</dc:title>
              </office:meta>
            </office:document-meta>
        """.trimIndent())
    }

    private fun writeSettings(zos: ZipOutputStream) {
        zos.writeEntry("settings.xml", """
            <?xml version="1.0" encoding="UTF-8"?>
            <office:document-settings xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:config="urn:oasis:names:tc:opendocument:xmlns:config:1.0" office:version="1.2">
              <office:settings>
                <config:config-item-set config:name="ooo:view-settings">
                  <config:config-item config:name="VisibleAreaTop" config:type="int">0</config:config-item>
                  <config:config-item config:name="VisibleAreaLeft" config:type="int">0</config:config-item>
                </config:config-item-set>
              </office:settings>
            </office:document-settings>
        """.trimIndent())
    }

    private fun writeStyles(zos: ZipOutputStream) {
        zos.writeEntry("styles.xml", """
            <?xml version="1.0" encoding="UTF-8"?>
            <office:document-styles xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0" xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0" office:version="1.2">
              <office:font-face-decls>
                <style:font-face style:name="Arial" svg:font-family="Arial" style:font-family-generic="swiss"/>
              </office:font-face-decls>
              <office:styles>
                <style:default-style style:family="table-cell">
                  <style:paragraph-properties style:tab-stop-distance="1.25cm"/>
                  <style:text-properties style:font-name="Arial" fo:font-size="10pt"/>
                </style:default-style>
              </office:styles>
              <office:automatic-styles>
                <style:page-layout style:name="pm1">
                  <style:page-layout-properties style:writing-mode="lr-tb"/>
                </style:page-layout>
              </office:automatic-styles>
              <office:master-styles>
                <style:master-page style:name="Default" style:page-layout-name="pm1"/>
              </office:master-styles>
            </office:document-styles>
        """.trimIndent())
    }

}