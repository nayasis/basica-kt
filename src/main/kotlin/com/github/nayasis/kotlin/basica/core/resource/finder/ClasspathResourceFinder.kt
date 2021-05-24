package com.github.nayasis.kotlin.basica.core.resource.finder

import com.github.nayasis.kotlin.basica.core.resource.loader.ResourceLoader
import com.github.nayasis.kotlin.basica.core.resource.type.UrlResource
import com.github.nayasis.kotlin.basica.core.resource.type.interfaces.Resource
import com.github.nayasis.kotlin.basica.core.resource.util.Resources.URL_PREFIX_FILE
import com.github.nayasis.kotlin.basica.core.resource.util.Resources.URL_PREFIX_JAR
import com.github.nayasis.kotlin.basica.core.resource.util.Resources.URL_SEPARATOR_JAR
import mu.KotlinLogging
import java.io.File
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.net.URLClassLoader
import java.util.*

private val log = KotlinLogging.logger {}

class ClasspathResourceFinder(private val resourceLoader: ResourceLoader) {
    /**
     * Find all class location resources with the given location via the ClassLoader.
     * Delegates to [.findAllClassPathResources].
     * @param location the absolute path within the classpath
     * @return the result as Resource array
     * @throws IOException in case of I/O errors
     * @see ClassLoader.getResources
     */
    @Throws(IOException::class)
    fun findAll(location: String): Set<Resource> {
        var path = location
        if (path.startsWith("/")) {
            path = path.substring(1)
        }
        val result = findAllClassPathResources(path)
        log.trace("Resolved classpath location [{}] to resources {}", location, result)
        return result
    }

    /**
     * Find all class location resources with the given path via the ClassLoader.
     * @param path the absolute path within the classpath (never a leading slash)
     * @return a mutable Set of matching Resource instances
     * @since 4.1.1
     */
    @Throws(IOException::class)
    private fun findAllClassPathResources(path: String): Set<Resource> {
        val result: MutableSet<Resource> = LinkedHashSet(16)
        val cl = resourceLoader.getClassLoader()
        val urls: Enumeration<URL> = if (cl != null) cl.getResources(path) else ClassLoader.getSystemResources(path)
        while (urls.hasMoreElements()) {
            result.add(UrlResource(urls.nextElement()))
        }
        if ("" == path) {
            // The above result is likely to be incomplete, i.e. only containing file system references.
            // We need to have pointers to each of the jar files on the classpath as well...
            addAllClassLoaderJarRoots(cl, result)
        }
        return result
    }

    /**
     * Search all [URLClassLoader] URLs for jar file references and add them to the
     * given set of resources in the form of pointers to the root of the jar file content.
     * @param classLoader the ClassLoader to search (including its ancestors)
     * @param result the set of resources to add jar roots to
     * @since 4.1.1
     */
    protected fun addAllClassLoaderJarRoots(classLoader: ClassLoader?, result: MutableSet<Resource>) {
        if (classLoader is URLClassLoader) {
            try {
                for (url in (classLoader as URLClassLoader?)?.urLs ?: emptyArray()) {
                    try {
                        val jarResource = UrlResource(URL_PREFIX_JAR + url + URL_SEPARATOR_JAR)
                        if (jarResource.exists()) {
                            result.add(jarResource)
                        }
                    } catch (e: MalformedURLException) {
                        log.debug{
                            "Cannot search for matching files underneath [$url] because it cannot be converted to a valid 'jar:' URL: ${e.message}"
                        }
                    }
                }
            } catch (e: Exception) {
                log.debug{
                    "Cannot introspect jar files since ClassLoader [$classLoader] does not support 'getURLs()': $e"
                }
            }
        }
        if (classLoader === ClassLoader.getSystemClassLoader()) {
            // "java.class.path" manifest evaluation...
            addClassPathManifestEntries(result)
        }
        if (classLoader != null) {
            try {
                addAllClassLoaderJarRoots(classLoader.parent, result)
            } catch (e: Exception) {
                log.debug{
                    "Cannot introspect jar files in parent ClassLoader since [$classLoader] does not support 'getParent()': $e"
                }
            }
        }
    }

    /**
     * Determine jar file references from the "java.class.path." manifest property and add them
     * to the given set of resources in the form of pointers to the root of the jar file content.
     * @param result the set of resources to add jar roots to
     * @since 4.3
     */
    protected fun addClassPathManifestEntries(result: MutableSet<Resource>) {
        try {
            val javaClassPathProperty = System.getProperty("java.class.path")
            for (path in javaClassPathProperty.split(File.separator.replace("\\", "\\\\"))) {
                try {
                    var filePath: String = File(path).absolutePath
                    val prefixIndex = filePath.indexOf(':')
                    if (prefixIndex == 1) {
                        // Possibly "c:" drive prefix on Windows, to be upper-cased for proper duplicate detection
                        filePath = filePath.capitalize()
                    }
                    val jarResource = UrlResource(
                        URL_PREFIX_JAR +
                            URL_PREFIX_FILE + filePath + URL_SEPARATOR_JAR
                    )
                    // Potentially overlapping with URLClassLoader.getURLs() result above!
                    if (!result.contains(jarResource) && !hasDuplicate(filePath, result) && jarResource.exists()) {
                        result.add(jarResource)
                    }
                } catch (e: MalformedURLException) {
                    log.debug{
                        "Cannot search for matching files underneath [$path] because it cannot be converted to a valid 'jar:' URL: ${e.message}"
                    }
                }
            }
        } catch (e: Exception) {
            log.debug{ "Failed to evaluate 'java.class.path' manifest entries: $e" }
        }
    }

    /**
     * Check whether the given file path has a duplicate but differently structured entry
     * in the existing result, i.e. with or without a leading slash.
     * @param filePath the file path (with or without a leading slash)
     * @param result the current result
     * @return `true` if there is a duplicate (i.e. to ignore the given file path),
     * `false` to proceed with adding a corresponding resource to the current result
     */
    private fun hasDuplicate(filePath: String, result: Set<Resource>): Boolean {
        if (result.isEmpty()) return false
        val duplicatePath = if (filePath.startsWith("/")) filePath.substring(1) else "/$filePath"
        return try {
            result.contains(
                UrlResource("${URL_PREFIX_JAR}${URL_PREFIX_FILE}${duplicatePath}${URL_SEPARATOR_JAR}")
            )
        } catch (e: MalformedURLException) {
            false
        }
    }

}