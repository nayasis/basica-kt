package com.github.nayasis.kotlin.basica.core.collection

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal
import java.math.RoundingMode

internal class CollectionsKtTest {

    @Test
    fun sumByLong() {
        listOf(1, 99, 120).sumByLong { it.toLong() }.let {
            assertEquals(220, it)
        }
    }

    @Test
    fun sumByBigDecimal() {
        listOf(1, 99, 120).sumByBigDecimal { it.toBigDecimal() }.let {
            assertEquals(220.toBigDecimal(), it)
        }
    }

    @Test
    fun sumByBigInteger() {
        listOf(1, 99, 120).sumByBigInteger { it.toBigInteger() }.let {
            assertEquals(220.toBigInteger(), it)
        }
    }
}