/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.nayasis.kotlin.basica.core.resource

import com.github.nayasis.kotlin.basica.core.resource.finder.ClasspathResourceFinder
import com.github.nayasis.kotlin.basica.core.resource.finder.FileResourceFinder
import com.github.nayasis.kotlin.basica.core.resource.finder.JarResourceFinder
import com.github.nayasis.kotlin.basica.core.resource.finder.VfsResourceFinder
import com.github.nayasis.kotlin.basica.core.resource.invocation.EquinoxInvoker
import com.github.nayasis.kotlin.basica.core.resource.loader.DefaultResourceLoader
import com.github.nayasis.kotlin.basica.core.resource.loader.ResourceLoader
import com.github.nayasis.kotlin.basica.core.resource.matcher.AntPathMatcher
import com.github.nayasis.kotlin.basica.core.resource.matcher.PathMatcher
import com.github.nayasis.kotlin.basica.core.resource.resolver.ResourcePatternResolver
import com.github.nayasis.kotlin.basica.core.resource.type.UrlResource
import com.github.nayasis.kotlin.basica.core.resource.type.interfaces.Resource
import com.github.nayasis.kotlin.basica.core.resource.util.Resources
import com.github.nayasis.kotlin.basica.core.resource.util.URL_PREFIX_CLASSPATH
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.IOException

private val log = KotlinLogging.logger {}

class PathMatchingResourceLoader: ResourcePatternResolver {

    private val resourceLoader: ResourceLoader = DefaultResourceLoader()
    private var pathMatcher: PathMatcher = AntPathMatcher()
    private val fileFinder = FileResourceFinder(pathMatcher)
    private val vfsFinder = VfsResourceFinder(pathMatcher)
    private val jarFinder = JarResourceFinder(pathMatcher)
    private val classpathFinder = ClasspathResourceFinder(resourceLoader)
    private val equinoxInvoker = EquinoxInvoker()

    override fun getClassLoader(): ClassLoader? {
        return resourceLoader.getClassLoader()
    }

    override fun getResource(location: String): Resource {
        return resourceLoader.getResource(location)
    }

    @Throws(IOException::class)
    override fun getResources(pattern: String): Set<Resource> {
        if ( pattern.isEmpty() ) return emptySet()
        return if (isClasspath(pattern)) {
            // a class path resource (multiple resources for same name possible)
            if (pathMatcher.isPattern(pattern.substring(URL_PREFIX_CLASSPATH.length))) {
                // a class path resource pattern
                findResources(pattern)
            } else {
                // all class path resources with the given name
                classpathFinder.findAll(pattern.substring(URL_PREFIX_CLASSPATH.length))
            }
        } else {
            // Generally only look for a pattern after a prefix here,
            // and on Tomcat only after the "*/" separator for its "war:" protocol.
            val prefixEnd = if (pattern.startsWith("war:")) pattern.indexOf("*/") + 1 else pattern.indexOf(':') + 1
            if (pathMatcher.isPattern(pattern.substring(prefixEnd))) {
                // a file pattern
                findResources(pattern)
            } else {
                // a single resource with the given name
                LinkedHashSet<Resource>().apply { add(getResource(pattern)) }
            }
        }
    }

    private fun isClasspath(pattern: String): Boolean = pattern.startsWith(URL_PREFIX_CLASSPATH)

    /**
     * find all resources matched with given pattern.
     * it could find resources in JAR, ZIP, File system.
     *
     * @param pattern    pattern to match
     * @return matched resources
     */
    @Throws(IOException::class)
    private fun findResources(pattern: String): Set<Resource> {
        val rootDir = getRootDir(pattern)
        val remain = pattern.substring(rootDir.length)
        val result = LinkedHashSet<Resource>(16)
        for (resource in getResources(rootDir)) {
            var rootResource = resource
            var rootUrl = rootResource.getURL()
            if( equinoxInvoker.isEquinoxUrl(rootUrl) ) {
                equinoxInvoker.unwrap(rootUrl)?.let{ rootUrl = it }
                rootResource = UrlResource(rootUrl)
            }
            when {
                Resources.isVfsURL(rootUrl) -> result.addAll(vfsFinder.find(rootUrl, remain))
                Resources.isJarURL(rootUrl) -> result.addAll(jarFinder.find(rootResource, rootUrl, remain))
                else -> result.addAll(fileFinder.find(rootResource, remain))
            }
        }
        return result
    }

    /**
     * extract root directory for given path to determine file matching starting point.
     *
     * ex. "classpath:/WEB-INF/ *.xml" returns "classpath:/WEB-INF"
     *
     * @param path
     * @return root directory
     */
    private fun getRootDir(path: String): String {
        val prefixEnd = path.indexOf(':') + 1
        var rootDirEnd = path.length

        // climb up directory until remain path is not matched with pattern
        while( rootDirEnd > prefixEnd && pathMatcher.isPattern(path.substring(prefixEnd, rootDirEnd))) {
            rootDirEnd = path.lastIndexOf('/', rootDirEnd - 2) + 1
        }
        if (rootDirEnd == 0) rootDirEnd = prefixEnd
        return path.substring(0, rootDirEnd)
    }

}