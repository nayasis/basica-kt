package com.github.nayasis.kotlin.basica.exception

import com.github.nayasis.basica.exception.helper.ProxyThrowables
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*

class Exceptions { companion object{

    var filter: Regex? = null

    private var useLogback = true

    /**
     * filter stacktrace in exception
     *
     * @param exception exception to exclude stacktrace
     * @return  exception excluding stacktrace by filter regular expression.
     */
    fun filter(exception: Throwable): Throwable {
        val filtered = Throwable(exception.message)
        val traces = ArrayList<StackTraceElement>()
        for (trace in exception.stackTrace)
            if( filter?.matches(trace.toString()) != true )
                traces.add(trace)
        filtered.stackTrace = traces.toArray(arrayOf<StackTraceElement>())
        if (exception.cause != null) {
            val cause = filter(exception.cause!!)
            filtered.initCause(cause)
        }
        return filtered
    }

    /**
     * get root cause of given exception.
     *
     * @param exception exception to inspect.
     * @return  innermost exception or null if not exist.
     */
    fun rootCause(exception: Throwable?): Throwable? {
        if (exception == null) return null
        var root: Throwable? = null
        var cause = exception.cause
        while (cause != null && cause !== root) {
            root = cause
            cause = cause.cause
        }
        return root
    }

    /**
     * convert to string
     *
     * @param exception
     * @return error
     */
    fun toString(exception: Throwable?): String {
        if (exception == null) return ""
        if (useLogback)
            try {
                return ProxyThrowables().toString(exception)
            } catch (e: Throwable) {
                useLogback = false
            }
        StringWriter().use { writer ->
            PrintWriter(writer).use { printer ->
                exception.printStackTrace(printer)
                return writer.toString()
            }
        }
    }

}}
