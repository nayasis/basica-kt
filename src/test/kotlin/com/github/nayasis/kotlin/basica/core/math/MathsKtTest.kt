package com.github.nayasis.kotlin.basica.core.math

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import mu.KotlinLogging
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime.now

private val log = KotlinLogging.logger {}

internal class MathsKtTest: StringSpec({

    "maxOf" {
        val a = now()
        val b = now().plusDays(1)
        maxOf(a,b) shouldBe b
    }

    "minOf" {
        val a = now()
        val b = now().plusDays(1)
        minOf(a,b) shouldBe a
    }

})