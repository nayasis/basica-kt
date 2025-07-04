package com.github.nayasis.kotlin.basica.model.dataframe.helper.exporter

import com.github.nayasis.kotlin.basica.model.dataframe.DataFrame
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * CSV exporter
 */
class CsvExporter(
    private val dataframe: DataFrame,
    private val delimiter: String = ",",
    private val showLabel: Boolean = true,
    private val charset: Charset = Charsets.UTF_8,
    private val startIndex: Int? = null,
): DataFrameExporter() {

    override fun export(outputStream: OutputStream) {
        PrintWriter(OutputStreamWriter(outputStream, charset)).use { writer ->
            val keys = dataframe.keys.also { if(it.isEmpty()) return }
            // write header
            (if (showLabel) dataframe.labels else keys).let { writer.write(it) }
            // write body
            for(i in (startIndex ?: dataframe.firstIndex ?: 0) .. (dataframe.lastIndex ?: -1)) {
                keys.map { key -> dataframe.getData(i,key) }.let { writer.write(it) }
                writer.println()
            }
        }
    }

    fun PrintWriter.write(values: Collection<Any?>) {
        values.joinToString(delimiter) { value ->
            when {
                value == null -> ""
                value is CharSequence && value.isEmpty() -> ""
                value is Number -> value.toString()
                else -> value.toString().replace("\"", "\"\"").let { "\"$it\"" }
            }
        }.let { print(it) }
    }

}