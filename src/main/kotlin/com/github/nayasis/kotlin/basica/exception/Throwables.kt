package com.github.nayasis.kotlin.basica.exception

import ch.qos.logback.classic.spi.ThrowableProxy
import ch.qos.logback.classic.spi.ThrowableProxyUtil
import java.io.PrintWriter
import java.io.StringWriter

private var enableLogback = true

fun Throwable.filterStackTrace(pattern: Regex? = null): Throwable {
    if( pattern == null ) return this
    val self = this
    return Throwable(message).apply {
        stackTrace = self.stackTrace.filter { pattern.find("$it") != null }.toTypedArray()
        self.cause?.let { initCause(it.filterStackTrace(pattern)) }
    }
}

fun Throwable.toString(pattern: Regex? = null): String {
    if( enableLogback ) {
        try {
            val proxy = ThrowableProxy(filterStackTrace(pattern)).apply { calculatePackagingData() }
            return ThrowableProxyUtil.asString(proxy)
        } catch (e: NoClassDefFoundError) {
            enableLogback = false
        }
    }
    StringWriter().use { writer ->
        PrintWriter(writer).use { printer ->
            printStackTrace(printer)
            return writer.toString()
        }
    }
}