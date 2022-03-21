package com.github.nayasis.kotlin.basica.core.klass

import mu.KotlinLogging
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

private val log = KotlinLogging.logger {}

internal class ClassesTest {

    @Test
    fun `get resource`() {

        val resource = Classes.getResource("-1")

        log.debug { "resource : $resource" }

    }

    @Test
    fun `is enum`() {

        assertTrue( ExampleEnum.LOW::class.isEnum )

    }

}

enum class ExampleEnum {
    LOW, HIGH
}