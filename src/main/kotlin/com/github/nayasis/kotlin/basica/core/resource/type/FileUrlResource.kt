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

import com.github.nayasis.kotlin.basica.core.resource.type.interfaces.Resource
import com.github.nayasis.kotlin.basica.core.resource.util.Resources
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.net.MalformedURLException
import java.net.URL
import java.nio.channels.FileChannel
import java.nio.channels.WritableByteChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption

/**
 * Subclass of [UrlResource] which assumes file resolution, to the degree
 * of implementing the [WritableResource] interface for it. This resource
 * variant also caches resolved [File] handles from [.getFile].
 *
 *
 * This is the class resolved by [DefaultResourceLoader] for a "file:..."
 * URL location, allowing a downcast to [WritableResource] for it.
 *
 *
 * Alternatively, for direct construction from a [File] handle
 * or NIO [java.nio.file.Path], consider using [FileSystemResource].
 *
 * @author Juergen Hoeller
 * @since 5.0.2
 */
class FileUrlResource: UrlResource, WritableResource {

    @Volatile
    private var file: File? = null

    /**
     * Create a new `FileUrlResource` based on the given URL object.
     *
     * Note that this does not enforce "file" as URL protocol. If a protocol
     * is known to be resolvable to a file,
     * @param url a URL
     * @see Resources.isFileURL
     * @see .getFile
     */
    constructor(url: URL): super(url) {}

    /**
     * Create a new `FileUrlResource` based on the given file location,
     * using the URL protocol "file".
     *
     * The given parts will automatically get encoded if necessary.
     * @param location the location (i.e. the file path within that protocol)
     * @throws MalformedURLException if the given URL specification is not valid
     * @see UrlResource.UrlResource
     * @see Resources.URL_PROTOCOL_FILE
     */
    constructor(location: String?): super(Resources.URL_PROTOCOL_FILE, location) {}

    @Throws(IOException::class)
    override fun getFile(): File {
        if( file != null ) return file!!
        this.file = super.getFile()
        return file!!
    }

    override fun isWritable(): Boolean {
        return try {
            val url = getURL()
            if (Resources.isFileURL(url)) {
                // Proceed with file system resolution
                val file = getFile()
                file.canWrite() && !file.isDirectory
            } else {
                true
            }
        } catch (ex: IOException) {
            false
        }
    }

    @Throws(IOException::class)
    override fun getOutputStream(): OutputStream {
        return Files.newOutputStream(getFile().toPath())
    }

    @Throws(IOException::class)
    override fun writableChannel(): WritableByteChannel {
        return FileChannel.open(getFile().toPath(), StandardOpenOption.WRITE)
    }

    @Throws(MalformedURLException::class)
    override fun createRelative(relativePath: String): Resource {
        var relativePath = relativePath.let{
            if(it.startsWith("/")) it.substring(1) else it
        }
        return FileUrlResource(URL(getURL(), relativePath))
    }
}