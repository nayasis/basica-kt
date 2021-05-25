package com.github.nayasis.kotlin.basica.core.klass

import mu.KotlinLogging
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

private val log = KotlinLogging.logger {}

internal class ClassesTest {

    @Test
    fun `get resource`() {

        val resource = Classes.getResource("")

        log.debug { "resource : $resource" }

    }

}