package com.github.nayasis.kotlin.basica.model.dataframe

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

class DataFrameExporterImporterTest : FunSpec({

    val tempDir = Files.createTempDirectory("dataframe_test")
    
    afterTest {
        // 테스트 후 임시 파일들 정리
        if (tempDir.exists()) {
            Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
        }
    }

    test("CSV export and import") {
        val df = createTestDataFrame()
        
        val csvFile = tempDir.resolve("test.csv")
        df.exportToCsv(csvFile)
        
        csvFile.exists() shouldBe true
        
        val importedDf = DataFrame.fromCsv(csvFile)
        
        importedDf.size shouldBe df.size
        importedDf.keys shouldBe df.keys
        
        // 데이터 검증
        for (rowIndex in 0 until df.size) {
            for (key in df.keys) {
                importedDf.getData(rowIndex, key) shouldBe df.getData(rowIndex, key)
            }
        }
    }

    test("CSV string export and import") {
        val df = createTestDataFrame()
        
        val csvString = df.exportToCsvString()
        csvString shouldNotBe null
        csvString.isNotEmpty() shouldBe true
        
        val importedDf = DataFrame.fromCsvString(csvString)
        
        importedDf.size shouldBe df.size
        importedDf.keys shouldBe df.keys
    }

    test("JSON export and import") {
        val df = createTestDataFrame()
        
        val jsonFile = tempDir.resolve("test.json")
        df.exportToJson(jsonFile)
        
        jsonFile.exists() shouldBe true
        
        val importedDf = DataFrame.fromJson(jsonFile)
        
        importedDf.size shouldBe df.size
        importedDf.keys shouldBe df.keys
        
        // 데이터 검증
        for (rowIndex in 0 until df.size) {
            for (key in df.keys) {
                importedDf.getData(rowIndex, key) shouldBe df.getData(rowIndex, key)
            }
        }
    }

    test("JSON string export and import") {
        val df = createTestDataFrame()
        
        val jsonString = df.exportToJsonString(prettyPrint = true)
        jsonString shouldNotBe null
        jsonString.isNotEmpty() shouldBe true
        
        val importedDf = DataFrame.fromJsonString(jsonString)
        
        importedDf.size shouldBe df.size
        importedDf.keys shouldBe df.keys
    }

    test("XLSX export and import") {
        val df = createTestDataFrame()
        
        val xlsxFile = tempDir.resolve("test.xlsx")
        df.exportToXlsx(xlsxFile)
        
        xlsxFile.exists() shouldBe true
        
        val importedDf = DataFrame.fromXlsx(xlsxFile)
        
        importedDf.size shouldBe df.size
        importedDf.keys shouldBe df.keys
        
        // 데이터 검증
        for (rowIndex in 0 until df.size) {
            for (key in df.keys) {
                val originalValue = df.getData(rowIndex, key)
                val importedValue = importedDf.getData(rowIndex, key)
                
                // 숫자 타입의 경우 정확한 비교가 어려울 수 있으므로 문자열로 비교
                originalValue.toString() shouldBe importedValue.toString()
            }
        }
    }

    test("ODS export and import") {
        val df = createTestDataFrame()
        
        val odsFile = tempDir.resolve("test.ods")
        df.exportToOds(odsFile)
        
        odsFile.exists() shouldBe true
        
        val importedDf = DataFrame.fromOds(odsFile)
        
        importedDf.size shouldBe df.size
        importedDf.keys shouldBe df.keys
        
        // 데이터 검증
        for (rowIndex in 0 until df.size) {
            for (key in df.keys) {
                val originalValue = df.getData(rowIndex, key)
                val importedValue = importedDf.getData(rowIndex, key)
                
                // 숫자 타입의 경우 정확한 비교가 어려울 수 있으므로 문자열로 비교
                originalValue.toString() shouldBe importedValue.toString()
            }
        }
    }

    test("Stream export and import") {
        val df = createTestDataFrame()
        
        // CSV 스트림 테스트
        val csvFile = tempDir.resolve("stream_test.csv")
        Files.newOutputStream(csvFile).use { stream ->
            df.exportToStream(stream, "csv", mapOf("delimiter" to ';', "hasQuotes" to false))
        }
        
        csvFile.exists() shouldBe true
        
        val importedCsvDf = DataFrame.fromStream(Files.newInputStream(csvFile), "csv", mapOf("delimiter" to ';', "hasQuotes" to false))
        importedCsvDf.size shouldBe df.size
        
        // JSON 스트림 테스트
        val jsonFile = tempDir.resolve("stream_test.json")
        Files.newOutputStream(jsonFile).use { stream ->
            df.exportToStream(stream, "json", mapOf("prettyPrint" to true))
        }
        
        jsonFile.exists() shouldBe true
        
        val importedJsonDf = DataFrame.fromStream(Files.newInputStream(jsonFile), "json")
        importedJsonDf.size shouldBe df.size
    }

    test("Large dataset CSV streaming") {
        val df = createLargeTestDataFrame(10000) // 10,000행
        
        val csvFile = tempDir.resolve("large_test.csv")
        df.exportToCsv(csvFile)
        
        csvFile.exists() shouldBe true
        
        val importedDf = DataFrame.fromCsv(csvFile)
        
        importedDf.size shouldBe df.size
        importedDf.keys shouldBe df.keys
    }

    test("DataFrame with labels") {
        val df = DataFrame()
        df.setLabel("col_0", "이름")
        df.setLabel("col_1", "나이")
        df.setLabel("col_2", "직업")
        
        df.setData(0, "col_0", "김철수")
        df.setData(0, "col_1", 25)
        df.setData(0, "col_2", "개발자")
        
        df.setData(1, "col_0", "이영희")
        df.setData(1, "col_1", 30)
        df.setData(1, "col_2", "디자이너")
        
        val csvFile = tempDir.resolve("labeled_test.csv")
        df.exportToCsv(csvFile)
        
        val importedDf = DataFrame.fromCsv(csvFile)
        
        importedDf.getLabel("col_0") shouldBe "이름"
        importedDf.getLabel("col_1") shouldBe "나이"
        importedDf.getLabel("col_2") shouldBe "직업"
    }

    private fun createTestDataFrame(): DataFrame {
        val df = DataFrame()
        
        // 테스트 데이터 추가
        df.setData(0, "name", "Alice")
        df.setData(0, "age", 25)
        df.setData(0, "city", "Seoul")
        df.setData(0, "salary", 50000.0)
        df.setData(0, "active", true)
        
        df.setData(1, "name", "Bob")
        df.setData(1, "age", 30)
        df.setData(1, "city", "Busan")
        df.setData(1, "salary", 60000.0)
        df.setData(1, "active", false)
        
        df.setData(2, "name", "Charlie")
        df.setData(2, "age", 35)
        df.setData(2, "city", "Incheon")
        df.setData(2, "salary", 70000.0)
        df.setData(2, "active", true)
        
        return df
    }

    private fun createLargeTestDataFrame(rowCount: Int): DataFrame {
        val df = DataFrame()
        
        for (i in 0 until rowCount) {
            df.setData(i, "id", i)
            df.setData(i, "name", "User$i")
            df.setData(i, "value", i * 1.5)
            df.setData(i, "active", i % 2 == 0)
        }
        
        return df
    }
}) 