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
package com.github.nayasis.kotlin.basica.core.resource.util

import com.github.nayasis.kotlin.basica.core.klass.Classes
import com.github.nayasis.kotlin.basica.core.string.toUri
import com.github.nayasis.kotlin.basica.core.url.toUri
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.net.URLConnection
import java.util.jar.JarFile

const val URL_PREFIX_CLASSPATH = "classpath:"
const val URL_PREFIX_FILE      = "file:"
const val URL_PREFIX_JAR       = "jar:"
const val URL_PREFIX_WAR       = "war:"
const val URL_PROTOCOL_FILE    = "file"
const val URL_PROTOCOL_JAR     = "jar"
const val URL_PROTOCOL_WAR     = "war"
const val URL_PROTOCOL_ZIP     = "zip"
const val URL_PROTOCOL_WSJAR   = "wsjar"
const val URL_PROTOCOL_VFSZIP  = "vfszip"
const val URL_PROTOCOL_VFSFILE = "vfsfile"
const val URL_PROTOCOL_VFS     = "vfs"
const val FILE_EXTENSION_JAR   = ".jar"
const val URL_SEPARATOR_JAR    = "!/"
const val URL_SEPARATOR_WAR    = "*/"

object Resources {

    /**
     * Resolve the given resource location to a `java.net.URL`.
     *
     * Does not check whether the URL actually exists; simply returns
     * the URL that the given location would correspond to.
     * @param resourceLocation the resource location to resolve: either a
     * "classpath:" pseudo URL, a "file:" URL, or a plain file path
     * @return a corresponding URL object
     * @throws FileNotFoundException if the resource cannot be resolved to a URL
     */
    @Throws(FileNotFoundException::class)
    fun getURL(resourceLocation: String): URL {
        when {
            resourceLocation.startsWith(URL_PREFIX_CLASSPATH) -> {
                val path = resourceLocation.substring(URL_PREFIX_CLASSPATH.length)
                return Classes.getResource(path)
                    ?: throw FileNotFoundException("class path resource [$path] cannot be resolved to URL because it does not exist")
            }
            else -> {
                return try {
                    URL(resourceLocation)
                } catch (e: MalformedURLException) {
                    // no URL -> treat as file path
                    try {
                        File(resourceLocation).toURI().toURL()
                    } catch (e2: MalformedURLException) {
                        throw FileNotFoundException("Resource location [$resourceLocation] is neither a URL not a well-formed file path")
                    }
                }
            }
        }
    }

    /**
     * Resolve the given resource location to a `java.io.File`,
     * i.e. to a file in the file system.
     *
     * Does not check whether the file actually exists; simply returns
     * the File that the given location would correspond to.
     * @param resourceLocation the resource location to resolve: either a
     * "classpath:" pseudo URL, a "file:" URL, or a plain file path
     * @return a corresponding File object
     * @throws FileNotFoundException if the resource cannot be resolved to
     * a file in the file system
     */
    @Throws(FileNotFoundException::class)
    fun getFile(resourceLocation: String): File {
        when {
            resourceLocation.startsWith(URL_PREFIX_CLASSPATH) -> {
                val path = resourceLocation.substring(URL_PREFIX_CLASSPATH.length)
                val description = "class path resource [$path]"
                val url = Classes.getResource(path)
                    ?: throw FileNotFoundException("${description} cannot be resolved to absolute file path because it does not exist")
                return getFile(url, description)
            }
            else -> {
                return try {
                    getFile(URL(resourceLocation))
                } catch (ex: MalformedURLException) {
                    // no URL -> treat as file path
                    File(resourceLocation)
                }
            }
        }
    }

    /**
     * Resolve the given resource URL to a `java.io.File`,
     * i.e. to a file in the file system.
     * @param resourceUrl the resource URL to resolve
     * @return a corresponding File object
     * @throws FileNotFoundException if the URL cannot be resolved to
     * a file in the file system
     */
    @Throws(FileNotFoundException::class)
    fun getFile(resourceUrl: URL): File = getFile(resourceUrl, "URL")

    /**
     * Resolve the given resource URL to a `java.io.File`,
     * i.e. to a file in the file system.
     * @param resourceUrl the resource URL to resolve
     * @param description a description of the original resource that
     * the URL was created for (for example, a class path location)
     * @return a corresponding File object
     * @throws FileNotFoundException if the URL cannot be resolved to
     * a file in the file system
     */
    @Throws(FileNotFoundException::class)
    fun getFile(resourceUrl: URL, description: String?): File {
        when {
            URL_PROTOCOL_FILE != resourceUrl.protocol ->
                throw FileNotFoundException("${description} cannot be resolved to absolute file path because it does not reside in the file system: ${resourceUrl}")
            else -> {
                return try {
                    File(resourceUrl.toUri().schemeSpecificPart)
                } catch (ex: URISyntaxException) {
                    // Fallback for URLs that are not valid URIs (should hardly ever happen).
                    File(resourceUrl.file)
                }
            }
        }
    }

    /**
     * Resolve the given resource URI to a `java.io.File`,
     * i.e. to a file in the file system.
     * @param resourceUri the resource URI to resolve
     * @return a corresponding File object
     * @throws FileNotFoundException if the URL cannot be resolved to
     * a file in the file system
     * @since 2.5
     */
    @Throws(FileNotFoundException::class)
    fun getFile(resourceUri: URI): File = getFile(resourceUri, "URI")

    /**
     * Resolve the given resource URI to a `java.io.File`,
     * i.e. to a file in the file system.
     * @param resourceUri the resource URI to resolve
     * @param description a description of the original resource that
     * the URI was created for (for example, a class path location)
     * @return a corresponding File object
     * @throws FileNotFoundException if the URL cannot be resolved to
     * a file in the file system
     * @since 2.5
     */
    @Throws(FileNotFoundException::class)
    fun getFile(resourceUri: URI, description: String?): File {
        when {
            URL_PROTOCOL_FILE != resourceUri.scheme ->
                throw FileNotFoundException("${description} cannot be resolved to absolute file path because it does not reside in the file system: ${resourceUri}")
            else -> return File(resourceUri.schemeSpecificPart)
        }
    }

    /**
     * Determine whether the given URL points to a resource in the file system,
     * i.e. has protocol "file", "vfsfile" or "vfs".
     * @param url the URL to check
     * @return whether the URL has been identified as a file system URL
     */
    fun isFileURL(url: URL): Boolean =
        url.protocol in listOf(URL_PROTOCOL_FILE, URL_PROTOCOL_VFSFILE, URL_PROTOCOL_VFS)

    /**
     * Determine whether the given URL points to a resource in a jar file.
     * i.e. has protocol "jar", "war, ""zip", "vfszip" or "wsjar".
     * @param url the URL to check
     * @return whether the URL has been identified as a JAR URL
     */
    fun isJarURL(url: URL): Boolean =
        url.protocol in listOf(URL_PROTOCOL_JAR, URL_PROTOCOL_WAR, URL_PROTOCOL_ZIP, URL_PROTOCOL_VFSZIP, URL_PROTOCOL_WSJAR)

    fun isVfsURL(url: URL?): Boolean = url != null && url.protocol.startsWith(URL_PROTOCOL_VFS)

    /**
     * Determine whether the given URL points to a jar file itself,
     * that is, has protocol "file" and ends with the ".jar" extension.
     * @param url the URL to check
     * @return whether the URL has been identified as a JAR file URL
     * @since 4.1
     */
    fun isJarFileURL(url: URL?): Boolean =
        url != null && URL_PROTOCOL_FILE == url.protocol && url.path.toLowerCase().endsWith(FILE_EXTENSION_JAR)

    /**
     * Extract the URL for the actual jar file from the given URL
     * (which may point to a resource in a jar file or to a jar file itself).
     * @param jarUrl the original URL
     * @return the URL for the actual jar file
     * @throws MalformedURLException if no valid jar file URL could be extracted
     */
    @Throws(MalformedURLException::class)
    fun extractJarFileURL(jarUrl: URL): URL {
        val urlFile = jarUrl.file
        val separatorIndex = urlFile.indexOf(URL_SEPARATOR_JAR)
        return if (separatorIndex != -1) {
            var jarFile = urlFile.substring(0, separatorIndex)
            try {
                URL(jarFile)
            } catch (ex: MalformedURLException) {
                // Probably no protocol in original jar URL, like "jar:C:/mypath/myjar.jar".
                // This usually indicates that the jar file resides in the file system.
                if (!jarFile.startsWith("/")) {
                    jarFile = "/$jarFile"
                }
                URL("$URL_PREFIX_FILE$jarFile")
            }
        } else {
            jarUrl
        }
    }

    /**
     * Extract the URL for the outermost archive from the given jar/war URL
     * (which may point to a resource in a jar file or to a jar file itself).
     *
     * In the case of a jar file nested within a war file, this will return
     * a URL to the war file since that is the one resolvable in the file system.
     * @param jarUrl the original URL
     * @return the URL for the actual jar file
     * @throws MalformedURLException if no valid jar file URL could be extracted
     * @since 4.1.8
     * @see .extractJarFileURL
     */
    @Throws(MalformedURLException::class)
    fun extractArchiveURL(jarUrl: URL): URL {
        val urlFile = jarUrl.file
        val endIndex = urlFile.indexOf(URL_SEPARATOR_WAR)
        if (endIndex != -1) {
            // Tomcat's "war:file:...mywar.war*/WEB-INF/lib/myjar.jar!/myentry.txt"
            val warFile = urlFile.substring(0, endIndex)
            if (URL_PROTOCOL_WAR == jarUrl.protocol) {
                return URL(warFile)
            }
            val startIndex = warFile.indexOf(URL_PREFIX_WAR)
            if (startIndex != -1) {
                return URL(warFile.substring(startIndex + URL_PREFIX_WAR.length))
            }
        }

        // Regular "jar:file:...myjar.jar!/myentry.txt"
        return extractJarFileURL(jarUrl)
    }

    /**
     * Set the [&quot;useCaches&quot;][URLConnection.setUseCaches] flag on the
     * given connection, preferring `false` but leaving the
     * flag at `true` for JNLP based resources.
     * @param connection the URLConnection to set the flag on
     */
    fun useCachesIfNecessary(connection: URLConnection?) {
        if( connection != null )
            connection.useCaches = connection.javaClass.simpleName.startsWith("JNLP")
    }

    @Throws(IOException::class)
    fun getJarFile(url: String): JarFile {
        return if (url.startsWith(URL_PREFIX_FILE)) {
            try {
                JarFile(url.toUri().schemeSpecificPart)
            } catch (e: URISyntaxException) {
                // Fallback for URLs that are not valid URIs (should hardly ever happen).
                JarFile(url.substring(URL_PREFIX_FILE.length))
            }
        } else {
            JarFile(url)
        }
    }

}