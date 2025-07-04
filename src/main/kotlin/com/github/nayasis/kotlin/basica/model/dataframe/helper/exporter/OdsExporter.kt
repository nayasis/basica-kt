package com.github.nayasis.kotlin.basica.model.dataframe.helper.exporter

import com.github.nayasis.kotlin.basica.model.dataframe.DataFrame
import com.github.nayasis.kotlin.basica.xml.children
import java.io.OutputStream
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
    private val startIndex: Int? = null,
): DataFrameExporter() {

    override fun export(outputStream: OutputStream) {
        val keys = dataframe.keys.toList()
        if (keys.isEmpty()) return

        ZipOutputStream(outputStream).use { zos ->
            writeManifest(zos)
            writeContent(zos, keys)
            writeSettings(zos)
            writeMeta(zos)
            writeStyles(zos)
        }
    }


    private fun writeManifest(zos: ZipOutputStream) {
        zos.putNextEntry(ZipEntry("META-INF/manifest.xml"))
        val content = """<?xml version="1.0" encoding="UTF-8"?>
<manifest:manifest xmlns:manifest="urn:oasis:names:tc:opendocument:xmlns:manifest:1.0">
  <manifest:file-entry manifest:media-type="application/vnd.oasis.opendocument.spreadsheet" manifest:full-path="/"/>
  <manifest:file-entry manifest:media-type="text/xml" manifest:full-path="content.xml"/>
  <manifest:file-entry manifest:media-type="text/xml" manifest:full-path="meta.xml"/>
  <manifest:file-entry manifest:media-type="text/xml" manifest:full-path="settings.xml"/>
  <manifest:file-entry manifest:media-type="text/xml" manifest:full-path="styles.xml"/>
</manifest:manifest>"""
        zos.write(content.toByteArray())
        zos.closeEntry()
    }

    @Throws(Exception::class)
    private fun writeContent(zos: ZipOutputStream) {

        zos.putNextEntry(ZipEntry("content.xml"))

        val first = startIndex ?: dataframe.firstIndex ?: 0
        val last  = dataframe.lastIndex ?: -1

        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = builder.newDocument()

        // Root element
        val root = doc.createElement("office:document-content").apply {
            setAttribute("xmlns:office", "urn:oasis:names:tc:opendocument:xmlns:office:1.0")
            setAttribute("xmlns:table", "urn:oasis:names:tc:opendocument:xmlns:table:1.0")
            setAttribute("xmlns:text", "urn:oasis:names:tc:opendocument:xmlns:text:1.0")
            setAttribute("office:version", "1.2")
            doc.appendChild(this)
        }

        // Automatic styles
        val automaticStyles = doc.createElement("office:automatic-styles")
        root.appendChild(automaticStyles)

        // Body
        val body = doc.createElement("office:body").apply { root.appendChild(this) }

        val spreadsheet = doc.createElement("office:spreadsheet").apply{ body.appendChild(this) }

        // Table (Sheet)
        val table = doc.createElement("table:table").apply {
            setAttribute("table:name", sheetName)
            spreadsheet.appendChild(this)
        }

        val columnNames: MutableList<String?> = ArrayList<String?>(columns.keys)
        val maxRows: Int = columns.values.stream().mapToInt { obj: MutableList<Any?>? -> obj!!.size }.max().orElse(0)

        // Header row
        val headerRow = doc.createElement("table:table-row").also { table.appendChild(it) }

        for(header in if(showLabel) dataframe.labels else dataframe.keys) {
            val cell = doc.createElement("table:table-cell").apply {
                setAttribute("office:value-type", "string")
                headerRow.appendChild(this)
            }
            cell.appendChild( doc.createElement("text:p").apply {
                setTextContent(header)
            })
        }

        // Data rows
        for (row in 0..<maxRows) {
            val dataRow = doc.createElement("table:table-row").apply { table.appendChild(this) }

            for (columnName in dataframe.keys) {
                val columnData = columns.get(columnName)!!
                val cell = doc.createElement("table:table-cell").apply { dataRow.appendChild(this) }

                if (row < columnData.size && columnData.get(row) != null) {
                    val cellValue = columnData.get(row)

                    if (cellValue is Number) {
                        cell.setAttribute("office:value-type", "float")
                        cell.setAttribute("office:value", cellValue.toString())
                    } else {
                        cell.setAttribute("office:value-type", "string")
                    }

                    val paragraph = doc.createElement("text:p")
                    paragraph.setTextContent(cellValue.toString())
                    cell.appendChild(paragraph)
                } else {
                    // Empty cell
                    cell.setAttribute("office:value-type", "string")
                    val paragraph = doc.createElement("text:p")
                    cell.appendChild(paragraph)
                }

                dataRow.appendChild(cell)
            }
        }

        writeDocumentToZip(doc, zos)
        zos.closeEntry()
    }

    private fun writeContent(zos: ZipOutputStream, keys: List<String>) {
        zos.putNextEntry(ZipEntry("content.xml"))






        val first = startIndex ?: dataframe.firstIndex ?: 0
        val last  = dataframe.lastIndex ?: -1





        val content = buildString {
            append("""<?xml version="1.0" encoding="UTF-8"?>
<office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0" xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0">
  <office:body>
    <office:spreadsheet>
      <table:table table:name="$sheetName">""")
            // 헤더 행
            append("<table:table-row>")
            val headers = if (showLabel) dataframe.labels else keys
            for (header in headers) {
                append("""<table:table-cell office:value-type="string"><text:p>${escapeXml(header)}</text:p></table:table-cell>""")
            }
            append("</table:table-row>")
            // 데이터 행
            for (i in first..last) {
                append("<table:table-row>")
                for (key in keys) {
                    val value = dataframe.getData(i, key)
                    when (value) {
                        null -> append("<table:table-cell/>")
                        is Number -> append("<table:table-cell office:value-type="float" office:value="${value}"><text:p>${value}</text:p></table:table-cell>")
                        is Boolean -> append("<table:table-cell office:value-type="boolean" office:value="${if (value) "true" else "false"}"><text:p>${if (value) "true" else "false"}</text:p></table:table-cell>")
                        else -> append("<table:table-cell office:value-type="string"><text:p>${escapeXml(value.toString())}</text:p></table:table-cell>")
                    }
                }
                append("</table:table-row>")
            }
            append("""
      </table:table>
    </office:spreadsheet>
  </office:body>
</office:document-content>""")
        }
        zos.write(content.toByteArray())
        zos.closeEntry()
    }

    private fun writeSettings(zos: ZipOutputStream) {
        zos.putNextEntry(ZipEntry("settings.xml"))
        val content = """<?xml version="1.0" encoding="UTF-8"?>
<office:document-settings xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0">
</office:document-settings>"""
        zos.write(content.toByteArray())
        zos.closeEntry()
    }

    private fun writeMeta(zos: ZipOutputStream) {
        zos.putNextEntry(ZipEntry("meta.xml"))
        val content = """<?xml version="1.0" encoding="UTF-8"?>
<office:document-meta xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:meta="urn:oasis:names:tc:opendocument:xmlns:meta:1.0">
  <office:meta>
    <meta:generator>DataFrame ODS Exporter</meta:generator>
  </office:meta>
</office:document-meta>"""
        zos.write(content.toByteArray())
        zos.closeEntry()
    }

    private fun writeStyles(zos: ZipOutputStream) {
        zos.putNextEntry(ZipEntry("styles.xml"))
        val content = """<?xml version="1.0" encoding="UTF-8"?>
<office:document-styles xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0">
  <office:styles>
    <style:default-style style:family="table-cell" xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0">
      <style:paragraph-properties style:tab-stop-distance="0.5in"/>
    </style:default-style>
  </office:styles>
</office:document-styles>"""
        zos.write(content.toByteArray())
        zos.closeEntry()
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace(""", "&quot;")
            .replace("'", "&apos;")
    }
}
}

