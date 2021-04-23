package com.github.nayasis.kotlin.basica.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class StringsTest {

    @Test
    fun toNumber() {
        assertEquals( 1, "1".toNumber() )
        assertEquals( 1.2, "1.2".toNumber(Double::class) )
        assertEquals( 0.0, "nayasis".toNumber(Double::class) )
        assertEquals( 0L, "nayasis".toNumber(Long::class) )
    }

}