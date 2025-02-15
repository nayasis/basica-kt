package com.github.nayasis.kotlin.basica.etc

import io.github.oshai.kotlinlogging.KLogger

fun KLogger.trace(e: Throwable) {
    this.trace(e) {e.message}
}

fun KLogger.debug(e: Throwable) {
    this.debug(e) {e.message}
}

fun KLogger.info(e: Throwable) {
    this.info(e) {e.message}
}

fun KLogger.warn(e: Throwable) {
    this.warn(e) {e.message}
}


fun KLogger.error(e: Throwable) {
    this.error(e) {e.message}
}