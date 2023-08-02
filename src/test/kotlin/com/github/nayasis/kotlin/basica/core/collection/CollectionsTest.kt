package com.github.nayasis.kotlin.basica.core.collection

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CollectionsTest: StringSpec({
    "sum by long" {
        listOf(1, 99, 120).sumByLong { it.toLong() } shouldBe 220L
    }

    "sum by BigDecimal" {
        listOf(1, 99, 120).sumByBigDecimal { it.toBigDecimal() } shouldBe 220.toBigDecimal()
    }

    "sum by BigInteger" {
        listOf(1, 99, 120).sumByBigInteger { it.toBigInteger() } shouldBe 220.toBigInteger()
    }
})