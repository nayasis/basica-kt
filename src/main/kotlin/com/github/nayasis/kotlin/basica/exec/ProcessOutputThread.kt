package com.github.nayasis.kotlin.basica.exec

import com.github.nayasis.kotlin.basica.etc.Platforms
import com.github.nayasis.kotlin.basica.etc.error
import mu.KotlinLogging
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.CountDownLatch

private val logger = KotlinLogging.logger{}

class ProcessOutputThread(
    val inputStream: InputStream,
    private val reader: ((String) -> Unit)?,
    private val latch: CountDownLatch,
): Thread() {

    init {
        isDaemon = true
    }

    override fun run() {
        inputStream.use { InputStreamReader(it, Platforms.os.charset).use { stream -> BufferedReader(stream).use { reader ->
            try {
                read(reader)
            } catch (e: Exception) {
                logger.error(e)
            } finally {
                latch.countDown()
            }
        }}}
    }

    private fun read(bufferedReader: BufferedReader) {
        var line: String? = null
        while( ! isInterrupted && bufferedReader.readLine().also { line = it } != null ) {
            this.reader?.invoke(line!!)
        }
    }

}