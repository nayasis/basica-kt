package com.github.nayasis.kotlin.basica.core

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class LocalDatesKtTest{

    @Test
    fun toLocalDateTime() {
        println( "1977-01-22T23:59:59.999".toLocalDateTime())
    }

    @Test
    fun atStartOfMonth() {

        val current = "2020-12-22".toLocalDateTime()

        Assertions.assertEquals("2020-12-01", current.atStartOfMonth().toString("YYYY-MM-DD"))
        Assertions.assertEquals("2020-12-31", current.atEndOfMonth().toString("YYYY-MM-DD"))

    }

}