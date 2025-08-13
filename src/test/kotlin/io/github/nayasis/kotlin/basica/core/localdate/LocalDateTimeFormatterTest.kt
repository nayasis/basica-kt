package io.github.nayasis.kotlin.basica.core.localdate

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private val logger = KotlinLogging.logger {}

internal class LocalDateTimeFormatterTest: StringSpec({

    "basic" {
        "yyyy-MM-dd'T'HH:mm:ss".toJvmTimeFormat(false) shouldBe "yyyy-MM-dd'T'HH:mm:ss"
        "yyyy-MM-dd'T'HH:mm:ss".toJvmTimeFormat(true) shouldBe "yyyy-MM-ddHH:mm:ss"
    }

})