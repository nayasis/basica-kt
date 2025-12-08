package io.github.nayasis.kotlin.basica.core.io

import com.sigpwned.chardet4j.Chardet
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.CopyOption
import java.nio.file.Files
import java.nio.file.Path

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
 * copy contents to given target file
 *
 * @receiver InputStream
 * @param target file path
 * @param option options how the copy should be done
 * @return written number of bytes
 */
fun InputStream.copyTo(target: Path, vararg option: CopyOption): Long {
    return Files.copy(this, target, *option )
}

/**
 * drain remaining contents
 *
 * @param bufferSize buffer size in operation (default: DEFAULT_BUFFER_SIZE)
 * @return number of bytes drained
 */
fun InputStream.drain(bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
    val buffer = ByteArray(bufferSize)
    var read: Int
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

/**
 * Returns a new [BufferedReader] for this input stream.
 *
 * @param charset default charset to use if detection fails (UTF-8 by default)
 * @param autodetect when true, chardet4j detects BOM/charset; when false, uses {@code charset} as-is
 * @return buffered reader for this input stream
 */
fun InputStream.reader(charset: Charset = Charsets.UTF_8, autodetect: Boolean = true): BufferedReader {
    return if(autodetect) {
        Chardet.decode(this, null, charset).let { BufferedReader(it) }
    } else {
        this.bufferedReader(charset)
    }
}
