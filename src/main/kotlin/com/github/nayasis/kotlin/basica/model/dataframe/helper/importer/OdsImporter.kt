package com.github.nayasis.kotlin.basica.model.dataframe.helper.importer

import com.github.nayasis.kotlin.basica.model.dataframe.DataFrame
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipInputStream

/**
 * ODS 형식에서 DataFrame을 가져오는 importer
 * 순수 Kotlin으로 구현된 ODS 파서
 */
class OdsImporter(
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
        
        ZipInputStream(inputStream).use { zis ->
            var entry: java.util.zip.ZipEntry?
            
            while (zis.nextEntry.also { entry = it } != null) {
                when (entry?.name) {
                    "content.xml" -> {
                        parseContent(zis, dataframe)
                    }
                }
                zis.closeEntry()
            }
        }
        
        return dataframe
    }

    override fun importFromString(content: String): DataFrame {
        // ODS는 바이너리 형식이므로 문자열에서 직접 가져올 수 없습니다.
        throw UnsupportedOperationException("ODS format cannot be imported from string")
    }

    private fun parseContent(zis: ZipInputStream, dataframe: DataFrame) {
        val content = zis.reader().readText()
        val rows = mutableListOf<MutableList<String>>()
        
        // <table:table-row> 태그들을 찾아서 파싱
        val rowStartTag = "<table:table-row>"
        val rowEndTag = "</table:table-row>"
        var startIndex = 0
        
        while (true) {
            val rowStart = content.indexOf(rowStartTag, startIndex)
            if (rowStart == -1) break
            
            val rowEnd = content.indexOf(rowEndTag, rowStart)
            if (rowEnd == -1) break
            
            val rowContent = content.substring(rowStart, rowEnd + rowEndTag.length)
            val cells = parseOdsRowCells(rowContent)
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
                    val value = parseOdsCellValue(row[colIndex])
                    dataframe.setData(dataframeRowIndex, columnKeys[colIndex], value)
                }
            }
        }
    }

    private fun parseOdsRowCells(rowContent: String): MutableList<String> {
        val cells = mutableListOf<String>()
        val cellStartTag = "<table:table-cell"
        val cellEndTag = "</table:table-cell>"
        val textStartTag = "<text:p>"
        val textEndTag = "</text:p>"
        
        var startIndex = 0
        while (true) {
            val cellStart = rowContent.indexOf(cellStartTag, startIndex)
            if (cellStart == -1) break
            
            val cellEnd = rowContent.indexOf(cellEndTag, cellStart)
            if (cellEnd == -1) break
            
            val cellContent = rowContent.substring(cellStart, cellEnd + cellEndTag.length)
            val value = parseOdsCellContent(cellContent, textStartTag, textEndTag)
            cells.add(value)
            
            startIndex = cellEnd + cellEndTag.length
        }
        
        return cells
    }

    private fun parseOdsCellContent(cellContent: String, textStartTag: String, textEndTag: String): String {
        val textStart = cellContent.indexOf(textStartTag)
        val textEnd = cellContent.indexOf(textEndTag, textStart)
        
        if (textStart != -1 && textEnd != -1) {
            val value = cellContent.substring(textStart + textStartTag.length, textEnd)
            return unescapeXml(value)
        }
        
        return ""
    }

    private fun parseOdsCellValue(value: String): Any? {
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