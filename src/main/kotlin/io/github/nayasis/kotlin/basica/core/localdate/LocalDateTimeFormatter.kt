package io.github.nayasis.kotlin.basica.core.localdate

import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.*

private val REGEX_USER_TEXT = "'.*?'".toRegex()

private val cacheJvmDateFormat = HashMap<Pair<String,Boolean>,String>()
private val cacheFormatter     = HashMap<Pair<String,Boolean>,DateTimeFormatter>()

fun String.toDateTimeFormat(native: Boolean = false): String {
    return when {
        native -> this
        this.isEmpty() -> "yyyyMMddHHmmssSSS"
        else -> this.toJvmTimeFormat(true)
    }
}

fun String.toTimeFormat(native: Boolean = false): String {
    return when {
        native -> this
        this.isEmpty() -> "HHmmssSSS"
        else -> this.toJvmTimeFormat(true)
    }
}

fun String.toJvmTimeFormat(clearCustomPattern: Boolean): String {
    val key = this to clearCustomPattern
    return cacheJvmDateFormat[key] ?: run {
        val sb = StringBuilder()
        var i = 0
        val len = this.length
        var inQuote = false
        while (i < len) {
            val c = this[i]
            if (c == '\'') {
                if(! clearCustomPattern) sb.append(c)
                inQuote = !inQuote
                i++
                continue
            }
            if (inQuote) {
                if(! clearCustomPattern) sb.append(c)
                i++
                continue
            }
            when {
                this.startsWith("YYYY", i) && (i == 0 || this[i-1] != 'Y') && (i+4 >= len || this[i+4] != 'Y') -> {
                    sb.append("yyyy"); i += 4
                }
                this.startsWith("DD", i) && (i == 0 || this[i-1] != 'D') && (i+2 >= len || this[i+2] != 'D') -> {
                    sb.append("dd"); i += 2
                }
                this.startsWith("MI", i) -> {
                    sb.append("mm"); i += 2
                }
                this.startsWith("SS", i) && (i == 0 || this[i-1] != 'S') && (i+2 >= len || this[i+2] != 'S') -> {
                    sb.append("ss"); i += 2
                }
                this.startsWith("FFF", i) && (i == 0 || this[i-1] != 'F') && (i+3 >= len || this[i+3] != 'F') -> {
                    sb.append("SSS"); i += 3
                }
                else -> {
                    sb.append(c); i++
                }
            }
        }
        sb.toString()
    }.also {
        cacheJvmDateFormat[key] = it
    }
}

fun String.toDateTimeFormatter(native: Boolean = false): DateTimeFormatter {
    val key = this to native
    return cacheFormatter[key] ?: runCatching{ when {
        isEmpty() -> ISO_LOCAL_DATE_TIME
        native    -> ofPattern(this)
        else      -> ofPattern(this.toJvmTimeFormat(false))
    }}.getOrElse {
        throw IllegalArgumentException("Invalid pattern : $this", it)
    }.also { cacheFormatter[key] = it }
}

fun String.toTimeFormatter(native: Boolean = false): DateTimeFormatter {
    val key = this to native
    return cacheFormatter[key] ?: runCatching{ when {
        isEmpty() -> ISO_LOCAL_DATE
        native    -> ofPattern(this)
        else      -> ofPattern(this.toJvmTimeFormat(false))
    }}.getOrElse {
        throw IllegalArgumentException("Invalid pattern : $this", it)
    }.also { cacheFormatter[key] = it }
}

