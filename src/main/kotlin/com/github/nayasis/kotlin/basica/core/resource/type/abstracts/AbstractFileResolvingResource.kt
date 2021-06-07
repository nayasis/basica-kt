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
import com.github.nayasis.kotlin.basica.core.resource.util.URL_PROTOCOL_FILE
import com.github.nayasis.kotlin.basica.core.resource.util.URL_PROTOCOL_VFS
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
            val url = getURL()
            if (Resources.isFileURL(url)) {
                // Proceed with file system resolution
                getFile().exists()
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
                    getInputStream().close()
                    true
                }
            }
        } catch (ex: IOException) {
            false
        }
    }

    override fun isReadable(): Boolean {
        return try {
            val url = getURL()
            if (Resources.isFileURL(url)) {
                // Proceed with file system resolution
                val file = getFile()
                file.canRead() && !file.isDirectory
            } else {
                // Try InputStream resolution for jar resources
                val con = url.openConnection()
                customizeConnection(con)
                if (con is HttpURLConnection) {
                    val httpCon = con
                    val code = httpCon.responseCode
                    if (code != HttpURLConnection.HTTP_OK) {
                        httpCon.disconnect()
                        return false
                    }
                }
                val contentLength = con.contentLengthLong
                if (contentLength > 0) {
                    true
                } else if (contentLength == 0L) {
                    // Empty file or directory -> not considered readable...
                    false
                } else {
                    // Fall back to stream existence: can we open the stream?
                    getInputStream().close()
                    true
                }
            }
        } catch (ex: IOException) {
            false
        }
    }

    override fun isFile(): Boolean {
        return try {
            val url = getURL()
            if (url.protocol.startsWith(URL_PROTOCOL_VFS)) {
                VfsResourceDelegate.getResource(url).isFile()
            } else URL_PROTOCOL_FILE == url.protocol
        } catch (ex: IOException) {
            false
        }
    }

    /**
     * This implementation returns a File reference for the underlying class path
     * resource, provided that it refers to a file in the file system.
     */
    @Throws(IOException::class)
    override fun getFile(): File {
        val url = getURL()
        return if (url.protocol.startsWith(URL_PROTOCOL_VFS)) {
            VfsResourceDelegate.getResource(url).getFile()
        } else Resources.getFile(url, getDescription())
    }

    /**
     * This implementation determines the underlying File
     * (or jar file, in case of a resource in a jar/zip).
     */
    @Throws(IOException::class)
    override fun getFileForLastModifiedCheck(): File {
        val url = getURL()
        return if (Resources.isJarURL(url)) {
            val actualUrl = Resources.extractArchiveURL(url)
            if (actualUrl.protocol.startsWith(URL_PROTOCOL_VFS)) {
                VfsResourceDelegate.getResource(
                    actualUrl
                ).getFile()
            } else Resources.getFile(actualUrl, "Jar URL")
        } else {
            getFile()
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
            if (uri.scheme.startsWith(URL_PROTOCOL_VFS)) {
                VfsResourceDelegate.getResource(uri).isFile()
            } else URL_PROTOCOL_FILE == uri.scheme
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
        return if (uri.scheme.startsWith(URL_PROTOCOL_VFS)) {
            VfsResourceDelegate.getResource(uri).getFile()
        } else Resources.getFile(uri, getDescription())
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
            FileChannel.open(getFile().toPath(), StandardOpenOption.READ)
        } catch (ex: FileNotFoundException) {
            // Fall back to InputStream adaptation in superclass
            super.readableChannel()
        } catch (ex: NoSuchFileException) {
            super.readableChannel()
        }
    }

    @Throws(IOException::class)
    override fun contentLength(): Long {
        val url = getURL()
        return if (Resources.isFileURL(url)) {
            // Proceed with file system resolution
            val file = getFile()
            val length = file.length()
            if (length == 0L && !file.exists()) {
                throw FileNotFoundException(
                    "${getDescription()} cannot be resolved in the file system for checking its content length"
                )
            }
            length
        } else {
            // Try a URL connection content-length header
            val con = url.openConnection()
            customizeConnection(con)
            con.contentLengthLong
        }
    }

    @Throws(IOException::class)
    override fun lastModified(): Long {
        val url = getURL()
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
                "${getDescription()} cannot be resolved in the file system for checking its last-modified timestamp"
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
     * @param con the HttpURLConnection to customize
     * @throws IOException if thrown from HttpURLConnection methods
     */
    @Throws(IOException::class)
    protected fun customizeConnection(con: HttpURLConnection) {
        con.requestMethod = "HEAD"
    }

    /**
     * Inner delegate class, avoiding a hard JBoss VFS API dependency at runtime.
     */
    private object VfsResourceDelegate {
        @Throws(IOException::class)
        fun getResource(url: URL?): Resource = VfsResource(VfsUtils.getRoot(url))

        @Throws(IOException::class)
        fun getResource(uri: URI?): Resource = VfsResource(VfsUtils.getRoot(uri))
    }
}