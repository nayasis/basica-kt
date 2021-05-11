package com.github.nayasis.kotlin.basica.reflection

import com.github.nayasis.kotlin.basica.core.collection.toJson
import com.github.nayasis.kotlin.basica.core.localdate.toLocalDateTime
import com.github.nayasis.kotlin.basica.core.localdate.toStr
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.LinkedHashMap

internal class ReflectorTest {

    @Test
    fun toJson() {

        val map1 = mapOf( "C" to "2021-01-01T23:59:59".toLocalDateTime(),3 to null, 2 to "two", "A" to LocalDateTime.now(),1 to "one", )
        val json = """{"C":"2021-03-26T15:16:31.154","2":"two","A":"2021-03-26T15:16:31.172","1":"one"}"""

        println( Reflector.toJson(map1) )

        val map2 = Reflector.toMap(map1)
        println( map2 )
        var map3 = Reflector.toMap(json)
        println( map3 )
        val map4: Map<String,Any?> = Reflector.toMap(map1)
        println( map4 )
        var obj1 = Reflector.toObject<Dummy>(json)
        print(obj1)

        assertEquals( "2021-03-26T15:16:31.154", obj1.C.toStr() )

    }

    @Test
    fun nullMapping() {

        val map = mapOf( "A" to null, "B" to 1, "C" to "2021-01-01T23:59:59".toLocalDateTime(), "D" to null );

        val json1 = Reflector.toJson(map)
        val json2 = Reflector.toJson(map,ignoreNull = false)

        println(map)

        println("convert 1-1 : $json1 -> ${Reflector.toMap(json1)}")
        println("convert 1-2 : $json1 -> ${Reflector.toMap(json1,ignoreNull = false)}")
        println("convert 1-3 : $json2 -> ${Reflector.toMap(json2)}")
        println("convert 1-4 : $json2 -> ${Reflector.toMap(json2,ignoreNull = false)}")

        val map1 = Reflector.toMap(json1)
        val map2 = Reflector.toMap(json2)
        assertTrue( ! map1.containsKey("D") && map1["D"] == null )
        assertTrue(   map2.containsKey("D") && map2["D"] == null )

        println( "convert 2-1 : $map1 -> ${Reflector.toMap(map1)}" )
        println( "convert 2-2 : $map1 -> ${Reflector.toMap(map1,ignoreNull = false)}" )
        println( "convert 2-3 : $map2 -> ${Reflector.toMap(map2)}" )
        println( "convert 2-4 : $map2 -> ${Reflector.toMap(map2,ignoreNull = false)}" )

        val map3 = Reflector.toMap(map2)
        val map4 = Reflector.toMap(map2,ignoreNull = false)
        assertTrue( ! map3.containsKey("D") && map3["D"] == null )
        assertTrue(   map4.containsKey("D") && map4["D"] == null )

    }

    @Test
    fun toJsonFromObject() {
        val person = Person("nayasis",45,"sungnam", null)
        println( Reflector.toJson(person))
        println( Reflector.toMap(person) )
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun getFunctions() {
        var line = "-".repeat(100)
        println(line)
        javaClass.classLoader.loadClass("com.github.nayasis.kotlin.basica.core.Validator")
            .methods.forEach { println(it) }
        println(line)
        javaClass.classLoader.loadClass("com.github.nayasis.kotlin.basica.core.Strings")
            .methods.forEach { println(it) }
        println(line)
        Reflector::class.java.methods.forEach { println(it) }
        println(line)
    }

    @Test
    fun keyUnsorted() {

        var map = LinkedHashMap<String,Any>()
        map["c"] = 1
        map["a"] = "merong"
        map["z"] = "qqq"
        map["b"] = "b"

        println(map)

        val json = map.toJson()
        println(json)

        assertEquals( "{\"c\":1,\"a\":\"merong\",\"z\":\"qqq\",\"b\":\"b\"}", json )

    }

    @Test
    fun isJson() {
        assertFalse( Reflector.isJson(null) )
        assertFalse( Reflector.isJson("") )
        assertTrue( Reflector.isJson("{}") )
        assertTrue( Reflector.isJson("""
            {
              "a": "merong",
              "b": [1,2,3,4],
              "c": "20170304"
            }            
        """.trimIndent()) )
        assertFalse( Reflector.isJson("{c}") )
    }

}

data class Dummy(
    val A: Date,
    val B: Int,
    val C: LocalDateTime,
    val D: String = "D",
)

data class Person(
    val name: String,
    val age: Int,
    val address: String,
    val etc: String?
)