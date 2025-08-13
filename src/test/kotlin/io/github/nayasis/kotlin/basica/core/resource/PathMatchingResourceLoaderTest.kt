package io.github.nayasis.kotlin.basica.core.resource

import io.github.nayasis.kotlin.basica.core.resource.util.Resources
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.shouldNotBe
import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

private val log = KotlinLogging.logger {}

internal class PathMatchingResourceLoaderTest: StringSpec({

    "find resources" {
        val loader = PathMatchingResourceLoader()
        val resources = loader.getResources("classpath:/message/*.prop")
        resources.isNotEmpty() shouldBe true
    }

    "find resources in JAR" {
        PathMatchingResourceLoader().getResources("classpath:/META-INF/LICENSE.md").firstOrNull{
            Resources.isJarURL(it.getURL())
        }.also { println(it) } shouldNotBe null
    }

})