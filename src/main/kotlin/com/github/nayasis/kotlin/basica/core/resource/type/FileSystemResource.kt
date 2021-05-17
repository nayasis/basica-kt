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
import com.github.nayasis.kotlin.basica.core.resource.util.PathModifier
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.net.URL
import java.nio.channels.FileChannel
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * [Resource] implementation for `java.io.File` and
 * `java.nio.file.Path` handles with a file system target.
 * Supports resolution as a `File` and also as a `URL`.
 * Implements the extended [WritableResource] interface.
 *
 *
 * Note: As of Spring Framework 5.0, this [Resource] implementation uses
 * NIO.2 API for read/write interactions. As of 5.1, it may be constructed with a
 * [Path] handle in which case it will perform all file system
 * interactions via NIO.2, only resorting to [File] on [.getFile].
 *
 * @author Juergen Hoeller
 * @since 28.12.2003
 * @see .FileSystemResource
 * @see .FileSystemResource
 * @see File
 *
 * @see Files
 */
class FileSystemResource: AbstractResource, WritableResource {
    /**
     * Return the file path for this resource.
     */
    val path: String
    private val file: File?
    private val filePath: Path

    /**
     * Create a new `FileSystemResource` from a file path.
     *
     * Note: When building relative resources via [.createRelative],
     * it makes a difference whether the specified resource base path here
     * ends with a slash or not. In the case of "C:/dir1/", relative paths
     * will be built underneath that root: e.g. relative path "dir2" ->
     * "C:/dir1/dir2". In the case of "C:/dir1", relative paths will apply
     * at the same directory level: relative path "dir2" -> "C:/dir2".
     * @param path a file path
     * @see .FileSystemResource
     */
    constructor(path: String?) {
        this.path = PathModifier.clean(path)
        file = File(path)
        filePath = file.toPath()
    }

    /**
     * Create a new `FileSystemResource` from a [File] handle.
     *
     * Note: When building relative resources via [.createRelative],
     * the relative path will apply *at the same directory level*:
     * e.g. new File("C:/dir1"), relative path "dir2" -> "C:/dir2"!
     * If you prefer to have relative paths built underneath the given root directory,
     * use the [constructor with a file path][.FileSystemResource]
     * to append a trailing slash to the root path: "C:/dir1/", which indicates
     * this directory as root for all relative paths.
     * @param file a File handle
     * @see .FileSystemResource
     * @see .getFile
     */
    constructor(file: File) {
        path = PathModifier.clean(file.path)
        this.file = file
        filePath = file.toPath()
    }

    /**
     * Create a new `FileSystemResource` from a [Path] handle,
     * performing all file system interactions via NIO.2 instead of [File].
     *
     * In contrast to PathResource, this variant strictly follows the
     * general [com.github.nayasis.basica.resource.type.FileSystemResource] conventions, in particular in terms of
     * path cleaning and [.createRelative] handling.
     * @param filePath a Path handle to a file
     * @since 5.1
     * @see .FileSystemResource
     */
    constructor(filePath: Path) {
        path = PathModifier.clean(filePath.toString())
        file = null
        this.filePath = filePath
    }

    /**
     * Create a new `FileSystemResource` from a [FileSystem] handle,
     * locating the specified path.
     *
     * This is an alternative to [.FileSystemResource],
     * performing all file system interactions via NIO.2 instead of [File].
     * @param fileSystem the FileSystem to locate the path within
     * @param path a file path
     * @since 5.1.1
     * @see .FileSystemResource
     */
    constructor(fileSystem: FileSystem, path: String) {
        this.path = PathModifier.clean(path)
        file = null
        filePath = fileSystem.getPath(this.path).normalize()
    }

    /**
     * This implementation returns whether the underlying file exists.
     * @see File.exists
     */
    override fun exists(): Boolean {
        return if (file != null) file.exists() else Files.exists(filePath)
    }

    /**
     * This implementation checks whether the underlying file is marked as readable
     * (and corresponds to an actual file with content, not to a directory).
     * @see File.canRead
     * @see File.isDirectory
     */
    override fun isReadable(): Boolean {
        return if (file != null) file.canRead() && !file.isDirectory else Files.isReadable(filePath) && !Files.isDirectory(
            filePath
        )
    }

    /**
     * This implementation opens a NIO file stream for the underlying file.
     * @see java.io.FileInputStream
     */
    @Throws(IOException::class)
    override fun getInputStream(): InputStream {
        return try {
            Files.newInputStream(filePath)
        } catch (ex: NoSuchFileException) {
            throw FileNotFoundException(ex.message)
        }
    }

    /**
     * This implementation checks whether the underlying file is marked as writable
     * (and corresponds to an actual file with content, not to a directory).
     * @see File.canWrite
     * @see File.isDirectory
     */
    override fun isWritable(): Boolean {
        return if (file != null) file.canWrite() && !file.isDirectory else Files.isWritable(filePath) && !Files.isDirectory(
            filePath
        )
    }

    /**
     * This implementation opens a FileOutputStream for the underlying file.
     * @see java.io.FileOutputStream
     */
    @Throws(IOException::class)
    override fun getOutputStream(): OutputStream {
        return Files.newOutputStream(filePath)
    }

    /**
     * This implementation returns a URL for the underlying file.
     * @see File.toURI
     */
    @Throws(IOException::class)
    override fun getURL(): URL {
        return if (file != null) file.toURI().toURL() else filePath.toUri().toURL()
    }

    /**
     * This implementation returns a URI for the underlying file.
     * @see File.toURI
     */
    @Throws(IOException::class)
    override fun getURI(): URI {
        return if (file != null) file.toURI() else filePath.toUri()
    }

    /**
     * This implementation always indicates a file.
     */
    override fun isFile(): Boolean {
        return true
    }

    /**
     * This implementation returns the underlying File reference.
     */
    override fun getFile(): File {
        return file ?: filePath.toFile()
    }

    /**
     * This implementation opens a FileChannel for the underlying file.
     * @see FileChannel
     */
    @Throws(IOException::class)
    override fun readableChannel(): ReadableByteChannel {
        return try {
            FileChannel.open(filePath, StandardOpenOption.READ)
        } catch (ex: NoSuchFileException) {
            throw FileNotFoundException(ex.message)
        }
    }

    /**
     * This implementation opens a FileChannel for the underlying file.
     * @see FileChannel
     */
    @Throws(IOException::class)
    override fun writableChannel(): WritableByteChannel {
        return FileChannel.open(filePath, StandardOpenOption.WRITE)
    }

    /**
     * This implementation returns the underlying File/Path length.
     */
    @Throws(IOException::class)
    override fun contentLength(): Long {
        return if (file != null) {
            val length = file.length()
            if (length == 0L && !file.exists()) {
                throw FileNotFoundException(
                    description +
                        " cannot be resolved in the file system for checking its content length"
                )
            }
            length
        } else {
            try {
                Files.size(filePath)
            } catch (ex: NoSuchFileException) {
                throw FileNotFoundException(ex.message)
            }
        }
    }

    /**
     * This implementation returns the underlying File/Path last-modified time.
     */
    @Throws(IOException::class)
    override fun lastModified(): Long {
        return if (file != null) {
            super.lastModified()
        } else {
            try {
                Files.getLastModifiedTime(filePath).toMillis()
            } catch (ex: NoSuchFileException) {
                throw FileNotFoundException(ex.message)
            }
        }
    }

    /**
     * This implementation creates a FileSystemResource, applying the given path
     * relative to the path of the underlying file of this resource descriptor.
     */
    override fun createRelative(relativePath: String): Resource {
        val pathToUse = applyRelativePath(path, relativePath)
        return if (file != null) FileSystemResource(pathToUse) else FileSystemResource(
            filePath.fileSystem, pathToUse
        )
    }

    /**
     * This implementation returns the name of the file.
     * @see File.getName
     */
    override fun getFilename(): String {
        return if (file != null) file.name else filePath.fileName.toString()
    }

    /**
     * This implementation returns a description that includes the absolute
     * path of the file.
     * @see File.getAbsolutePath
     */
    override fun getDescription(): String {
        return "file [" + (if (file != null) file.absolutePath else filePath.toAbsolutePath()) + "]"
    }

    /**
     * This implementation compares the underlying File references.
     */
    override fun equals(other: Any?): Boolean {
        return this === other || other is FileSystemResource && path == other.path
    }

    /**
     * This implementation returns the hash code of the underlying File reference.
     */
    override fun hashCode(): Int {
        return path.hashCode()
    }
}