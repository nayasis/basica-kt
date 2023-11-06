package com.github.nayasis.kotlin.basica.reflection

import com.github.nayasis.kotlin.basica.core.collection.toJson
import com.github.nayasis.kotlin.basica.core.localdate.format
import com.github.nayasis.kotlin.basica.core.localdate.toLocalDateTime
import com.github.nayasis.kotlin.basica.core.string.loadClass
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import mu.KotlinLogging
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.LinkedHashMap

private val log = KotlinLogging.logger {}

@Suppress("UNCHECKED_CAST")
internal class ReflectorTest: StringSpec({

    "to json" {

        val map1 = mapOf( "C" to "2021-01-01T23:59:59".toLocalDateTime(),3 to null, 2 to "two", "A" to LocalDateTime.now(),1 to "one", )
            .also { println("map1: $it") }
        val json = """{"C":"2021-03-26T15:16:31.154","2":"two","A":"2021-03-26T15:16:31.172","1":"one"}"""

        val map2 = Reflector.toMap(map1)
            .also { println("map2: $it") }

        val map3 = Reflector.toMap(json)
            .also { println("map3: $it") }

        val map4: Map<String,Any?> = Reflector.toMap(map1)
            .also { println("map4: $it") }

        val obj1 = Reflector.toObject<Dummy>(json)
            .also { println("obj1: $it") }

        obj1.C.format() shouldBe "2021-03-26T15:16:31.154"

    }

    "null mapping" {

        val map = mapOf( "A" to null, "B" to 1, "C" to "2021-01-01T23:59:59".toLocalDateTime() )
            .also { println("map: $it") }
        val json1 = Reflector.toJson(map)
            .also { println("json1: $it") }
        val json2 = Reflector.toJson(map,ignoreNull = false)
            .also { println("json2: $it") }

        val map1 = Reflector.toMap(json1)
            .also { println("map1: $it") }
        val map2 = Reflector.toMap(json2)
            .also { println("map2: $it") }

        map1.containsKey("A") shouldBe false
        map2.containsKey("A") shouldBe true

        val map3 = Reflector.toMap(map2)
            .also { println("map3: $it") }
        val map4 = Reflector.toMap(map2,ignoreNull = false)
            .also { println("map4: $it") }

        map3.containsKey("A") shouldBe false
        map4.containsKey("A") shouldBe true

    }

    "object -> json" {
        val person = Person("nayasis",45,"sungnam", null)
        Reflector.toJson(person) shouldBe """{"name":"nayasis","age":45,"address":"sungnam"}"""
        Reflector.toMap(person).toString() shouldBe "{name=nayasis, age=45, address=sungnam}"
    }

    "get functions" {
        val line = "-".repeat(100)
        println(line)
        "com.github.nayasis.kotlin.basica.core.validator.Validator".loadClass()
            .methods.forEach { println(it) }
        println(line)
        "com.github.nayasis.kotlin.basica.core.string.Strings".loadClass()
            .methods.forEach { println(it) }
        println(line)
        Reflector::class.java.methods.forEach { println(it) }
        println(line)
    }

    "key unsorted" {
        val map = mapOf(
            "c" to 1,
            "a" to "merong",
            "z" to "qqq",
            "b" to "b"
        ).also { println(">> map: $it") }

        val json = map.toJson().also { println(">> json: $it") }

        json shouldBe """
            {"c":1,"a":"merong","z":"qqq","b":"b"}
        """.trim()
    }

    "is json" {
        Reflector.isJson(null) shouldBe false
        Reflector.isJson("") shouldBe false
        Reflector.isJson("{}") shouldBe true
        Reflector.isJson("""
            {
              "a": "merong",
              "b": [1,2,3,4],
              "c": "20170304"
            }            
        """.trimIndent()) shouldBe true
        Reflector.isJson("{c}") shouldBe false
    }

    "merge map" {
        val map1 = mapOf(1 to 1, 2 to 2)
        val map2 = mapOf(3 to 3, 4 to 4)
        val merged = Reflector.merge(map1,map2).also { println(it) }
        (1..4).forEach { i ->
            merged[i] shouldBe i
        }
    }

    "merge List" {
        val list1 = listOf(0,1,2,3)
        val list2 = arrayOf(5,6,7,8,4)
        val merged = Reflector.merge(list1,list2).also { println(it.joinToString(",")) }
        for( i in 0..4 )
            merged[i] shouldBe i
    }

    "merge Map & List" {

        val map1 = mapOf(1 to 1, 2 to 2, 5 to mapOf("name" to "nayasis"))
        val map2 = mapOf(3 to 3, 4 to 4, 5 to listOf(
            mapOf("age" to 45),
            mapOf("name" to "jake", "age" to 11)
        ))
        val merged = Reflector.merge(map1,map2).also { println(it) }

        for( i in 1..4 )
            merged[i] shouldBe i

        val e5 = merged[5] as List<Map<String,Any>>

        e5[0]["name"]  shouldBe "nayasis"
        e5[0]["age"]   shouldBe 45
        e5[1]["name"]  shouldBe "jake"
        e5[1]["age"]   shouldBe 11

    }

    "merge Bean" {
        val p1 = Person("nayasis")
        val p2 = Person(age = 14, address = "sungnam")
        val merged = Reflector.merge(p2,p1).also { println(it) }
        merged.name    shouldBe "nayasis"
        merged.age     shouldBe 14
        merged.address shouldBe "sungnam"
    }

    "empty" {
        Reflector.toObject<Set<String>>("")          shouldBe emptySet()
        Reflector.toObject<Set<String>>(null)        shouldBe emptySet()
        Reflector.toObject<Map<String,String>>("")   shouldBe emptyMap()
        Reflector.toObject<Map<String,String>>(null) shouldBe emptyMap()
        Reflector.toObject<List<String>>("")         shouldBe emptyList()
        Reflector.toObject<List<String>>(null)       shouldBe emptyList()
    }

    "clone test" {
        val p1 = Person(name="nayasis", age = 14, address = "sungnam", etc = "merong")
        val p2 = Reflector.clone(p1)
        p1 shouldBe p2
    }

})

data class Dummy(
    val A: Date,
    val B: Int,
    val C: LocalDateTime,
    val D: String = "D",
)

data class Person(
    val name: String?    = null,
    val age: Int?        = null,
    val address: String? = null,
    val etc: String?     = null
)