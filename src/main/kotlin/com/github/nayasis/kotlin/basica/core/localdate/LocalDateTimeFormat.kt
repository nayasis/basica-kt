package com.github.nayasis.kotlin.basica.core.localdate

import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ofPattern

private val REGEX_USER_TEXT = "'.*?'".toRegex()
private val REGEX_YEAR      = "YYYY".toRegex()
private val REGEX_DATE      = "(^|[^D])DD([^D]|$)".toRegex()
private val REGEX_MIN       = "MI".toRegex()
private val REGEX_SEC       = "(^|[^S])SS([^S]|$)".toRegex()
private val REGEX_MILISEC   = "(^|[^F])FFF([^F]|$)".toRegex()


private val cacheDateFormat    = HashMap<String,String>()
private val cacheFormatter = HashMap<Pair<String,Boolean>,DateTimeFormatter?>()

fun String.toDateTimeFormat(native: Boolean = false): String {
    return cacheDateFormat[this] ?: when {
        isEmpty() -> "yyyyMMddHHmmssSSS"
        native -> this
        else -> {
            try { this
                .replace(REGEX_USER_TEXT, "") // remove user text
                .replace(REGEX_YEAR, "yyyy")
                .replace(REGEX_DATE, "$1dd$2")
                .replace(REGEX_MIN, "mm")
                .replace(REGEX_SEC, "$1ss$2")
                .replace(REGEX_MILISEC, "$1SSS$2")
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid pattern : $this", e)
            }
        }
    }.also {
        cacheDateFormat[this] = it
    }
}

fun String.toTimeFormat(native: Boolean = false): String {
    return when {
        isEmpty() -> "HHmmssSSS"
        else -> this.toDateTimeFormat(native)
    }
}

fun String.toDateTimeFormatter(native: Boolean = false): DateTimeFormatter? {
    return when {
        isEmpty() -> null
        else -> {
            try {
                val key = this to native
                cacheFormatter[key] ?: ofPattern(this.toDateTimeFormat(native))
                    .also { cacheFormatter[key] = it }
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid pattern : $this", e)
            }
        }
    }
}

fun String.toTimeFormatter(native: Boolean = false): DateTimeFormatter? {
    return when {
        isEmpty() -> null
        else -> {
            try {
                val key = this to native
                cacheFormatter[key] ?: ofPattern(this.toTimeFormat(native))
                    .also { cacheFormatter[key] = it }
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid pattern : $this", e)
            }
        }
    }
}

