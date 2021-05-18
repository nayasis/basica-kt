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
package com.github.nayasis.kotlin.basica.`resource-backup`

import com.github.nayasis.basica.base.Strings
import com.github.nayasis.basica.resource.finder.ClasspathResourceFinder
import com.github.nayasis.basica.resource.finder.FileResourceFinder
import com.github.nayasis.basica.resource.finder.JarResourceFinder
import com.github.nayasis.basica.resource.finder.VfsResourceFinder
import com.github.nayasis.basica.resource.invocation.EquinoxInvocater
import com.github.nayasis.basica.resource.loader.DefaultResourceLoader
import com.github.nayasis.basica.resource.loader.ResourceLoader
import com.github.nayasis.basica.resource.matcher.AntPathMatcher
import com.github.nayasis.basica.resource.matcher.PathMatcher
import com.github.nayasis.basica.resource.resolver.ResourcePatternResolver
import com.github.nayasis.basica.resource.type.UrlResource
import com.github.nayasis.basica.resource.type.interfaces.Resource
import com.github.nayasis.basica.resource.util.Resources
import com.github.nayasis.basica.validation.Assert
import lombok.extern.slf4j.Slf4j

@Slf4j
class PathMatchingResourceLoader: ResourcePatternResolver {
    private val resourceLoader: ResourceLoader = DefaultResourceLoader()
    private var pathMatcher: PathMatcher = AntPathMatcher()
    private val fileFinder = FileResourceFinder(pathMatcher)
    private val vfsFinder = VfsResourceFinder(pathMatcher)
    private val jarFinder = JarResourceFinder(pathMatcher)
    private val classpathFinder = ClasspathResourceFinder(resourceLoader)
    override fun getClassLoader(): ClassLoader {
        return resourceLoader.classLoader
    }

    /**
     * Set PathMatcher.
     * default is AntPathMatcher.
     */
    fun setPathMatcher(pathMatcher: PathMatcher) {
        Assert.notNull(pathMatcher, "[pathMatcher] must not be null")
        this.pathMatcher = pathMatcher
        fileFinder.setPathMatcher(pathMatcher)
        vfsFinder.setPathMatcher(pathMatcher)
        jarFinder.setPathMatcher(pathMatcher)
    }

    override fun getResource(location: String): Resource {
        return resourceLoader.getResource(location)
    }

    @Throws(IOException::class)
    override fun getResources(pattern: String): Set<Resource> {
        if (Strings.isEmpty(pattern)) return LinkedHashSet()
        return if (isClasspath(pattern)) {
            // a class path resource (multiple resources for same name possible)
            if (pathMatcher.isPattern(pattern.substring(Resources.URL_PREFIX_CLASSPATH.length))) {
                // a class path resource pattern
                findResources(pattern)
            } else {
                // all class path resources with the given name
                classpathFinder.findAll(pattern.substring(Resources.URL_PREFIX_CLASSPATH.length))
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
                val resources: MutableSet<Resource> =
                    LinkedHashSet()
                resources.add(getResource(pattern))
                resources
            }
        }
    }

    private fun isClasspath(pattern: String): Boolean {
        return pattern.startsWith(Resources.URL_PREFIX_CLASSPATH)
    }

    /**
     * find all resources matched with given pattern.
     *
     *
     * it could find resources in JAR, ZIP, File system.
     *
     * @param pattern    pattern to match
     * @return    matched resources
     * @throws IOException occurs I/O errors
     */
    @Throws(IOException::class)
    private fun findResources(pattern: String): Set<Resource> {
        val ptnRoot = getRootDir(pattern)
        val ptnRemain = pattern.substring(ptnRoot.length)
        val result: MutableSet<Resource> = LinkedHashSet(16)
        for (root in getResources(ptnRoot)) {
            var rootUrl = root.url
            if (EquinoxInvocater.isEquinoxUrl(rootUrl)) {
                val unwrapped = EquinoxInvocater.unwrap(rootUrl)
                if (unwrapped != null) {
                    rootUrl = unwrapped
                }
                root = UrlResource(rootUrl)
            }
            if (Resources.isVfsURL(rootUrl)) {
                result.addAll(vfsFinder.find(rootUrl, ptnRemain))
            } else if (Resources.isJarURL(rootUrl)) {
                result.addAll(jarFinder.find(root, rootUrl, ptnRemain))
            } else {
                result.addAll(fileFinder.find(root, ptnRemain))
            }
        }
        return result
    }

    /**
     * extract root directory for given path to determine file matching starting point.
     *
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
        while (rootDirEnd > prefixEnd && pathMatcher.isPattern(path.substring(prefixEnd, rootDirEnd))) {
            rootDirEnd = path.lastIndexOf('/', rootDirEnd - 2) + 1
        }
        if (rootDirEnd == 0) rootDirEnd = prefixEnd
        return path.substring(0, rootDirEnd)
    }
}