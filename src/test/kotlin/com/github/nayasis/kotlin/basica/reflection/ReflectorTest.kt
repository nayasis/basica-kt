package com.github.nayasis.kotlin.basica.reflection

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class ReflectorTest {

    val mapper = jacksonObjectMapper()

    @Test
    fun toJson() {

        val map  =  mapOf( "C" to Date(),3 to null, 2 to "two", "A" to LocalDateTime.now(),1 to "one", )
        val json = """{"C":"2021-03-26T15:16:31.154","2":"two","A":"2021-03-26T15:16:31.172","1":"one"}"""

        println( Reflector.toJson(map) )
        assertEquals(81, Reflector.toJson(map).length )

        val map2 = Reflector.toMap(map)
        println( map2 )
//        val map3: Map<String,Any?> = mapper.convertValue(map)
//        println( map3 )

        assertEquals( map["C"], map2["C"] )

    }

    @Test
    fun toJsonFromObject() {
        val person = Person("nayasis",45,"sungnam", null)
        println( Reflector.toJson(person))
        println( Reflector.toMap(person) )
    }



}

data class Person(
    val name: String,
    val age: Int,
    val address: String,
    val etc: String?
)