package com.github.nayasis.kotlin.basica.core.string.binder

import java.lang.StringBuilder
import java.util.regex.Pattern

class ExtractPattern(
    val pattern: Pattern,
    val firstEscape: String? = null,
    val firstEscaoeReplace: String = "",
    val lastEscape: String? = null,
    val lastEscaoeReplace: String = "",
) {

    fun escapable(word: String): Boolean {
        return when {
            firstEscape != null && word.startsWith(firstEscape) -> true
            lastEscape  != null && word.endsWith(lastEscape) -> true
            else -> false
        }
    }

    fun restoreEscape(word: String): String {

        var sb = StringBuilder()

        var i = 0
        val end = word.length - (lastEscape?.length ?: 0)
        while(i < end) {
            if(i == 0 && firstEscape != null) {
                i += firstEscape.length
                sb.append(firstEscaoeReplace)
                continue
            }
            sb.append(word[i])
        }

        if(lastEscape != null) {
            sb.append(lastEscaoeReplace)
        }

        return sb.toString()

    }

}