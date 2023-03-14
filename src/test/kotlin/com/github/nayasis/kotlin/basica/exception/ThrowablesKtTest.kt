package com.github.nayasis.kotlin.basica.exception

import com.github.nayasis.kotlin.basica.etc.error
import mu.KotlinLogging
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.security.InvalidParameterException

private val logger = KotlinLogging.logger {}

class ThrowablesKtTest {

    @Test
    fun filterStackTrace() {

        val pattern = "^java\\.util|^sun\\.reflect|^java\\.lang|^org\\.junit|^org\\.gradle|^com\\.sun".toRegex()

        try {
            stack1()
        } catch (e: Exception) {

            logger.debug(">> original")
            logger.error(e)
            logger.debug(">> filtered")
            logger.error(e.filterStackTrace(pattern))

            assertEquals(4, e.filterStackTrace(pattern).stackTrace.size)

        }

    }

    private fun stack1() {
        stack2()
    }

    private fun stack2() {
        stack3()
    }

    private fun stack3() {
        throw InvalidParameterException("merong")
    }


}