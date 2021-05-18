/*
 * Copyright 2002-2017 the original author or authors.
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
package com.github.nayasis.kotlin.basica.`resource-backup`.loader

import com.github.nayasis.basica.base.Classes
import com.github.nayasis.basica.resource.loader.ResourceLoader
import com.github.nayasis.basica.resource.resolver.ProtocolResolver
import com.github.nayasis.basica.resource.type.ClassPathResource
import com.github.nayasis.basica.resource.type.FileUrlResource
import com.github.nayasis.basica.resource.type.UrlResource
import com.github.nayasis.basica.resource.type.interfaces.Resource
import com.github.nayasis.basica.resource.util.Resources
import com.github.nayasis.basica.validation.Assert
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

class DefaultResourceLoader: ResourceLoader {
    private var classLoader: ClassLoader
    private val protocolResolvers: MutableSet<ProtocolResolver> = LinkedHashSet(4)
    private val resourceCaches: MutableMap<Class<*>, Map<Resource, *>> = ConcurrentHashMap(4)

    /**
     * Create a new DefaultResourceLoader.
     *
     * ClassLoader access will happen using the thread context class loader
     * at the time of this ResourceLoader's initialization.
     * @see Thread.getContextClassLoader
     */
    constructor() {
        classLoader = Classes.getClassLoader()
    }

    /**
     * Create a new DefaultResourceLoader.
     * @param classLoader the ClassLoader to load class path resources with, or `null`
     * for using the thread context class loader at the time of actual resource access
     */
    constructor(classLoader: ClassLoader) {
        this.classLoader = classLoader
    }

    /**
     * Specify the ClassLoader to load class path resources with, or `null`
     * for using the thread context class loader at the time of actual resource access.
     *
     * The default is that ClassLoader access will happen using the thread context
     * class loader at the time of this ResourceLoader's initialization.
     */
    fun setClassLoader(classLoader: ClassLoader) {
        this.classLoader = classLoader
    }

    /**
     * Return the ClassLoader to load class path resources with.
     *
     * Will get passed to ClassPathResource's constructor for all
     * ClassPathResource objects created by this resource loader.
     */
    override fun getClassLoader(): ClassLoader {
        return if (classLoader != null) classLoader else Classes.getClassLoader()
    }

    /**
     * Register the given resolver with this resource loader, allowing for
     * additional protocols to be handled.
     *
     * Any such resolver will be invoked ahead of this loader's standard
     * resolution rules. It may therefore also override any default rules.
     * @since 4.3
     * @see .getProtocolResolvers
     */
    fun addProtocolResolver(resolver: ProtocolResolver) {
        Assert.notNull(resolver, "ProtocolResolver must not be null")
        protocolResolvers.add(resolver)
    }

    /**
     * Return the collection of currently registered protocol resolvers,
     * allowing for introspection as well as modification.
     * @since 4.3
     */
    fun getProtocolResolvers(): Collection<ProtocolResolver> {
        return protocolResolvers
    }

    /**
     * Obtain a cache for the given value type, keyed by [Resource].
     * @param valueType the value type, e.g. an ASM `MetadataReader`
     * @return the cache [Map], shared at the `ResourceLoader` level
     * @since 5.0
     */
    fun <T> getResourceCache(valueType: Class<T>): Map<Resource, T> {
        return resourceCaches.computeIfAbsent(valueType) { key: Class<*>? -> ConcurrentHashMap<Resource, Any?>() } as Map<Resource, T>
    }

    /**
     * Clear all resource caches in this resource loader.
     * @since 5.0
     * @see .getResourceCache
     */
    fun clearResourceCaches() {
        resourceCaches.clear()
    }

    override fun getResource(location: String): Resource {
        Assert.notNull(location, "Location must not be null")
        for (protocolResolver in protocolResolvers) {
            val resource = protocolResolver.resolve(location, this)
            if (resource != null) {
                return resource
            }
        }
        return if (location.startsWith("/")) {
            getResourceByPath(location)
        } else if (location.startsWith(Resources.URL_PREFIX_CLASSPATH)) {
            ClassPathResource(location.substring(Resources.URL_PREFIX_CLASSPATH.length), getClassLoader())
        } else {
            try {
                // Try to parse the location as a URL...
                val url = URL(location)
                if (Resources.isFileURL(url)) FileUrlResource(url) else UrlResource(url)
            } catch (ex: MalformedURLException) {
                // No URL -> resolve as resource path.
                getResourceByPath(location)
            }
        }
    }

    /**
     * Return a Resource handle for the resource at the given path.
     *
     * The default implementation supports class path locations. This should
     * be appropriate for standalone implementations but can be overridden,
     * e.g. for implementations targeted at a Servlet container.
     * @param path the path to the resource
     * @return the corresponding Resource handle
     * @see ClassPathResource
     */
    protected fun getResourceByPath(path: String?): Resource {
        return ClassPathContextResource(path, getClassLoader())
    }

    /**
     * ClassPathResource that explicitly expresses a context-relative path
     * through implementing the ContextResource interface.
     */
    protected class ClassPathContextResource(path: String?, classLoader: ClassLoader?):
        ClassPathResource(path, classLoader) {
        override fun createRelative(relativePath: String): Resource {
            val pathToUse = applyRelativePath(path, relativePath)
            return ClassPathContextResource(pathToUse, classLoader)
        }
    }
}