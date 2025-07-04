package com.github.nayasis.kotlin.basica.model.dataframe.helper.importer

import com.github.nayasis.kotlin.basica.model.dataframe.DataFrame
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path

/**
 * CSV 형식에서 DataFrame을 가져오는 importer
 */
class CsvImporter(
    private val delimiter: Char = ',',
    private val hasQuotes: Boolean = true,
    private val charset: Charset = Charsets.UTF_8,
    private val memoryEfficientThreshold: Long = 10_000_000 // 10MB
) : DataFrameImporter {

    override fun import(filePath: Path): DataFrame {
        val fileSize = Files.size(filePath)
        
        return if (fileSize < memoryEfficientThreshold) {
            try {
                importInMemory(filePath)
            } catch (e: OutOfMemoryError) {
                // 메모리 부족 시 스트리밍 방식으로 전환
                System.gc()
                importStreaming(filePath)
            }
        } else {
            importStreaming(filePath)
        }
    }

    override fun import(inputStream: InputStream): DataFrame {
        return importStreaming(inputStream)
    }

    override fun importFromString(content: String): DataFrame {
        val dataframe = DataFrame()
        val lines = content.lines()
        if (lines.isEmpty()) return dataframe

        val format = detectCsvFormat(lines.first())
        val headers = parseCsvLine(lines.first(), format)
        
        // 헤더 설정
        headers.forEachIndexed { index, header ->
            dataframe.setLabel("col_$index", header.trim())
        }

        // 데이터 처리
        lines.drop(1).forEachIndexed { rowIndex, line ->
            val values = parseCsvLine(line, format)
            values.forEachIndexed { colIndex, value ->
                if (colIndex < headers.size) {
                    dataframe.setData(rowIndex, "col_$colIndex", parseValue(value.trim()))
                }
            }
        }

        return dataframe
    }

    private fun importInMemory(filePath: Path): DataFrame {
        val lines = Files.readAllLines(filePath, charset)
        if (lines.isEmpty()) return DataFrame()

        val format = detectCsvFormat(lines.first())
        val headers = parseCsvLine(lines.first(), format)
        
        val dataframe = DataFrame()
        
        // 헤더 설정
        headers.forEachIndexed { index, header ->
            dataframe.setLabel("col_$index", header.trim())
        }

        // 데이터 처리
        lines.drop(1).forEachIndexed { rowIndex, line ->
            val values = parseCsvLine(line, format)
            values.forEachIndexed { colIndex, value ->
                if (colIndex < headers.size) {
                    dataframe.setData(rowIndex, "col_$colIndex", parseValue(value.trim()))
                }
            }
        }

        return dataframe
    }

    private fun importStreaming(filePath: Path): DataFrame {
        return Files.newBufferedReader(filePath, charset).use { reader ->
            importStreaming(reader)
        }
    }

    private fun importStreaming(inputStream: InputStream): DataFrame {
        return BufferedReader(InputStreamReader(inputStream, charset)).use { reader ->
            importStreaming(reader)
        }
    }

    private fun importStreaming(reader: BufferedReader): DataFrame {
        val dataframe = DataFrame()
        val headerLine = reader.readLine() ?: return dataframe

        val format = detectCsvFormat(headerLine)
        val headers = parseCsvLine(headerLine, format)
        
        // 헤더 설정
        headers.forEachIndexed { index, header ->
            dataframe.setLabel("col_$index", header.trim())
        }

        // 데이터 처리
        var rowIndex = 0
        reader.lineSequence().forEach { line ->
            val values = parseCsvLine(line, format)
            values.forEachIndexed { colIndex, value ->
                if (colIndex < headers.size) {
                    dataframe.setData(rowIndex, "col_$colIndex", parseValue(value.trim()))
                }
            }
            rowIndex++
        }

        return dataframe
    }

    private fun detectCsvFormat(sampleLine: String): CsvFormat {
        val delimiters = charArrayOf(',', '\t', ';', '|', '#', ':')
        val delimiterCounts = delimiters.map { delimiter ->
            sampleLine.count { it == delimiter }
        }

        val maxIndex = delimiterCounts.indices.maxByOrNull { delimiterCounts[it] } ?: 0
        val detectedDelimiter = delimiters[maxIndex]
        val detectedHasQuotes = sampleLine.contains("\"")

        return CsvFormat(detectedDelimiter, detectedHasQuotes)
    }

    private fun parseCsvLine(line: String, format: CsvFormat): Array<String> {
        if (!format.hasQuotes) {
            return line.split(format.delimiter).toTypedArray()
        }

        val fields = mutableListOf<String>()
        var inQuotes = false
        val currentField = StringBuilder()

        var i = 0
        while (i < line.length) {
            val c = line[i]

            when {
                c == '"' -> {
                    if (inQuotes && i < line.length - 1 && line[i + 1] == '"') {
                        // 이스케이프된 따옴표
                        currentField.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == format.delimiter && !inQuotes -> {
                    fields.add(currentField.toString())
                    currentField.clear()
                }
                else -> {
                    currentField.append(c)
                }
            }
            i++
        }

        fields.add(currentField.toString())
        return fields.toTypedArray()
    }

    private fun parseValue(value: String): Any? {
        if (value.isEmpty()) return null

        return try {
            when {
                value.contains(".") -> value.toDouble()
                else -> value.toLong()
            }
        } catch (e: NumberFormatException) {
            value
        }
    }

    private data class CsvFormat(val delimiter: Char, val hasQuotes: Boolean)
} 