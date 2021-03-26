package com.github.nayasis.kotlin.basica.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.nayasis.kotlin.basica.reflection.Reflector
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class MapsKtTest {

//    val mapper = ObjectMapper().registerModule(KotlinModule())
    val mapper = jacksonObjectMapper()

    @Test
    fun flattenKeys() {

        var map = Reflector.toObject<Map<String,Any>>("""
            {
              "name" : {
                "item" : [
                      { "key" : "A", "value" : 1 }
                    ]
              }
            }
        """.trimIndent())

        println( map.flattenKeys() )

    }

    @Test
    fun whenDeserializeMap_thenSuccess() {
        val json = """{"1":"one","2":"two"}"""
        val aMap: Map<Int,String> = mapper.readValue(json)

        println(aMap)

    }

    @Test
    fun whenSerializeMap_thenSuccess() {
        val map =  mapOf(1 to "one", 2 to "two")
        println( mapper.writeValueAsString(map) )
    }

}