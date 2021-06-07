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

import com.github.nayasis.kotlin.basica.core.resource.type.abstracts.AbstractResource
import com.github.nayasis.kotlin.basica.core.resource.type.interfaces.Resource
import com.github.nayasis.kotlin.basica.core.resource.util.VfsUtils
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.URL

/**
 * JBoss VFS based [Resource] implementation.
 *
 *
 * As of Spring 4.0, this class supports VFS 3.x on JBoss AS 6+ (package
 * `org.jboss.vfs`) and is in particular compatible with JBoss AS 7 and
 * WildFly 8.
 *
 * @author Ales Justin
 * @author Juergen Hoeller
 * @author Costin Leau
 * @author Sam Brannen
 * @since 3.0
 */
class VfsResource(private val resource: Any): AbstractResource() {

    @Throws(IOException::class)
    override fun getInputStream(): InputStream {
        return VfsUtils.getInputStream(resource)
    }

    override fun exists(): Boolean {
        return VfsUtils.exists(resource)
    }

    override fun isReadable(): Boolean {
        return VfsUtils.isReadable(resource)
    }

    @Throws(IOException::class)
    override fun getURL(): URL {
        return try {
            VfsUtils.getURL(resource)
        } catch (e: Exception) {
            throw IOException("Failed to obtain URL for file ${resource}", e)
        }
    }

    @Throws(IOException::class)
    override fun getURI(): URI {
        return try {
            VfsUtils.getURI(resource)
        } catch (e: Exception) {
            throw IOException("Failed to obtain URI for file ${resource}", e)
        }
    }

    @Throws(IOException::class)
    override fun getFile(): File {
        return VfsUtils.getFile(resource)
    }

    @Throws(IOException::class)
    override fun contentLength(): Long {
        return VfsUtils.getSize(resource)
    }

    @Throws(IOException::class)
    override fun lastModified(): Long {
        return VfsUtils.getLastModified(resource)
    }

    @Throws(IOException::class)
    override fun createRelative(relativePath: String): Resource {
        if (!relativePath.startsWith(".") && relativePath.contains("/")) {
            try {
                return VfsResource(VfsUtils.getChild(resource, relativePath))
            } catch (ex: IOException) {
                // fall back to getRelative
            }
        }
        return VfsResource(
            VfsUtils.getRelative(URL(getURL(), relativePath))
        )
    }

    override fun getFilename(): String {
        return VfsUtils.getName(resource)
    }

    override fun getDescription(): String {
        return "VFS resource [$resource]"
    }

    override fun equals(other: Any?): Boolean {
        return this === other || other is VfsResource && resource == other.resource
    }

    override fun hashCode(): Int {
        return resource.hashCode()
    }

}