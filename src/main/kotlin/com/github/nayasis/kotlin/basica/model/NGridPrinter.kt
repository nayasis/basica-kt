package com.github.nayasis.kotlin.basica.model

private val REGEX_N = "\n".toRegex()
private val REGEX_R = "\r".toRegex()

class NGridPrinter(
    val showCount: Int = 500,
    val maxColumnWidth: Int = 255,
) {

    private fun toString(value: Any?): String {
        when (value) {
            null -> return ""
            is Map<*, *> -> if (value.isEmpty()) return "{}"
            is Collection<*> -> if (value.isEmpty()) return "[]"
        }
        return value.toString().replace(REGEX_N, "\\\\n").replace(REGEX_R, "\\\\r")
    }

}