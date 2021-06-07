package com.github.nayasis.kotlin.basica.core.math

import mu.KotlinLogging
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

private val log = KotlinLogging.logger {}

internal class MathsKtTest {

    @Test
    fun roundTest() {
        assertEquals(3.242, round(3.241592,3) )
    }

}