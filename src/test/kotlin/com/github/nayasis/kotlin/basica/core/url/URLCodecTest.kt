package com.github.nayasis.kotlin.basica.core.url

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class URLCodecTest {

    @Test
    fun basic() {
        val text = "Railroad Tycoon Deluxe"
        val encoded = URLCodec().encode(text).also { println(it) }
        val decoded = URLCodec().decode(encoded).also { println(it) }
        assertEquals(text, decoded)
    }
}