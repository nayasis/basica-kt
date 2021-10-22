package com.github.nayasis.kotlin.basica.core.resource.util

import com.github.nayasis.kotlin.basica.core.path.FOLDER_SEPARATOR_UNIX
import com.github.nayasis.kotlin.basica.core.path.FOLDER_SEPARATOR_WINDOWS
import java.util.*

object PathModifier {
    private const val PATH_PARENT = ".."
    private const val PATH_CURRENT = "."
    fun clean(path: String?): String {
        if(path.isNullOrEmpty()) return ""
        var pathToUse = path.replace(FOLDER_SEPARATOR_WINDOWS, FOLDER_SEPARATOR_UNIX)

        // Strip prefix from path to analyze, to not treat it as part of the
        // first path element. This is necessary to correctly parse paths like
        // "file:core/../core/io/Resource.class", where the ".." should just
        // strip the first "core" directory while keeping the "file:" prefix.
        val prefixIndex = pathToUse.indexOf(':')
        var prefix = ""
        if (prefixIndex != -1) {
            prefix = pathToUse.substring(0, prefixIndex + 1)
            if (prefix.contains(FOLDER_SEPARATOR_UNIX)) {
                prefix = ""
            } else {
                pathToUse = pathToUse.substring(prefixIndex + 1)
            }
        }
        if (pathToUse.startsWith(FOLDER_SEPARATOR_UNIX)) {
            prefix = prefix + FOLDER_SEPARATOR_UNIX
            pathToUse = pathToUse.substring(1)
        }
        val pathArray = pathToUse.split(FOLDER_SEPARATOR_UNIX)
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
        if (pathElements.size == 1 && "" == pathElements.last && !prefix.endsWith(FOLDER_SEPARATOR_UNIX)) {
            pathElements.add(0, PATH_CURRENT)
        }
        return prefix + pathElements.joinToString("$FOLDER_SEPARATOR_UNIX")
    }
}