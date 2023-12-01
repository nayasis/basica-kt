package com.github.nayasis.kotlin.basica.model

import com.fasterxml.jackson.core.type.TypeReference
import com.github.nayasis.kotlin.basica.annotation.NoArg
import com.github.nayasis.kotlin.basica.core.character.Characters
import com.github.nayasis.kotlin.basica.core.collection.toNGrid
import com.github.nayasis.kotlin.basica.core.localdate.toLocalDateTime
import com.github.nayasis.kotlin.basica.core.localdate.toString
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

internal class NGridTest: StringSpec({

    Characters.fullwidth = 2.0

    "print" {

        val grid = NGrid()

        grid.addRow("key", "controller")
        grid.addRow("key", 1)
        grid.addRow("val", "컨트롤러는 이런 것입니다.")
        grid.addRow("val", 3359)

        grid.header.setAlias("key", "이것은 KEY 입니다.")
        grid.header.setAlias("val", "これは VALUE です")

        logger.debug { "\n${grid}" }
        logger.debug { "\n${grid.toString(false)}" }
        logger.debug { "\n${grid.toString(showIndexColumn = true)}" }

        grid.toString() shouldBe """
            +------------------+-------------------------+
            |이것은 KEY 입니다.|これは VALUE です        |
            +------------------+-------------------------+
            |controller        |컨트롤러는 이런 것입니다.|
            |                 1|                     3359|
            +------------------+-------------------------+            
        """.trimIndent().trim()

        grid.toString(false) shouldBe """
            +------------------+-------------------------+
            |controller        |컨트롤러는 이런 것입니다.|
            |                 1|                     3359|
            +------------------+-------------------------+            
        """.trimIndent().trim()

        grid.toString(showIndexColumn = true) shouldBe """
            +-----+------------------+-------------------------+
            |index|이것은 KEY 입니다.|これは VALUE です        |
            +-----+------------------+-------------------------+
            |    0|controller        |컨트롤러는 이런 것입니다.|
            |    1|                 1|                     3359|
            +-----+------------------+-------------------------+          
        """.trimIndent().trim()

    }

    "print empty data" {
        val grid = NGrid()
        grid.toString() shouldBe """
            +---+
            |   |
            +---+            
        """.trimIndent().trim()

        grid.header.add("name")
        grid.toString() shouldBe """
            +----+
            |name|
            +----+
            +----+            
        """.trimIndent().trim()
    }

    "print empty with header" {
        val grid = NGrid(Person::class)
        grid.toString() shouldBe """
            +---+----+
            |age|name|
            +---+----+
            +---+----+            
        """.trimIndent().trim()
    }

    "print empty generic collection with header" {
        val list = ArrayList<Person>()
        val grid = list.toNGrid().also { logger.debug { "\n${it}" } }
        grid.toString() shouldBe """
            +---+----+
            |age|name|
            +---+----+
            +---+----+            
        """.trimIndent().trim()
    }

    "print empty object collection with non-header" {
        val list = ArrayList<Any>()
        val grid = list.toNGrid().also { logger.debug { "\n${it}" } }
        grid.toString() shouldBe """
            +---+
            |   |
            +---+            
        """.trimIndent().trim()
    }

    "toListFromColumn" {

        val grid = NGrid().apply {
            addRow("key", "nayasis")
            addRow("key", 1)
            addRow("val", mapOf("name" to "nayasis", "age" to 40))
            addRow("val", mapOf("name" to "jake", "age" to 11))
        }

        val rs1 = grid.toListFrom("key", String::class).also { logger.debug { it } }
        val rs2 = grid.toListFrom("value", Person::class).also { logger.debug { it } }
        val rs3 = grid.toListFrom("value", object: TypeReference<List<Person>>(){}).also { logger.debug { it } }
        val rs4 = grid.toListFrom("val", Person::class).also { logger.debug { it } }
        val rs5 = grid.toListFrom("key", Double::class).also { logger.debug { it } }

        logger.debug { "\n${grid.toString(showIndexColumn = true)}" }

        rs1.toString() shouldBe "[nayasis, 1]"
        rs2.toString() shouldBe "[null, null]"
        rs3.toString() shouldBe "[null, null]"
        rs4.toString() shouldBe "[Person(name=nayasis, age=40), Person(name=jake, age=11)]"
        rs5.toString() shouldBe "[0.0, 1.0]"

    }

    "print overflow" {
        val grid = NGrid().apply {
            addRow(Person("우리나라 좋은나라 대한민국",1234567890))
            addRow(Person("우리나라 좋은나라 미국",1234567890))
            addRow(Person("우리나라 좋은나라 오스트레일리아",1234567890))
        }
        grid.toString(maxColumnWidth=20) shouldBe """
            +--------------------+----------+
            |name                |age       |
            +--------------------+----------+
            |우리나라 좋은나라 ..|1234567890|
            |우리나라 좋은나라 ..|1234567890|
            |우리나라 좋은나라 ..|1234567890|
            +--------------------+----------+            
        """.trimIndent().trim()
    }

    "print Vo overflow" {

        val grid = NGrid().apply {
            addRow("key","A")
            addRow("key","B")
            addRow("key","C")
            addRow("value",ComplexVo("우리나라 좋은나라 대한민국",1234590))
            addRow("value",ComplexVo("우리나라 좋은나라 미국",1234212312))
            addRow("value",ComplexVo("우리나라 좋은나라 오스트레일리아",12347890))
        }.also { println(it) }

        "$grid" shouldBe """
            +---+----------------------------------------------------------------------------------------------------+
            |key|value                                                                                               |
            +---+----------------------------------------------------------------------------------------------------+
            |A  |ComplexVo(name=우리나라 좋은나라 대한민국, age=1234590, birth=2017-04-06 00:00:00.000, address=매.. |
            |B  |ComplexVo(name=우리나라 좋은나라 미국, age=1234212312, birth=2017-04-06 00:00:00.000, address=매우..|
            |C  |ComplexVo(name=우리나라 좋은나라 오스트레일리아, age=12347890, birth=2017-04-06 00:00:00.000, addr..|
            +---+----------------------------------------------------------------------------------------------------+           
        """.trimIndent().trim()

    }

    "ignore carriage return" {
        val grid = NGrid().apply {
            addRow(Person("우리나라 \n좋은나라 대한민국",1234567890))
            addRow(Person("우리나라 \n좋은나라 미국",1234567890))
            addRow(Person("우리나라 \n좋은나라 오스트레일리아",1234567890))
        }
        grid.toString(maxColumnWidth=20, showIndexColumn = true) shouldBe """
            +-----+--------------------+----------+
            |index|name                |age       |
            +-----+--------------------+----------+
            |    0|우리나라 \n좋은나.. |1234567890|
            |    1|우리나라 \n좋은나.. |1234567890|
            |    2|우리나라 \n좋은나.. |1234567890|
            +-----+--------------------+----------+            
        """.trimIndent().trim()
    }

    "control to print alias" {

        val data = listOf(
            Person("A",1),
            Person("B",2),
            Person("C",3),
        )

        NGrid(data).toString().also {
            logger.debug { it }
        } shouldBe """
            +----+---+
            |name|age|
            +----+---+
            |A   |  1|
            |B   |  2|
            |C   |  3|
            +----+---+
        """.trimIndent().trim()

        NGrid().apply {
            header.setAlias(Person::age.name, "a")
            header.setAlias(Person::name.name, "n")
        }.addRow(data).toString().also {
            logger.debug { it }
        } shouldBe """
            +-+-+
            |a|n|
            +-+-+
            |1|A|
            |2|B|
            |3|C|
            +-+-+
        """.trimIndent().trim()
    }

})

data class Person(
    val name: String? = null,
    val age: Int? = null,
)

data class ComplexVo(
    val name: String,
    val age: Int,
    val birth: String = "2020-01-01".toLocalDateTime().minusDays(1000).toString("YYYY-MM-DD HH:MI:SS.FFF"),
    val address: String = "매우매우 긴 주소입니다.",
    val regDt: String = "2021-07-23".toLocalDateTime().toString("YYYY-MM-DD HH:MI:SS.FFF"),
    val updDt: String = "2022-09-25".toLocalDateTime().toString("YYYY-MM-DD HH:MI:SS.FFF"),
)