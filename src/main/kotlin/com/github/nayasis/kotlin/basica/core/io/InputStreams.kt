package com.github.nayasis.kotlin.basica.core.io

import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8

/**
 * copy contents to given output stream
 *
 * @param out OutputStream to copy to
 * @param bufferSize buffer size in operation (default: DEFAULT_BUFFER_SIZE)
 * @param callback progress callback
 * @return number of bytes copied
 */
fun InputStream.copyTo(out: OutputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE, callback: ((copiedSize: Long) -> Unit)?): Long {
    var copied = 0L
    val buffer = ByteArray(bufferSize)
    var bytes = read(buffer)
    while (bytes >= 0) {
        out.write(buffer, 0, bytes)
        copied += bytes
        bytes = read(buffer)
        callback?.invoke(copied)
    }
    out.flush()
    return copied
}

/**
 * copy range of contents to given output stream
 *
 * @receiver InputStream
 * @param out OutputStream to copy to
 * @param start position to start copying
 * @param end position to end copying
 * @param bufferSize buffer size in operation (default: DEFAULT_BUFFER_SIZE)
 * @param callback progress callback
 * @return number of bytes copied
 */
fun InputStream.copyTo(out: OutputStream, start: Long, end: Long, bufferSize: Int = DEFAULT_BUFFER_SIZE, callback: ((copiedSize: Long) -> Unit)?): Long {

    this.skip(start).also { skipped -> if(skipped < start) throw IOException("Skipped only $skipped bytes out of $start required") }

    var remainToCopy = end - start + 1
    var copied = 0L
    val buffer = ByteArray(minOf(bufferSize.toLong(), remainToCopy).toInt())

    while(remainToCopy > 0) {
        val read = this.read(buffer).also {
            copied += it
            callback?.invoke(copied)
        }
        when {
            read < 0 -> break
            read <= remainToCopy -> {
                out.write(buffer,0,read)
                remainToCopy -= read
            }
            else -> {
                out.write(buffer,0,remainToCopy.toInt())
                remainToCopy = 0
            }
        }
    }
    return end - start + 1 - remainToCopy

}

/**
 * drain remaining contents
 *
 * @param bufferSize buffer size in operation (default: DEFAULT_BUFFER_SIZE)
 * @return number of bytes drained
 */
fun InputStream.drain(bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
    val buffer = ByteArray(bufferSize)
    var read  = 0
    var total = 0L
    while (read(buffer).also { read = it } >= 0) {
        total += read
    }
    this.readBytes()
    return total
}

/**
 * convert to string
 *
 * @param charset {@link Charset} in decoding
 * @param bufferSize buffer size in operation (default: DEFAULT_BUFFER_SIZE)
 * @return converted string
 */
fun InputStream.toString(charset: Charset = UTF_8, bufferSize: Int = DEFAULT_BUFFER_SIZE): String {
    val out    = StringBuilder(bufferSize)
    val reader = InputStreamReader(this, charset)
    val buffer = CharArray(bufferSize)
    var bytes  = reader.read(buffer)
    while(bytes >= 0) {
        out.append(buffer,0,bytes)
        bytes  = reader.read(buffer)
    }
    return out.toString()
}