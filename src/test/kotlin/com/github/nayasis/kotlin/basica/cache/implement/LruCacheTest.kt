package com.github.nayasis.kotlin.basica.cache.implement

import mu.KotlinLogging
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.lang.Thread.sleep

private val log = KotlinLogging.logger {  }

internal class LruCacheTest {

    @Test
    fun basic() {

        val cache = LruCache<Int,Int>().apply { flushMiliseconds = 200 }
        cache.put(0, 88)
        cache.put(1, 99)

        assertEquals( 88, cache[0] )
        assertEquals( 99, cache[1] )

        sleep(500)

        assertEquals( null, cache[0] )
        assertEquals( null, cache[1] )

    }

    @Test
    fun capacity() {

        val cache = LruCache<Int,Int>(5)

        for( i in 1..10)
            cache.put(i,i)

        for( i in 1..5)
            assertNull(cache[i])

        for( i in 6..10)
            assertNotNull(cache[i])

    }

    @Test
    fun lru() {

        val cache = LruCache<Int,Int>(10)

        for( i in 1..10)
            cache.put(i,i)

        for( n in 1..10 ) {
            for( i in 1..5)
                cache[i]
        }

        for( i in 11..15)
            cache.put(i,i)

        for( i in 1..15)
            log.debug { "$i : ${cache[i]}" }

        for( i in 1..5)
            assertNotNull(cache[i])

        for( i in 6..10)
            assertNull(cache[i])

        for( i in 1..5)
            assertNotNull(cache[i])

    }

    @Test
    fun extendByAccessing() {

        val cache = LruCache<Int,Int>().apply { flushMiliseconds = 1000 }

        for( i in 1..10)
            cache.put(i,i)

        for( n in 1..5 ) {
            sleep(100)
            for( i in 1..5)
                cache[i]
        }

        sleep(500)

        for( i in 1..10)
            log.debug { "$i : ${cache[i]}" }

        for( i in 1..5)
            assertNotNull(cache[i])

        for( i in 6..10)
            assertNull(cache[i])

    }

}