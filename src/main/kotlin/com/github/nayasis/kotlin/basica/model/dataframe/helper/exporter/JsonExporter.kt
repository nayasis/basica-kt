package com.github.nayasis.kotlin.basica.model.dataframe.helper.exporter

import com.github.nayasis.kotlin.basica.model.dataframe.DataFrame
import com.github.nayasis.kotlin.basica.reflection.Reflector
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.nio.charset.Charset

/**
 * JSON exporter
 */
class JsonExporter(
    private val dataframe: DataFrame,
    private val prettyPrint: Boolean = false,
    private val startIndex: Int? = null,
) : DataFrameExporter() {

    override fun export(outputStream: OutputStream) {
        val newLine = if (prettyPrint) "\n" else ""
        PrintWriter(OutputStreamWriter(outputStream, Charsets.UTF_8)).use { writer ->
            val first = startIndex ?: dataframe.firstIndex ?: 0
            val last  = dataframe.lastIndex ?: -1
            writer.print("[${newLine}")
            for (i in first..last) {
                val json = Reflector.toJson(dataframe.getRow(i), prettyPrint)
                writer.print(json)
                if (i < last) {
                    writer.print(",${newLine}")
                } else {
                    writer.print(newLine)
                }
            }
            writer.print("]")
        }
    }

} 