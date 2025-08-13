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
package io.github.nayasis.kotlin.basica.core.resource.type

import io.github.nayasis.kotlin.basica.core.io.name
import io.github.nayasis.kotlin.basica.core.resource.type.abstracts.AbstractFileResolvingResource
import io.github.nayasis.kotlin.basica.core.resource.type.interfaces.Resource
import io.github.nayasis.kotlin.basica.core.resource.util.PathModifier
import io.github.nayasis.kotlin.basica.core.resource.util.Resources
import io.github.nayasis.kotlin.basica.core.string.toPath
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL

private val log = KotlinLogging.logger {}

/**
 * [Resource] implementation for `java.net.URL` locators.
 * Supports resolution as a `URL` and also as a `File` in
 * case of the `"file:"` protocol.
 *
 * @author Juergen Hoeller
 * @since 28.12.2003
 * @see URL
 */
open class UrlResource: AbstractFileResolvingResource {
    /**
     * Original URI, if available; used for URI and File access.
     */
    private val uri: URI?

    /**
     * Original URL, used for actual access.
     */
    private val url: URL

    /**
     * Cleaned URL (with normalized path), used for comparisons.
     */
    private val cleanedUrl: URL

    /**
     * Create a new `UrlResource` based on the given URI object.
     * @param uri a URI
     * @throws MalformedURLException if the given URL path is not valid
     * @since 2.5
     */
    constructor(uri: URI) {
        this.uri = uri
        url = uri.toURL()
        cleanedUrl = getCleanedUrl(url, uri.toString())
    }

    /**
     * Create a new `UrlResource` based on the given URL object.
     * @param url a URL
     */
    constructor(url: URL) {
        this.url = url
        cleanedUrl = getCleanedUrl(this.url, url.toString())
        uri = null
    }

    /**
     * Create a new `UrlResource` based on a URL path.
     *
     * Note: The given path needs to be pre-encoded if necessary.
     * @param path a URL path
     * @throws MalformedURLException if the given URL path is not valid
     * @see URL.URL
     */
    constructor(path: String) {
        uri = null
        url = URL(path)
        cleanedUrl = getCleanedUrl(url, path)
    }
    /**
     * Create a new `UrlResource` based on a URI specification.
     *
     * The given parts will automatically get encoded if necessary.
     * @param protocol the URL protocol to use (e.g. "jar" or "file" - without colon);
     * also known as "scheme"
     * @param location the location (e.g. the file path within that protocol);
     * also known as "scheme-specific part"
     * @param fragment the fragment within that location (e.g. anchor on an HTML page,
     * as following after a "#" separator)
     * @throws MalformedURLException if the given URL specification is not valid
     * @see URI.URI
     */
    /**
     * Create a new `UrlResource` based on a URI specification.
     *
     * The given parts will automatically get encoded if necessary.
     * @param protocol the URL protocol to use (e.g. "jar" or "file" - without colon);
     * also known as "scheme"
     * @param location the location (e.g. the file path within that protocol);
     * also known as "scheme-specific part"
     * @throws MalformedURLException if the given URL specification is not valid
     * @see URI.URI
     */
    @JvmOverloads
    constructor(protocol: String?, location: String?, fragment: String? = null) {
        try {
            uri = URI(protocol, location, fragment)
            url = uri.toURL()
            cleanedUrl = getCleanedUrl(url, uri.toString())
        } catch (e: URISyntaxException) {
            throw MalformedURLException(e.message).apply { initCause(e) }
        }
    }

    /**
     * Determine a cleaned URL for the given original URL.
     * @param originalUrl the original URL
     * @param originalPath the original URL path
     * @return the cleaned URL (possibly the original URL as-is)
     */
    private fun getCleanedUrl(originalUrl: URL, originalPath: String): URL {
        val cleanedPath = PathModifier.clean(originalPath)
        log.trace{"""
            - original url  : $originalUrl
            - original path : $originalPath
            - cleaned path  : $cleanedPath
        """.trimIndent()}
        if (cleanedPath != originalPath) {
            try {
                return URL(cleanedPath)
            } catch (ex: MalformedURLException) {
                // Cleaned URL path cannot be converted to URL -> take original URL.
            }
        }
        return originalUrl
    }

    /**
     * This implementation opens an InputStream for the given URL.
     *
     * It sets the `useCaches` flag to `false`,
     * mainly to avoid jar file locking on Windows.
     * @see URL.openConnection
     * @see URLConnection.setUseCaches
     * @see URLConnection.getInputStream
     */
    @Throws(IOException::class)
    override fun getInputStream(): InputStream {
        val con = url.openConnection()
        Resources.useCachesIfNecessary(con)
        return try {
            con.getInputStream()
        } catch (ex: IOException) {
            // Close the HTTP connection (if applicable).
            if (con is HttpURLConnection) {
                con.disconnect()
            }
            throw ex
        }
    }

    /**
     * This implementation returns the underlying URL reference.
     */
    override fun getURL(): URL {
        return url
    }

    /**
     * This implementation returns the underlying URI directly,
     * if possible.
     */
    @Throws(IOException::class)
    override fun getURI(): URI {
        return uri ?: super.getURI()
    }

    override fun isFile(): Boolean {
        return if (uri != null) {
            super.isFile(uri)
        } else {
            super.isFile()
        }
    }

    /**
     * This implementation returns a File reference for the underlying URL/URI,
     * provided that it refers to a file in the file system.
     */
    @Throws(IOException::class)
    override fun getFile(): File {
        return if (uri != null) {
            super.getFile(uri)
        } else {
            super.getFile()
        }
    }

    /**
     * This implementation creates a `UrlResource`, applying the given path
     * relative to the path of the underlying URL of this resource descriptor.
     * @see URL.URL
     */
    @Throws(MalformedURLException::class)
    override fun createRelative(relativePath: String): Resource {
        val path = relativePath.let{
            if(it.startsWith("/")) it.substring(1) else it
        }
        return UrlResource(URL(url, path))
    }

    /**
     * This implementation returns the name of the file that this URL refers to.
     * @see URL.getPath
     */
    override fun getFilename(): String {
        return cleanedUrl.path.toPath().name
    }

    /**
     * This implementation returns a description that includes the URL.
     */
    override fun getDescription(): String {
        return "URL [$url]"
    }

    /**
     * This implementation compares the underlying URL references.
     */
    override fun equals(other: Any?): Boolean {
        return this === other || other is UrlResource && cleanedUrl == other.cleanedUrl
    }

    /**
     * This implementation returns the hash code of the underlying URL reference.
     */
    override fun hashCode(): Int {
        return cleanedUrl.hashCode()
    }
}