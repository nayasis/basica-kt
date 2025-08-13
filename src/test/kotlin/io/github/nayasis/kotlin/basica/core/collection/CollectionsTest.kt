package io.github.nayasis.kotlin.basica.core.collection

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

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

    "sum by Duration" {
        listOf(1.seconds, 1.minutes, 1.hours).sumByDuration { it } shouldBe 1.hours + 1.minutes + 1.seconds
    }

})