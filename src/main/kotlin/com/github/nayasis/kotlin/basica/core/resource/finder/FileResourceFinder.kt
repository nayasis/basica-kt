package com.github.nayasis.kotlin.basica.core.resource.finder

import com.github.nayasis.kotlin.basica.core.resource.matcher.PathMatcher
import com.github.nayasis.kotlin.basica.core.resource.type.FileSystemResource
import com.github.nayasis.kotlin.basica.core.resource.type.interfaces.Resource
import mu.KotlinLogging
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.function.Function

private val log = KotlinLogging.logger {}

class FileResourceFinder(private var pathMatcher: PathMatcher) {
    fun setPathMatcher(pathMatcher: PathMatcher) {
        this.pathMatcher = pathMatcher
    }

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
            val rootDir: File = root.file.absoluteFile
            toFileSystemResource(findFiles(rootDir, pattern))
        } catch (e: FileNotFoundException) {
            log.debug{"Cannot search for matching files underneath $root in the file system: ${e.message}"}
            emptySet()
        } catch (e: Exception) {
            log.info("Failed to resolve {} in the file system: {}", root, e)
            emptySet()
        }
    }

    private fun toFileSystemResource(files: Collection<File?>): Set<Resource> {
        val result: MutableSet<Resource> = LinkedHashSet(files.size)
        for (file in files) {
            result.add(FileSystemResource(file))
        }
        return result
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
    private fun findFiles(rootDir: File, pattern: String): Set<File?> {
        if (Files.notExists(rootDir) || !rootDir.isDirectory() || !rootDir.canRead()) return emptySet<File>()
        var fullPattern = Files.normalizeSeparator(rootDir.getAbsolutePath())
        if (!pattern.startsWith("/")) {
            fullPattern += "/"
        }
        fullPattern = fullPattern + Files.normalizeSeparator(pattern)
        val result: MutableSet<File?> = LinkedHashSet<File>(8)
        findFiles(fullPattern, rootDir, result)
        return result
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
    protected fun findFiles(pattern: String?, dir: File?, result: MutableSet<File?>) {
        for (content in listDirectory(dir)) {
            val currPath = Files.normalizeSeparator(content.getAbsolutePath())
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

    private fun listDirectory(dir: File?): Array<File?> {
        val files: Array<File?> = dir.listFiles() ?: return arrayOfNulls<File>(0)
        Arrays.sort<File>(
            files,
            Comparator.comparing<File, String>(Function<File, String> { obj: File -> obj.getName() })
        )
        return files
    }
}