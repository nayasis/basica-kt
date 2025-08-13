package io.github.nayasis.kotlin.basica.core.klass

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

private val log = KotlinLogging.logger {}

internal class ClassesTest: StringSpec({

    "get resource" {
        val resource = Classes.getResource("-1")
        log.debug { "resource : $resource" }
    }

    "is enum" {
        ExampleEnum.LOW::class.isEnum shouldBe true
    }

})

enum class ExampleEnum {
    LOW, HIGH
}