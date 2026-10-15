package io.github.nayasis.kotlin.basica.core.resource

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.net.JarURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.JarFile
import kotlin.streams.toList

private val log = KotlinLogging.logger {}

private const val WILDCARDS = "*?{["

/**
 * find resources in classpath (directory or jar) by glob pattern
 *
 * <pre>
 * *    : any characters in a directory
 * **   : any characters across directories ("a/&#42;&#42;/b" also matches "a/b")
 * ?    : one character
 * {,}  : one of sub patterns (ex. *.{xml,json})
 * [ ]  : one character in set (ex. [a-c])
 * \    : escape next character
 * </pre>
 */
internal class ResourceFinder(private val classLoader: ClassLoader) {

    fun find(pattern: String): List<URL> {
        val path = pattern.removePrefix("/")
        val wildcard = path.indexOfFirst { it in WILDCARDS }
        if( wildcard < 0 )
            return classLoader.getResources(path).toList()

        val rootDir = path.substring(0, path.lastIndexOf('/', wildcard) + 1)
        val matcher = globToRegex(path.substring(rootDir.length))

        val result = LinkedHashSet<URL>()
        for( root in roots(rootDir) ) {
            runCatching {
                when(root.protocol) {
                    "file" -> findInDirectory(root, matcher)
                    else   -> findInJar(root, matcher)
                }
            }.onSuccess { result.addAll(it) }
             .onFailure { log.warn(it) { "fail to search resources in $root" } }
        }
        return result.toList()
    }

    /**
     * classpath roots containing root directory.
     * (when root directory is empty, jars in classpath are added because class loader returns directories only)
     */
    private fun roots(rootDir: String): List<URL> {
        val roots = classLoader.getResources(rootDir).toList()
        if( rootDir.isNotEmpty() ) return roots
        val jars = System.getProperty("java.class.path", "").split(File.pathSeparator)
            .filter { it.endsWith(".jar", ignoreCase = true) && File(it).isFile }
            .map { URL("jar:${File(it).toURI()}!/") }
        return (roots + jars).distinct()
    }

    private fun findInDirectory(root: URL, matcher: Regex): List<URL> {
        val dir = Paths.get(root.toURI())
        if( ! Files.isDirectory(dir) ) return emptyList()
        return Files.walk(dir).use { paths ->
            paths.filter { Files.isRegularFile(it) && matcher.matches(dir.relativePath(it)) }
                .map { it.toUri().toURL() }
                .toList()
        }
    }

    private fun Path.relativePath(child: Path): String =
        relativize(child).toString().replace(File.separatorChar, '/')

    private fun findInJar(root: URL, matcher: Regex): List<URL> {
        val connection = root.openConnection()
        if( connection !is JarURLConnection ) {
            log.warn { "unsupported resource protocol: $root" }
            return emptyList()
        }
        connection.useCaches = false
        val prefix = connection.entryName?.let { if(it.endsWith("/")) it else "$it/" } ?: ""
        val base   = if(root.toString().endsWith("/")) root else URL("$root/")
        return connection.jarFile.use { jar: JarFile ->
            jar.entries().asSequence()
                .filter { ! it.isDirectory && it.name.startsWith(prefix) }
                .map { it.name.substring(prefix.length) }
                .filter { matcher.matches(it) }
                .map { URL(base, it) }
                .toList()
        }
    }

    private fun globToRegex(glob: String): Regex {
        val regex = StringBuilder()
        var inGroup = false
        var i = 0
        while( i < glob.length ) {
            val c = glob[i]
            val setEnd = if(c == '[') glob.indexOf(']', i + 1) else -1
            when {
                glob.startsWith("**/", i) -> { regex.append("(?:.*/)?"); i += 2 }
                glob.startsWith("**", i)  -> { regex.append(".*"); i += 1 }
                c == '*'                  -> regex.append("[^/]*")
                c == '?'                  -> regex.append("[^/]")
                c == '{'                  -> { regex.append("(?:"); inGroup = true }
                c == '}' && inGroup       -> { regex.append(")"); inGroup = false }
                c == ',' && inGroup       -> regex.append("|")
                setEnd > i -> {
                    regex.append('[').append(glob.substring(i + 1, setEnd).replaceFirst("^!".toRegex(), "^")).append(']')
                    i = setEnd
                }
                c == '\\' && i + 1 < glob.length -> { regex.append(Regex.escape(glob[i + 1].toString())); i += 1 }
                else -> regex.append(Regex.escape(c.toString()))
            }
            i++
        }
        return regex.toString().toRegex()
    }

}
