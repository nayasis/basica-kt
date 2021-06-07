package com.github.nayasis.kotlin.basica.core.path

import java.io.File
import java.net.URL

fun File.toUrl(): URL {
    return this.toURI().toURL()
}