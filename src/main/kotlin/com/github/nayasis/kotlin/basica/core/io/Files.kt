package com.github.nayasis.kotlin.basica.core.io

import java.io.File
import java.net.URL

fun File.toUrl(): URL {
    return this.toURI().toURL()
}

val File.directory: File
    get() {
        return if( exists() ) {
            if( isDirectory ) this else this.parentFile
        } else {
            this.parentFile
        }
    }