package com.github.nayasis.kotlin.basica.core

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class StringsKtTest {

    @Test
    fun `glob test`() {

        println( "glob:*.kt".glob().joinToString("\n") )
        Assertions.assertTrue( "glob:*.kt".glob().size > 1 )
        println("----------")
        println( "glob:./*.kt".glob().joinToString("\n") )
        Assertions.assertTrue( "glob:./*.kt".glob().size > 1 )

    }

    @Test
    fun isDate() {

        "2021-01-01".toLocalDateTime()


        Assertions.assertTrue( "2021-01-01".isDate() )
        Assertions.assertFalse( "2021-01-33".isDate() )
    }

}