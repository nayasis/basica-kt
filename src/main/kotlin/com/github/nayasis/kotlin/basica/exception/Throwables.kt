package com.github.nayasis.kotlin.basica.exception

import java.io.PrintWriter
import java.io.StringWriter
import kotlin.reflect.KClass

fun Throwable.filterStackTrace(pattern: Regex? = null, exclusive: Boolean = true): Throwable {
    if( pattern == null ) return this

    val newStackTrace = this.stackTrace.filter { element ->
        if(exclusive)
            pattern.find(element.className) == null
        else
            pattern.find(element.className) != null
    }.toTypedArray()
    
    val newCause = this.cause?.filterStackTrace(pattern, exclusive)

    return Throwable(message).apply {
        stackTrace = newStackTrace
        newCause?.let { this.initCause(it) }
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


fun Throwable.toString(exclusive: Regex? = null): String {
    return StringWriter().use { sw -> PrintWriter(sw).use { pw ->

        // print the throwable message
        pw.println(this.toString())

        // print stack trace
        if (exclusive != null) {
            this.stackTrace.filter { ! it.className.matches(exclusive) }
        } else {
            this.stackTrace.toList()
        }.forEach { element ->
            pw.println("\tat $element")
        }

        // print cause if exists
        var cause = this.cause
        while (cause != null) {
            pw.println("Caused by: ${cause.toString()}")
            if (exclusive != null) {
                cause.stackTrace.filter { ! it.className.matches(exclusive) }
            } else {
                cause.stackTrace.toList()
            }.forEach { element ->
                pw.println("\tat $element")
            }
            cause = cause.cause
        }

    }}.toString()
}

val Throwable.rootCause
    get(): Throwable {
        var cause = this
        while( cause.cause != null ) {
            cause = cause.cause!!
        }
        return cause
    }