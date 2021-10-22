package com.github.nayasis.kotlin.basica.core.collection

import com.github.nayasis.kotlin.basica.core.localdate.toDate
import com.github.nayasis.kotlin.basica.reflection.Reflector
import mu.KotlinLogging
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.collections.HashMap

private val log = KotlinLogging.logger {}

private val JSON_EXAMPLE = """
            {
              "name" : {
                "item" : [
                      { "key" : "A", "value" : 1 }
                    ]
              }
            }
        """.trimIndent()

@Suppress("UNCHECKED_CAST")
internal class MapsKtTest {

    @Test
    @Suppress("UNCHECKED_CAST")
    fun flattenKeys() {

        var map = Reflector.toObject<Map<String,Any>>(JSON_EXAMPLE)

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

    @Test
    fun toObject() {

        val json = Reflector.toJson(Dummy())
        println(json)
        val map  = Reflector.toMap(json)
        println(map)
        val obj1 = Reflector.toObject<Dummy>(json)
        println(obj1)
        val obj2 = map.toObject<Dummy>()
        println(obj2)

        assertEquals(obj1.name, obj2.name)
        assertEquals(obj1.age, obj2.age)
        assertEquals(obj1.birth, obj2.birth)

    }

    @Test
    fun getByPath() {

        val map = Reflector.toMap(JSON_EXAMPLE)

        assertEquals( "A", map.getByExpr("name.item[0].key") )
        assertEquals( 1, map.getByExpr("name.item[0].value") )
        assertNull( map.getByExpr("name.item[0].q") )

    }

    @Test
    fun mergeTest() {

        val map1 = mapOf(1 to 1, 2 to 2, 5 to mapOf("name" to "nayasis"))
        val map2 = mapOf(3 to 3, 4 to 4, 5 to listOf(
            mapOf("age" to 45),
            mapOf("name" to "jake", "age" to 11)
        ))

        val merged = map2.merge(map1)

        log.debug { merged }

        for( i in 1..4 )
            assertEquals(i, merged[i])

        val e5 = merged[5] as List<Map<String,Any>>

        assertEquals( "nayasis" , e5[0]["name"] )
        assertEquals( 45        , e5[0]["age"]  )
        assertEquals( "jake"    , e5[1]["name"] )
        assertEquals( 11        , e5[1]["age"]  )

    }

}

data class Dummy(
    val name: String = "nayasis",
    val age: Int = 10,
    val birth: Date = "2000-01-01".toDate(),
)