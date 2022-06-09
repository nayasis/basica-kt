package com.github.nayasis.kotlin.basica.core.math

import mu.KotlinLogging
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime.now

private val log = KotlinLogging.logger {}

internal class MathsKtTest {

    @Test
    fun max() {
        val a = now()
        val b = now().plusDays(1)
        assertEquals(b, max(a,b))
    }

    @Test
    fun min() {
        val a = now()
        val b = now().plusDays(1)
        assertEquals(a, min(a,b))
    }

}