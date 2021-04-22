package com.github.nayasis.kotlin.basica.model

import com.github.nayasis.kotlin.basica.core.Characters
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
        grid.addData("val", "컨트롤러는 이런 것입니다.")

        grid.addData("key", 1)
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

}