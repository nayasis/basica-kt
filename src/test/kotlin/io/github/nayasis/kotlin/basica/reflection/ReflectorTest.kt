package io.github.nayasis.kotlin.basica.reflection

import io.github.nayasis.kotlin.basica.core.collection.toJson
import io.github.nayasis.kotlin.basica.core.localdate.format
import io.github.nayasis.kotlin.basica.core.localdate.toCalendar
import io.github.nayasis.kotlin.basica.core.localdate.toDate
import io.github.nayasis.kotlin.basica.core.localdate.toLocalDateTime
import io.github.nayasis.kotlin.basica.core.localdate.toZonedDateTime
import io.github.nayasis.kotlin.basica.core.string.loadClass
import io.github.nayasis.kotlin.basica.core.validator.cast
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

private val logger = KotlinLogging.logger {}

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

        obj1.C.format() shouldBe "2021-03-26T15:16:31"
        obj1.C.format("YYYY-MM-DD HH:MI:SS.SSS") shouldBe "2021-03-26 15:16:31.154"

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
        "io.github.nayasis.kotlin.basica.core.validator.Validator".loadClass()
            .methods.forEach { println(it) }
        println(line)
        "io.github.nayasis.kotlin.basica.core.string.Strings".loadClass()
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

    "from list" {
        val list = listOf(
            Dummy(Date(), 1, LocalDateTime.now(), "A"),
            Dummy(Date(), 2, LocalDateTime.now(), "B"),
            Dummy(Date(), 3, LocalDateTime.now(), "C"),
            Dummy(Date(), 4, LocalDateTime.now(), "D"),
        )
        val json = Reflector.toJson(list,pretty = true)
        println(json)
    }

    "calender" {

        data class VoCalendar(
            val A: ZonedDateTime,
            val B: LocalDateTime,
        )

        // Create Calendar with explicit timezone
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul")).apply {
            set(2025, 6, 10, 14, 30, 25) // July 10, 2025 14:30:25 (KST)
            set(java.util.Calendar.MILLISECOND, 0)
        }

        // Debug: Check calendar timezone and ZonedDateTime conversion
        logger.debug { ">> Calendar timezone: ${calendar.timeZone.id}" }
        logger.debug { ">> Calendar toZonedDateTime: ${calendar.toZonedDateTime()}" }
        logger.debug { ">> Calendar toZonedDateTime format: ${calendar.toZonedDateTime().format()}" }

        val map = mapOf(
            "A" to calendar,
            "B" to "2025-07-10 14:30:25".toLocalDateTime(),
        ).also { logger.debug { ">> map\n${(it["A"] as Calendar).toDate()}" } }

        val json = Reflector.toJson(map,pretty = true).also { logger.debug { ">> json\n$it" } }

        val converted = Reflector.toObject<VoCalendar>(json).also { logger.debug { ">> converted\n$it" } }

        converted.A.also {
            logger.debug { ">> A.toLocalDateTime() = ${it.toLocalDateTime()}" }
        }.toLocalDateTime().format() shouldBe "2025-07-10T14:30:25"
        converted.B.format() shouldBe "2025-07-10T14:30:25"

    }

    "extension function - Any.toJson()" {
        // Basic object conversion
        val person = Person("nayasis", 45, "sungnam", null)
        person.toJson() shouldBe """{"name":"nayasis","age":45,"address":"sungnam"}"""

        // Test pretty option
        val prettyJson = person.toJson(pretty = true)
        prettyJson.contains("\n") shouldBe true
        prettyJson.contains("\"name\" : \"nayasis\"") shouldBe true

        // Test ignoreNull = false
        val jsonWithNull = person.toJson(ignoreNull = false)
        jsonWithNull shouldBe """{"name":"nayasis","age":45,"address":"sungnam","etc":null}"""

        // Map object conversion
        val map = mapOf("a" to 1, "b" to null, "c" to "test")
        map.toJson() shouldBe """{"a":1,"c":"test"}"""
        map.toJson(ignoreNull = false) shouldBe """{"a":1,"b":null,"c":"test"}"""

        // List object conversion
        val list = listOf(1, 2, 3)
        list.toJson() shouldBe "[1,2,3]"
    }

    "extension function - String.toObject()" {
        // Convert JSON string to Map
        val jsonStr = """{"name":"nayasis","age":45,"address":"sungnam"}"""
        val map = jsonStr.toObject<Map<String, Any?>>()
        map["name"] shouldBe "nayasis"
        map["age"] shouldBe 45
        map["address"] shouldBe "sungnam"

        // Convert JSON string to data class
        val person = jsonStr.toObject<Person>()
        person.name shouldBe "nayasis"
        person.age shouldBe 45
        person.address shouldBe "sungnam"

        // Convert JSON array to List
        val listJson = """[1,2,3,4,5]"""
        val list = listJson.toObject<List<Int>>()
        list shouldBe listOf(1, 2, 3, 4, 5)

        // Test with null values
        val jsonWithNull = """{"name":"jake","age":null,"address":"seoul","etc":"test"}"""
        val personWithNull = jsonWithNull.toObject<Person>()
        personWithNull.name shouldBe "jake"
        personWithNull.age shouldBe null
        personWithNull.address shouldBe "seoul"
        personWithNull.etc shouldBe "test"

        // Test ignoreNull = false
        val personIgnoreNull = jsonWithNull.toObject<Person>(ignoreNull = false)
        personIgnoreNull.name shouldBe "jake"
        personIgnoreNull.age shouldBe null
        personIgnoreNull.address shouldBe "seoul"
    }

    "extension functions - combined usage" {
        // Combined usage of toJson() and toObject()
        val original = Person("test", 30, "seoul", "extra")
        val json = original.toJson()
        val restored: Person = json.toObject()

        restored.name shouldBe original.name
        restored.age shouldBe original.age
        restored.address shouldBe original.address

        // Test List conversion
        val personList = listOf(
            Person("alice", 25, "busan", null),
            Person("bob", 35, "daegu", null)
        )
        val listJson = personList.toJson()
        val restoredList = listJson.toObject<List<Person>>()

        restoredList.size shouldBe 2
        restoredList[0].name shouldBe "alice"
        restoredList[1].name shouldBe "bob"
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