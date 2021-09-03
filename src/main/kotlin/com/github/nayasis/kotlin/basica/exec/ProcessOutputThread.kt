package com.github.nayasis.kotlin.basica.exec

import com.github.nayasis.kotlin.basica.etc.Platforms
import com.github.nayasis.kotlin.basica.etc.error
import mu.KotlinLogging
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.CountDownLatch

private val log = KotlinLogging.logger{}

class ProcessOutputThread(
    val inputStream: InputStream,
    private val reader: ((String) -> Unit)?,
    private val latch: CountDownLatch,
): Thread() {

    init {
        isDaemon = true
    }

    override fun run() {
        inputStream.use { stream -> InputStreamReader(stream, Platforms.os.charset).use { streamReader -> BufferedReader(streamReader).use { reader ->
            try {
                read(reader)
            } catch (e: Exception) {
                log.error(e)
            } finally {
                latch.countDown()
            }
        }}}
    }

    private fun read(bufferedReader: BufferedReader) {
        val buffer = CharArray(1024)
        var length = 0
        while( ! isInterrupted && bufferedReader.read(buffer,0,buffer.size).also{length = it} != -1 ) {
            val txt = StringBuilder(length).append(buffer,0,length).toString()
            reader?.let{it(txt)}
        }
    }

}