package io.github.nayasis.kotlin.basica.model.dataframe.helper.importer

import io.github.nayasis.kotlin.basica.model.dataframe.DataFrame
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset

class CsvImporter(
    private val delimiter: Char = ',',
    private val firstRowAsHeader: Boolean = true,
    private val lastColumnIndex: Int = -1,
    private val charset: Charset = Charsets.UTF_8,
): DataFrameImporter() {

    override fun import(inputStream: InputStream): DataFrame {
        val dataframe = DataFrame()
        BufferedReader(InputStreamReader(inputStream, charset)).use { reader ->
            // read first until not empty line
            lateinit var firstRow: List<String>
            while (true) {
                val line = reader.readLine().also { if(it == null) return dataframe }
                firstRow = parseCsvLine(line).also { if(it.isEmpty()) continue }
                break
            }
            val rowCache = mutableMapOf<Int, MutableMap<Int, Any?>>()
            var appliedLastColumnIndex = when {
                lastColumnIndex < 0 -> firstRow.lastIndex
                firstRowAsHeader -> minOf(firstRow.lastIndex, lastColumnIndex)
                else -> lastColumnIndex
            }

            if (appliedLastColumnIndex >= 0) {
                for (col in 0..appliedLastColumnIndex) {
                    val key = if (firstRowAsHeader) {
                        firstRow.getOrNull(col)?.takeIf { it.isNotBlank() } ?: "$col"
                    } else {
                        "$col"
                    }
                    dataframe.addKey(key)
                }
            }
            // set header
            var rowIdx = 0
            if (!firstRowAsHeader) {
                val firstValues = firstRow.map { normalizeCell(it) }
                val firstMap = mutableMapOf<Int, Any?>()
                firstValues.forEachIndexed { colIdx, value ->
                    firstMap[colIdx] = value
                }
                rowCache[rowIdx] = firstMap
                if (lastColumnIndex < 0) {
                    val maxIndexInRow = firstMap.keys.maxOrNull() ?: -1
                    while (maxIndexInRow > appliedLastColumnIndex) {
                        appliedLastColumnIndex += 1
                        dataframe.addKey("$appliedLastColumnIndex")
                        for (r in 0..rowIdx) {
                            dataframe.setData(r, appliedLastColumnIndex, rowCache[r]?.get(appliedLastColumnIndex))
                        }
                    }
                }
                firstMap.forEach { (colIdx, value) ->
                    if (colIdx <= appliedLastColumnIndex) {
                        dataframe.setData(rowIdx, colIdx, value)
                    }
                }
                rowIdx++
            }
            // set data
            reader.lineSequence().forEach { line ->
                val row = parseCsvLine(line)
                val rowMap = mutableMapOf<Int, Any?>()
                row.forEachIndexed { colIdx, value ->
                    rowMap[colIdx] = normalizeCell(value)
                }
                rowCache[rowIdx] = rowMap
                if (lastColumnIndex < 0) {
                    val maxIndexInRow = rowMap.keys.maxOrNull() ?: -1
                    while (maxIndexInRow > appliedLastColumnIndex) {
                        appliedLastColumnIndex += 1
                        dataframe.addKey("$appliedLastColumnIndex")
                        for (r in 0..rowIdx) {
                            dataframe.setData(r, appliedLastColumnIndex, rowCache[r]?.get(appliedLastColumnIndex))
                        }
                    }
                }
                rowMap.forEach { (colIdx, value) ->
                    if (colIdx <= appliedLastColumnIndex) {
                        dataframe.setData(rowIdx, colIdx, value)
                    }
                }
                rowIdx++
            }
        }
        return dataframe
    }

    private fun normalizeCell(value: String): Any? {
        return value.trimStart('\uFEFF').ifEmpty { null }
    }

    private fun parseCsvLine(line: String): List<String> {
        if (line.isBlank() || line.all { it == delimiter }) return emptyList()
        val result = mutableListOf<String>()
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
