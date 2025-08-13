package io.github.nayasis.kotlin.basica.exception

import io.github.nayasis.kotlin.basica.etc.error
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.github.oshai.kotlinlogging.KotlinLogging
import java.security.InvalidParameterException

private val logger = KotlinLogging.logger {}

class ThrowableTest: AnnotationSpec() {

    @Test
    fun filterStackTrace() {

        try {
            stack1()
        } catch (e: Exception) {

            e.filterStackTrace("""
                io\.github\.nayasis
            """.trimIndent().toRegex(), false
            ).also {
                logger.error(it)
            }.stackTrace.size shouldBe 4

            e.filterStackTrace(
                listOf(
                    "java.util",
                    "java.lang",
                    "kotlin.reflect",
                    "kotlin.coroutines",
                    "kotlinx.coroutines",
                    "io.kotest",
                    "sun.reflect",
                ).joinToString("|") { it.replace(".", "\\.") }.toRegex()
            ).also {
                logger.error(it)
            }.stackTrace.size shouldBe 4

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