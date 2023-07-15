package com.github.nayasis.kotlin.basica.cache.implement

import mu.KotlinLogging
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

private val log = KotlinLogging.logger {  }

internal class FifoCacheTest {

    @Test
    fun basic() {

        val cache = FifoCache<Int, Int>(2)
        cache[0] = 0
        cache[1] = 1

        Assertions.assertEquals(0, cache[0])
        Assertions.assertEquals(1, cache[1])

        cache[2] = 2

        Assertions.assertEquals(null, cache[0])
        Assertions.assertEquals(1, cache[1])
        Assertions.assertEquals(2, cache[2])

    }

    @Test
    fun capacity() {

        val cache = FifoCache<Int, Int>(5)

        for( i in 1..10)
            cache[i] = i

        for( i in 1..5)
            Assertions.assertNull(cache[i])

        for( i in 6..10)
            Assertions.assertNotNull(cache[i])

    }

    @Test
    fun fifo() {

        val cache = FifoCache<Int, Int>(10)

        for( i in 1..10)
            cache[i] = i

        for( i in 1..5)
            cache[i]

        for( i in 11..15)
            cache[i] = i

        for( i in 1..15)
            log.debug { "$i : ${cache[i]}" }

        for( i in 1..5)
            Assertions.assertNull(cache[i])

        for( i in 6..15)
            Assertions.assertNotNull(cache[i])

    }

}