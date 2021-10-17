package com.github.nayasis.kotlin.basica.exec

import com.github.nayasis.kotlin.basica.core.extention.ifNotEmpty
import com.github.nayasis.kotlin.basica.core.extention.isNotEmpty
import com.github.nayasis.kotlin.basica.core.string.toFile
import com.github.nayasis.kotlin.basica.etc.Platforms
import mu.KotlinLogging
import java.io.BufferedWriter
import java.io.InputStream
import java.io.OutputStreamWriter
import java.security.InvalidParameterException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {}

/**
 * Commandline executor
 */
class CommandExecutor {

    var process: Process? = null
    var onProcessFailed: ((Throwable)->Unit)? = null

    private var output: ProcessOutputThread? = null
    private var error: ProcessOutputThread? = null
    private var latch: CountDownLatch? = null

    private var inputPipe: BufferedWriter? = null
        get() {
            if(process == null) return null
            if(field == null)
                field = BufferedWriter(OutputStreamWriter(process!!.outputStream, Platforms.os.charset))
            return field
        }

    val outputStream: InputStream?
        get() = process?.inputStream

    val errorStream: InputStream?
        get() = process?.errorStream

    /**
     * run command
     *
     * @param command       command to execute
     * @param outputReader  output stream line reader
     * @param errorReader   error stream line reader
     */
    fun run(command: Command, outputReader: ((String) -> Unit)? = {}, errorReader: ((String) -> Unit)? = {}): CommandExecutor {

        if(alive)
            throw IllegalAccessException("process is running.")
        if(command.isEmpty())
            throw InvalidParameterException("command is empty.")

        val builder = ProcessBuilder(command.command).apply {
            environment().putAll(command.environment)
            command.workingDirectory?.toFile().ifNotEmpty { directory(it) }
        }

        try {
            process = builder.start()
        } catch (e: Throwable) {
            onProcessFailed?.let{it(e)}
            throw e
        }

        latch = listOfNotNull(outputReader, errorReader).size.let { if(it > 0) CountDownLatch(it) else null }

        if( latch != null ) {
            outputReader?.let { output = ProcessOutputThread(process!!.inputStream,it,latch!!).apply { start() } }
            errorReader?.let { error = ProcessOutputThread(process!!.errorStream,it,latch!!).apply { start() } }
        }

        return this

    }

    /**
     * run command
     *
     * @param command       command to execute
     * @param outputReader  output stream line reader (include error stream)
     */
    fun run(command: Command, outputReader: (String) -> Unit): CommandExecutor {
        return run(command,outputReader,outputReader)
    }

    /**
     * run command
     *
     * @param command   command to execute
     * @param output    printed output
     * @param error     printed error
     */
    fun run(command: Command, output: StringBuffer, error: StringBuffer): CommandExecutor {
        return run(command,{output.append(it)}, {error.append(it)})
    }

    /**
     * run command
     *
     * @param command   command to execute
     * @param output    printed output (include error)
     */
    fun run(command: Command, output: StringBuffer): CommandExecutor {
        return run(command,output,output)
    }

    /**
     * run command
     * - print stream to System.out and System.err
     *
     * @param command   command to execute
     */
    fun runOnSystemOut(command: Command): CommandExecutor {
        return run(command,{print(it)},{System.err.print(it)})
    }

    /**
     * run command
     *
     * @param command       command to execute
     * @param outputReader  output stream line reader
     * @param errorReader   error stream line reader
     */
    fun run(command: String, outputReader: ((String) -> Unit)? = {}, errorReader: ((String) -> Unit)? = {}): CommandExecutor {
        return run(Command(command),outputReader,errorReader)
    }

    /**
     * run command
     *
     * @param command       command to execute
     * @param outputReader  output stream line reader (include error stream)
     */
    fun run(command: String, outputReader: (String) -> Unit): CommandExecutor {
        return run(command,outputReader,outputReader)
    }

    /**
     * run command
     *
     * @param command   command to execute
     * @param output    printed output
     * @param error     printed error
     */
    fun run(command: String, output: StringBuffer, error: StringBuffer): CommandExecutor {
        return run(command,{output.append(it)}, {error.append(it)})
    }

    /**
     * run command
     *
     * @param command   command to execute
     * @param output    printed output (include error)
     */
    fun run(command: String, output: StringBuffer): CommandExecutor {
        return run(command,output,output)
    }

    /**
     * run command
     * - print stream to System.out and System.err
     *
     * @param command   command to execute
     */
    fun runOnSystemOut(command: String): CommandExecutor {
        return run(command,{print(it)},{System.err.print(it)})
    }

    /**
     * process is alive or not
     */
    val alive: Boolean
        get() = when {
            process?.isAlive == true -> true
            output?.isAlive == true -> true
            error?.isAlive == true -> true
            else -> false
        }

    /**
     * process termination code
     */
    var exitValue: Int? = null
        private set

    /**
     * wait until process is closed.
     *
     * @param timeout	max wait time (milli-seconds)
     * @return	process termination code ( 0 : success )
     */
    fun waitFor(timeout: Long = -1): Int? {

        if(!alive) return null

        try {
            process?.let {
                if( timeout < 0) {
                    it.waitFor()
                } else {
                    it.waitFor(timeout,TimeUnit.MILLISECONDS)
                }
                exitValue = it.exitValue()
            }
        } catch (e: Throwable) {
            onProcessFailed?.let { it(e) }
            return destroy()
        }

        try {
            latch?.let {
                if(timeout < 0) {
                    it.await()
                } else {
                    it.await(timeout,TimeUnit.MILLISECONDS)
                }
            }
        } finally {
            return destroy()
        }

    }

    /**
     * terminate process forcibly.
     * @return	process termination code ( 0 : success )
     */
    fun destroy(): Int? {
        exitValue = process?.exitValue()
        runCatching { process?.destroyForcibly() }; process = null
        runCatching { output?.interrupt() }; output = null
        runCatching { error?.interrupt() }; error = null
        runCatching { inputPipe?.close() }; inputPipe = null
        latch = null
        return exitValue
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
            it.write("\n")
            it.flush()
            true
        } ?: false
    }

}