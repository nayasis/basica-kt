package com.github.nayasis.kotlin.basica.core.resource

import com.github.nayasis.kotlin.basica.core.resource.util.Resources
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

internal class PathMatchingResourceLoaderTest {

    @Test
    @Throws(IOException::class)
    fun findResources() {
        val loader = PathMatchingResourceLoader()
        val resources = loader.getResources("classpath:/message/*.prop")
        assertTrue(resources.isNotEmpty(), "there are no resources.")
    }

    @Test
    @Throws(IOException::class)
    fun findResourcesInJar() {
        var hasJarUrl = false
        val loader = PathMatchingResourceLoader()
        val resources = loader.getResources("classpath:/META-INF/LICENSE.md")
        for (resource in resources) {
            if (!Resources.isJarURL(resource.getURL())) continue
            hasJarUrl = true
            log.debug{"resource : ${resource.getURL()}"}
        }
        assertTrue(hasJarUrl, "there are no resources in JAR.")
    }

}