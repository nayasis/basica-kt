package com.github.nayasis.kotlin.basica.model.dataframe.helper.exporter

import com.github.nayasis.kotlin.basica.model.dataframe.DataFrame
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.io.ByteArrayOutputStream

private val logger = KotlinLogging.logger {}

internal class CsvExporterTest : StringSpec({

    fun createTestDataframe(): DataFrame {
        return DataFrame().apply {
            addRow(mapOf("key" to "controller", "val" to "컨트롤러는 이런 것입니다."))
            addRow(mapOf("key" to 1, "val" to 3359))
            setLabel("key", "이것은 KEY 입니다.")
            setLabel("val", "これは VALUE です")
        }
    }

    "기본 CSV 내보내기" {
        val dataframe = createTestDataframe()
        val outputStream = ByteArrayOutputStream()
        
        CsvExporter(dataframe).export(outputStream)

        val result = outputStream.toString("UTF-8")
        result shouldBe """이것은 KEY 입니다.,これは VALUE です
controller,컨트롤러는 이런 것입니다.
1,3359
"""
    }

    "헤더 없이 CSV 내보내기" {
        val dataframe = createTestDataframe()
        val outputStream = ByteArrayOutputStream()
        
        CsvExporter(dataframe, showLabel = false).export(outputStream)
        
        val result = outputStream.toString("UTF-8")
        result shouldBe """key,val
controller,컨트롤러는 이런 것입니다.
1,3359
"""
    }

    "다른 구분자로 CSV 내보내기" {
        val dataframe = createTestDataframe()
        val outputStream = ByteArrayOutputStream()
        
        CsvExporter(dataframe, delimiter = ";").export(outputStream)
        
        val result = outputStream.toString("UTF-8")
        result shouldBe """이것은 KEY 입니다.;これは VALUE です
controller;컨트롤러는 이런 것입니다.
1;3359
"""
    }

    "빈 데이터프레임 CSV 내보내기" {
        val dataframe = DataFrame()
        val outputStream = ByteArrayOutputStream()
        
        CsvExporter(dataframe).export(outputStream)
        
        val result = outputStream.toString("UTF-8")
        result shouldBe ""
    }

    "null 값이 포함된 CSV 내보내기" {
        val dataframe = DataFrame().apply {
            addRow(mapOf("name" to "A", "age" to 1))
            addRow(mapOf("name" to null, "age" to 2))
            addRow(mapOf("name" to "C", "age" to null))
        }
        val outputStream = ByteArrayOutputStream()
        
        CsvExporter(dataframe).export(outputStream)
        
        val result = outputStream.toString("UTF-8")
        result shouldBe """name,age
A,1
,2
C,
"""
    }

    "특수문자가 포함된 CSV 내보내기" {
        val dataframe = DataFrame().apply {
            addRow(mapOf("text" to "Hello,World", "number" to 1))
            addRow(mapOf("text" to "Hello\"World", "number" to 2))
        }
        val outputStream = ByteArrayOutputStream()
        
        CsvExporter(dataframe).export(outputStream)
        
        val result = outputStream.toString("UTF-8")
        result shouldBe """text,number
"Hello,World",1
"Hello""World",2
"""
    }

    "startIndex를 사용한 CSV 내보내기" {
        val dataframe = DataFrame().apply {
            addRow(mapOf("name" to "A", "age" to 1))
            addRow(mapOf("name" to "B", "age" to 2))
            addRow(mapOf("name" to "C", "age" to 3))
        }
        val outputStream = ByteArrayOutputStream()
        
        CsvExporter(dataframe, startIndex = 1).export(outputStream)
        
        val result = outputStream.toString("UTF-8")
        result shouldBe """name,age
B,2
C,3
"""
    }

})