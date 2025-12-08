package io.github.nayasis.kotlin.basica.model

import io.github.nayasis.kotlin.basica.core.io.reader
import io.github.nayasis.kotlin.basica.core.string.toPath
import io.github.nayasis.kotlin.basica.core.url.toInputStream
import java.io.File
import java.io.InputStream
import java.net.URL
import java.nio.charset.Charset
import java.nio.file.Path
import java.util.*

class NProperties: Properties {

    constructor(resourcePath: String, charset: Charset = Charsets.UTF_8) {
        load(resourcePath.toPath(), charset)
    }

    constructor(file: File, charset: Charset = Charsets.UTF_8) {
        load(file.toPath(), charset)
    }

    constructor(path: Path, charset: Charset = Charsets.UTF_8) {
        load(path, charset)
    }

    constructor(url: URL, charset: Charset = Charsets.UTF_8) {
        load(url.toInputStream(), charset)
    }

    constructor(defaults: Properties?): super(defaults)
    constructor(): super()


    @Synchronized
    fun load(inStream: InputStream?, charset: Charset = Charsets.UTF_8) {
        inStream?.reader(charset)?.use { super.load(it) }
    }

    @Synchronized
    fun load(path: Path?, charset: Charset = Charsets.UTF_8) {
        path?.reader(charset)?.use { super.load(it) }
    }

    operator fun get(key: String): String? {
        return super.getProperty(key)
    }

    fun get(key: String, default: String): String {
        return super.getProperty(key, default)
    }

    operator fun set(key: String, value: String?) {
        super.setProperty(key, value ?: "")
    }

    fun toMap(): Map<String,String> {
        return this.map { "${it.key}" to "${it.value}" }.toMap()
    }

}