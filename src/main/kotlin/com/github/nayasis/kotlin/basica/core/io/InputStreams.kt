package com.github.nayasis.kotlin.basica.core.io

import java.io.InputStream
import java.io.OutputStream

fun InputStream.copyTo(out: OutputStream,bufferSize: Int = DEFAULT_BUFFER_SIZE,callback: ((copiedSize: Long) -> Unit)?): Long {
    var copied = 0L
    val buffer = ByteArray(bufferSize)
    var bytes = read(buffer)
    while (bytes >= 0) {
        out.write(buffer, 0, bytes)
        copied += bytes
        bytes = read(buffer)
        callback?.invoke(copied)
    }
    return copied
}