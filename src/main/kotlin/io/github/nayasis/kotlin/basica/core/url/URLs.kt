package io.github.nayasis.kotlin.basica.core.url

import io.github.nayasis.kotlin.basica.core.io.detectCharset
import io.github.nayasis.kotlin.basica.core.string.toUri
import java.io.File
import java.io.InputStream
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.nio.charset.Charset

fun URL?.toUri(): URI = (this?.toString() ?: "").toUri()

fun URL.toFile(): File {
    return try {
        this.toURI().toFile()
    } catch (e: URISyntaxException) {
        File(this.file)
    }
}

fun URL.toInputStream(): InputStream = this.openStream()

fun URL.detectCharset(default: Charset = Charsets.UTF_8): Charset =
    this.toInputStream().use { detectCharset(it,default) }

fun URI.toFile(): File = File(this.schemeSpecificPart)