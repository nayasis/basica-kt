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
package com.github.nayasis.kotlin.basica.core.resource.type

import com.github.nayasis.kotlin.basica.core.klass.SEPARATOR_PACKAGE
import com.github.nayasis.kotlin.basica.core.klass.SEPARATOR_PATH
import com.github.nayasis.kotlin.basica.core.io.name
import com.github.nayasis.kotlin.basica.core.resource.type.abstracts.AbstractFileResolvingResource
import com.github.nayasis.kotlin.basica.core.resource.type.interfaces.Resource
import com.github.nayasis.kotlin.basica.core.resource.util.PathModifier
import com.github.nayasis.kotlin.basica.core.string.toPath
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.URL

/**
 * [Resource] implementation for class path resources. Uses either a
 * given [ClassLoader] or a given [Class] for loading resources.
 *
 *
 * Supports resolution as `java.io.File` if the class path
 * resource resides in the file system, but not for resources in a JAR.
 * Always supports resolution as URL.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 28.12.2003
 * @see ClassLoader.getResourceAsStream
 * @see Class.getResourceAsStream
 */
open class ClassPathResource: AbstractFileResolvingResource {
    /**
     * Return the path for this resource (as resource path within the class path).
     */
    val path: String
    private var classLoader: ClassLoader? = null
    private var clazz: Class<*>? = null
    /**
     * Create a new `ClassPathResource` for `ClassLoader` usage.
     * A leading slash will be removed, as the ClassLoader resource access
     * methods will not accept it.
     * @param path the absolute path within the classpath
     * @param classLoader the class loader to load the resource with,
     * or `null` for the thread context class loader
     * @see ClassLoader.getResourceAsStream
     */
    /**
     * Create a new `ClassPathResource` for `ClassLoader` usage.
     * A leading slash will be removed, as the ClassLoader resource access
     * methods will not accept it.
     *
     * The thread context class loader will be used for
     * loading the resource.
     * @param path the absolute path within the class path
     * @see ClassLoader.getResourceAsStream
     */
    @JvmOverloads
    constructor(path: String, classLoader: ClassLoader? = null) {
        var pathToUse = PathModifier.clean(path).let{
            if(it.startsWith("/")) it.substring(1) else it
        }
        this.path = pathToUse
        this.classLoader = classLoader ?: getClassLoader()
    }

    /**
     * Create a new `ClassPathResource` for `Class` usage.
     * The path can be relative to the given class, or absolute within
     * the classpath via a leading slash.
     * @param path relative or absolute path within the class path
     * @param clazz the class to load resources with
     * @see Class.getResourceAsStream
     */
    constructor(path: String, clazz: Class<*>?) {
        this.path = PathModifier.clean(path)
        this.clazz = clazz
    }

    /**
     * Create a new `ClassPathResource` with optional `ClassLoader`
     * and `Class`. Only for internal usage.
     * @param path relative or absolute path within the classpath
     * @param classLoader the class loader to load the resource with, if any
     * @param clazz the class to load resources with, if any
     */
    @Deprecated(
        """as of 4.3.13, in favor of selective use of
	  {@link #ClassPathResource(String, ClassLoader)} vs {@link #ClassPathResource(String, Class)}"""
    )
    protected constructor(path: String?, classLoader: ClassLoader?, clazz: Class<*>?) {
        this.path = PathModifier.clean(path)
        this.classLoader = classLoader
        this.clazz = clazz
    }

    /**
     * Return the ClassLoader that this resource will be obtained from.
     */
    fun getClassLoader(): ClassLoader {
        return if (clazz != null) clazz!!.classLoader else classLoader!!
    }

    /**
     * This implementation checks for the resolution of a resource URL.
     * @see ClassLoader.getResource
     * @see Class.getResource
     */
    override fun exists(): Boolean {
        return resolveURL() != null
    }

    /**
     * Resolves a URL for the underlying class path resource.
     * @return the resolved URL, or `null` if not resolvable
     */
    protected fun resolveURL(): URL? {
        return if (clazz != null) {
            clazz!!.getResource(path)
        } else if (classLoader != null) {
            classLoader!!.getResource(path)
        } else {
            ClassLoader.getSystemResource(path)
        }
    }

    /**
     * This implementation opens an InputStream for the given class path resource.
     * @see ClassLoader.getResourceAsStream
     * @see Class.getResourceAsStream
     */
    @Throws(IOException::class)
    override fun getInputStream(): InputStream {
        return when {
            clazz != null -> clazz!!.getResourceAsStream(path)
            classLoader != null -> classLoader!!.getResourceAsStream(path)
            else -> ClassLoader.getSystemResourceAsStream(path)
        } ?: throw FileNotFoundException("${getDescription()} cannot be opened because it does not exist")
    }

    /**
     * This implementation returns a URL for the underlying class path resource,
     * if available.
     * @see ClassLoader.getResource
     * @see Class.getResource
     */
    @Throws(IOException::class)
    override fun getURL(): URL {
        return resolveURL()
            ?: throw FileNotFoundException("${getDescription()} cannot be resolved to URL because it does not exist")
    }

    /**
     * This implementation creates a ClassPathResource, applying the given path
     * relative to the path of the underlying resource of this descriptor.
     */
    override fun createRelative(relativePath: String): Resource {
        val pathToUse = applyRelativePath(path, relativePath)
        return if (clazz != null) ClassPathResource(pathToUse, clazz) else ClassPathResource(pathToUse, classLoader)
    }

    /**
     * This implementation returns the name of the file that this class path
     * resource refers to.
     */
    override fun getFilename(): String {
        return path.toPath().name
    }

    /**
     * This implementation returns a description that includes the class path location.
     */
    override fun getDescription(): String {
        val builder = StringBuilder("class path resource [")
        var pathToUse = path
        if (clazz != null && !pathToUse.startsWith("/")) {
            builder.append(classPackageAsResourcePath(clazz))
            builder.append('/')
        }
        if (pathToUse.startsWith("/")) {
            pathToUse = pathToUse.substring(1)
        }
        builder.append(pathToUse)
        builder.append(']')
        return builder.toString()
    }

    private fun classPackageAsResourcePath(clazz: Class<*>?): String {
        if (clazz == null) return ""
        val className = clazz.name
        val packageEndIndex = className.lastIndexOf(SEPARATOR_PACKAGE)
        if (packageEndIndex == -1) {
            return ""
        }
        val packageName = className.substring(0, packageEndIndex)
        return packageName.replace(SEPARATOR_PACKAGE, SEPARATOR_PATH)
    }

    /**
     * This implementation compares the underlying class path locations.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ClassPathResource) return false
        return path == other.path && classLoader == other.classLoader && clazz == other.clazz
    }

    /**
     * This implementation returns the hash code of the underlying
     * class path location.
     */
    override fun hashCode(): Int {
        return path.hashCode()
    }

}