package com.github.nayasis.kotlin.basica.exception

import ch.qos.logback.classic.spi.ThrowableProxy
import ch.qos.logback.classic.spi.ThrowableProxyUtil
import kotlin.reflect.KClass

private var enableLogback = true

fun Throwable.filterStackTrace(pattern: Regex? = null): Throwable {
    if( pattern == null ) return this
    val self = this
    return Throwable(message).apply {
        stackTrace = self.stackTrace.filter { pattern.find("$it") == null }.toTypedArray()
        self.cause?.let { initCause(it.filterStackTrace(pattern)) }
    }
}

fun <T: Exception> Throwable.findCause(klass: KClass<T>): T? {
    var cause: Throwable? = this
    while(cause != null) {
        if(cause.javaClass == klass.java) {
            @Suppress("UNCHECKED_CAST")
            return cause as T
        } else {
            cause = cause.cause
        }
    }
    return null
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
    return stackTraceToString()
}

val Throwable.rootCause
    get(): Throwable {
        var cause = this
        while( cause.cause != null ) {
            cause = cause.cause!!
        }
        return cause
    }