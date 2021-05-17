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
package com.github.nayasis.kotlin.basica.core.resource.type.interfaces

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.URL
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel

interface Resource {
    /**
     * Determine whether this resource actually exists in physical form.
     *
     * This method performs a definitive existence check, whereas the
     * existence of a `Resource` handle only guarantees a valid
     * descriptor handle.
     */
    fun exists(): Boolean

    /**
     * Indicate whether non-empty contents of this resource can be read via
     * [.getInputStream].
     *
     * Will be `true` for typical resource descriptors that exist
     * since it strictly implies [.exists] semantics as of 5.1.
     * Note that actual content reading may still fail when attempted.
     * However, a value of `false` is a definitive indication
     * that the resource content cannot be read.
     * @see .getInputStream
     * @see .exists
     */
    val isReadable: Boolean
        get() = exists()

    /**
     * Indicate whether this resource represents a handle with an open stream.
     * If `true`, the InputStream cannot be read multiple times,
     * and must be read and closed to avoid resource leaks.
     *
     * Will be `false` for typical resource descriptors.
     */
    val isOpen: Boolean
        get() = false

    /**
     * Determine whether this resource represents a file in a file system.
     * A value of `true` strongly suggests (but does not guarantee)
     * that a [.getFile] call will succeed.
     *
     * This is conservatively `false` by default.
     * @since 5.0
     * @see .getFile
     */
    val isFile: Boolean
        get() = false

    /**
     * Return a URL handle for this resource.
     * @throws IOException if the resource cannot be resolved as URL,
     * i.e. if the resource is not available as descriptor
     */
    @get:Throws(IOException::class)
    val url: URL

    /**
     * Return a URI handle for this resource.
     * @throws IOException if the resource cannot be resolved as URI,
     * i.e. if the resource is not available as descriptor
     * @since 2.5
     */
    @get:Throws(IOException::class)
    val uri: URI

    /**
     * Return a File handle for this resource.
     * @throws java.io.FileNotFoundException if the resource cannot be resolved as
     * absolute file path, i.e. if the resource is not available in a file system
     * @throws IOException in case of general resolution/reading failures
     * @see .getInputStream
     */
    @get:Throws(IOException::class)
    val file: File

    /**
     * Return a [ReadableByteChannel].
     *
     * It is expected that each call creates a *fresh* channel.
     *
     * The default implementation returns [Channels.newChannel]
     * with the result of [.getInputStream].
     * @return the byte channel for the underlying resource (must not be `null`)
     * @throws java.io.FileNotFoundException if the underlying resource doesn't exist
     * @throws IOException if the content channel could not be opened
     * @since 5.0
     * @see .getInputStream
     */
    @Throws(IOException::class)
    fun readableChannel(): ReadableByteChannel? {
        return Channels.newChannel(inputStream)
    }

    /**
     * Determine the content length for this resource.
     * @throws IOException if the resource cannot be resolved
     * (in the file system or as some other known physical resource type)
     */
    @get:Throws(IOException::class)
    val contentLength: Long

    /**
     * Determine the last-modified timestamp for this resource.
     * @throws IOException if the resource cannot be resolved
     * (in the file system or as some other known physical resource type)
     */
    @get:Throws(IOException::class)
    val lastModified: Long

    /**
     * Create a resource relative to this resource.
     * @param relativePath the relative path (relative to this resource)
     * @return the resource handle for the relative resource
     * @throws IOException if the relative resource cannot be determined
     */
    @Throws(IOException::class)
    fun createRelative(relativePath: String): Resource

    /**
     * Determine a filename for this resource, i.e. typically the last
     * part of the path: for example, "myfile.txt".
     *
     * Returns `null` if this type of resource does not
     * have a filename.
     */
    val filename: String?

    /**
     * Return a description for this resource,
     * to be used for error output when working with the resource.
     *
     * Implementations are also encouraged to return this value
     * from their `toString` method.
     * @see Object.toString
     */
    val description: String?

    /**
     * Return an [InputStream] for the content of an underlying resource.
     *
     * It is expected that each call creates a *fresh* stream.
     *
     * This requirement is particularly important when you consider an API such
     * as JavaMail, which needs to be able to read the stream multiple times when
     * creating mail attachments. For such a use case, it is *required*
     * that each `getInputStream()` call returns a fresh stream.
     * @return the input stream for the underlying resource (must not be `null`)
     * @throws java.io.FileNotFoundException if the underlying resource doesn't exist
     * @throws IOException if the content stream could not be opened
     */
    @get:Throws(IOException::class)
    val inputStream: InputStream
}