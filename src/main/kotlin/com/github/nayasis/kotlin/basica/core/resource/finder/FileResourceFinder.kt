package com.github.nayasis.kotlin.basica.core.resource.finder

import com.github.nayasis.kotlin.basica.core.path.invariantSeparators
import com.github.nayasis.kotlin.basica.core.resource.matcher.PathMatcher
import com.github.nayasis.kotlin.basica.core.resource.type.FileSystemResource
import com.github.nayasis.kotlin.basica.core.resource.type.interfaces.Resource
import com.github.nayasis.kotlin.basica.core.string.invariantSeparators
import mu.KotlinLogging
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

private val log = KotlinLogging.logger {}

class FileResourceFinder(private var pathMatcher: PathMatcher) {

    /**
     * Find all resources in the file system that match the given location pattern
     * via the Ant-style PathMatcher.
     * @param root the root directory as Resource
     * @param pattern the sub pattern to match (below the root directory)
     * @return a mutable Set of matching Resource instances
     * @throws IOException in case of I/O errors
     * @see .findFiles
     */
    @Throws(IOException::class)
    fun find(root: Resource, pattern: String): Set<Resource> {
        return try {
            val rootDir: File = root.getFile().absoluteFile
            toFileSystemResource(findFiles(rootDir, pattern))
        } catch (e: FileNotFoundException) {
            log.debug{"Cannot search for matching files underneath $root in the file system: ${e.message}"}
            emptySet()
        } catch (e: Exception) {
            log.info{"Failed to resolve $root in the file system: $e"}
            emptySet()
        }
    }

    private fun toFileSystemResource(files: Collection<File>): Set<Resource> {
        return files.map { FileSystemResource(it) }.toSet()
    }

    /**
     * Retrieve files that match the given path pattern,
     * checking the given directory and its subdirectories.
     * @param rootDir the directory to start from
     * @param pattern the pattern to match against,
     * relative to the root directory
     * @return a mutable Set of matching Resource instances
     * @throws IOException if directory contents could not be retrieved
     */
    @Throws(IOException::class)
    private fun findFiles(rootDir: File, pattern: String): Set<File> {
        if(! rootDir.exists() || !rootDir.isDirectory() || !rootDir.canRead()) return emptySet()
        var fullPattern = rootDir.toPath().toAbsolutePath().invariantSeparators
        if (!pattern.startsWith("/")) {
            fullPattern += "/"
        }
        fullPattern += pattern.invariantSeparators()
        return LinkedHashSet<File>().apply { findFiles(fullPattern,rootDir,this) }
    }

    /**
     * Recursively retrieve files that match the given pattern,
     * adding them to the given result list.
     *
     * @param pattern   pattern to match against,
     * with prepended root directory path
     * @param dir       current directory
     * @param result Set of matching File instances to add to
     */
    private fun findFiles(pattern: String, dir: File?, result: MutableSet<File>) {
        for (content in listDirectory(dir)) {
            val currPath = content.toPath().toAbsolutePath().invariantSeparators
            if (content.isDirectory() && pathMatcher.matchStart(pattern, "$currPath/")) {
                if (content.canRead()) {
                    findFiles(pattern, content, result)
                }
            }
            if (pathMatcher.match(pattern, currPath)) {
                result.add(content)
            }
        }
    }

    private fun listDirectory(dir: File?): Array<File> {
        return dir?.listFiles()?.apply { sortBy { it.name } } ?: emptyArray()
    }

}