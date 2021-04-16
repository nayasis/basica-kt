package com.github.nayasis.kotlin.basica.exception

import ch.qos.logback.classic.spi.ThrowableProxy
import ch.qos.logback.classic.spi.ThrowableProxyUtil

class ProxyThrowables {
    fun toString(throwable: Throwable): String {
        val proxy = ThrowableProxy(throwable)
        proxy.calculatePackagingData()
        return ThrowableProxyUtil.asString(proxy)
    }
}