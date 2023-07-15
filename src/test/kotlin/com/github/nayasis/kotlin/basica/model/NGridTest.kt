package com.github.nayasis.kotlin.basica.model

import com.fasterxml.jackson.core.type.TypeReference
import com.github.nayasis.kotlin.basica.annotation.NoArg
import com.github.nayasis.kotlin.basica.core.character.Characters
import com.github.nayasis.kotlin.basica.core.collection.toNGrid
import com.github.nayasis.kotlin.basica.core.localdate.toLocalDateTime
import com.github.nayasis.kotlin.basica.core.localdate.toString
import mu.KotlinLogging
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime
import java.util.*
import kotlin.random.Random.Default.nextInt

private val log = KotlinLogging.logger {}

internal class NGridTest {

    init {
        Characters.fullwidth = 2.0
    }

    @Test
    fun print() {

        var grid = NGrid()

        grid.addData("key", "controller")
        grid.addData("key", 1)
        grid.addData("val", "컨트롤러는 이런 것입니다.")
        grid.addData("val", 3359)

        grid.header.setAlias("key", "이것은 KEY 입니다.")
        grid.header.setAlias("val", "これは VALUE です")

        log.debug { "\n${grid}" }
        log.debug { "\n${grid.toString(false)}" }
        log.debug { "\n${grid.toString(showIndexColumn = true)}" }

        assertEquals("""
            +------------------+-------------------------+
            |이것은 KEY 입니다.|これは VALUE です        |
            +------------------+-------------------------+
            |controller        |컨트롤러는 이런 것입니다.|
            |                 1|                     3359|
            +------------------+-------------------------+            
        """.trimIndent().trim(), grid.toString())

        assertEquals("""
            +------------------+-------------------------+
            |controller        |컨트롤러는 이런 것입니다.|
            |                 1|                     3359|
            +------------------+-------------------------+            
        """.trimIndent().trim(), grid.toString(false))

        assertEquals("""
            +-----+------------------+-------------------------+
            |index|이것은 KEY 입니다.|これは VALUE です        |
            +-----+------------------+-------------------------+
            |    0|controller        |컨트롤러는 이런 것입니다.|
            |    1|                 1|                     3359|
            +-----+------------------+-------------------------+          
        """.trimIndent().trim(), grid.toString(showIndexColumn = true))

    }

    @Test
    fun `print empty data`() {

        val grid = NGrid()

        log.debug { "\n${grid}" }

        assertEquals("""
            +---+
            |   |
            +---+            
        """.trimIndent().trim(), grid.toString())

        grid.header.add("name")

        log.debug { "\n${grid}" }

        assertEquals("""
            +----+
            |name|
            +----+
            +----+            
        """.trimIndent().trim(), grid.toString())

    }

    @Test
    fun `print empty with header`() {

        val grid = NGrid(Person::class)

        log.debug { "\n${grid}" }

        assertEquals("""
            +---+----+
            |age|name|
            +---+----+
            +---+----+            
        """.trimIndent().trim(), grid.toString())

    }

    @Test
    fun `print empty generic collection with header`() {

        val list = ArrayList<Person>()

        val grid = list.toNGrid()

        log.debug { "\n${grid}" }

        assertEquals("""
            +---+----+
            |age|name|
            +---+----+
            +---+----+            
        """.trimIndent().trim(), grid.toString())

    }

    @Test
    fun `print empty object collection with non-header`() {

        val list = ArrayList<Any>()

        val grid = list.toNGrid()

        log.debug { "\n${grid}" }

        assertEquals("""
            +---+
            |   |
            +---+            
        """.trimIndent().trim(), grid.toString())

    }

    @Test
    fun toListFromColumn() {

        val grid = NGrid()

        grid.addData("key", "nayasis")
        grid.addData("key", 1)
        grid.addData("val", mapOf("name" to "nayasis", "age" to 40))
        grid.addData("val", mapOf("name" to "jake", "age" to 11))

//        val rs1 = grid.toListFrom("key", String::class)
//                log.debug { rs1 }
//        val rs2 = grid.toListFrom("value", Person::class)
//                log.debug { rs2 }
//        val rs3 = grid.toListFrom("value", object:TypeReference<List<Person>>(){})
//                log.debug { rs3 }
        val rs4 = grid.toListFrom("val", Person::class)
                log.debug { rs4 }
//        val rs5 = grid.toListFrom("key", Double::class)
//                log.debug { rs5 }

        log.debug { "\n${grid.toString(showIndexColumn = true)}" }

//        assertEquals( "[nayasis, 1]", rs1.toString() )
//        assertEquals( "[null, null]", rs2.toString() )
//        assertEquals( "[null, null]", rs3.toString() )
//        assertEquals( "[Person(name=nayasis, age=40), Person(name=jake, age=11)]", rs4.toString() )
//        assertEquals( "[0.0, 1.0]", rs5.toString() )

    }

    @Test
    fun printOverflow() {

        Characters.fullwidth = 2.0

        val grid = NGrid()

        grid.addRow(Person("우리나라 좋은나라 대한민국",1234567890))
        grid.addRow(Person("우리나라 좋은나라 미국",1234567890))
        grid.addRow(Person("우리나라 좋은나라 오스트레일리아",1234567890))

        assertEquals("""
            +--------------------+----------+
            |name                |age       |
            +--------------------+----------+
            |우리나라 좋은나라 ..|1234567890|
            |우리나라 좋은나라 ..|1234567890|
            |우리나라 좋은나라 ..|1234567890|
            +--------------------+----------+            
        """.trimIndent().trim(), grid.toString(maxColumnWidth=20))

    }

    @Test
    fun printVoOverflow() {

        Characters.fullwidth = 2.0

        val grid = NGrid()

        grid.addData("key","A")
        grid.addData("key","B")
        grid.addData("key","C")

        grid.addData("value",ComplexVo("우리나라 좋은나라 대한민국",1234590))
        grid.addData("value",ComplexVo("우리나라 좋은나라 미국",1234212312))
        grid.addData("value",ComplexVo("우리나라 좋은나라 오스트레일리아",12347890))

        println(grid)

        assertEquals("""
            +---+----------------------------------------------------------------------------------------------------+
            |key|value                                                                                               |
            +---+----------------------------------------------------------------------------------------------------+
            |A  |ComplexVo(name=우리나라 좋은나라 대한민국, age=1234590, birth=2017-04-06 00:00:00.000, address=매.. |
            |B  |ComplexVo(name=우리나라 좋은나라 미국, age=1234212312, birth=2017-04-06 00:00:00.000, address=매우..|
            |C  |ComplexVo(name=우리나라 좋은나라 오스트레일리아, age=12347890, birth=2017-04-06 00:00:00.000, addr..|
            +---+----------------------------------------------------------------------------------------------------+           
        """.trimIndent().trim(), grid.toString())

    }

    @Test
    fun `ignore carriage return`() {

        Characters.fullwidth = 2.0

        val grid = NGrid()

        grid.addRow(Person("우리나라 \n좋은나라 대한민국",1234567890))
        grid.addRow(Person("우리나라 \n좋은나라 미국",1234567890))
        grid.addRow(Person("우리나라 \n좋은나라 오스트레일리아",1234567890))

        assertEquals("""
            +-----+--------------------+----------+
            |index|name                |age       |
            +-----+--------------------+----------+
            |    0|우리나라 \n좋은나.. |1234567890|
            |    1|우리나라 \n좋은나.. |1234567890|
            |    2|우리나라 \n좋은나.. |1234567890|
            +-----+--------------------+----------+            
        """.trimIndent().trim(), grid.toString(maxColumnWidth=20, showIndexColumn = true))

    }

}

@NoArg
data class Person(
    val name: String?,
    val age: Int?,
)

data class ComplexVo(
    val name: String,
    val age: Int,
    val birth: String = "2020-01-01".toLocalDateTime().minusDays(1000).toString("YYYY-MM-DD HH:MI:SS.FFF"),
    val address: String = "매우매우 긴 주소입니다.",
    val regDt: String = "2021-07-23".toLocalDateTime().toString("YYYY-MM-DD HH:MI:SS.FFF"),
    val updDt: String = "2022-09-25".toLocalDateTime().toString("YYYY-MM-DD HH:MI:SS.FFF"),
)