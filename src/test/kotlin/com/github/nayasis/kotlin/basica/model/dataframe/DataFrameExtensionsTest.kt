package com.github.nayasis.kotlin.basica.model.dataframe

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DataFrameExtensionsTest {

    @Test
    fun `test CSV import and export`(@TempDir tempDir: Path) {
        // Given
        val csvContent = """
            name,age,city
            John,25,Seoul
            Jane,30,Busan
            Bob,35,Incheon
        """.trimIndent()
        
        val csvFile = tempDir.resolve("test.csv")
        csvFile.toFile().writeText(csvContent)
        
        // When
        val df = DataFrame.fromCsv(csvFile)
        
        // Then
        assertEquals(3, df.size)
        assertEquals(setOf("name", "age", "city"), df.keys)
        assertEquals("John", df.getData(0, "name"))
        assertEquals(25, df.getData(0, "age"))
        assertEquals("Seoul", df.getData(0, "city"))
        
        // Test export
        val outputFile = tempDir.resolve("output.csv")
        df.toCsv(outputFile)
        
        val exportedDf = DataFrame.fromCsv(outputFile)
        assertEquals(df.size, exportedDf.size)
        assertEquals(df.keys, exportedDf.keys)
    }

    @Test
    fun `test CSV stream import and export`() {
        // Given
        val csvString = """
            name,age,city
            John,25,Seoul
            Jane,30,Busan
        """.trimIndent()
        
        val inputStream = ByteArrayInputStream(csvString.toByteArray(Charsets.UTF_8))
        
        // When
        val df = DataFrame.fromCsv(inputStream)
        
        // Then
        assertEquals(2, df.size)
        assertEquals(setOf("name", "age", "city"), df.keys)
        
        // Test export to stream
        val outputStream = ByteArrayOutputStream()
        df.toCsv(outputStream)
        val exportedString = outputStream.toString(Charsets.UTF_8)
        assertTrue(exportedString.contains("name,age,city"))
        assertTrue(exportedString.contains("John,25,Seoul"))
    }

    @Test
    fun `test JSON import and export`(@TempDir tempDir: Path) {
        // Given
        val jsonContent = """
            [
              {"name": "John", "age": 25, "city": "Seoul"},
              {"name": "Jane", "age": 30, "city": "Busan"},
              {"name": "Bob", "age": 35, "city": "Incheon"}
            ]
        """.trimIndent()
        
        val jsonFile = tempDir.resolve("test.json")
        jsonFile.toFile().writeText(jsonContent)
        
        // When
        val df = DataFrame.fromJson(jsonFile)
        
        // Then
        assertEquals(3, df.size)
        assertEquals(setOf("name", "age", "city"), df.keys)
        assertEquals("John", df.getData(0, "name"))
        assertEquals(25, df.getData(0, "age"))
        
        // Test export
        val outputFile = tempDir.resolve("output.json")
        df.toJson(outputFile, prettyPrint = true)
        
        val exportedDf = DataFrame.fromJson(outputFile)
        assertEquals(df.size, exportedDf.size)
        assertEquals(df.keys, exportedDf.keys)
    }

    @Test
    fun `test JSON stream import and export`() {
        // Given
        val jsonString = """
            [
              {"name": "John", "age": 25, "city": "Seoul"},
              {"name": "Jane", "age": 30, "city": "Busan"}
            ]
        """.trimIndent()
        
        val inputStream = ByteArrayInputStream(jsonString.toByteArray(Charsets.UTF_8))
        
        // When
        val df = DataFrame.fromJson(inputStream)
        
        // Then
        assertEquals(2, df.size)
        assertEquals(setOf("name", "age", "city"), df.keys)
        
        // Test export to stream
        val outputStream = ByteArrayOutputStream()
        df.toJson(outputStream, prettyPrint = true)
        val exportedString = outputStream.toString(Charsets.UTF_8)
        assertTrue(exportedString.contains("John"))
        assertTrue(exportedString.contains("Jane"))
    }

    @Test
    fun `test DataFrame copy`() {
        // Given
        val df = DataFrame()
        df.addRow(mapOf("name" to "John", "age" to 25))
        df.addRow(mapOf("name" to "Jane", "age" to 30))
        
        // When
        val dfCopy = df.copy()
        
        // Then
        assertEquals(df.size, dfCopy.size)
        assertEquals(df.keys, dfCopy.keys)
        assertEquals(df.getData(0, "name"), dfCopy.getData(0, "name"))
        
        // Verify it's a deep copy
        dfCopy.setData(0, "name", "Modified")
        assertFalse(df.getData(0, "name") == dfCopy.getData(0, "name"))
    }

    @Test
    fun `test select rows`() {
        // Given
        val df = DataFrame()
        df.addRow(mapOf("name" to "John", "age" to 25))
        df.addRow(mapOf("name" to "Jane", "age" to 30))
        df.addRow(mapOf("name" to "Bob", "age" to 35))
        df.addRow(mapOf("name" to "Alice", "age" to 28))
        
        // When
        val selectedDf = df.selectRows(listOf(0, 2))
        
        // Then
        assertEquals(2, selectedDf.size)
        assertEquals("John", selectedDf.getData(0, "name"))
        assertEquals("Bob", selectedDf.getData(1, "name"))
    }

    @Test
    fun `test select columns`() {
        // Given
        val df = DataFrame()
        df.addRow(mapOf("name" to "John", "age" to 25, "city" to "Seoul"))
        df.addRow(mapOf("name" to "Jane", "age" to 30, "city" to "Busan"))
        
        // When
        val selectedDf = df.selectColumns(listOf("name", "age"))
        
        // Then
        assertEquals(2, selectedDf.size)
        assertEquals(setOf("name", "age"), selectedDf.keys)
        assertEquals("John", selectedDf.getData(0, "name"))
        assertEquals(25, selectedDf.getData(0, "age"))
        assertFalse(selectedDf.keys.contains("city"))
    }

    @Test
    fun `test save with auto format detection`(@TempDir tempDir: Path) {
        // Given
        val df = DataFrame()
        df.addRow(mapOf("name" to "John", "age" to 25))
        df.addRow(mapOf("name" to "Jane", "age" to 30))
        
        // When & Then
        val csvFile = tempDir.resolve("test.csv")
        df.save(csvFile)
        assertTrue(csvFile.toFile().exists())
        
        val jsonFile = tempDir.resolve("test.json")
        df.save(jsonFile)
        assertTrue(jsonFile.toFile().exists())
        
        val xlsxFile = tempDir.resolve("test.xlsx")
        df.save(xlsxFile)
        assertTrue(xlsxFile.toFile().exists())
    }

    @Test
    fun `test load with auto format detection`(@TempDir tempDir: Path) {
        // Given
        val csvContent = """
            name,age
            John,25
            Jane,30
        """.trimIndent()
        
        val csvFile = tempDir.resolve("test.csv")
        csvFile.toFile().writeText(csvContent)
        
        // When
        val df = DataFrame.load(csvFile)
        
        // Then
        assertEquals(2, df.size)
        assertEquals(setOf("name", "age"), df.keys)
    }

    @Test
    fun `test unsupported file extension`(@TempDir tempDir: Path) {
        // Given
        val unsupportedFile = tempDir.resolve("test.txt")
        unsupportedFile.toFile().writeText("test")
        
        val df = DataFrame()
        df.addRow(mapOf("name" to "John"))
        
        // When & Then
        assertThrows<IllegalArgumentException> {
            DataFrame.load(unsupportedFile)
        }
        
        assertThrows<IllegalArgumentException> {
            df.save(unsupportedFile)
        }
    }

    @Test
    fun `test save with options`(@TempDir tempDir: Path) {
        // Given
        val df = DataFrame()
        df.addRow(mapOf("name" to "John", "age" to 25))
        df.addRow(mapOf("name" to "Jane", "age" to 30))
        
        // When
        val csvFile = tempDir.resolve("test.csv")
        df.save(csvFile, mapOf(
            "delimiter" to ';',
            "includeHeader" to true
        ))
        
        val jsonFile = tempDir.resolve("test.json")
        df.save(jsonFile, mapOf(
            "prettyPrint" to true
        ))
        
        val xlsxFile = tempDir.resolve("test.xlsx")
        df.save(xlsxFile, mapOf(
            "sheetName" to "테스트 데이터"
        ))
        
        // Then
        assertTrue(csvFile.toFile().exists())
        assertTrue(jsonFile.toFile().exists())
        assertTrue(xlsxFile.toFile().exists())
    }
} 