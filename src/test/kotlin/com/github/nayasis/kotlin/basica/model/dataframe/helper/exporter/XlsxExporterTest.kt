package com.github.nayasis.kotlin.basica.model.dataframe.helper.exporter

import com.github.nayasis.kotlin.basica.core.character.Characters
import com.github.nayasis.kotlin.basica.core.io.Path
import com.github.nayasis.kotlin.basica.core.io.Paths
import com.github.nayasis.kotlin.basica.core.localdate.toCalendar
import com.github.nayasis.kotlin.basica.core.localdate.toDate
import com.github.nayasis.kotlin.basica.core.localdate.toLocalDate
import com.github.nayasis.kotlin.basica.core.localdate.toLocalDateTime
import com.github.nayasis.kotlin.basica.core.localdate.toZonedDateTime
import com.github.nayasis.kotlin.basica.core.string.toPath
import com.github.nayasis.kotlin.basica.model.dataframe.DataFrame
import com.github.nayasis.kotlin.basica.model.dataframe.helper.importer.CsvImporter
import com.github.nayasis.kotlin.basica.model.dataframe.helper.importer.JsonImporter
import com.github.nayasis.kotlin.basica.model.dataframe.helper.importer.OdsImporter
import com.github.nayasis.kotlin.basica.model.dataframe.helper.importer.XlsxImporter
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

private val logger = KotlinLogging.logger {}

internal class XlsxExporterTest : StringSpec({

    val testDir = Paths.userHome.resolve("basica-test/dataframe")

    Characters.fullwidth = 2.0

    "create basic XLSX file" {
        val filePath = testDir.resolve("text.xlsx")
        val testdata = createTestDataframe().also { logger.debug { "\n${it.toString(showIndex = true)}" } }
        XlsxExporter(testdata).export(filePath)
        XlsxImporter().import(filePath).let { dataframe -> logger.debug { "\n${dataframe.toString(showIndex = true)}" } }
    }

    "create basic CSV file" {
        val filePath = testDir.resolve("text.csv")
        val testdata = createTestDataframe().also { logger.debug { "\n${it.toString(showIndex = true)}" } }
        CsvExporter(testdata).export(filePath)
        CsvImporter().import(filePath).let { dataframe -> logger.debug { "\n${dataframe.toString(showIndex = true)}" } }
    }

    "create basic JSON file" {
        val filePath = testDir.resolve("text.json")
        val testdata = createTestDataframe().also { logger.debug { "\n${it.toString(showIndex = true)}" } }
        JsonExporter(testdata).export(filePath)
        JsonImporter().import(filePath).let { dataframe -> logger.debug { "\n${dataframe.toString(showIndex = true)}" } }
    }

    "create basic ODS file" {
        val filePath = testDir.resolve("text.ods")
        val testdata = createTestDataframe().also { logger.debug { "\n${it.toString(showIndex = true)}" } }
        OdsExporter(testdata).export(filePath)
        OdsImporter().import(filePath).let { dataframe -> logger.debug {
            "\n${dataframe.toString(showIndex = true)}" }
        }
    }

//    "헤더 없이 XLSX 내보내기" {
//        val dataframe = createTestDataframe()
//        val outputStream = ByteArrayOutputStream()
//
//        XlsxExporter(dataframe, showLabel = false).export(outputStream)
//
//        val bytes = outputStream.toByteArray()
//        bytes.size shouldNotBe 0
//
//        // sharedStrings.xml에서 헤더가 key, val로 되어있는지 확인
//        val zipInput = ZipInputStream(bytes.inputStream())
//        var entry = zipInput.nextEntry
//        while (entry != null) {
//            if (entry.name == "xl/sharedStrings.xml") {
//                val content = zipInput.readAllBytes().toString(Charsets.UTF_8)
//                content shouldBe content.contains("key") && content.contains("val")
//                break
//            }
//            entry = zipInput.nextEntry
//        }
//    }
//
//    "빈 데이터프레임 XLSX 내보내기" {
//        val dataframe = DataFrame()
//        val outputStream = ByteArrayOutputStream()
//
//        XlsxExporter(dataframe).export(outputStream)
//
//        val bytes = outputStream.toByteArray()
//        bytes.size shouldNotBe 0
//
//        // ZIP 파일 구조 확인
//        val zipInput = ZipInputStream(bytes.inputStream())
//        val entries = mutableListOf<String>()
//        var entry = zipInput.nextEntry
//        while (entry != null) {
//            entries.add(entry.name)
//            entry = zipInput.nextEntry
//        }
//
//        entries shouldBe listOf("[Content_Types].xml", "_rels/.rels", "xl/workbook.xml", "xl/_rels/workbook.xml.rels", "xl/sharedStrings.xml", "xl/styles.xml", "xl/worksheets/sheet1.xml")
//    }
//
//    "null 값이 포함된 XLSX 내보내기" {
//        val dataframe = DataFrame().apply {
//            addRow(mapOf("name" to "A", "age" to 1))
//            addRow(mapOf("name" to null, "age" to 2))
//            addRow(mapOf("name" to "C", "age" to null))
//        }
//        val outputStream = ByteArrayOutputStream()
//
//        XlsxExporter(dataframe).export(outputStream)
//
//        val bytes = outputStream.toByteArray()
//        bytes.size shouldNotBe 0
//    }
//
//    "다양한 데이터 타입 XLSX 내보내기" {
//        val dataframe = DataFrame().apply {
//            addRow(mapOf("string" to "text", "number" to 123, "boolean" to true, "null" to null))
//            addRow(mapOf("string" to "another", "number" to 456.78, "boolean" to false, "null" to null))
//        }
//        val outputStream = ByteArrayOutputStream()
//
//        XlsxExporter(dataframe).export(outputStream)
//
//        val bytes = outputStream.toByteArray()
//        bytes.size shouldNotBe 0
//    }
//
//    "startIndex를 사용한 XLSX 내보내기" {
//        val dataframe = DataFrame().apply {
//            addRow(mapOf("name" to "A", "age" to 1))
//            addRow(mapOf("name" to "B", "age" to 2))
//            addRow(mapOf("name" to "C", "age" to 3))
//        }
//        val outputStream = ByteArrayOutputStream()
//
//        XlsxExporter(dataframe, startIndex = 1).export(outputStream)
//
//        val bytes = outputStream.toByteArray()
//        bytes.size shouldNotBe 0
//    }
//
//    "사용자 정의 시트명으로 XLSX 내보내기" {
//        val dataframe = createTestDataframe()
//        val outputStream = ByteArrayOutputStream()
//
//        XlsxExporter(dataframe, sheetName = "TestSheet").export(outputStream)
//
//        val bytes = outputStream.toByteArray()
//        bytes.size shouldNotBe 0
//
//        // workbook.xml에서 시트명 확인
//        val zipInput = ZipInputStream(bytes.inputStream())
//        var entry = zipInput.nextEntry
//        while (entry != null) {
//            if (entry.name == "xl/workbook.xml") {
//                val content = zipInput.readAllBytes().toString(Charsets.UTF_8)
//                content shouldBe content.contains("TestSheet")
//                break
//            }
//            entry = zipInput.nextEntry
//        }
//    }
//
//    "Content Types 확인" {
//        val dataframe = createTestDataframe()
//        val outputStream = ByteArrayOutputStream()
//
//        XlsxExporter(dataframe).export(outputStream)
//
//        val bytes = outputStream.toByteArray()
//        val zipInput = ZipInputStream(bytes.inputStream())
//        var entry = zipInput.nextEntry
//        while (entry != null) {
//            if (entry.name == "[Content_Types].xml") {
//                val content = zipInput.readAllBytes().toString(Charsets.UTF_8)
//                content shouldBe content.contains("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml")
//                content shouldBe content.contains("application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml")
//                content shouldBe content.contains("application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml")
//                content shouldBe content.contains("application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml")
//                break
//            }
//            entry = zipInput.nextEntry
//        }
//    }
//
//    "Shared Strings 구조 확인" {
//        val dataframe = createTestDataframe()
//        val outputStream = ByteArrayOutputStream()
//
//        XlsxExporter(dataframe).export(outputStream)
//
//        val bytes = outputStream.toByteArray()
//        val zipInput = ZipInputStream(bytes.inputStream())
//        var entry = zipInput.nextEntry
//        while (entry != null) {
//            if (entry.name == "xl/sharedStrings.xml") {
//                val content = zipInput.readAllBytes().toString(Charsets.UTF_8)
//                content shouldBe content.contains("<sst")
//                content shouldBe content.contains("</sst>")
//                content shouldBe content.contains("<si>")
//                content shouldBe content.contains("</si>")
//                break
//            }
//            entry = zipInput.nextEntry
//        }
//    }
//
//    "Worksheet 구조 확인" {
//        val dataframe = createTestDataframe()
//        val outputStream = ByteArrayOutputStream()
//
//        XlsxExporter(dataframe).export(outputStream)
//
//        val bytes = outputStream.toByteArray()
//        val zipInput = ZipInputStream(bytes.inputStream())
//        var entry = zipInput.nextEntry
//        while (entry != null) {
//            if (entry.name == "xl/worksheets/sheet1.xml") {
//                val content = zipInput.readAllBytes().toString(Charsets.UTF_8)
//                content shouldBe content.contains("<worksheet")
//                content shouldBe content.contains("</worksheet>")
//                content shouldBe content.contains("<sheetData>")
//                content shouldBe content.contains("</sheetData>")
//                content shouldBe content.contains("<row")
//                content shouldBe content.contains("</row>")
//                break
//            }
//            entry = zipInput.nextEntry
//        }
//    }
//
//    "ZIP 파일 구조 검증" {
//        val dataframe = createTestDataframe()
//        val outputStream = ByteArrayOutputStream()
//
//        XlsxExporter(dataframe).export(outputStream)
//
//        val bytes = outputStream.toByteArray()
//        val zipInput = ZipInputStream(bytes.inputStream())
//
//        val expectedEntries = listOf(
//            "[Content_Types].xml",
//            "_rels/.rels",
//            "xl/workbook.xml",
//            "xl/_rels/workbook.xml.rels",
//            "xl/sharedStrings.xml",
//            "xl/styles.xml",
//            "xl/worksheets/sheet1.xml"
//        )
//        val actualEntries = mutableListOf<String>()
//
//        var entry = zipInput.nextEntry
//        while (entry != null) {
//            actualEntries.add(entry.name)
//            entry = zipInput.nextEntry
//        }
//
//        actualEntries shouldBe expectedEntries
//    }
//
//    "복잡한 데이터 XLSX 내보내기" {
//        val dataframe = DataFrame().apply {
//            addRow(mapOf("name" to "Alice", "age" to 25, "city" to "Seoul"))
//            addRow(mapOf("name" to "Bob", "age" to 30, "city" to "Busan"))
//            addRow(mapOf("name" to "Charlie", "age" to 35, "city" to "Incheon"))
//        }
//        val outputStream = ByteArrayOutputStream()
//
//        XlsxExporter(dataframe).export(outputStream)
//
//        val bytes = outputStream.toByteArray()
//        bytes.size shouldNotBe 0
//    }

})


fun createTestDataframe(): DataFrame {
    return DataFrame().apply {
        setRow(10, mapOf("key" to "controller", "val" to "컨트롤러는 이런 것입니다."))
        setRow(13, mapOf("key" to 1, "val" to 3359))
        setRow(14, mapOf("key" to "LocalDate", "val" to "2025-07-10".toLocalDate()))
        setRow(15, mapOf("key" to "날짜시간", "val" to "2025-07-10".toLocalDateTime()))
        setRow(16, mapOf("key" to "date", "val" to "2025-07-10".toDate()))
        setRow(17, mapOf("key" to "날짜시간", "val" to "2025-07-10 11:23:49".toDate()))
        setRow(18, mapOf("key" to "Calendar", "val" to "2025-07-10 14:30:25".toCalendar()))
        setRow(19, mapOf("key" to "ZonedDateTime", "val" to "2025-07-10T15:45:30+09:00".toZonedDateTime()))
        setLabel("key", "이것은 KEY 입니다.")
        setLabel("val", "これは VALUE です")
    }
}