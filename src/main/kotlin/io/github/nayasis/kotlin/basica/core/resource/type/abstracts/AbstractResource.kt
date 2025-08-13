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
package io.github.nayasis.kotlin.basica.core.resource.type.abstracts

import io.github.nayasis.kotlin.basica.core.resource.type.interfaces.Resource
import io.github.nayasis.kotlin.basica.core.url.toUri
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
            getFile().exists()
        } catch (ex: IOException) {
            // Fall back to stream existence: can we open the stream?
            try {
                getInputStream().close()
                true
            } catch (isEx: Throwable) {
                false
            }
        }
    }

    /**
     * This implementation always returns `true` for a resource
     * that [exists][.exists] (revised as of 5.1).
     */
    override fun isReadable(): Boolean {
        return exists()
    }

    /**
     * This implementation always returns `false`.
     */
    override fun isOpen(): Boolean {
        return false
    }

    /**
     * This implementation always returns `false`.
     */
    override fun isFile(): Boolean {
        return false
    }

    /**
     * This implementation throws a FileNotFoundException, assuming
     * that the resource cannot be resolved to a URL.
     */
    @Throws(IOException::class)
    override fun getURL(): URL {
        throw FileNotFoundException("${getDescription()} cannot be resolved to URL")
    }

    /**
     * This implementation builds a URI based on the URL returned
     * by [.getURL].
     */
    @Throws(IOException::class)
    override fun getURI(): URI {
        return try {
            getURL().toUri()
        } catch (e: URISyntaxException) {
            throw IOException("Invalid URI [${getURL()}]", e)
        }
    }

    /**
     * This implementation throws a FileNotFoundException, assuming
     * that the resource cannot be resolved to an absolute file path.
     */
    @Throws(IOException::class)
    override fun getFile(): File {
        throw FileNotFoundException("${getDescription()} cannot be resolved to absolute file path")
    }

    /**
     * This implementation returns [Channels.newChannel]
     * with the result of [.getInputStream].
     *
     * This is the same as in [Resource]'s corresponding default method
     * but mirrored here for efficient JVM-level dispatching in a class hierarchy.
     */
    @Throws(IOException::class)
    override fun readableChannel(): ReadableByteChannel {
        return Channels.newChannel(getInputStream())
    }

    /**
     * This implementation reads the entire InputStream to calculate the
     * content length. Subclasses will almost always be able to provide
     * a more optimal version of this, e.g. checking a File length.
     * @see .getInputStream
     */
    @Throws(IOException::class)
    override fun contentLength(): Long {
        return try {
            getInputStream().use { stream ->
                var size: Long = 0
                val buf = ByteArray(256)
                var read: Int
                while (stream.read(buf).also { read = it } != -1) {
                    size += read.toLong()
                }
                size

            }
        } catch (e: Exception) {0}
    }

    /**
     * This implementation checks the timestamp of the underlying File,
     * if available.
     * @see .getFileForLastModifiedCheck
     */
    @Throws(IOException::class)
    override fun lastModified(): Long {
        val fileToCheck = getFileForLastModifiedCheck()
        val lastModified = fileToCheck.lastModified()
        if (lastModified == 0L && !fileToCheck.exists()) {
            throw FileNotFoundException(
                "${getDescription()} cannot be resolved in the file system for checking its last-modified timestamp"
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
    protected open fun getFileForLastModifiedCheck(): File = getFile()

    /**
     * This implementation throws a FileNotFoundException, assuming
     * that relative resources cannot be created for this resource.
     */
    @Throws(IOException::class)
    override fun createRelative(relativePath: String): Resource {
        throw FileNotFoundException("Cannot create a relative resource for ${getDescription()}")
    }

    /**
     * This implementation always returns `null`,
     * assuming that this resource type does not have a filename.
     */
    override fun getFilename(): String {
        return ""
    }

    /**
     * This implementation compares description strings.
     * @see .getDescription
     */
    override fun equals(other: Any?): Boolean {
        return this === other || other is Resource && other.getDescription() == getDescription()
    }

    /**
     * This implementation returns the description's hash code.
     * @see .getDescription
     */
    override fun hashCode(): Int {
        return getDescription().hashCode()
    }

    /**
     * This implementation returns the description of this resource.
     * @see .getDescription
     */
    override fun toString(): String {
        return getDescription()
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