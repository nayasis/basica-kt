@file:JvmMultifileClass

package com.github.nayasis.kotlin.basica.core.io

import com.github.nayasis.kotlin.basica.core.io.Paths.Companion.FOLDER_SEPARATOR_UNIX
import com.github.nayasis.kotlin.basica.core.io.Paths.Companion.FOLDER_SEPARATOR_WINDOWS
import com.github.nayasis.kotlin.basica.core.string.invariantSeparators
import com.github.nayasis.kotlin.basica.core.string.toFile
import com.github.nayasis.kotlin.basica.core.string.toPath
import org.mozilla.universalchardet.UniversalDetector
import java.io.*
import java.net.URI
import java.net.URL
import java.nio.charset.Charset
import java.nio.file.*
import java.nio.file.StandardOpenOption.APPEND
import java.nio.file.attribute.*
import java.nio.file.spi.FileSystemProvider
import java.util.stream.Stream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.io.path.Path
import kotlin.io.path.inputStream
import kotlin.reflect.KClass
import kotlin.streams.toList

class Paths { companion object {

    val FOLDER_SEPARATOR = File.separatorChar
    const val FOLDER_SEPARATOR_UNIX    = '/'
    const val FOLDER_SEPARATOR_WINDOWS = '\\'

    fun get(first: String, vararg more: String): Path =
        FileSystems.getDefault().getPath(first.trim(),*more)

    fun get(uri: URI): Path {
        val scheme = uri.scheme ?: throw IllegalArgumentException("Missing scheme")
        if(scheme.equals("file",ignoreCase=true))
            return FileSystems.getDefault().provider().getPath(uri)
        for(provider in FileSystemProvider.installedProviders()) {
            if(provider.scheme.equals(scheme,ignoreCase=true))
                return provider.getPath(uri)
        }
        throw FileSystemNotFoundException("Provider \"$scheme\" not installed")
    }

    val userHome: Path
        get() = System.getProperty("user.home").toPath()

    val applicationRoot: Path
        get() = get("").toAbsolutePath()

    /**
     * create an empty file in the default temporary directory (vary by OS).
     *
     * @param directory base directory in which to create file (default: OS's default temporary directory)
     * @param prefix temporary file's prefix name
     * @param suffix temporary file's suffix name
     * @param attributes attribute options to create file
     * @return temporary file
     */
    fun makeTempFile(directory: Path? = null, prefix: String? = null, suffix: String? = null, vararg attributes: FileAttribute<*>): Path =
        if(directory != null)
            Files.createTempFile(directory, prefix, suffix, *attributes)
        else
            Files.createTempFile(prefix, suffix, *attributes)

    /**
     * create a empty directory in the default temporary directory (vary by OS).
     *
     * @param directory base directory in which to create directory (default: OS's default temporary directory)
     * @param prefix temporary directory's prefix name
     * @param attributes attribute options to create directory
     * @return temporary directory
     */
    fun makeTempDirectory(directory: Path? = null, prefix: String? = null, vararg attributes: FileAttribute<*>): Path =
        if(directory != null)
            Files.createTempDirectory(directory, prefix, *attributes)
        else
            Files.createTempDirectory(prefix, *attributes)

}}

val Path.name: String
    get() = fileName?.toString().orEmpty()

val Path.nameWithoutExtension: String
    get() = fileName?.toString()?.substringBeforeLast(".") ?: ""

val Path.extension: String
    get() = fileName?.toString()?.substringAfterLast('.', "") ?: ""

val Path.pathWithoutExtension: String
    get() = pathWithoutExtension().pathString

val Path.invariantSeparators: String
    get() = pathString.invariantSeparators()

val Path.pathString: String
    get() {
        return toString().let{
            if( it.length > 1 && it.last() in listOf(FOLDER_SEPARATOR_UNIX, FOLDER_SEPARATOR_WINDOWS) ) {
                it.substring(0, it.length - 1)
            } else {
                it
            }
        }
    }

val Path.directory: Path
    get() {
        return if( exists() ) {
            if( isDirectory() ) this else this.parent
        } else {
            this.parent
        }
    }

fun Path.pathWithoutExtension(): Path = parent / nameWithoutExtension

fun Path.toUrl(): URL = this.toUri().toURL()

/**
 * Calculates the relative path for this path from a [base] path.
 *
 * Note that the [base] path is treated as a directory.
 * If this path matches the [base] path, then a [Path] with an empty path will be returned.
 *
 * @return the relative path from [base] to this.
 */
fun Path.toRelative(base: Path): Path? = try {
    PathRelativizer.tryRelativeTo(this, base)
} catch (e: IllegalArgumentException) {
    null
}

/**
 * Calculates the relative path for this path from a [base] path.
 *
 * Note that the [base] path is treated as a directory.
 * If this path matches the [base] path, then a [Path] with an empty path will be returned.
 *
 * @return the relative path from [base] to this, or `null` if this and base paths have different roots.
 */
fun Path.toRelativeOrSelf(base: Path): Path = toRelative(base) ?: this

/**
 * Calculates the relative path for this path from a [base] path.
 *
 * Note that the [base] path is treated as a directory.
 * If this path matches the [base] path, then a [Path] with an empty path will be returned.
 *
 * @return the relative path from [base] to this.
 */
fun Path.toRelative(base: String): Path? = this.toRelative(base.toPath())

/**
 * Calculates the relative path for this path from a [base] path.
 *
 * Note that the [base] path is treated as a directory.
 * If this path matches the [base] path, then a [Path] with an empty path will be returned.
 *
 * @return the relative path from [base] to this, or `null` if this and base paths have different roots.
 */
fun Path.toRelativeOrSelf(base: String): Path = this.toRelativeOrSelf(base.toPath())

/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
private object PathRelativizer {
    private val emptyPath = Paths.get("")
    private val parentPath = Paths.get("..")

    // Workarounds some bugs in Path.relativize that were fixed only in JDK9
    fun tryRelativeTo(path: Path, base: Path): Path {
        val bn = base.normalize()
        val pn = path.normalize()
        val rn = bn.relativize(pn)
        // work around https://bugs.openjdk.java.net/browse/JDK-8066943
        for (i in 0 until minOf(bn.nameCount, pn.nameCount)) {
            if (bn.getName(i) != parentPath) break
            if (pn.getName(i) != parentPath) throw IllegalArgumentException("Unable to compute relative path")
        }
        // work around https://bugs.openjdk.java.net/browse/JDK-8072495
        val r = if (pn != bn && bn == emptyPath) {
            pn
        } else {
            val rnString = rn.toString()
            // drop invalid dangling separator from path string https://bugs.openjdk.java.net/browse/JDK-8140449
            if (rnString.endsWith(rn.fileSystem.separator))
                rn.fileSystem.getPath(rnString.dropLast(rn.fileSystem.separator.length))
            else
                rn
        }
        return r
    }
}

fun Path?.exists(vararg options: LinkOption): Boolean = this != null && Files.exists(this, *options)
fun Path?.notExists(vararg options: LinkOption): Boolean = this == null || Files.notExists(this, *options)
fun Path?.isRegularFile(vararg options: LinkOption): Boolean = this != null && Files.isRegularFile(this, *options)
fun Path?.isFile(vararg options: LinkOption): Boolean = this != null && Files.isRegularFile(this, *options)
fun Path?.isDirectory(vararg options: LinkOption): Boolean = this != null && Files.isDirectory(this, *options)

fun Path?.isSymbolicLink(): Boolean = this != null && Files.isSymbolicLink(this)
fun Path?.isExecutable(): Boolean = this != null && Files.isExecutable(this)
fun Path?.isHidden(): Boolean = this != null && Files.isHidden(this)
fun Path?.isReadable(): Boolean = this != null && Files.isReadable(this)
fun Path?.isWritable(): Boolean = this != null && Files.isWritable(this)
fun Path?.isSameFile(other: Path): Boolean = this != null && Files.isSameFile(this,other)

val Path?.fileSize: Long
    get() = Files.size(this)
val Path?.fileStore: FileStore
    get() = Files.getFileStore(this)

fun Path.getAttribute(key: String, vararg options: LinkOption): Any? = Files.getAttribute(this,key,*options)
fun Path.setAttribute(key: String, value: Any?, vararg options: LinkOption): Any? = Files.setAttribute(this,key,value,*options)
fun Path.getLastModifiedTime(vararg options: LinkOption): FileTime = Files.getLastModifiedTime(this,*options)
fun Path.setLastModifiedTime(time: FileTime): Path = Files.setLastModifiedTime(this,time)
fun Path.setLastModifiedTime(time: Long): Path = Files.setLastModifiedTime(this, FileTime.fromMillis(time))
fun Path.getOwner(vararg options: LinkOption): UserPrincipal? = Files.getOwner(this,*options)
fun Path.setOwner(owner: UserPrincipal): Path = Files.setOwner(this,owner)
fun Path.getPermissions(vararg options: LinkOption): Set<PosixFilePermission> = Files.getPosixFilePermissions(this,*options)
fun Path.setPermissions(permissions: Set<PosixFilePermission>): Path = Files.setPosixFilePermissions(this,permissions)

inline fun <reified T: BasicFileAttributes> Path.getAttributes(vararg options: LinkOption): T = Files.readAttributes(this,T::class.java,*options)
inline fun <reified T: BasicFileAttributeView> Path.getAttributeView(vararg options: LinkOption): T = Files.getFileAttributeView(this,T::class.java,*options)
fun Path.copyAttribute(target: Path) {
    val src = this.getAttributes<BasicFileAttributes>()
    var trg = target.getAttributeView<BasicFileAttributeView>()
    trg.setTimes(
        src.lastModifiedTime(),
        src.lastAccessTime(),
        src.creationTime(),
    )
}

fun Path.makeDir(vararg attributes: FileAttribute<*>): Path = Files.createDirectories(this, *attributes)
fun Path.makeHardLink(target: Path): Path = Files.createLink(this,target)
fun Path.makeSymbolicLink(target: Path, vararg attributes: FileAttribute<*>): Path = Files.createSymbolicLink(this,target,*attributes)
fun Path.readSymbolicLink(): Path = Files.readSymbolicLink(this)

fun Path.makeFile(vararg attributes: FileAttribute<*>): Path {
    if( isFile() ) return this
    parent.makeDir()
    return Files.createFile(this, *attributes)
}

fun Path.delete(recursive: Boolean = true): Boolean {
    if(notExists()) return false
    if( recursive && isDirectory() ) {
        Files.walkFileTree(this, object: SimpleFileVisitor<Path>() {
            override fun postVisitDirectory(dir: Path, e: IOException?): FileVisitResult? {
                if (e != null) throw e
                Files.delete(dir)
                return FileVisitResult.CONTINUE
            }
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                Files.delete(file)
                return FileVisitResult.CONTINUE
            }
        })
    } else {
        Files.delete(this)
    }
    return true
}

/**
 * update 'last updated timestamp' on file
 */
fun Path.touch(): Boolean {
    return try {
        setLastModifiedTime( System.currentTimeMillis() )
        true
    } catch (e: Exception) {
        false
    }
}

fun Path.copy(target: Path, overwrite: Boolean = true, vararg options: CopyOption): Path {
    if( this.notExists() )
        throw IOException("source($this) must exist.")
    if( this.isDirectory() ) {
        if( target.isFile() )
            throw IOException("cannot overwrite directory($this) to file($target)")
        return if( target.exists() ) {
            this.copyTree(target.resolve(this.fileName).makeDir(),overwrite,*options)
        } else {
            this.copyTree(target.makeDir(),overwrite,*options)
        }
    } else {
        val opt = toCopyOptions(overwrite, options)
        return if( target.isDirectory() ) {
            Files.copy(this,target.resolve(this.fileName),*opt)
        } else {
            target.parent.makeDir()
            Files.copy(this,target,*opt)
        }
    }
}

fun Path.move(target: Path, overwrite: Boolean = true, vararg options: CopyOption): Path {
    if( this.notExists() )
        throw IOException("source($this) must exist.")
    return if( this.isDirectory() && target.exists() ) {
        if( target.isFile() )
            throw IOException("cannot overwrite directory($this) to file($target)")
        Files.move(this,target.resolve(this.fileName),*toCopyOptions(overwrite, options))
    } else {
        target.parent.makeDir()
        Files.move(this,target,*toCopyOptions(overwrite, options))
    }
}

private fun toCopyOptions(overwrite: Boolean, options: Array<out CopyOption>): Array<CopyOption> {
    val set = options.toMutableSet()
    if (overwrite) {
        set.add(StandardCopyOption.REPLACE_EXISTING)
    } else {
        set.remove(StandardCopyOption.REPLACE_EXISTING)
    }
    return set.toTypedArray()
}

fun Path.copyTree(target: Path, overwrite: Boolean = true, vararg options: CopyOption): Path {
    if( ! isDirectory() )
        throw IOException("source($this) must be directory.")
    val opt = toCopyOptions(overwrite,options)
    val source = this
    Files.walkFileTree(this, object: SimpleFileVisitor<Path>() {
        override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
            target.resolve(source.relativize(dir)).makeDir()
            return FileVisitResult.CONTINUE
        }
        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
            Files.copy(file, target.resolve(source.relativize(file)), *opt)
            return FileVisitResult.CONTINUE
        }
    })
    return target
}

/**
 * search files or directories.
 *
 * @param glob   path matching glob expression
 * <pre>
 * ** : ignore directory variation
 * *  : filename LIKE search
 *
 * 1. **.xml           : all files having "xml" extension below searchDir and it's all sub directories.
 * 2. *.xml            : all files having "xml" extension in searchDir
 * 3. c:\home\*\*.xml  : all files having "xml" extension below 'c:\home\' and it's just 1 depth below directories.
 * 4. c:\home\**\*.xml : all files having "xml" extension below 'c:\home\' and it's all sub directories.
 *
 * 1. *  It matches zero , one or more than one characters. While matching, it will not cross directories boundaries.
 * 2. ** It does the same as * but it crosses the directory boundaries.
 * 3. ?  It matches only one character for the given name.
 * 4. \  It helps to avoid characters to be interpreted as special characters.
 * 5. [] In a set of characters, only single character is matched. If (-) hyphen is used then, it matches a range of characters. Example: [efg] matches "e","f" or "g" . [a-d] matches a range from a to d.
 * 6. {} It helps to matches the group of sub patterns.
 *
 * 1. *.java when given path is java , we will get true by PathMatcher.matches(path).
 * 2. *.* if file contains a dot, pattern will be matched.
 * 3. *.{java,txt} If file is either java or txt, path will be matched.
 * 4. abc.? matches a file which start with abc and it has extension with only single character.
 * </pre>
 * @param depth         depth to scan
 * <pre>
 *   -1 : infinite
 *    0 : in searchDir itself
 *    1 : from searchDir to 1 depth sub directory
 *    2 : from searchDir to 2 depth sub directory
 *    ...
 * </pre>
 * @param includeFile       include file
 * @param includeDirectory  include directory
 * @return file or directory paths via stream
 */
fun Path.findToStream(glob: String = "*", depth: Int = -1, includeFile: Boolean = true, includeDirectory: Boolean = true): Stream<Path> {
    var matcher = FileSystems.getDefault().getPathMatcher("glob:$glob")
    return Files.walk(this, if(depth < 0) Int.MAX_VALUE else depth + 1 ).filter{ it: Path? ->
        when {
            it == null -> false
            it == this -> false
            ! matcher.matches(it.fileName) -> false
            ! includeFile && it.isFile() -> false
            ! includeDirectory && it.isDirectory() -> false
            else -> true
        }
    }
}

/**
 * search files or directories.
 *
 * @param glob   path matching glob expression
 * <pre>
 * ** : ignore directory variation
 * *  : filename LIKE search
 *
 * 1. **.xml           : all files having "xml" extension below searchDir and it's all sub directories.
 * 2. *.xml            : all files having "xml" extension in searchDir
 * 3. c:\home\*\*.xml  : all files having "xml" extension below 'c:\home\' and it's just 1 depth below directories.
 * 4. c:\home\**\*.xml : all files having "xml" extension below 'c:\home\' and it's all sub directories.
 *
 * 1. *  It matches zero , one or more than one characters. While matching, it will not cross directories boundaries.
 * 2. ** It does the same as * but it crosses the directory boundaries.
 * 3. ?  It matches only one character for the given name.
 * 4. \  It helps to avoid characters to be interpreted as special characters.
 * 5. [] In a set of characters, only single character is matched. If (-) hyphen is used then, it matches a range of characters. Example: [efg] matches "e","f" or "g" . [a-d] matches a range from a to d.
 * 6. {} It helps to matches the group of sub patterns.
 *
 * 1. *.java when given path is java , we will get true by PathMatcher.matches(path).
 * 2. *.* if file contains a dot, pattern will be matched.
 * 3. *.{java,txt} If file is either java or txt, path will be matched.
 * 4. abc.? matches a file which start with abc and it has extension with only single character.
 * </pre>
 * @param depth         depth to scan
 * <pre>
 *   -1 : infinite
 *    0 : in searchDir itself
 *    1 : from searchDir to 1 depth sub directory
 *    2 : from searchDir to 2 depth sub directory
 *    ...
 * </pre>
 * @param includeFile       include file
 * @param includeDirectory  include directory
 * @return  file or directory paths
 */
fun Path.find(glob: String = "*", depth: Int = -1, includeFile: Boolean = true, includeDirectory: Boolean = true): List<Path> =
    findToStream(glob,depth,includeFile,includeDirectory).toList()

/**
 * search files
 *
 * @param glob   path matching glob expression
 * <pre>
 * ** : ignore directory variation
 * *  : filename LIKE search
 *
 * 1. **.xml           : all files having "xml" extension below searchDir and it's all sub directories.
 * 2. *.xml            : all files having "xml" extension in searchDir
 * 3. c:\home\*\*.xml  : all files having "xml" extension below 'c:\home\' and it's just 1 depth below directories.
 * 4. c:\home\**\*.xml : all files having "xml" extension below 'c:\home\' and it's all sub directories.
 *
 * 1. *  It matches zero , one or more than one characters. While matching, it will not cross directories boundaries.
 * 2. ** It does the same as * but it crosses the directory boundaries.
 * 3. ?  It matches only one character for the given name.
 * 4. \  It helps to avoid characters to be interpreted as special characters.
 * 5. [] In a set of characters, only single character is matched. If (-) hyphen is used then, it matches a range of characters. Example: [efg] matches "e","f" or "g" . [a-d] matches a range from a to d.
 * 6. {} It helps to matches the group of sub patterns.
 *
 * 1. *.java when given path is java , we will get true by PathMatcher.matches(path).
 * 2. *.* if file contains a dot, pattern will be matched.
 * 3. *.{java,txt} If file is either java or txt, path will be matched.
 * 4. abc.? matches a file which start with abc and it has extension with only single character.
 * </pre>
 * @param depth         depth to scan
 * <pre>
 *   -1 : infinite
 *    0 : in searchDir itself
 *    1 : from searchDir to 1 depth sub directory
 *    2 : from searchDir to 2 depth sub directory
 *    ...
 * </pre>
 * @return file via stream
 */
fun Path.findFilesToStream(glob: String = "*", depth: Int = -1): Stream<Path> = findToStream(glob,depth, includeFile=true, includeDirectory=false)

/**
 * search files.
 *
 * @param glob   path matching glob expression
 * <pre>
 * ** : ignore directory variation
 * *  : filename LIKE search
 *
 * 1. **.xml           : all files having "xml" extension below searchDir and it's all sub directories.
 * 2. *.xml            : all files having "xml" extension in searchDir
 * 3. c:\home\*\*.xml  : all files having "xml" extension below 'c:\home\' and it's just 1 depth below directories.
 * 4. c:\home\**\*.xml : all files having "xml" extension below 'c:\home\' and it's all sub directories.
 *
 * 1. *  It matches zero , one or more than one characters. While matching, it will not cross directories boundaries.
 * 2. ** It does the same as * but it crosses the directory boundaries.
 * 3. ?  It matches only one character for the given name.
 * 4. \  It helps to avoid characters to be interpreted as special characters.
 * 5. [] In a set of characters, only single character is matched. If (-) hyphen is used then, it matches a range of characters. Example: [efg] matches "e","f" or "g" . [a-d] matches a range from a to d.
 * 6. {} It helps to matches the group of sub patterns.
 *
 * 1. *.java when given path is java , we will get true by PathMatcher.matches(path).
 * 2. *.* if file contains a dot, pattern will be matched.
 * 3. *.{java,txt} If file is either java or txt, path will be matched.
 * 4. abc.? matches a file which start with abc and it has extension with only single character.
 * </pre>
 * @param depth         depth to scan
 * <pre>
 *   -1 : infinite
 *    0 : in searchDir itself
 *    1 : from searchDir to 1 depth sub directory
 *    2 : from searchDir to 2 depth sub directory
 *    ...
 * </pre>
 * @return  file paths
 */
fun Path.findFiles(glob: String = "*", depth: Int = -1): List<Path> = find(glob,depth, includeFile=true, includeDirectory=false)

/**
 * search directories.
 *
 * @param glob   path matching glob expression
 * <pre>
 * ** : ignore directory variation
 * *  : filename LIKE search
 *
 * 1. **.xml           : all files having "xml" extension below searchDir and it's all sub directories.
 * 2. *.xml            : all files having "xml" extension in searchDir
 * 3. c:\home\*\*.xml  : all files having "xml" extension below 'c:\home\' and it's just 1 depth below directories.
 * 4. c:\home\**\*.xml : all files having "xml" extension below 'c:\home\' and it's all sub directories.
 *
 * 1. *  It matches zero , one or more than one characters. While matching, it will not cross directories boundaries.
 * 2. ** It does the same as * but it crosses the directory boundaries.
 * 3. ?  It matches only one character for the given name.
 * 4. \  It helps to avoid characters to be interpreted as special characters.
 * 5. [] In a set of characters, only single character is matched. If (-) hyphen is used then, it matches a range of characters. Example: [efg] matches "e","f" or "g" . [a-d] matches a range from a to d.
 * 6. {} It helps to matches the group of sub patterns.
 *
 * 1. *.java when given path is java , we will get true by PathMatcher.matches(path).
 * 2. *.* if file contains a dot, pattern will be matched.
 * 3. *.{java,txt} If file is either java or txt, path will be matched.
 * 4. abc.? matches a file which start with abc and it has extension with only single character.
 * </pre>
 * @param depth         depth to scan
 * <pre>
 *   -1 : infinite
 *    0 : in searchDir itself
 *    1 : from searchDir to 1 depth sub directory
 *    2 : from searchDir to 2 depth sub directory
 *    ...
 * </pre>
 * @return directory paths via stream
 */
fun Path.findDirsToStream(glob: String = "*", depth: Int = -1): Stream<Path> = findToStream(glob,depth, includeFile=false, includeDirectory=true)

/**
 * search directories.
 *
 * @param glob   path matching glob expression
 * <pre>
 * ** : ignore directory variation
 * *  : filename LIKE search
 *
 * 1. **.xml           : all files having "xml" extension below searchDir and it's all sub directories.
 * 2. *.xml            : all files having "xml" extension in searchDir
 * 3. c:\home\*\*.xml  : all files having "xml" extension below 'c:\home\' and it's just 1 depth below directories.
 * 4. c:\home\**\*.xml : all files having "xml" extension below 'c:\home\' and it's all sub directories.
 *
 * 1. *  It matches zero , one or more than one characters. While matching, it will not cross directories boundaries.
 * 2. ** It does the same as * but it crosses the directory boundaries.
 * 3. ?  It matches only one character for the given name.
 * 4. \  It helps to avoid characters to be interpreted as special characters.
 * 5. [] In a set of characters, only single character is matched. If (-) hyphen is used then, it matches a range of characters. Example: [efg] matches "e","f" or "g" . [a-d] matches a range from a to d.
 * 6. {} It helps to matches the group of sub patterns.
 *
 * 1. *.java when given path is java , we will get true by PathMatcher.matches(path).
 * 2. *.* if file contains a dot, pattern will be matched.
 * 3. *.{java,txt} If file is either java or txt, path will be matched.
 * 4. abc.? matches a file which start with abc and it has extension with only single character.
 * </pre>
 * @param depth         depth to scan
 * <pre>
 *   -1 : infinite
 *    0 : in searchDir itself
 *    1 : from searchDir to 1 depth sub directory
 *    2 : from searchDir to 2 depth sub directory
 *    ...
 * </pre>
 * @return  directory paths
 */
fun Path.findDirs(glob: String = "*", depth: Int = -1): List<Path> = find(glob,depth, includeFile=false, includeDirectory=true)

/**
 * Resolves the given [other] path against this path.
 *
 * This operator is a shortcut for the [Path.resolve] function.
 */
operator fun Path.div(other: Path): Path = resolve(other)

/**
 * Resolves the given [other] path string against this path.
 *
 * This operator is a shortcut for the [Path.resolve] function.
 */
operator fun Path.div(other: String): Path = resolve(other.trim())

/**
 * Resolves the given [other] path against this path.
 *
 * This operator is a shortcut for the [Path.resolve] function.
 */
operator fun Path.plus(other: Path): Path = resolve(other)

/**
 * Resolves the given [other] path string against this path.
 *
 * This operator is a shortcut for the [Path.resolve] function.
 */
operator fun Path.plus(other: String): Path = resolve(other.trim().replace("^[\\/]".toRegex(),""))

/**
 * Converts the provided [path] string to a [Path] object of the [default][FileSystems.getDefault] filesystem.
 */
fun Path(path: String): Path = Paths.get(path)

/**
 * Converts the name sequence specified with the [base] path string and a number of [more] additional names
 * to a [Path] object of the [default][FileSystems.getDefault] filesystem.
 */
fun Path(base: String, vararg more: String): Path = Paths.get(base, *more)

/**
 * Converts this URI to a [Path] object.
 */
fun URI.toPath(): Path = Paths.get(this)

fun Path.readGzip(fn: (ObjectInputStream) -> Unit) {
    if( ! isFile() )
        throw IOException("file is not existed.($this)")
    ObjectInputStream(GZIPInputStream(FileInputStream(this.toFile()))).use { fn.invoke(it) }
}

fun Path.writeGzip(fn: (ObjectOutputStream) -> Unit) {
    makeFile()
    try {
        ObjectOutputStream(GZIPOutputStream(FileOutputStream(this.toFile()))).use {
            fn.invoke(it)
        }
    } catch (e: Exception) {
        delete(false)
    }
}

@Suppress("UNCHECKED_CAST")
fun <T> Path.readObject(): T? {
    var rs: T? = null
    readGzip { rs = it.readObject() as T? }
    return rs
}

fun Path.writeObject(any: Any?) = writeGzip {
    it.writeObject(any)
    it.flush()
}

val KClass<*>.rootPath: Path
    get() = this.java.protectionDomain.codeSource.location.file.toFile().toPath()

/**
 * Returns a new [BufferedReader] for reading the content of this file.
 *
 * @param charset character set to use for reading text, UTF-8 by default.
 */
fun Path.reader(charset: Charset = Charsets.UTF_8): BufferedReader {
    return Files.newBufferedReader(this, charset )
}

/**
 * Returns a new [BufferedWriter] for writing the content of this file.
 *
 * @param charset character set to use for writing text, UTF-8 by default.
 * @param options options to determine how the file is opened.
 * @param bufferSize necessary size of the buffer.
 */
fun Path.writer(charset: Charset = Charsets.UTF_8, vararg options: OpenOption, bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedWriter {
    this.makeFile()
    return outputStream(*options).writer(charset).buffered(bufferSize)
}

/**
 * Returns a new [BufferedWriter] for appending the content of this file.
 *
 * @param charset character set to use for writing text, UTF-8 by default.
 * @param bufferSize necessary size of the buffer.
 */
fun Path.appender(charset: Charset = Charsets.UTF_8, bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedWriter {
    return this.writer(charset,APPEND,bufferSize = bufferSize)
}

fun Path.inputStream(vararg options: OpenOption): InputStream {
    return Files.newInputStream(this, *options)
}

fun Path.outputStream(vararg options: OpenOption): OutputStream {
    return Files.newOutputStream(this, *options)
}

fun Path.detectCharset(default: Charset = Charsets.UTF_8): Charset {
    return this.inputStream().use { detectCharset(it,default) }
}

fun detectCharset(inputstream: InputStream, default: Charset = Charsets.UTF_8): Charset {

    val buffer = ByteArray(4096)
    var detector: UniversalDetector = UniversalDetector(null)

    inputstream.mark(1 shl 24)
    var nread: Int
    while (inputstream.read(buffer).also { nread = it } > 0 && !detector.isDone) {
        detector.handleData(buffer, 0, nread)
    }
    detector.dataEnd()

    @Suppress("UNCHECKED_CAST")
    return detector.detectedCharset.let {
        try {
            Charset.forName(it)
        } catch (e: Exception) {
            default
        }
    }

}

fun Path.readLines(charset: Charset = Charsets.UTF_8, reader: (line: String) -> Unit): Boolean {
    if( notExists() ) return false
    this.reader(charset = charset).useLines { it.forEach(reader) }

    return true
}

fun Path.readLines(charset: Charset = Charsets.UTF_8): String {
    if( notExists() ) return ""
    val sb = StringBuilder()
    this.readLines(charset) {
        if(sb.isNotEmpty()) sb.append('\n')
        sb.append(it)
    }
    return sb.toString()
}

fun Path.writeLines(lines: Iterable<CharSequence>, charset: Charset = Charsets.UTF_8, vararg options: OpenOption): Path {
    return Files.write(this, lines, charset, *options)
}

fun Path.appendLines(lines: Iterable<CharSequence>, charset: Charset = Charsets.UTF_8): Path {
    return Files.write(this, lines, charset, APPEND)
}

/**
 * Gets the entire content of this file as a byte array.
 *
 * It's not recommended to use this function on huge files.
 * It has an internal limitation of approximately 2 GB byte array size.
 * For reading large files or files of unknown size, open an [InputStream][Path.inputStream] and read blocks sequentially.
 *
 * @return the entire content of this file as a byte array.
 */
fun Path.readBytes(): ByteArray {
    return Files.readAllBytes(this)
}

/**
 * Writes an [array] of bytes to this file.
 *
 * By default, the file will be overwritten if it already exists, but you can control this behavior
 * with [options].
 *
 * @param array byte array to write into this file.
 * @param options options to determine how the file is opened.
 */
fun Path.writeBytes(array: ByteArray, vararg options: OpenOption) {
    Files.write(this, array, *options)
}

/**
 * Appends an [array] of bytes to the content of this file.
 *
 * @param array byte array to append to this file.
 */
fun Path.appendBytes(array: ByteArray) {
    Files.write(this, array, APPEND)
}

/**
 * Gets the entire content of this file as a String using UTF-8 or the specified [charset].
 *
 * It's not recommended to use this function on huge files.
 * For reading large files or files of unknown size, open a [Reader][Path.reader1] and read blocks of text sequentially.
 *
 * @param charset character set to use for reading text, UTF-8 by default.
 * @return the entire content of this file as a String.
 */
fun Path.readText(charset: Charset = Charsets.UTF_8): String {
    return reader(charset).use { it.readText() }
}


/**
 * Sets the content of this file as [text] encoded using UTF-8 or the specified [charset].
 *
 * By default, the file will be overwritten if it already exists, but you can control this behavior
 * with [options].
 *
 * @param text text to write into file.
 * @param charset character set to use for writing text, UTF-8 by default.
 * @param options options to determine how the file is opened.
 */
fun Path.writeText(text: CharSequence, charset: Charset = Charsets.UTF_8, vararg options: OpenOption) {
    writer(charset,*options).use { it.append(text) }
}

/**
 * Appends [text] to the content of this file using UTF-8 or the specified [charset].
 *
 * @param text text to append to file.
 * @param charset character set to use for writing text, UTF-8 by default.
 */
fun Path.appendText(text: CharSequence, charset: Charset = Charsets.UTF_8) {
    writer(charset, APPEND).use { it.append(text) }
}

/**
 * Rename path
 *
 * @param newName new file name
 * @param overwrite true if overwrite new path to previous.
 */
fun Path.rename(newName: String, overwrite: Boolean = false) {
    val trg = this.parent.resolve(newName)
    if(trg != this) {
        this.move(trg,overwrite)
    }
}

/**
 * resource statistics (file counts, directory counts, total size)
 */
val Path.statistics: ResourceStatistics
    get() {
        return if(this.isFile()) {
            ResourceStatistics(1,this.fileSize)
        } else if(this.isDirectory()) {
            val res = ResourceStatistics()
            Files.walkFileTree(this, object: SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    res.dirCount++
                    return FileVisitResult.CONTINUE
                }
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    res.fileCount++
                    res.size += file.fileSize
                    return FileVisitResult.CONTINUE
                }
            })
            res
        } else {
            ResourceStatistics()
        }
    }

data class ResourceStatistics(
    var fileCount: Long = 0,
    var dirCount: Long = 0,
    var size: Long = 0,
) {
    val totalCount: Long
        get() = fileCount + dirCount
}