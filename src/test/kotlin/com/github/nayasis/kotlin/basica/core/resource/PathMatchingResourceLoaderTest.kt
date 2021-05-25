package com.github.nayasis.kotlin.basica.core.resource

import com.github.nayasis.basica.resource.PathMatchingResourceLoader
import com.github.nayasis.basica.resource.util.Resources
import com.github.nayasis.basica.validation.Assert
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
            if (!Resources.isJarURL(resource.url)) continue
            hasJarUrl = true
            log.debug("resource : {}", resource.url)
        }
        Assert.beTrue(hasJarUrl, "there are no resources in JAR.")
    }

}