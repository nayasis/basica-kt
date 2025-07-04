package com.github.nayasis.kotlin.basica.model.dataframe.helper.importer

import com.github.nayasis.kotlin.basica.model.dataframe.DataFrame
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipInputStream

/**
 * XLSX 형식에서 DataFrame을 가져오는 importer
 * 순수 Kotlin으로 구현된 XLSX 파서
 */
class XlsxImporter(
    private val sheetIndex: Int = 0,
    private val hasHeader: Boolean = true
) : DataFrameImporter {

    override fun import(filePath: Path): DataFrame {
        return Files.newInputStream(filePath).use { stream ->
            import(stream)
        }
    }

    override fun import(inputStream: InputStream): DataFrame {
        val dataframe = DataFrame()
        val sharedStrings = mutableListOf<String>()
        
        ZipInputStream(inputStream).use { zis ->
            var entry: java.util.zip.ZipEntry?
            
            while (zis.nextEntry.also { entry = it } != null) {
                when (entry?.name) {
                    "xl/sharedStrings.xml" -> {
                        parseSharedStrings(zis, sharedStrings)
                    }
                    "xl/worksheets/sheet1.xml" -> {
                        parseWorksheet(zis, dataframe, sharedStrings)
                    }
                }
                zis.closeEntry()
            }
        }
        
        return dataframe
    }

    override fun importFromString(content: String): DataFrame {
        // XLSX는 바이너리 형식이므로 문자열에서 직접 가져올 수 없습니다.
        throw UnsupportedOperationException("XLSX format cannot be imported from string")
    }

    private fun parseSharedStrings(zis: ZipInputStream, sharedStrings: MutableList<String>) {
        val content = zis.reader().readText()
        val startTag = "<si>"
        val endTag = "</si>"
        val textStartTag = "<t>"
        val textEndTag = "</t>"
        
        var startIndex = 0
        while (true) {
            val siStart = content.indexOf(startTag, startIndex)
            if (siStart == -1) break
            
            val siEnd = content.indexOf(endTag, siStart)
            if (siEnd == -1) break
            
            val siContent = content.substring(siStart, siEnd + endTag.length)
            val textStart = siContent.indexOf(textStartTag)
            val textEnd = siContent.indexOf(textEndTag, textStart)
            
            if (textStart != -1 && textEnd != -1) {
                val text = siContent.substring(textStart + textStartTag.length, textEnd)
                sharedStrings.add(unescapeXml(text))
            }
            
            startIndex = siEnd + endTag.length
        }
    }

    private fun parseWorksheet(zis: ZipInputStream, dataframe: DataFrame, sharedStrings: List<String>) {
        val content = zis.reader().readText()
        val rows = mutableListOf<MutableList<String>>()
        
        // <row> 태그들을 찾아서 파싱
        val rowStartTag = "<row"
        val rowEndTag = "</row>"
        var startIndex = 0
        
        while (true) {
            val rowStart = content.indexOf(rowStartTag, startIndex)
            if (rowStart == -1) break
            
            val rowEnd = content.indexOf(rowEndTag, rowStart)
            if (rowEnd == -1) break
            
            val rowContent = content.substring(rowStart, rowEnd + rowEndTag.length)
            val cells = parseRowCells(rowContent, sharedStrings)
            rows.add(cells)
            
            startIndex = rowEnd + rowEndTag.length
        }
        
        // DataFrame에 데이터 추가
        if (rows.isNotEmpty()) {
            val headerRow = if (hasHeader) rows[0] else null
            val dataStartIndex = if (hasHeader) 1 else 0
            
            // 헤더 처리
            val columnKeys = if (hasHeader && headerRow != null) {
                headerRow.mapIndexed { index, header ->
                    val key = "col_$index"
                    dataframe.setLabel(key, header)
                    key
                }
            } else {
                // 헤더가 없는 경우 첫 번째 데이터 행에서 컬럼 수를 추정
                val firstDataRow = rows.getOrNull(dataStartIndex)
                if (firstDataRow != null) {
                    (0 until firstDataRow.size).map { "col_$it" }
                } else {
                    emptyList()
                }
            }
            
            // 데이터 처리
            for (rowIndex in dataStartIndex until rows.size) {
                val row = rows[rowIndex]
                val dataframeRowIndex = rowIndex - dataStartIndex
                
                for (colIndex in 0 until minOf(columnKeys.size, row.size)) {
                    val value = parseCellValue(row[colIndex])
                    dataframe.setData(dataframeRowIndex, columnKeys[colIndex], value)
                }
            }
        }
    }

    private fun parseRowCells(rowContent: String, sharedStrings: List<String>): MutableList<String> {
        val cells = mutableListOf<String>()
        val cellStartTag = "<c"
        val cellEndTag = "</c>"
        val valueStartTag = "<v>"
        val valueEndTag = "</v>"
        
        var startIndex = 0
        while (true) {
            val cellStart = rowContent.indexOf(cellStartTag, startIndex)
            if (cellStart == -1) break
            
            val cellEnd = rowContent.indexOf(cellEndTag, cellStart)
            if (cellEnd == -1) break
            
            val cellContent = rowContent.substring(cellStart, cellEnd + cellEndTag.length)
            val value = parseCellContent(cellContent, sharedStrings)
            cells.add(value)
            
            startIndex = cellEnd + cellEndTag.length
        }
        
        return cells
    }

    private fun parseCellContent(cellContent: String, sharedStrings: List<String>): String {
        val valueStartTag = "<v>"
        val valueEndTag = "</v>"
        
        val valueStart = cellContent.indexOf(valueStartTag)
        val valueEnd = cellContent.indexOf(valueEndTag, valueStart)
        
        if (valueStart != -1 && valueEnd != -1) {
            val value = cellContent.substring(valueStart + valueStartTag.length, valueEnd)
            
            // 공유 문자열인지 확인
            if (cellContent.contains("t=\"s\"")) {
                val stringIndex = value.toIntOrNull() ?: 0
                return if (stringIndex < sharedStrings.size) sharedStrings[stringIndex] else ""
            }
            
            return value
        }
        
        return ""
    }

    private fun parseCellValue(value: String): Any? {
        if (value.isEmpty()) return null
        
        return try {
            when {
                value.equals("true", ignoreCase = true) -> true
                value.equals("false", ignoreCase = true) -> false
                value.contains(".") -> value.toDouble()
                else -> value.toLong()
            }
        } catch (e: NumberFormatException) {
            value
        }
    }

    private fun unescapeXml(str: String): String {
        return str.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
    }
} 