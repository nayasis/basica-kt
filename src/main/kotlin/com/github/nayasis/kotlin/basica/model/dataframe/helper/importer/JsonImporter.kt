package com.github.nayasis.kotlin.basica.model.dataframe.helper.importer

import com.github.nayasis.kotlin.basica.model.dataframe.DataFrame
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path

/**
 * JSON 형식에서 DataFrame을 가져오는 importer
 */
class JsonImporter(
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
        val trimmedContent = content.trim()
        
        if (trimmedContent.startsWith("[")) {
            parseJsonArray(trimmedContent, dataframe)
        } else {
            throw IllegalArgumentException("Unsupported JSON structure. Must be a JSON array of objects.")
        }
        
        return dataframe
    }

    private fun importInMemory(filePath: Path): DataFrame {
        val content = Files.readAllLines(filePath, charset).joinToString("\n")
        return importFromString(content)
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
        val content = reader.lineSequence().joinToString("\n")
        parseJsonArray(content, dataframe)
        return dataframe
    }

    private fun parseJsonArray(content: String, dataframe: DataFrame) {
        var rowIndex = 0
        var braceCount = 0
        var insideObject = false
        val sb = StringBuilder()

        for (i in 1 until content.length - 1) {
            val c = content[i]

            when (c) {
                '{' -> {
                    braceCount++
                    insideObject = true
                }
                '}' -> {
                    braceCount--
                    if (braceCount == 0) {
                        val jsonObject = sb.toString()
                        val parsed = parseFlatJsonObject(jsonObject)
                        addRow(parsed, dataframe, rowIndex++)
                        sb.clear()
                        insideObject = false
                    }
                }
            }

            if (insideObject) {
                sb.append(c)
            }
        }
    }

    private fun parseFlatJsonObject(json: String): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        val trimmed = json.trim()
        
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return result
        }

        val content = trimmed.substring(1, trimmed.length - 1)
        val pairs = parseJsonPairs(content)

        for (pair in pairs) {
            val colonIdx = pair.indexOf(':')
            if (colonIdx == -1) continue

            val rawKey = pair.substring(0, colonIdx).trim()
            val rawValue = pair.substring(colonIdx + 1).trim()

            val key = unquote(rawKey)
            val value = parseValue(rawValue)
            result[key] = value
        }

        return result
    }

    private fun parseJsonPairs(content: String): List<String> {
        val pairs = mutableListOf<String>()
        var inQuotes = false
        var escaping = false
        val current = StringBuilder()

        for (i in content.indices) {
            val c = content[i]
            current.append(c)

            when {
                c == '"' && !escaping -> inQuotes = !inQuotes
                c == '\\' && !escaping -> escaping = true
                !inQuotes && c == ',' -> {
                    pairs.add(current.substring(0, current.length - 1).trim())
                    current.clear()
                }
                else -> escaping = false
            }
        }

        if (current.isNotEmpty()) {
            pairs.add(current.toString().trim())
        }

        return pairs
    }

    private fun addRow(row: Map<String, Any?>, dataframe: DataFrame, rowIndex: Int) {
        for ((key, value) in row) {
            dataframe.setData(rowIndex, key, value)
        }
    }

    private fun parseValue(rawValue: String): Any? {
        return when {
            rawValue.startsWith("\"") && rawValue.endsWith("\"") -> unquote(rawValue)
            rawValue.equals("null", ignoreCase = true) -> null
            rawValue.equals("true", ignoreCase = true) -> true
            rawValue.equals("false", ignoreCase = true) -> false
            else -> {
                try {
                    when {
                        rawValue.contains(".") || rawValue.contains("e") || rawValue.contains("E") -> 
                            rawValue.toDouble()
                        else -> rawValue.toLong()
                    }
                } catch (e: NumberFormatException) {
                    rawValue
                }
            }
        }
    }

    private fun unquote(str: String): String {
        if (str.length < 2) return str
        if (str.startsWith("\"") && str.endsWith("\"")) {
            return str.substring(1, str.length - 1).replace("\\\"", "\"")
        }
        return str
    }
} 