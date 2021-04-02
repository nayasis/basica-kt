package com.github.nayasis.kotlin.basica.core

import com.github.nayasis.kotlin.basica.reflection.Reflector
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class MapsKtTest {

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

        val flatten = map.flattenKeys()
        println(flatten)

        assertFalse( flatten.containsKey("name") )
        assertFalse( flatten.containsKey("name.item[0]") )
        assertTrue( flatten.containsKey("name.item[0].key") )
        assertTrue( flatten.containsKey("name.item[0].value") )
        assertTrue( flatten["name.item[0].key"] == "A" )
        assertTrue( flatten["name.item[0].value"] == 1 )

        val unflatten = flatten.unflattenKeys()
        println(unflatten)

        assertTrue( unflatten.containsKey("name") )
        assertTrue( ! unflatten.containsKey("name.item[0]") )
        assertTrue( ! unflatten.containsKey("name.item[0].key") )
        assertTrue( ! unflatten.containsKey("name.item[0].value") )
        assertTrue( (((unflatten["name"] as Map<String,Any?>)["item"] as List<Any?>)[0] as Map<String,Any?>)["key"] == "A" )
        assertTrue( (((unflatten["name"] as Map<String,Any?>)["item"] as List<Any?>)[0] as Map<String,Any?>)["value"] == 1 )

    }

    @Test
    fun whenDeserializeMap_thenSuccess() {
        val json = """{"1":"one","2":"two"}"""
        val aMap: Map<Int,String> = Reflector.toObject(json)
        println(aMap)
    }

    @Test
    fun whenSerializeMap_thenSuccess() {
        val map =  mapOf(1 to "one", 2 to "two")
        println( Reflector.toJson(map) )
    }

}