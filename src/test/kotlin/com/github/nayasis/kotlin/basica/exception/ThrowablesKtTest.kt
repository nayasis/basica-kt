package com.github.nayasis.kotlin.basica.exception

import com.github.nayasis.kotlin.basica.etc.error
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.github.oshai.kotlinlogging.KotlinLogging
import java.security.InvalidParameterException

private val logger = KotlinLogging.logger {}

class ThrowablesKtTest: AnnotationSpec() {

    @Test
    fun filterStackTrace() {

        try {
            stack1()
        } catch (e: Exception) {

            e.filterStackTrace("""
                com\.github\.nayasis
            """.trimIndent().toRegex()).also {
                logger.error(it)
            }.stackTrace.size shouldBe 4

            e.filterStackTrace(listOf(
                "java.util",
                "java.lang",
                "kotlin.reflect",
                "kotlin.coroutines",
                "kotlinx.coroutines",
                "io.kotest",
                "sun.reflect",
            ).map { it.replace(".","\\.") }.joinToString("|").toRegex(),false).also {
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