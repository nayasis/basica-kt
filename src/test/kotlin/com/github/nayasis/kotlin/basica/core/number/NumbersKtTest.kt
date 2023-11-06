package com.github.nayasis.kotlin.basica.core.number

import io.kotest.core.spec.style.StringSpec
import org.junit.jupiter.api.Assertions.assertEquals

internal class NumbersKtTest: StringSpec({
    "round Double" {
        val testNumber = 3.241592
        assertEquals(3.242, testNumber.round(3) )
        assertEquals(3.0, testNumber.round() )
        assertEquals(3.24, testNumber.floor(2) )
        assertEquals(3.25, testNumber.ceil(2) )
    }
    "round Float" {
        val testNumber = 3.241592F
        assertEquals(3.242F, testNumber.round(3) )
        assertEquals(3.0F, testNumber.round() )
        assertEquals(3.24F, testNumber.floor(2) )
        assertEquals(3.25F, testNumber.ceil(2) )
    }
})