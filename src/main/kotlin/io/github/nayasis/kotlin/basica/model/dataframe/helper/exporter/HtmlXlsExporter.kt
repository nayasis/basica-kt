package io.github.nayasis.kotlin.basica.model.dataframe.helper.exporter

import io.github.nayasis.kotlin.basica.core.localdate.format
import io.github.nayasis.kotlin.basica.core.localdate.toDate
import io.github.nayasis.kotlin.basica.core.string.escapeXml
import io.github.nayasis.kotlin.basica.model.dataframe.DataFrame
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.Date

/**
 * XLS exporter using HTML table format
 * Excel can read HTML files with .xls extension as spreadsheet
 */
class HtmlXlsExporter(
    private val dataframe: DataFrame,
    private val sheetName: String = "Sheet1",
    private val showLabel: Boolean = true,
    private val charset: Charset = Charsets.UTF_8,
    startIndex: Int? = null,
) : DataFrameExporter() {

    private val first: Int = startIndex?.takeIf { it >= 0 } ?: 0
    private val last: Int  = dataframe.lastIndex ?: -1

    override fun export(outputStream: OutputStream) {
        PrintWriter(OutputStreamWriter(outputStream, charset)).use { writer ->
            // HTML header (Excel can recognize)
            writer.write("""
                <html xmlns:o=\"urn:schemas-microsoft-com:office:office\" xmlns:x=\"urn:schemas-microsoft-com:office:excel\" xmlns=\"http://www.w3.org/TR/REC-html40\">
                <head>
                    <meta charset=\"UTF-8\">
                    <meta name=\"ProgId\" content=\"Excel.Sheet\">
                    <meta name=\"Generator\" content=\"Microsoft Excel 15\">
                    <meta name=\"Originator\" content=\"Microsoft Excel 15\">
                    <style>
                        table { border-collapse: collapse; }
                        td, th { border: 1px solid #000000; padding: 4px; }
                        th { background-color: #f0f0f0; font-weight: bold; }
                    </style>
                </head>
                <body><table>
            """.trimIndent())

            // Header
            writer.write("<tr>")
            for (key in dataframe.keys) {
                val label = if (showLabel) dataframe.getLabel(key) else key
                writer.write("<th>${label.escapeXml()}</th>")
            }
            writer.write("</tr>")

            // Body
            for (i in first..last) {
                writer.write("<tr>")
                for (key in dataframe.keys) {
                    writer.write("<td>${formatCellValue(dataframe.getData(i, key)).escapeXml()}</td>")
                }
                writer.write("</tr>")
            }

            // HTML footer
            writer.write("""
                </table></body>
                </html>
            """.trimIndent())
        }
    }

    private fun formatCellValue(value: Any?): String {
        return when (value) {
            null             -> ""
            is LocalDate     -> value.format()
            is LocalDateTime -> value.format()
            is ZonedDateTime -> value.format()
            is Date          -> value.format()
            is Calendar      -> value.toDate().format()
            else -> value.toString()
        }
    }


} 