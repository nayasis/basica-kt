package com.github.nayasis.kotlin.basica.exec

import com.github.nayasis.kotlin.basica.core.extension.ifNotEmpty
import com.github.nayasis.kotlin.basica.core.string.toFile
import com.github.nayasis.kotlin.basica.etc.Platforms
import mu.KotlinLogging
import java.io.BufferedWriter
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.security.InvalidParameterException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

/**
 * Commandline executor
 */
class CommandExecutor {

    /**
     * native process
     */
    var process: Process
        private set

    private var inputGobbler: ProcessOutputThread? = null
    private var errorGobbler: ProcessOutputThread? = null
    private var countdownLatch: CountDownLatch? = null
    private val commandWriter: BufferedWriter
        get() {
            if(_writer == null)
                _writer = BufferedWriter(OutputStreamWriter(outputStream, Platforms.os.charset))
            return _writer!!
        }
        private var _writer: BufferedWriter? = null

    /**
     * normal input stream (receiving printed output from process)
     */
    val inputStream: InputStream
        get() = process.inputStream

    /**
     * error input stream (pipe receiving error from process)
     */
    val errorStream: InputStream
        get() = process.errorStream

    /**
     * process output stream (pipe sending command to process)
     */
    val outputStream: OutputStream
        get() = process.outputStream

    /**
     * run command
     *
     * @param command       command to execute
     * @param charset       supported character set (default: OS default)
     * @param outputReader  output reader
     * @param errorReader   error reader
     */
    constructor(
        command: Command,
        charset: String = Platforms.os.charset,
        outputReader: ((String) -> Unit)? = null,
        errorReader: ((String) -> Unit)? = null,
    ) {

        if(command.isEmpty())
            throw InvalidParameterException("command is empty.")

        val builder = ProcessBuilder(command.command).apply {
            environment().putAll(command.environment)
            command.workingDirectory?.toFile().ifNotEmpty { if(it.exists()) directory(it) }
            when {
                outputReader == null && errorReader == null -> {
                    redirectInput(ProcessBuilder.Redirect.INHERIT)
                    redirectError(ProcessBuilder.Redirect.INHERIT)
                }
                outputReader != null -> {
                    redirectErrorStream(true)
                }
                errorReader != null -> {
                    redirectInput(ProcessBuilder.Redirect.INHERIT)
                }
            }
        }

        process = builder.start()

        countdownLatch = listOfNotNull(outputReader, errorReader).ifNotEmpty { CountDownLatch(it.size) }
        when {
            outputReader != null && errorReader != null -> {
                outputReader.let { inputGobbler = ProcessOutputThread(inputStream,it,countdownLatch,charset).apply { start() } }
                errorReader.let { errorGobbler = ProcessOutputThread(errorStream,it,countdownLatch,charset).apply { start() } }
            }
            outputReader != null -> {
                outputReader.let { inputGobbler = ProcessOutputThread(inputStream,it,countdownLatch,charset).apply { start() } }
            }
            errorReader != null -> {
                errorReader.let { errorGobbler = ProcessOutputThread(errorStream,it,countdownLatch,charset).apply { start() } }
            }
        }

    }

    /**
     * process is alive or not
     */
    val isAlive: Boolean
        get() = when {
            process.isAlive -> true
            inputGobbler?.isAlive == true -> true
            errorGobbler?.isAlive == true -> true
            else -> false
        }

    /**
     * wait until process is closed.
     *
     * @param timeout	max wait time (milli-seconds)
     * @return	process termination code ( 0 : success )
     */
    fun waitFor(timeout: Long = -1): Int {
        try {
            if( timeout < 0) {
                process.waitFor()
            } else {
                process.waitFor(timeout,TimeUnit.MILLISECONDS)
            }
            countdownLatch?.let {
                if(timeout < 0) {
                    it.await()
                } else {
                    it.await(timeout,TimeUnit.MILLISECONDS)
                }
            }
            return process.exitValue()
        } finally {
            destroy()
        }
    }

    /**
     * terminate process forcibly.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun destroy() {
        runCatching { process.destroyForcibly() };
        runCatching { inputGobbler?.interrupt() }; inputGobbler = null
        runCatching { errorGobbler?.interrupt() }; errorGobbler = null
        runCatching { _writer?.run{
            commandWriter.close()
            _writer = null
        }}
        runCatching { outputStream.close() }
        runCatching { inputStream.close() }
        runCatching { errorStream.close() }
        countdownLatch = null
    }

    /**
     * send command to process
     *
     * @param command command
     */
    fun sendCommand(command: String) {
        commandWriter.let {
            it.write(command)
            it.flush()
        }
    }

}