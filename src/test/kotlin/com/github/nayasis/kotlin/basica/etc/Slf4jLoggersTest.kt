package com.github.nayasis.kotlin.basica.etc

import mu.KotlinLogging
import org.junit.jupiter.api.Test

private val logger = KotlinLogging.logger {}

internal class Slf4jLoggersTest {

    @Test
    fun `no error raised`() {

        logger.error(RuntimeException("test"))

        val emptyException: Throwable? = null

        logger.error(emptyException)

    }
}