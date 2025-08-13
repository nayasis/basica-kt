package io.github.nayasis.kotlin.basica.model

import io.github.nayasis.kotlin.basica.core.io.detectCharset
import io.github.nayasis.kotlin.basica.core.io.inputStream
import io.github.nayasis.kotlin.basica.core.string.toPath
import io.github.nayasis.kotlin.basica.core.url.detectCharset
import io.github.nayasis.kotlin.basica.core.url.toInputStream
import java.io.File
import java.io.InputStream
import java.net.URL
import java.nio.charset.Charset
import java.nio.file.Path
import java.util.*

class NProperties: Properties {

    constructor(resourcePath: String, charset: Charset = resourcePath.toPath().detectCharset()) {
        load(resourcePath.toPath().inputStream(), charset)
    }

    constructor(file: File, charset: Charset = file.toPath().detectCharset()) {
        load(file.toPath().inputStream(), charset)
    }

    constructor(path: Path, charset: Charset = path.detectCharset()) {
        load(path.inputStream(), charset)
    }

    constructor(url: URL, charset: Charset = url.detectCharset()) {
        load(url.toInputStream(), charset)
    }

    constructor(defaults: Properties?): super(defaults)
    constructor(): super()


    @Synchronized
    fun load(inStream: InputStream?, charset: Charset = Charsets.UTF_8 ) {
        if( inStream == null ) return
        inStream.bufferedReader(charset).use { super.load(it) }
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