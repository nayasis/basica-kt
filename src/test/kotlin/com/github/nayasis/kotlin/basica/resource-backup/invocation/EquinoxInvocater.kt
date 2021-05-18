package com.github.nayasis.kotlin.basica.`resource-backup`.invocation

import com.github.nayasis.basica.base.Classes
import lombok.experimental.UtilityClass
import java.lang.reflect.Method
import java.net.URL

@UtilityClass
@Slf4j
class EquinoxInvocater {
    companion object {
        private var EQUINOX_RESOLVE_METHOD: Method? = null

        init {
            try {
                // Detect Equinox OSGi (e.g. on WebSphere 6.1)
                val fileLocator = Classes.forName("org.eclipse.core.runtime.FileLocator")
                EQUINOX_RESOLVE_METHOD =
                    com.github.nayasis.kotlin.basica.core.`resource-backup`.invocation.fileLocator.getMethod(
                        "resolve",
                        URL::class.java
                    )
                log.trace("Found Equinox FileLocator for OSGi bundle URL resolution")
            } catch (e: Throwable) {
                EQUINOX_RESOLVE_METHOD = null
            }
        }
    }

    fun isEquinoxUrl(url: URL?): Boolean {
        return url != null && EQUINOX_RESOLVE_METHOD != null && url.protocol.startsWith("bundle")
    }

    fun unwrap(url: URL?): URL? {
        return if (isEquinoxUrl(url)) {
            val rtn: Any = ClassReflector.invokeMethod(
                EQUINOX_RESOLVE_METHOD,
                null,
                url
            )
            if (rtn == null) null else rtn as URL
        } else {
            url
        }
    }
}