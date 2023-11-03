package com.github.nayasis.kotlin.basica.etc

import io.kotest.core.spec.style.StringSpec
import mu.KotlinLogging
import org.junit.jupiter.api.Test

private val logger = KotlinLogging.logger {}

internal class Slf4jLoggersTest: StringSpec({

    "no error raised" {

        logger.error(RuntimeException("test"))

        val emptyException: Throwable? = null

        logger.error(emptyException)

    }
})