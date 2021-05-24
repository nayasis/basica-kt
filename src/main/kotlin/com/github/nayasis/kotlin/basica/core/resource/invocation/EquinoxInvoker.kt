package com.github.nayasis.kotlin.basica.core.resource.invocation

import com.github.nayasis.kotlin.basica.core.klass.Classes
import mu.KotlinLogging
import java.lang.reflect.Method
import java.net.URL

private val log = KotlinLogging.logger {}

class EquinoxInvoker { companion object {

    private var EQUINOX_RESOLVE_METHOD: Method? = null

    init {
        try {
            // Detect Equinox OSGi (e.g. on WebSphere 6.1)
            val fileLocator = Classes.forName("org.eclipse.core.runtime.FileLocator")
            EQUINOX_RESOLVE_METHOD = fileLocator.getMethod("resolve", URL::class.java )
            log.trace{"Found Equinox FileLocator for OSGi bundle URL resolution"}
        } catch (e: Throwable) {
            EQUINOX_RESOLVE_METHOD = null
        }
    }

    fun isEquinoxUrl(url: URL?): Boolean {
        return url != null && EQUINOX_RESOLVE_METHOD != null && url.protocol.startsWith("bundle")
    }

    fun unwrap(url: URL?): URL? {
        return if (isEquinoxUrl(url)) {
            val rtn: Any? = EQUINOX_RESOLVE_METHOD?.invoke(null,url) ?: null
            if (rtn == null) null else rtn as URL
        } else {
            url
        }
    }

}}