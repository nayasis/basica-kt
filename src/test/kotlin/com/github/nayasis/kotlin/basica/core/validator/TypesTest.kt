package com.github.nayasis.kotlin.basica.core.validator

import com.github.nayasis.kotlin.basica.core.io.Path
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI
import java.net.URL
import java.time.LocalDateTime
import java.util.Calendar
import java.util.Date

class TypesTest: StringSpec({

    "detect pojo" {

        Types.isPojo(TestDataClass("nobody")) shouldBe true
        Types.isPojo(TestNormalClass("nobody")) shouldBe true
        Types.isPojo("nobody") shouldBe false
        Types.isPojo(StringBuffer("nobody")) shouldBe false
        Types.isPojo(StringBuilder("nobody")) shouldBe false

        assertEquals(false, Types.isPojo(1.toByte()))
        assertEquals(false, Types.isPojo(true))
        assertEquals(false, Types.isPojo(false))
        assertEquals(false, Types.isPojo(0.1))
        assertEquals(false, Types.isPojo(0.1F))
        assertEquals(false, Types.isPojo(1))
        assertEquals(false, Types.isPojo(1L))
        assertEquals(false, Types.isPojo(BigDecimal.valueOf(1)))
        assertEquals(false, Types.isPojo(BigInteger.valueOf(1)))
        assertEquals(false, Types.isPojo(LocalDateTime.now()))
        assertEquals(false, Types.isPojo(Date()))
        assertEquals(false, Types.isPojo(Calendar.getInstance()))
        assertEquals(false, Types.isPojo(File("/a/b")))
        assertEquals(false, Types.isPojo(Path("/a/b")))
        assertEquals(false, Types.isPojo(URI("/a/b")))
        assertEquals(false, Types.isPojo(URL("https://www.google.com")))
    }

    "type cast" {

        assertEquals("A", Types.cast<String>("A"))
        assertEquals(1, Types.cast(1))
        assertEquals(1.0, Types.cast(1.0))
        assertEquals(1.0F, Types.cast(1.0F))

        assertEquals(1, Types.cast(1.0))
        assertEquals(1, Types.cast(1.0F))
        assertEquals(1, Types.cast(1L))
        assertEquals(1, Types.cast(BigDecimal.valueOf(1)))
        assertEquals(1, Types.cast(BigInteger.valueOf(1)))
        assertEquals(1, Types.cast("1"))
        assertEquals(1, Types.cast("1.0"))

        assertEquals("1", Types.cast<String>(1))
        assertEquals("1", Types.cast<String>(BigDecimal.valueOf(1)))
        assertEquals("1", Types.cast<String>(BigInteger.valueOf(1)))
    }

})

data class TestDataClass(
    val name: String
)

class TestNormalClass(
    val name: String
)