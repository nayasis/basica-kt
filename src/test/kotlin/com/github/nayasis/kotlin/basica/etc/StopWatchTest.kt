package com.github.nayasis.kotlin.basica.etc

import mu.KotlinLogging
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.lang.Thread.sleep

private val log = KotlinLogging.logger {}

internal class StopWatchTest {

    @Test
    fun basic() {

        val stopWatch = StopWatch("task1")
        sleep(100)

        stopWatch.tick("task2")
        sleep(200)

        stopWatch.tick("task3")
        sleep(300)

        log.debug { "\n${stopWatch}" }

        sleep(100)
        stopWatch.stop()

        log.debug { "\n${stopWatch}" }

        stopWatch.reset()
        log.debug { "\n${stopWatch}" }

    }

    @Test
    fun elapsed() {

        val stopWatch = StopWatch()

        sleep(100)
        assertTrue( stopWatch.elapsedMillis() > 100 )
        log.debug { stopWatch.elapsedSeconds() }

        sleep(100)
        assertTrue( stopWatch.elapsedMillis() > 200 )
        log.debug { stopWatch.elapsedSeconds() }

        sleep(100)
        assertTrue( stopWatch.elapsedMillis() > 300 )
        log.debug { stopWatch.elapsedSeconds() }

    }

    @Test
    fun disabled() {

        val stopWatch = StopWatch("task1")
        stopWatch.enable = false
        sleep(100)

        stopWatch.tick("task2")
        sleep(200)

        stopWatch.tick("task3")
        sleep(300)

        stopWatch.stop()

        log.debug { "\n${stopWatch}" }
        assertEquals("", stopWatch.toString())

    }

}