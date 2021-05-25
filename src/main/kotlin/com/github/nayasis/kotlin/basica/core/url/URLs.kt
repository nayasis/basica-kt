package com.github.nayasis.kotlin.basica.core.url

import com.github.nayasis.kotlin.basica.core.string.toUri
import java.net.URI
import java.net.URL

fun URL?.toUri(): URI {
    return (this?.toString() ?: "").toUri()
}