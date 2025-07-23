package com.github.nayasis.kotlin.basica.model.dataframe.helper.importer

import com.github.nayasis.kotlin.basica.model.dataframe.DataFrame
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset

class CsvImporter(
    private val delimiter: Char = ',',
    private val charset: Charset = Charsets.UTF_8,
    private val useHeader: Boolean = true
): DataFrameImporter() {

    override fun import(inputStream: InputStream): DataFrame {
        val dataframe = DataFrame()
        BufferedReader(InputStreamReader(inputStream, charset)).use { reader ->
            val firstLine = reader.readLine() ?: return dataframe
            val firstRow = parseCsvLine(firstLine)
            val headers = if (useHeader) { firstRow } else { firstRow.indices }.map { "$it" }
            if (! useHeader) {
                headers.forEachIndexed { col, value ->
                    dataframe.setData(0, headers[col], value)
                }
            }
            // set data
            var rowIndex = if(useHeader) 0 else 1
            reader.lineSequence().forEach { line ->
                parseCsvLine(line).forEachIndexed { col, value ->
                    if (col < headers.size) {
                        dataframe.setData(rowIndex, headers[col], value)
                    }
                }
                rowIndex++
            }
        }
        return dataframe
    }

    private fun parseCsvLine(line: String): List<Any> {
        val result = mutableListOf<Any>()
        var inQuotes = false
        val sb = StringBuilder()
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == delimiter && !inQuotes -> {
                    result.add(sb.toString())
                    sb.clear()
                }
                else -> sb.append(c)
            }
            i++
        }
        result.add(sb.toString())
        return result
    }
} 