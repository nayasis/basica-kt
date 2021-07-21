package com.github.nayasis.kotlin.basica.model

import com.fasterxml.jackson.core.type.TypeReference
import com.github.nayasis.kotlin.basica.annotation.NoArg
import com.github.nayasis.kotlin.basica.core.character.Characters
import mu.KotlinLogging
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

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

        grid.header().setAlias("key", "이것은 KEY 입니다.")
        grid.header().setAlias("val", "これは VALUE です")

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

        grid.header().add("name")

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
    fun toListFromColumn() {

        val grid = NGrid()

        grid.addData("key", "nayasis")
        grid.addData("key", 1)
        grid.addData("val", mapOf("name" to "nayasis", "age" to 40))
        grid.addData("val", mapOf("name" to "jake", "age" to 11))

        val rs1 = grid.toListFrom("key", String::class)
                log.debug { rs1 }
        val rs2 = grid.toListFrom("value", Person::class)
                log.debug { rs2 }
        val rs3 = grid.toListFrom("value", object:TypeReference<List<Person>>(){})
                log.debug { rs3 }
        val rs4 = grid.toListFrom("val", Person::class)
                log.debug { rs4 }
        val rs5 = grid.toListFrom("key", Double::class)
                log.debug { rs5 }

        log.debug { "\n${grid.toString(showIndexColumn = true)}" }

        assertEquals( "[nayasis, 1]", rs1.toString() )
        assertEquals( "[null, null]", rs2.toString() )
        assertEquals( "[null, null]", rs3.toString() )
        assertEquals( "[Person(name=nayasis, age=40), Person(name=jake, age=11)]", rs4.toString() )
        assertEquals( "[0.0, 1.0]", rs5.toString() )

    }

}

@NoArg
data class Person(
    val name: String?,
    val age: Int?,
)