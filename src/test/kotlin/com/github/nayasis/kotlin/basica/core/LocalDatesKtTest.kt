package com.github.nayasis.kotlin.basica.core

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class LocalDatesKtTest{

    @Test
    fun toLocalDateTime() {
        Assertions.assertEquals("1977-01-01", "1977".toLocalDate().toString() )
        Assertions.assertEquals("1977-01-01", "1977-01".toLocalDate().toString() )
        Assertions.assertEquals("1977-01-22", "1977-01-22".toLocalDate().toString() )
        Assertions.assertEquals("1977-01-22T23:59:59.999", "1977-01-22T23:59:59.999".toLocalDateTime().toString() )
        Assertions.assertEquals("1977-01-22", "1977-01-22T23:59:59.999".toLocalDate().toString() )
    }

    @Test
    fun atStartOfMonth() {

        val current = "2020-12-22 13:42:59".toLocalDateTime()

        Assertions.assertEquals("2020-12-01", current.atStartOfMonth().toString("YYYY-MM-DD"))
        Assertions.assertEquals("2020-12-31", current.atEndOfMonth().toString("YYYY-MM-DD"))

        Assertions.assertEquals("2020-12-22 00:00:00", current.atStartOfDay().toString("YYYY-MM-DD HH:MI:SS"))
        Assertions.assertEquals("2020-12-22 23:59:59", current.atEndOfDay().toString("YYYY-MM-DD HH:MI:SS"))

    }

}