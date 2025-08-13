package io.github.nayasis.kotlin.basica.core.resource.invocation

import io.github.nayasis.kotlin.basica.core.klass.Classes
import io.github.oshai.kotlinlogging.KotlinLogging
import java.lang.reflect.Method
import java.net.URL

private val log = KotlinLogging.logger {}

class EquinoxInvoker {

    companion object {

        private var EQUINOX_METHOD: Method?

        init {
            try {
                // Detect Equinox OSGi (e.g. on WebSphere 6.1)
                val fileLocator = Classes.forName("org.eclipse.core.runtime.FileLocator")
                EQUINOX_METHOD = fileLocator.getMethod("resolve", URL::class.java )
                log.trace{"Found Equinox FileLocator for OSGi bundle URL resolution"}
            } catch (e: Throwable) {
                EQUINOX_METHOD = null
            }
        }

    }

    fun isEquinoxUrl(url: URL?): Boolean {
        return url != null && EQUINOX_METHOD != null && url.protocol.startsWith("bundle")
    }

    fun unwrap(url: URL?): URL? {
        return when {
            isEquinoxUrl(url) -> EQUINOX_METHOD?.invoke(null,url) as URL?
            else -> url
        }
    }

}