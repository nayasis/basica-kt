package com.github.nayasis.kotlin.basica.core.string.binder

import java.util.regex.Pattern

class ExtractPattern(
    val pattern: Pattern,
    val escapeChar: Char? = null,
    val replacer: ((String) -> String)? = null,
) {
    fun escapable(prefix: String?): Boolean {
        return escapeChar != null && ! prefix.isNullOrEmpty() && prefix[0] == escapeChar
    }
}