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
package com.github.nayasis.kotlin.basica.core.resource.type.abstracts

import com.github.nayasis.kotlin.basica.core.resource.type.interfaces.Resource
import com.github.nayasis.kotlin.basica.core.resource.util.Resources
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel

private const val SEPARATOR_FOLDER = "/"

/**
 * Convenience base class for [Resource] implementations,
 * pre-implementing typical behavior.
 *
 *
 * The "exists" method will check whether a File or InputStream can
 * be opened; "isOpen" will always return false; "getURL" and "getFile"
 * throw an exception; and "toString" will return the description.
 *
 * @author Juergen Hoeller
 * @since 28.12.2003
 */
abstract class AbstractResource: Resource {

    /**
     * This implementation checks whether a File can be opened,
     * falling back to whether an InputStream can be opened.
     * This will cover both directories and content resources.
     */
    override fun exists(): Boolean {
        // Try file existence: can we find the file in the file system?
        return try {
            file.exists()
        } catch (e: IOException) {
            // Fall back to stream existence: can we open the stream?
            try {
                inputStream.close()
                true
            } catch (ex: Throwable) {
                false
            }
        }
    }

    /**
     * This implementation throws a FileNotFoundException, assuming
     * that the resource cannot be resolved to a URL.
     */
    override val url: URL
        get() = throw FileNotFoundException("$description cannot be resolved to URL")

    /**
     * This implementation builds a URI based on the URL returned
     * by [.getURL].
     */
    override val uri: URI
        get() = try {
            Resources.toURI(url)
        } catch (e: URISyntaxException) {
            throw IOException("Invalid URI [$url]", e)
        }

    /**
     * This implementation throws a FileNotFoundException, assuming
     * that the resource cannot be resolved to an absolute file path.
     */
    override val file: File
        get() = throw FileNotFoundException("$description cannot be resolved to absolute file path")

    /**
     * This implementation returns [Channels.newChannel]
     * with the result of [.getInputStream].
     *
     * This is the same as in [Resource]'s corresponding default method
     * but mirrored here for efficient JVM-level dispatching in a class hierarchy.
     */
    @Throws(IOException::class)
    override fun readableChannel(): ReadableByteChannel {
        return Channels.newChannel(inputStream)
    }

    /**
     * This implementation reads the entire InputStream to calculate the
     * content length. Subclasses will almost always be able to provide
     * a more optimal version of this, e.g. checking a File length.
     * @see .getInputStream
     */
    override val contentLength: Long
        get() = inputStream.use {
            var size: Long = 0
            val buf = ByteArray(256)
            var read: Int
            while (it.read(buf).also { byte -> read = byte } != -1) {
                size += read.toLong()
            }
            size
        }

    /**
     * This implementation checks the timestamp of the underlying File,
     * if available.
     * @see .getFileForLastModifiedCheck
     */
    override val lastModified: Long
        get() {
            val fileToCheck = getFileForLastModifiedCheck()
            val lastModified = fileToCheck.lastModified()
            if (lastModified == 0L && !fileToCheck.exists()) {
                throw FileNotFoundException(
                    "${description} cannot be resolved in the file system for checking its last-modified timestamp"
                )
            }
            return lastModified
        }

    /**
     * Determine the File to use for timestamp checking.
     *
     * The default implementation delegates to [.getFile].
     * @return the File to use for timestamp checking (never `null`)
     * @throws FileNotFoundException if the resource cannot be resolved as
     * an absolute file path, i.e. is not available in a file system
     * @throws IOException in case of general resolution/reading failures
     */
    @Throws(IOException::class)
    protected open fun getFileForLastModifiedCheck(): File {
        return file
    }

    /**
     * This implementation throws a FileNotFoundException, assuming
     * that relative resources cannot be created for this resource.
     */
    override fun createRelative(relativePath: String): Resource {
        throw FileNotFoundException("Cannot create a relative resource for $description")
    }

    /**
     * This implementation always returns `null`,
     * assuming that this resource type does not have a filename.
     */
    override val filename: String?
        get() = null

    /**
     * This implementation compares description strings.
     * @see .getDescription
     */
    override fun equals(other: Any?): Boolean {
        return this === other || other is Resource && other.description == description
    }

    /**
     * This implementation returns the description's hash code.
     * @see .getDescription
     */
    override fun hashCode(): Int {
        return description.hashCode()
    }

    /**
     * This implementation returns the description of this resource.
     * @see .getDescription
     */
    override fun toString(): String {
        return description ?: ""
    }

    protected fun applyRelativePath(path: String, relativePath: String): String {
        val separatorIndex = path.lastIndexOf(SEPARATOR_FOLDER)
        return if (separatorIndex != -1) {
            var newPath = path.substring(0, separatorIndex)
            if (!relativePath.startsWith(SEPARATOR_FOLDER)) {
                newPath += SEPARATOR_FOLDER
            }
            newPath + relativePath
        } else {
            relativePath
        }
    }

 }