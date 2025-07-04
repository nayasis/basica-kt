package com.github.nayasis.kotlin.basica.model.dataframe.helper.exporter

import com.github.nayasis.kotlin.basica.model.dataframe.DataFrame
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

internal class OdsExporterTest : StringSpec({

    "기본 ODS 내보내기" {
        val dataframe = createTestDataframe()
        val outputStream = ByteArrayOutputStream()
        
        OdsExporter(dataframe).export(outputStream)
        
        val bytes = outputStream.toByteArray()
        bytes.size shouldNotBe 0
        
        // ZIP 파일 구조 확인
        val zipInput = ZipInputStream(bytes.inputStream())
        val entries = mutableListOf<String>()
        var entry = zipInput.nextEntry
        while (entry != null) {
            entries.add(entry.name)
            entry = zipInput.nextEntry
        }
        
        entries shouldBe listOf("mimetype", "meta.xml", "settings.xml", "styles.xml", "content.xml")
    }

//    "헤더 없이 ODS 내보내기" {
//        val dataframe = createTestDataframe()
//        val outputStream = ByteArrayOutputStream()
//
//        OdsExporter(dataframe, showLabel = false).export(outputStream)
//
//        val bytes = outputStream.toByteArray()
//        bytes.size shouldNotBe 0
//
//        // content.xml에서 헤더가 key, val로 되어있는지 확인
//        val zipInput = ZipInputStream(bytes.inputStream())
//        var entry = zipInput.nextEntry
//        while (entry != null) {
//            if (entry.name == "content.xml") {
//                val content = zipInput.readAllBytes().toString(Charsets.UTF_8)
//                content shouldBe content.contains("key") && content.contains("val")
//                break
//            }
//            entry = zipInput.nextEntry
//        }
//    }
//
//    "빈 데이터프레임 ODS 내보내기" {
//        val dataframe = DataFrame()
//        val outputStream = ByteArrayOutputStream()
//
//        OdsExporter(dataframe).export(outputStream)
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
//        entries shouldBe listOf("mimetype", "meta.xml", "settings.xml", "styles.xml", "content.xml")
//    }
//
//    "null 값이 포함된 ODS 내보내기" {
//        val dataframe = DataFrame().apply {
//            addRow(mapOf("name" to "A", "age" to 1))
//            addRow(mapOf("name" to null, "age" to 2))
//            addRow(mapOf("name" to "C", "age" to null))
//        }
//        val outputStream = ByteArrayOutputStream()
//
//        OdsExporter(dataframe).export(outputStream)
//
//        val bytes = outputStream.toByteArray()
//        bytes.size shouldNotBe 0
//    }
//
//    "다양한 데이터 타입 ODS 내보내기" {
//        val dataframe = DataFrame().apply {
//            addRow(mapOf("string" to "text", "number" to 123, "boolean" to true, "null" to null))
//            addRow(mapOf("string" to "another", "number" to 456.78, "boolean" to false, "null" to null))
//        }
//        val outputStream = ByteArrayOutputStream()
//
//        OdsExporter(dataframe).export(outputStream)
//
//        val bytes = outputStream.toByteArray()
//        bytes.size shouldNotBe 0
//    }
//
//    "startIndex를 사용한 ODS 내보내기" {
//        val dataframe = DataFrame().apply {
//            addRow(mapOf("name" to "A", "age" to 1))
//            addRow(mapOf("name" to "B", "age" to 2))
//            addRow(mapOf("name" to "C", "age" to 3))
//        }
//        val outputStream = ByteArrayOutputStream()
//
//        OdsExporter(dataframe, startIndex = 1).export(outputStream)
//
//        val bytes = outputStream.toByteArray()
//        bytes.size shouldNotBe 0
//    }
//
//    "사용자 정의 시트명으로 ODS 내보내기" {
//        val dataframe = createTestDataframe()
//        val outputStream = ByteArrayOutputStream()
//
//        OdsExporter(dataframe, sheetName = "TestSheet").export(outputStream)
//
//        val bytes = outputStream.toByteArray()
//        bytes.size shouldNotBe 0
//
//        // content.xml에서 시트명 확인
//        val zipInput = ZipInputStream(bytes.inputStream())
//        var entry = zipInput.nextEntry
//        while (entry != null) {
//            if (entry.name == "content.xml") {
//                val content = zipInput.readAllBytes().toString(Charsets.UTF_8)
//                content shouldBe content.contains("TestSheet")
//                break
//            }
//            entry = zipInput.nextEntry
//        }
//    }
//
//    "MIME 타입 확인" {
//        val dataframe = createTestDataframe()
//        val outputStream = ByteArrayOutputStream()
//
//        OdsExporter(dataframe).export(outputStream)
//
//        val bytes = outputStream.toByteArray()
//        val zipInput = ZipInputStream(bytes.inputStream())
//        var entry = zipInput.nextEntry
//        while (entry != null) {
//            if (entry.name == "mimetype") {
//                val mimeType = zipInput.readAllBytes().toString(Charsets.UTF_8)
//                mimeType shouldBe "application/vnd.oasis.opendocument.spreadsheet"
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
//        OdsExporter(dataframe).export(outputStream)
//
//        val bytes = outputStream.toByteArray()
//        val zipInput = ZipInputStream(bytes.inputStream())
//
//        val expectedEntries = listOf("mimetype", "meta.xml", "settings.xml", "styles.xml", "content.xml")
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

})