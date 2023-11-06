package com.github.nayasis.kotlin.basica.cache.implement

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import mu.KotlinLogging

private val logger = KotlinLogging.logger{}

class FifoCacheTest: StringSpec({

    "basic" {
        val cache = FifoCache<Int, Int>(2)
        cache[0] = 0
        cache[1] = 1

        cache[0] shouldBe 0
        cache[1] shouldBe 1

        cache[2] = 2

        cache[0] shouldBe null
        cache[1] shouldBe 1
        cache[2] shouldBe 2
    }

    "capacity" {

        val cache = FifoCache<Int, Int>(5)

        for( i in 1..10)
            cache[i] = i

        for( i in 1..5)
            cache[i] shouldBe null

        for( i in 6..10)
            cache[i] shouldNotBe null

    }

    "fifo" {

        val cache = FifoCache<Int, Int>(10)

        for( i in 1..10)
            cache[i] = i

        for( i in 1..5)
            cache[i]

        for( i in 11..15)
            cache[i] = i

        for( i in 1..15)
            logger.debug { "$i : ${cache[i]}" }

        for( i in 1..5)
            cache[i] shouldBe null

        for( i in 6..15)
            cache[i] shouldNotBe null

    }

})