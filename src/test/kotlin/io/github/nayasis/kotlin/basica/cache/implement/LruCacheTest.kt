package io.github.nayasis.kotlin.basica.cache.implement

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger{}

class LruCacheTest: StringSpec({

    "basic" {
        val cache = LruCache<Int, Int>(2)
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

        val cache = LruCache<Int, Int>(5)

        for( i in 1..10)
            cache[i] = i

        for( i in 1..5)
            cache[i] shouldBe null

        for( i in 6..10)
            cache[i] shouldNotBe null

    }

    "lru" {

        val cache = LruCache<Int, Int>(10)

        for( i in 1..10)
            cache[i] = i

        for( i in 1..5)
            cache[i]

        for( i in 11..15)
            cache[i] = i

        for( i in 1..15)
            logger.debug { "$i : ${cache[i]}" }

        for( i in 1..5)
            cache[i] shouldNotBe null

        for( i in 6..10)
            cache[i] shouldBe null

        for( i in 11..15)
            cache[i] shouldNotBe null

    }

})