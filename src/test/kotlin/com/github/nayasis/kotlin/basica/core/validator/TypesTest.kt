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
        TestDataClass("nobody").isPojo shouldBe true
        TestNormalClass("nobody").isPojo shouldBe true
        "nobody".isPojo shouldBe false
        StringBuffer("nobody").isPojo shouldBe false
        StringBuilder("nobody").isPojo shouldBe false
        1.toByte().isPojo shouldBe false
        true.isPojo shouldBe false
        false.isPojo shouldBe false
        0.1.isPojo shouldBe false
        0.1F.isPojo shouldBe false
        1.isPojo shouldBe false
        1L.isPojo shouldBe false
        BigDecimal.valueOf(1).isPojo shouldBe false
        BigInteger.valueOf(1).isPojo shouldBe false
        LocalDateTime.now().isPojo shouldBe false
        Date().isPojo shouldBe false
        Calendar.getInstance().isPojo shouldBe false
        File("/a/b").isPojo shouldBe false
        Path("/a/b").isPojo shouldBe false
        URI("/a/b").isPojo shouldBe false
        URL("https://www.google.com").isPojo shouldBe false
    }

    "type cast" {
        "A".cast<String>() shouldBe "A"
        1.cast<Int>() shouldBe 1
        1.0.cast<Int>() shouldBe 1
        1.0F.cast<Int>() shouldBe 1
        1L.cast<Int>() shouldBe 1
        BigDecimal.valueOf(1).cast<Int>() shouldBe 1
        BigInteger.valueOf(1).cast<Int>() shouldBe 1
        "1".cast<Int>() shouldBe 1
        "1.0".cast<Int>() shouldBe 1
        1.cast<String>() shouldBe "1"
        BigDecimal.valueOf(1).cast<String>() shouldBe "1"
        BigInteger.valueOf(1).cast<String>() shouldBe "1"
    }

})

data class TestDataClass(
    val name: String
)

class TestNormalClass(
    val name: String
)