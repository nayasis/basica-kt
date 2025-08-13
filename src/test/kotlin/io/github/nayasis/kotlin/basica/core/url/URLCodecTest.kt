package io.github.nayasis.kotlin.basica.core.url

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class URLCodecTest: StringSpec({
    "basic" {
        val text = "Railroad Tycoon Deluxe"
        val encoded = URLCodec().encode(text).also { println(it) }
        val decoded = URLCodec().decode(encoded).also { println(it) }
        text shouldBe decoded
    }
})