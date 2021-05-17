package com.github.nayasis.kotlin.basica.core.resource.util

import com.github.nayasis.basica.base.Strings
import com.github.nayasis.basica.file.Files
import java.util.*

object PathModifier {
    private const val PATH_PARENT = ".."
    private const val PATH_CURRENT = "."
    fun clean(path: String): String {
        if (Strings.isEmpty(path)) return path
        var pathToUse = path.replace(Files.FOLDER_SEPARATOR_WINDOWS, Files.FOLDER_SEPARATOR)

        // Strip prefix from path to analyze, to not treat it as part of the
        // first path element. This is necessary to correctly parse paths like
        // "file:core/../core/io/Resource.class", where the ".." should just
        // strip the first "core" directory while keeping the "file:" prefix.
        val prefixIndex = pathToUse.indexOf(':')
        var prefix = ""
        if (prefixIndex != -1) {
            prefix = pathToUse.substring(0, prefixIndex + 1)
            if (prefix.contains(Files.FOLDER_SEPARATOR)) {
                prefix = ""
            } else {
                pathToUse = pathToUse.substring(prefixIndex + 1)
            }
        }
        if (pathToUse.startsWith(Files.FOLDER_SEPARATOR)) {
            prefix = prefix + Files.FOLDER_SEPARATOR
            pathToUse = pathToUse.substring(1)
        }
        val pathArray = Strings.split(pathToUse, Files.FOLDER_SEPARATOR)
        val pathElements = LinkedList<String?>()
        var tops = 0
        for (i in pathArray.indices.reversed()) {
            val element = pathArray[i]
            if (PATH_CURRENT == element) {
                // Points to current directory - drop it.
            } else if (PATH_PARENT == element) {
                // Registering top path found.
                tops++
            } else {
                if (tops > 0) {
                    // Merging path element with element corresponding to top path.
                    tops--
                } else {
                    // Normal path element found.
                    pathElements.add(0, element)
                }
            }
        }

        // Remaining top paths need to be retained.
        for (i in 0 until tops) {
            pathElements.add(0, PATH_PARENT)
        }
        // If nothing else left, at least explicitly point to current path.
        if (pathElements.size == 1 && "" == pathElements.last && !prefix.endsWith(Files.FOLDER_SEPARATOR)) {
            pathElements.add(0, PATH_CURRENT)
        }
        return prefix + Strings.join(pathElements, Files.FOLDER_SEPARATOR)
    }
}