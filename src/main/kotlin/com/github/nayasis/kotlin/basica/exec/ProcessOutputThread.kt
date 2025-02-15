package com.github.nayasis.kotlin.basica.exec

import com.github.nayasis.kotlin.basica.etc.error
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.CountDownLatch

private val logger = KotlinLogging.logger{}

class ProcessOutputThread(
    private val inputStream: InputStream,
    private val reader: ((String) -> Unit)?,
    private val countDownLatch: CountDownLatch?,
    private val charset: String,
): Thread() {

    init {
        isDaemon = true
    }

    override fun run() {
        inputStream.use { InputStreamReader(it, charset).use { stream -> BufferedReader(stream).use { reader ->
            try {
                read(reader)
            } catch (e: Exception) {
                logger.error(e)
            } finally {
                countDownLatch?.countDown()
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