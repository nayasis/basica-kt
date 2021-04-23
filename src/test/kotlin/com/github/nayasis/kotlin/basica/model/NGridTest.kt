package com.github.nayasis.kotlin.basica.model

import com.github.nayasis.kotlin.basica.annotation.NoArg
import com.github.nayasis.kotlin.basica.core.Characters
import jdk.internal.org.objectweb.asm.TypeReference
import mu.KotlinLogging
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.lang.RuntimeException
import java.util.*
import kotlin.collections.HashMap

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
    fun toListColumn() {

        val grid = NGrid()

        grid.addData("key", "nayasis")
        grid.addData("key", 1)
        grid.addData("val", mapOf("name" to "nayasis", "age" to 40))
        grid.addData("val", mapOf("name" to "jake", "age" to 11))

        val rs1 = grid.toListColumn("key", String::class)
        val rs2 = grid.toListColumn("value", Person::class)
        val rs3 = grid.toListColumn("value", TypeReference<List<Person>>())

        log.debug { rs1 }
        log.debug { rs2 }
        log.debug { rs3 }

    }

}

@NoArg
data class Person(
    val name: String?,
    val age: Int?
)