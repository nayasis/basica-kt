package com.github.nayasis.kotlin.basica.core.string

import com.github.nayasis.kotlin.basica.core.localdate.toLocalDateTime
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class StringsKtTest {

    @Test
    fun `glob test`() {

        println( ".".glob("*.kt").joinToString("\n") )
        Assertions.assertTrue( ".".glob("*.kt").size > 1 )
        println("----------")
        println( "".glob("*.kt").joinToString("\n") )
        Assertions.assertTrue( "".glob("*.kt").size > 1 )

    }

    @Test
    fun isDate() {

        "2021-01-01".toLocalDateTime()

        Assertions.assertTrue( "2021-01-01".isDate() )
        Assertions.assertFalse( "2021-01-33".isDate() )
    }

    @Test
    fun toNumber() {
        Assertions.assertEquals(1, "1".toNumber())
        Assertions.assertEquals(1.2, "1.2".toNumber(Double::class))
        Assertions.assertEquals(0.0, "nayasis".toNumber(Double::class))
        Assertions.assertEquals(0L, "nayasis".toNumber(Long::class))
    }

}