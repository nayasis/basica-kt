package com.github.nayasis.kotlin.basica.exec

import com.github.nayasis.kotlin.basica.core.extention.ifNotEmpty
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

private val log = KotlinLogging.logger {}

/**
 * Commandline executor
 */
class CommandExecutor {

    /**
     * native process
     */
    var process: Process
        private set

    private var outputGobbler: ProcessOutputThread? = null
    private var errorGobbler:  ProcessOutputThread? = null
    private var latch: CountDownLatch? = null

    private var inputPipe: BufferedWriter? = null
        get() {
            if(process == null) return null
            if(field == null)
                field = BufferedWriter(OutputStreamWriter(input, Platforms.os.charset))
            return field
        }

    /**
     * normal output of process
     */
    val output: InputStream
        get() = process.inputStream

    /**
     * error output of process
     */
    val error: InputStream
        get() = process.errorStream

    /**
     * normal input stream of process
     */
    val input: OutputStream
        get() = process.outputStream

    /**
     * run command
     *
     * @param command       command to execute
     * @param outputReader  output stream line reader
     * @param errorReader   error stream line reader
     */
    constructor(command: Command, outputReader: ((String) -> Unit)? = null, errorReader: ((String) -> Unit)? = null) {

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

        latch = listOfNotNull(outputReader, errorReader).size.let { if(it > 0) CountDownLatch(it) else null }

        when {
            outputReader != null && errorReader != null -> {
                outputReader.let { outputGobbler = ProcessOutputThread(output!!,it,latch!!).apply { start() } }
                errorReader.let { errorGobbler = ProcessOutputThread(error!!,it,latch!!).apply { start() } }
            }
            outputReader != null -> {
                outputReader.let { outputGobbler = ProcessOutputThread(output!!,it,latch!!).apply { start() } }
            }
            errorReader != null -> {
                errorReader.let { errorGobbler = ProcessOutputThread(error!!,it,latch!!).apply { start() } }
            }
        }

    }

    /**
     * run command
     *
     * @param command       command to execute
     * @param outputReader  output stream line reader (include error stream)
     */
    constructor(command: Command, outputReader: (String) -> Unit): this(command,outputReader,null)

    /**
     * run command
     *
     * @param command   command to execute
     * @param output    printed output
     * @param error     printed error
     */
    constructor(command: Command, output: StringBuffer, error: StringBuffer): this(command,{output.append(it)}, {error.append(it)})

    /**
     * run command
     *
     * @param command   command to execute
     * @param output    printed output (include error)
     */
    constructor(command: Command, output: StringBuffer): this(command,{output.append(it)}, null)

    /**
     * process is alive or not
     */
    val alive: Boolean
        get() = when {
            process?.isAlive == true -> true
            outputGobbler?.isAlive == true -> true
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
            process.let {
                if( timeout < 0) {
                    it.waitFor()
                } else {
                    it.waitFor(timeout,TimeUnit.MILLISECONDS)
                }
            }

            latch?.let {
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
    fun destroy() {
        runCatching { process.destroyForcibly() };
        runCatching { outputGobbler?.interrupt() }; outputGobbler = null
        runCatching { errorGobbler?.interrupt() }; errorGobbler = null
        runCatching { inputPipe?.close() }; inputPipe = null
        runCatching { input.close() }
        runCatching { output.close() }
        runCatching { error.close() }
        latch = null
    }

    /**
     * send command to process
     *
     * @param command command
     * @return true if command is sent to process.
     */
    fun sendCommand(command: String): Boolean {
        return inputPipe?.let {
            it.write(command)
            it.flush()
            true
        } ?: false
    }

}