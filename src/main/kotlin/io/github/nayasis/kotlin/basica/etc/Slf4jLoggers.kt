package io.github.nayasis.kotlin.basica.etc

import org.slf4j.Logger
import org.slf4j.Marker

fun Logger.trace(throwable: Throwable?) {
    this.trace(throwable?.message,throwable)
}

fun Logger.trace(marker: Marker, throwable: Throwable?) {
    this.trace(marker,throwable?.message,throwable)
}

fun Logger.debug(throwable: Throwable?) {
    this.debug(throwable?.message,throwable)
}

fun Logger.debug(marker: Marker, throwable: Throwable?) {
    this.debug(marker,throwable?.message,throwable)
}

fun Logger.info(throwable: Throwable?) {
    this.info(throwable?.message,throwable)
}

fun Logger.info(marker: Marker, throwable: Throwable?) {
    this.info(marker,throwable?.message,throwable)
}

fun Logger.warn(throwable: Throwable?) {
    this.warn(throwable?.message,throwable)
}

fun Logger.warn(marker: Marker, throwable: Throwable?) {
    this.warn(marker,throwable?.message,throwable)
}

fun Logger.error(throwable: Throwable?) {
    this.error(throwable?.message,throwable)
}

fun Logger.error(marker: Marker, throwable: Throwable?) {
    this.error(marker,throwable?.message,throwable)
}