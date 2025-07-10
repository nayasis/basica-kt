package com.github.nayasis.kotlin.basica.model.dataframe.helper.exporter

import com.github.nayasis.kotlin.basica.model.dataframe.DataFrame
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.io.ByteArrayOutputStream

internal class JsonExporterTest : StringSpec({

    fun createTestDataframe(): DataFrame {
        return DataFrame().apply {
            addRow(mapOf("key" to "controller", "val" to "컨트롤러는 이런 것입니다."))
            addRow(mapOf("key" to 1, "val" to 3359))
        }
    }

    "기본 JSON 내보내기" {
        val dataframe = createTestDataframe()
        val outputStream = ByteArrayOutputStream()
        
        JsonExporter(dataframe).export(outputStream)
        
        val result = outputStream.toString("UTF-8")
        result shouldBe """[{"key":"controller","val":"컨트롤러는 이런 것입니다."},{"key":1,"val":3359}]"""
    }

    "pretty print JSON 내보내기" {
        val dataframe = createTestDataframe()
        val outputStream = ByteArrayOutputStream()
        
        JsonExporter(dataframe, prettyPrint = true).export(outputStream)
        
        val result = outputStream.toString("UTF-8")
        result shouldBe """[
{"key":"controller","val":"컨트롤러는 이런 것입니다."},
{"key":1,"val":3359}
]"""
    }

    "빈 데이터프레임 JSON 내보내기" {
        val dataframe = DataFrame()
        val outputStream = ByteArrayOutputStream()
        
        JsonExporter(dataframe).export(outputStream)
        
        val result = outputStream.toString("UTF-8")
        result shouldBe "[]"
    }

    "null 값이 포함된 JSON 내보내기" {
        val dataframe = DataFrame().apply {
            addRow(mapOf("name" to "A", "age" to 1))
            addRow(mapOf("name" to null, "age" to 2))
            addRow(mapOf("name" to "C", "age" to null))
        }
        val outputStream = ByteArrayOutputStream()
        
        JsonExporter(dataframe).export(outputStream)
        
        val result = outputStream.toString("UTF-8")
        result shouldBe """[{"name":"A","age":1},{"name":null,"age":2},{"name":"C","age":null}]"""
    }

    "다양한 데이터 타입 JSON 내보내기" {
        val dataframe = DataFrame().apply {
            addRow(mapOf("string" to "text", "number" to 123, "boolean" to true, "null" to null))
            addRow(mapOf("string" to "another", "number" to 456.78, "boolean" to false, "null" to null))
        }
        val outputStream = ByteArrayOutputStream()
        
        JsonExporter(dataframe).export(outputStream)
        
        val result = outputStream.toString("UTF-8")
        result shouldBe """[{"string":"text","number":123,"boolean":true,"null":null},{"string":"another","number":456.78,"boolean":false,"null":null}]"""
    }

    "startIndex를 사용한 JSON 내보내기" {
        val dataframe = DataFrame().apply {
            addRow(mapOf("name" to "A", "age" to 1))
            addRow(mapOf("name" to "B", "age" to 2))
            addRow(mapOf("name" to "C", "age" to 3))
        }
        val outputStream = ByteArrayOutputStream()
        
        JsonExporter(dataframe, startIndex = 1).export(outputStream)
        
        val result = outputStream.toString("UTF-8")
        result shouldBe """[{"name":"B","age":2},{"name":"C","age":3}]"""
    }

    "startIndex와 pretty print 조합" {
        val dataframe = DataFrame().apply {
            addRow(mapOf("name" to "A", "age" to 1))
            addRow(mapOf("name" to "B", "age" to 2))
            addRow(mapOf("name" to "C", "age" to 3))
        }
        val outputStream = ByteArrayOutputStream()
        
        JsonExporter(dataframe, prettyPrint = true, startIndex = 1).export(outputStream)
        
        val result = outputStream.toString("UTF-8")
        result shouldBe """[
{"name":"B","age":2},
{"name":"C","age":3}
]"""
    }

    "복잡한 객체 JSON 내보내기" {
        data class Person(val name: String, val age: Int)
        
        val dataframe = DataFrame().apply {
            addRow(mapOf("person" to Person("Alice", 25)))
            addRow(mapOf("person" to Person("Bob", 30)))
        }
        val outputStream = ByteArrayOutputStream()
        
        JsonExporter(dataframe).export(outputStream)
        
        val result = outputStream.toString("UTF-8")
        result shouldBe """[{"person":{"name":"Alice","age":25}},{"person":{"name":"Bob","age":30}}]"""
    }


}) 