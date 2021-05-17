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

import com.github.nayasis.kotlin.basica.core.resource.type.VfsResource
import com.github.nayasis.kotlin.basica.core.resource.type.interfaces.Resource
import com.github.nayasis.kotlin.basica.core.resource.util.Resources
import com.github.nayasis.kotlin.basica.core.resource.util.VfsUtils
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLConnection
import java.nio.channels.FileChannel
import java.nio.channels.ReadableByteChannel
import java.nio.file.NoSuchFileException
import java.nio.file.StandardOpenOption

/**
 * Abstract base class for resources which resolve URLs into File references,
 * such as [UrlResource] or [ClassPathResource].
 *
 *
 * Detects the "file" protocol as well as the JBoss "vfs" protocol in URLs,
 * resolving file system references accordingly.
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
abstract class AbstractFileResolvingResource: AbstractResource() {
    override fun exists(): Boolean {
        return try {
            val url = url
            if (Resources.isFileURL(url)) {
                // Proceed with file system resolution
                file.exists()
            } else {
                // Try a URL connection content-length header
                val con = url.openConnection()
                customizeConnection(con)
                val httpCon = if (con is HttpURLConnection) con else null
                if (httpCon != null) {
                    val code = httpCon.responseCode
                    if (code == HttpURLConnection.HTTP_OK) {
                        return true
                    } else if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                        return false
                    }
                }
                if (con.contentLengthLong > 0) {
                    return true
                }
                if (httpCon != null) {
                    // No HTTP OK status, and no content-length header: give up
                    httpCon.disconnect()
                    false
                } else {
                    // Fall back to stream existence: can we open the stream?
                    inputStream.close()
                    true
                }
            }
        } catch (ex: IOException) {
            false
        }
    }

    override val isReadable: Boolean
        get() {
            try {
                val url = url
                if (Resources.isFileURL(url)) {
                    // Proceed with file system resolution
                    return file.let { it.canRead() && ! it.isDirectory }
                } else {
                    // Try InputStream resolution for jar resources
                    val conn = url.openConnection()
                    customizeConnection(conn)
                    if (conn is HttpURLConnection) {
                        val code = conn.responseCode
                        if (code != HttpURLConnection.HTTP_OK) {
                            conn.disconnect()
                            return false
                        }
                    }
                    val contentLength = conn.contentLengthLong
                    return when {
                        contentLength > 0 -> true
                        // Empty file or directory -> not considered readable...
                        contentLength == 0L -> false
                        // Fall back to stream existence: can we open the stream?
                        else -> {
                            inputStream.close(); true
                        }
                    }
                }
            } catch (ex: IOException) {
                return false
            }
        }

    override val isFile: Boolean
        get() = try {
            val url = url
            if (url.protocol.startsWith(Resources.URL_PROTOCOL_VFS)) {
                VfsResourceDelegate.getResource(url).isFile
            } else Resources.URL_PROTOCOL_FILE == url.protocol
        } catch (ex: IOException) {
            false
        }

    /**
     * This implementation returns a File reference for the underlying class path
     * resource, provided that it refers to a file in the file system.
     */
    override val file: File
        get() {
            val url = url
            return if (url.protocol.startsWith(Resources.URL_PROTOCOL_VFS)) {
                VfsResourceDelegate.getResource(
                    url
                ).file
            } else Resources.getFile(url, description)
        }

    /**
     * This implementation determines the underlying File
     * (or jar file, in case of a resource in a jar/zip).
     */
    override fun getFileForLastModifiedCheck(): File {
        val url = url
        return if (Resources.isJarURL(url)) {
            val actualUrl = Resources.extractArchiveURL(url)
            if (actualUrl.protocol.startsWith(Resources.URL_PROTOCOL_VFS)) {
                VfsResourceDelegate.getResource(
                    actualUrl
                ).file
            } else Resources.getFile(actualUrl, "Jar URL")
        } else {
            file
        }
    }

    /**
     * This implementation returns a File reference for the given URI-identified
     * resource, provided that it refers to a file in the file system.
     * @since 5.0
     * @see .getFile
     */
    protected fun isFile(uri: URI): Boolean {
        return try {
            if (uri.scheme.startsWith(Resources.URL_PROTOCOL_VFS)) {
                VfsResourceDelegate.getResource(uri).isFile
            } else Resources.URL_PROTOCOL_FILE == uri.scheme
        } catch (ex: IOException) {
            false
        }
    }

    /**
     * This implementation returns a File reference for the given URI-identified
     * resource, provided that it refers to a file in the file system.
     */
    @Throws(IOException::class)
    protected fun getFile(uri: URI): File {
        return if (uri.scheme.startsWith(Resources.URL_PROTOCOL_VFS)) {
            VfsResourceDelegate.getResource(
                uri
            ).file
        } else Resources.getFile(uri, description)
    }

    /**
     * This implementation returns a FileChannel for the given URI-identified
     * resource, provided that it refers to a file in the file system.
     * @since 5.0
     * @see .getFile
     */
    @Throws(IOException::class)
    override fun readableChannel(): ReadableByteChannel {
        return try {
            // Try file system channel
            FileChannel.open(file.toPath(), StandardOpenOption.READ)
        } catch (ex: FileNotFoundException) {
            // Fall back to InputStream adaptation in superclass
            super.readableChannel()
        } catch (ex: NoSuchFileException) {
            super.readableChannel()
        }
    }

    override val contentLength: Long
        get() {
            val url = url
            return if (Resources.isFileURL(url)) {
                // Proceed with file system resolution
                val file = file
                val length = file.length()
                if (length == 0L && !file.exists())
                    throw FileNotFoundException("$description cannot be resolved in the file system for checking its content length")
                length
            } else {
                // Try a URL connection content-length header
                val con = url.openConnection()
                customizeConnection(con)
                con.contentLengthLong
            }
        }

    override val lastModified: Long
        get() {
        val url = url
        var fileCheck = false
        if (Resources.isFileURL(url) || Resources.isJarURL(url)) {
            // Proceed with file system resolution
            fileCheck = true
            try {
                val fileToCheck = getFileForLastModifiedCheck()
                val lastModified = fileToCheck.lastModified()
                if (lastModified > 0L || fileToCheck.exists()) {
                    return lastModified
                }
            } catch (ex: FileNotFoundException) {
                // Defensively fall back to URL connection check instead
            }
        }
        // Try a URL connection last-modified header
        val con = url.openConnection()
        customizeConnection(con)
        val lastModified = con.lastModified
        if (fileCheck && lastModified == 0L && con.contentLengthLong <= 0) {
            throw FileNotFoundException(
                description +
                    " cannot be resolved in the file system for checking its last-modified timestamp"
            )
        }
        return lastModified
    }

    /**
     * Customize the given [URLConnection], obtained in the course of an
     * [.exists], [.contentLength] or [.lastModified] call.
     *
     * Calls [Resources.useCachesIfNecessary] and
     * delegates to [.customizeConnection] if possible.
     * Can be overridden in subclasses.
     * @param conn the URLConnection to customize
     * @throws IOException if thrown from URLConnection methods
     */
    @Throws(IOException::class)
    protected fun customizeConnection(conn: URLConnection?) {
        Resources.useCachesIfNecessary(conn)
        if (conn is HttpURLConnection) {
            customizeConnection(conn)
        }
    }

    /**
     * Customize the given [HttpURLConnection], obtained in the course of an
     * [.exists], [.contentLength] or [.lastModified] call.
     *
     * Sets request method "HEAD" by default. Can be overridden in subclasses.
     * @param conn the HttpURLConnection to customize
     * @throws IOException if thrown from HttpURLConnection methods
     */
    @Throws(IOException::class)
    protected fun customizeConnection(conn: HttpURLConnection) {
        conn.requestMethod = "HEAD"
    }

    /**
     * Inner delegate class, avoiding a hard JBoss VFS API dependency at runtime.
     */
    private object VfsResourceDelegate {
        @Throws(IOException::class)
        fun getResource(url: URL?): Resource {
            return VfsResource(VfsUtils.getRoot(url))
        }

        @Throws(IOException::class)
        fun getResource(uri: URI?): Resource {
            return VfsResource(VfsUtils.getRoot(uri))
        }
    }
}