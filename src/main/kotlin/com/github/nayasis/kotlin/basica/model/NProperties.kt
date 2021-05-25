package com.github.nayasis.kotlin.basica.model

import com.github.nayasis.kotlin.basica.core.path.detectCharset
import com.github.nayasis.kotlin.basica.core.path.inStream
import com.github.nayasis.kotlin.basica.core.string.toPath
import com.github.nayasis.kotlin.basica.core.url.detectCharset
import com.github.nayasis.kotlin.basica.core.url.inStream
import java.io.File
import java.io.InputStream
import java.net.URL
import java.nio.charset.Charset
import java.nio.file.Path
import java.util.*

class NProperties: Properties {

    constructor(resourcePath: String, charset: Charset = resourcePath.toPath().detectCharset()) {
        load(resourcePath.toPath().inStream(), charset)
    }

    constructor(file: File, charset: Charset = file.toPath().detectCharset()) {
        load(file.toPath().inStream(), charset)
    }

    constructor(path: Path, charset: Charset = path.detectCharset()) {
        load(path.inStream(), charset)
    }

    constructor(url: URL, charset: Charset = url.detectCharset()) {
        load(url.inStream(), charset)
    }

    constructor(defaults: Properties?): super(defaults)

    @Synchronized
    fun load(inStream: InputStream?, charset: Charset = Charsets.UTF_8 ) {
        if( inStream == null ) return
        inStream.bufferedReader(charset).use { super.load(it) }
    }

    fun get(key: String): String? {
        return super.getProperty(key)
    }

    fun get(key: String, default: String): String {
        return super.getProperty(key, default)
    }

    fun set(key: String, value: String?) {
        super.setProperty(key, value ?: "")
    }

    fun toMap(): Map<String,String> {
        return this.map { "${it.key}" to "${it.value}" }.toMap()
    }

}