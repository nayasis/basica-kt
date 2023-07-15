package com.github.nayasis.kotlin.basica.core.localdate

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.format.DateTimeFormatter

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

        Assertions.assertEquals("2020-12-01", current.atStartOfMonth().format("YYYY-MM-DD"))
        Assertions.assertEquals("2020-12-31", current.atEndOfMonth().format("YYYY-MM-DD"))

        Assertions.assertEquals("2020-12-22 00:00:00", current.atStartOfDay().toString("YYYY-MM-DD HH:MI:SS"))
        Assertions.assertEquals("2020-12-22 23:59:59", current.atEndOfDay().toString("YYYY-MM-DD HH:MI:SS"))

    }

    @Test
    fun offset() {

        Assertions.assertEquals("2020-01-01T16:00:00", "2020-01-01 13:00:00 Z+0300".toLocalDateTime().format())
        Assertions.assertEquals("2020-01-01T16:00:00", "2020-01-01 13:00:00 Z+03:00".toLocalDateTime().format())
        Assertions.assertEquals("2020-01-01T16:00:00", "2020-01-01 13:00:00 +03:00".toLocalDateTime().format())
        Assertions.assertEquals("2020-01-01T16:00:00", "2020-01-01 13:00:00 +0300".toLocalDateTime().format())

    }

    @Test
    fun oldTime() {
        Assertions.assertEquals("0423-01-01", "0423-01-01".toDate().format("YYYY-MM-DD") )
        Assertions.assertEquals(-48818622472000, "0423-01-01".toDate().time )
        Assertions.assertEquals("0423-01-01", "0423-01-01".toLocalDate().format("YYYY-MM-DD") )
        Assertions.assertEquals(-48818622472000, "0423-01-01".toLocalDate().toLong() )
        Assertions.assertEquals("0423-01-01", "0423-01-01".toLocalDateTime().format("YYYY-MM-DD") )
        Assertions.assertEquals(-48818622472000, "0423-01-01".toLocalDateTime().toLong() )
    }

    @Test
    fun formatting() {
        println(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        println(DateTimeFormatter.ISO_LOCAL_DATE)
        println(DateTimeFormatter.ISO_LOCAL_TIME)
    }

}