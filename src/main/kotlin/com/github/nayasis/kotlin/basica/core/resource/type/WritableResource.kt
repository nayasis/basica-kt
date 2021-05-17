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
package com.github.nayasis.kotlin.basica.core.resource.type

import com.github.nayasis.basica.resource.type.interfaces.Resource
import java.io.IOException
import java.io.OutputStream
import java.nio.channels.Channels
import java.nio.channels.WritableByteChannel

/**
 * Extended interface for a resource that supports writing to it.
 * Provides an [OutputStream accessor][.getOutputStream].
 *
 * @author Juergen Hoeller
 * @since 3.1
 * @see OutputStream
 */
interface WritableResource: Resource {
    /**
     * Indicate whether the contents of this resource can be written
     * via [.getOutputStream].
     *
     * Will be `true` for typical resource descriptors;
     * note that actual content writing may still fail when attempted.
     * However, a value of `false` is a definitive indication
     * that the resource content cannot be modified.
     * @see .getOutputStream
     * @see .isReadable
     */
    val isWritable: Boolean
        get() = true

    /**
     * Return an [OutputStream] for the underlying resource,
     * allowing to (over-)write its content.
     * @throws IOException if the stream could not be opened
     * @see .getInputStream
     */
    @get:Throws(IOException::class)
    val outputStream: OutputStream?

    /**
     * Return a [WritableByteChannel].
     *
     * It is expected that each call creates a *fresh* channel.
     *
     * The default implementation returns [Channels.newChannel]
     * with the result of [.getOutputStream].
     * @return the byte channel for the underlying resource (must not be `null`)
     * @throws java.io.FileNotFoundException if the underlying resource doesn't exist
     * @throws IOException if the content channel could not be opened
     * @since 5.0
     * @see .getOutputStream
     */
    @Throws(IOException::class)
    fun writableChannel(): WritableByteChannel? {
        return Channels.newChannel(outputStream)
    }
}