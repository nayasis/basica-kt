package com.github.nayasis.kotlin.basica.core.resource.finder

import com.github.nayasis.kotlin.basica.core.resource.matcher.PathMatcher
import com.github.nayasis.kotlin.basica.core.resource.type.interfaces.Resource
import com.github.nayasis.kotlin.basica.core.resource.util.Resources
import com.github.nayasis.kotlin.basica.core.resource.util.URL_SEPARATOR_JAR
import com.github.nayasis.kotlin.basica.core.resource.util.URL_SEPARATOR_WAR
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.IOException
import java.net.JarURLConnection
import java.net.URL
import java.util.jar.JarFile
import java.util.zip.ZipException

private val log = KotlinLogging.logger {}

class JarResourceFinder(private var pathMatcher: PathMatcher) {

    /**
     * Find all resources in jar files that match the given location pattern
     * via the Ant-style PathMatcher.
     * @param root          root directory as Resource
     * @param rootDir       the pre-resolved root directory URL
     * @param pattern       pattern to match (below the root directory)
     * @return a mutable Set of matching Resource instances
     * @throws IOException in case of I/O errors
     * @since 4.3
     */
    @Throws(IOException::class)
    fun find(root: Resource, rootDir: URL, pattern: String): Set<Resource> {
        val conn = rootDir.openConnection()
        val jarFile: JarFile
        val jarFileUrl: String
        var rootEntryPath: String
        val closeJarFile: Boolean
        if (conn is JarURLConnection) {

            // Should usually be the case for traditional JAR files.
            val jarCon = conn
            Resources.useCachesIfNecessary(jarCon)
            jarFile = jarCon.jarFile
            val jarEntry = jarCon.jarEntry
            rootEntryPath = if (jarEntry != null) jarEntry.name else ""
            closeJarFile = !jarCon.useCaches
        } else {
            // No JarURLConnection -> need to resort to URL file parsing.
            // We'll assume URLs of the format "jar:path!/entry", with the protocol
            // being arbitrary as long as following the entry format.
            // We'll also handle paths with and without leading "file:" prefix.
            val urlFile = rootDir.file
            try {
                var separatorIndex = urlFile.indexOf(URL_SEPARATOR_WAR)
                if (separatorIndex == -1)
                    separatorIndex = urlFile.indexOf(URL_SEPARATOR_JAR)
                if (separatorIndex != -1) {
                    jarFileUrl = urlFile.substring(0, separatorIndex)
                    rootEntryPath = urlFile.substring(separatorIndex + 2) // both separators are 2 chars
                    jarFile = Resources.getJarFile(jarFileUrl)
                } else {
                    jarFile = JarFile(urlFile)
                    rootEntryPath = ""
                }
                closeJarFile = true
            } catch (ex: ZipException) {
                log.debug{"Skipping invalid jar classpath entry [$urlFile]"}
                return emptySet()
            }
        }
        return try {
            if ("" != rootEntryPath && !rootEntryPath.endsWith("/")) {
                // Root entry path must end with slash to allow for proper matching.
                // The Sun JRE does not return a slash here, but BEA JRockit does.
                rootEntryPath = "$rootEntryPath/"
            }
            val result: MutableSet<Resource> = LinkedHashSet(8)
            for (entry in jarFile.entries()) {
                val entryPath = entry!!.name
                if (entryPath.startsWith(rootEntryPath)) {
                    val relativePath = entryPath.substring(rootEntryPath.length)
                    if (pathMatcher.match(pattern, relativePath)) {
                        val relative = root.createRelative(relativePath)
                        result.add(relative)
                    }
                }
            }
            result
        } finally {
            if (closeJarFile) {
                jarFile.close()
            }
        }
    }
}