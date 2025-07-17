package com.github.nayasis.kotlin.basica.model.dataframe.helper.importer

import com.github.nayasis.kotlin.basica.model.dataframe.DataFrame
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset

class CsvImporter(
    private val delimiter: Char = ',',
    private val useHeader: Boolean = true,
    private val charset: Charset = Charsets.UTF_8,
): DataFrameImporter() {

    override fun import(inputStream: InputStream): DataFrame {
        val dataframe = DataFrame()
        BufferedReader(InputStreamReader(inputStream, charset)).use { reader ->
            // read first until not empty line
            lateinit var firstRow: List<Any>
            while (true) {
                val line = reader.readLine().also { if(it == null) return dataframe }
                firstRow = parseCsvLine(line).also { if(it.isEmpty()) continue }
                break
            }
            // set header
            var rowIdx = 0
            if (useHeader) {
                firstRow.forEach { dataframe.addKey("$it") }
            } else {
                firstRow.forEachIndexed { colIdx, value ->
                    dataframe.addKey("$colIdx")
                    dataframe.setData(0, colIdx, value)
                }
                rowIdx++
            }
            // set data
            reader.lineSequence().forEach { line ->
                val row = parseCsvLine(line)
                row.forEachIndexed { colIdx, value ->
                    dataframe.setData(rowIdx, colIdx, value)
                }
                rowIdx++
            }
        }
        return dataframe
    }

    private fun parseCsvLine(line: String): List<Any> {
        if (line.isBlank() || line.all { it == delimiter }) return emptyList()
        val result = mutableListOf<Any>()
        var inQuotes = false
        val sb = StringBuilder()
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when (c) {
                '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                delimiter if !inQuotes -> {
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