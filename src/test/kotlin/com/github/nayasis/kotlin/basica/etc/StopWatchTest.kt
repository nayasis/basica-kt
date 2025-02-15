package com.github.nayasis.kotlin.basica.etc

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.Assertions.*
import java.lang.Thread.sleep
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

private val logger = KotlinLogging.logger {}

internal class StopWatchTest: StringSpec({

    "basic" {

        val stopWatch = StopWatch()
        delay(100)
        stopWatch.tick("task1")

        delay(200)
        stopWatch.tick("task2")

        delay(300)
        stopWatch.tick("task3")

        delay(100)
        stopWatch.toString().also { logger.debug { "\n$it" } }
        logger.debug { "\n${stopWatch}" }

        stopWatch.reset()
        logger.debug { "\n${stopWatch}" }

    }

    "elapsed" {

        val stopWatch = StopWatch()

        delay(100)
        stopWatch.elapsed shouldBeGreaterThan 100.milliseconds

        delay(100)
        stopWatch.elapsed shouldBeGreaterThan 200.milliseconds

        delay(100)
        stopWatch.elapsed shouldBeGreaterThan 300.milliseconds

    }

    "lambda" {
        val stopwatch = StopWatch()

        stopwatch.tickSuspendable("task1") {
            delay(100)
        }.let { logger.debug { it } }
        stopwatch.tick("another 2") {
            sleep(200)
        }.let { logger.debug { it } }
        stopwatch.tick {
            runBlocking {
                delay(300)
            }
        }.let { logger.debug { it } }

        logger.debug{ "\n${stopwatch}" }
        logger.debug{ "\n${stopwatch.toString(DurationUnit.NANOSECONDS)}" }
    }

    "no error without ticking" {
        val stopwatch = StopWatch()
        stopwatch.toString().let { logger.debug { "\n$it" } }
    }

})