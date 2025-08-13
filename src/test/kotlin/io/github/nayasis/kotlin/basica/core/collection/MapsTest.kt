package io.github.nayasis.kotlin.basica.core.collection

import io.github.nayasis.kotlin.basica.core.localdate.toDate
import io.github.nayasis.kotlin.basica.core.localdate.toLocalDateTime
import io.github.nayasis.kotlin.basica.reflection.Reflector
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.Assertions.*
import java.util.*

private val logger = KotlinLogging.logger {}

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
class MapsTest: StringSpec({
    "flatten keys" {

        val map = Reflector.toObject<Map<String,Any>>(JSON_EXAMPLE)

        val flatten = map.flattenKeys().also { println(it) }

        flatten.containsKey("name") shouldBe false
        flatten.containsKey("name.item[0]") shouldBe false
        flatten.containsKey("name.item[0].key") shouldBe true
        flatten.containsKey("name.item[0].value") shouldBe true
        flatten["name.item[0].key"] shouldBe "A"
        flatten["name.item[0].value"] shouldBe 1

        val unflatten = flatten.unflattenKeys().also { println(it) }

        unflatten.containsKey("name") shouldBe true
        unflatten.containsKey("name.item[0]") shouldBe false
        unflatten.containsKey("name.item[0].key") shouldBe false
        unflatten.containsKey("name.item[0].value") shouldBe false

        (((unflatten["name"] as Map<String,Any?>)["item"] as List<Any?>)[0] as Map<String,Any?>)["key"] shouldBe "A"
        (((unflatten["name"] as Map<String,Any?>)["item"] as List<Any?>)[0] as Map<String,Any?>)["value"] shouldBe 1

    }

    "json to map" {
        val json = """
            {"1":"one","2":"two"}
        """.trimIndent()
        val map = Reflector.toObject<Map<String,String>>(json).also { println(it) }
        map["1"] shouldBe "one"
        map["2"] shouldBe "two"
    }

    "map to json" {
        val map = mapOf(1 to "one", 2 to "two")
        val json = Reflector.toJson(map).also { println(it) }
        json shouldBe """
            {"1":"one","2":"two"}
        """.trimIndent()
    }

    "convert to Object" {

        val json = Reflector.toJson(Dummy()).also { println(it) }
        val map  = Reflector.toMap(json).also { println(it) }
        val obj1 = Reflector.toObject<Dummy>(json).also { println(it) }
        val obj2 = map.toObject<Dummy>().also { println(it) }

        obj1.name shouldBe obj2.name
        obj1.age shouldBe obj2.age
        obj1.birth shouldBe obj2.birth

    }

    "get by path" {

        val map = Reflector.toMap(JSON_EXAMPLE)

        map.getByExpr<String>("name.item[0].key") shouldBe "A"
        map.getByExpr<String>("name.item[0].value") shouldBe 1
        map.getByExpr<Any?>("name.item[0].q") shouldBe null

    }

    "merge" {

        val map1 = mapOf(1 to 1, 2 to 2, 5 to mapOf("name" to "nayasis"))
        val map2 = mapOf(3 to 3, 4 to 4, 5 to listOf(
            mapOf("age" to 45),
            mapOf("name" to "jake", "age" to 11)
        ))

        val merged = map2.merge(map1).also { logger.debug { it } }

        for( i in 1..4 )
            merged[i] shouldBe i

        val e5 = merged[5] as List<Map<String,Any>>

        e5[0]["name"] shouldBe "nayasis"
        e5[0]["age"]  shouldBe 45
        e5[1]["name"] shouldBe "jake"
        e5[1]["age"]  shouldBe 11

    }

    "print map value as table" {

        val map = mapOf(
            4 to 772425,
            "age" to 45,
            "name" to "jake",
            "birth" to "2021-10-16 23:10".toLocalDateTime()
        )

        map.toString(true) shouldBe """
            +-----+-------------+----------------+
            |    4|Int          |          772425|
            |age  |Int          |              45|
            |name |String       |jake            |
            |birth|LocalDateTime|2021-10-16T23:10|
            +-----+-------------+----------------+
        """.trimIndent()

        map.toString(false) shouldBe """
            +-----+----------------+
            |    4|          772425|
            |age  |              45|
            |name |jake            |
            |birth|2021-10-16T23:10|
            +-----+----------------+
        """.trimIndent()

    }

})

data class Dummy(
    val name: String = "nayasis",
    val age: Int = 10,
    val birth: Date = "2000-01-01".toDate(),
)