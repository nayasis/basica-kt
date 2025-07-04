package com.github.nayasis.kotlin.basica.model.dataframe.helper.exporter

import com.github.nayasis.kotlin.basica.model.dataframe.DataFrame
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * XLSX 형식으로 DataFrame을 내보내는 exporter
 * 순수 Kotlin으로 구현된 XLSX 생성기
 */
class XlsxExporter(
    private val sheetName: String = "Sheet1"
) : DataFrameExporter {

    private val sharedStrings = mutableListOf<String>()
    private val stringIndexMap = mutableMapOf<String, Int>()

    override fun export(dataframe: DataFrame, filePath: Path) {
        // 부모 디렉토리가 없으면 생성
        val parent = filePath.parent
        if (parent != null) {
            Files.createDirectories(parent)
        }
        
        Files.newOutputStream(filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).use { stream ->
            export(dataframe, stream)
        }
    }

    override fun export(dataframe: DataFrame, outputStream: OutputStream) {
        val keys = dataframe.keys.toList()
        if (keys.isEmpty()) return

        // 공유 문자열 빌드
        buildSharedStrings(dataframe, keys)

        ZipOutputStream(outputStream).use { zos ->
            // Content Types
            writeContentTypes(zos)
            
            // Relationships
            writeRels(zos)
            
            // Workbook
            writeWorkbook(zos)
            
            // Workbook Relationships
            writeWorkbookRels(zos)
            
            // Shared Strings
            writeSharedStrings(zos)
            
            // Styles
            writeStyles(zos)
            
            // Sheet
            writeSheet(zos, dataframe, keys)
        }
    }

    override fun exportToString(dataframe: DataFrame): String {
        // XLSX는 바이너리 형식이므로 문자열로 변환할 수 없습니다.
        throw UnsupportedOperationException("XLSX format cannot be exported as string")
    }

    private fun buildSharedStrings(dataframe: DataFrame, keys: List<String>) {
        sharedStrings.clear()
        stringIndexMap.clear()

        // 컬럼 헤더 추가
        keys.forEach { key ->
            val label = dataframe.getLabel(key) ?: key
            addSharedString(label)
        }

        // 데이터 값들 추가
        val firstIndex = dataframe.firstIndex ?: 0
        val lastIndex = dataframe.lastIndex ?: -1
        
        for (rowIndex in firstIndex..lastIndex) {
            keys.forEach { key ->
                val value = dataframe.getData(rowIndex, key)
                if (value != null && value !is Number) {
                    addSharedString(value.toString())
                }
            }
        }
    }

    private fun addSharedString(str: String) {
        if (!stringIndexMap.containsKey(str)) {
            stringIndexMap[str] = sharedStrings.size
            sharedStrings.add(str)
        }
    }

    private fun writeContentTypes(zos: ZipOutputStream) {
        zos.putNextEntry(ZipEntry("[Content_Types].xml"))
        
        val content = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""
        
        zos.write(content.toByteArray())
        zos.closeEntry()
    }

    private fun writeRels(zos: ZipOutputStream) {
        zos.putNextEntry(ZipEntry("_rels/.rels"))
        
        val content = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
        
        zos.write(content.toByteArray())
        zos.closeEntry()
    }

    private fun writeWorkbook(zos: ZipOutputStream) {
        zos.putNextEntry(ZipEntry("xl/workbook.xml"))
        
        val content = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="$sheetName" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""
        
        zos.write(content.toByteArray())
        zos.closeEntry()
    }

    private fun writeWorkbookRels(zos: ZipOutputStream) {
        zos.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
        
        val content = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
        
        zos.write(content.toByteArray())
        zos.closeEntry()
    }

    private fun writeSharedStrings(zos: ZipOutputStream) {
        zos.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
        
        val content = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${sharedStrings.size}" uniqueCount="${sharedStrings.size}">""")
            
            sharedStrings.forEach { str ->
                append("<si><t>${escapeXml(str)}</t></si>")
            }
            
            append("</sst>")
        }
        
        zos.write(content.toByteArray())
        zos.closeEntry()
    }

    private fun writeStyles(zos: ZipOutputStream) {
        zos.putNextEntry(ZipEntry("xl/styles.xml"))
        
        val content = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <numFmts count="0"/>
  <fonts count="1">
    <font>
      <sz val="11"/>
      <name val="Calibri"/>
    </font>
  </fonts>
  <fills count="1">
    <fill>
      <patternFill patternType="none"/>
    </fill>
  </fills>
  <borders count="1">
    <border>
      <left/>
      <right/>
      <top/>
      <bottom/>
      <diagonal/>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
  </cellXfs>
</styleSheet>"""
        
        zos.write(content.toByteArray())
        zos.closeEntry()
    }

    private fun writeSheet(zos: ZipOutputStream, dataframe: DataFrame, keys: List<String>) {
        zos.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
        
        val firstIndex = dataframe.firstIndex ?: 0
        val lastIndex = dataframe.lastIndex ?: -1
        val rowCount = if (lastIndex >= firstIndex) lastIndex - firstIndex + 2 else 1 // +2 for header
        
        val content = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>""")
            
            // 헤더 행
            append("<row r=\"1\">")
            keys.forEachIndexed { colIndex, key ->
                val label = dataframe.getLabel(key) ?: key
                val cellRef = getCellReference(colIndex, 0)
                val stringIndex = stringIndexMap[label] ?: 0
                append("<c r=\"$cellRef\" t=\"s\"><v>$stringIndex</v></c>")
            }
            append("</row>")
            
            // 데이터 행
            for (rowIndex in firstIndex..lastIndex) {
                val excelRowIndex = rowIndex - firstIndex + 2 // 헤더 다음 행부터 시작
                append("<row r=\"$excelRowIndex\">")
                
                keys.forEachIndexed { colIndex, key ->
                    val value = dataframe.getData(rowIndex, key)
                    val cellRef = getCellReference(colIndex, excelRowIndex - 1)
                    
                    when (value) {
                        null -> append("<c r=\"$cellRef\"></c>")
                        is Number -> append("<c r=\"$cellRef\"><v>${value}</v></c>")
                        is Boolean -> append("<c r=\"$cellRef\" t=\"b\"><v>${if (value) 1 else 0}</v></c>")
                        else -> {
                            val stringIndex = stringIndexMap[value.toString()] ?: 0
                            append("<c r=\"$cellRef\" t=\"s\"><v>$stringIndex</v></c>")
                        }
                    }
                }
                append("</row>")
            }
            
            append("""
  </sheetData>
</worksheet>""")
        }
        
        zos.write(content.toByteArray())
        zos.closeEntry()
    }

    private fun getCellReference(col: Int, row: Int): String {
        val colRef = buildString {
            var colNum = col
            while (colNum >= 0) {
                insert(0, ('A' + colNum % 26).toChar())
                colNum = colNum / 26 - 1
            }
        }
        return "$colRef${row + 1}"
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
} 