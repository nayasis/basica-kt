package com.github.nayasis.kotlin.basica.exec

import com.github.nayasis.kotlin.basica.core.extention.ifNotEmpty
import com.github.nayasis.kotlin.basica.core.extention.isNotEmpty
import com.github.nayasis.kotlin.basica.core.path.isFile
import com.github.nayasis.kotlin.basica.core.string.toFile
import com.github.nayasis.kotlin.basica.core.string.toPath
import com.github.nayasis.kotlin.basica.core.string.tokenize
import java.security.InvalidParameterException

/**
 * command line
 */
class Command {

    val command = ArrayList<String>()
    val environment = HashMap<String,String>()
    var workingDirectory: String? = null

    constructor(cli: String? = null, workingDirectory: String? = null, environment: Map<String,String> = HashMap()) {
        this.workingDirectory = workingDirectory
        this.environment.putAll(environment)
        this.command.clear()
        appendParsing(cli)
    }

    fun isEmpty(): Boolean = command.isEmpty()

    fun append(command: String?): Command {
        if(command.isNotEmpty())
            this.command.add(command!!)
        return this
    }

    fun appendParsing(command: String?): Command {

        if(command.isNullOrEmpty()) return this

        runCatching {
            if(command.toPath().isFile()) {
                this.command.add(command)
                return this
            }
        }

        val buf = StringBuilder()

        fun StringBuilder.appendCommand(): Boolean {
            if(isNotEmpty()) {
                this@Command.command.add(toString())
                clear()
            }
            return true
        }

        var status = NONE

        for( token in command.tokenize("${SINGLE}${DOUBLE}${SPACE}", true) ) {
            if( status != NONE || token !in SPACE)
                buf.append(token)
            when(status) {
                SINGLE -> if(token == SINGLE) status = NONE
                DOUBLE -> if(token == DOUBLE) status = NONE
                NONE -> {
                    when (token) {
                        SINGLE -> status = SINGLE
                        DOUBLE -> status = DOUBLE
                        in SPACE -> {
                            buf.appendCommand()
                        }
                    }
                }
            }
        }

        buf.appendCommand()

        return this

    }

    override fun toString(): String = command.joinToString(" ")

    /**
     * run process
     *
     * @param redirectError redirect error stream to input stream
     * @return process
     */
    fun runProcess(redirectError: Boolean = true): Process {
        if(isEmpty())
            throw InvalidParameterException("command is empty.")
        return ProcessBuilder(command).apply {
            environment().putAll(environment)
            workingDirectory?.toFile().ifNotEmpty { if(it.exists()) directory(it) }
            if(redirectError)
                redirectErrorStream(true)
        }.start()
    }

    /**
     * run command
     */
    fun run(): CommandExecutor {
        return CommandExecutor(this, null,null)
    }

    /**
     * run command
     *
     * @param outputReader  output reader (include error)
     */
    fun run(outputReader: ((line: String) -> Unit)): CommandExecutor {
        return CommandExecutor(this, outputReader,null)
    }

    /**
     * run command
     *
     * @param outputReader  output reader
     * @param errorReader   error reader
     */
    fun run(outputReader: ((line: String) -> Unit), errorReader: ((line: String) -> Unit)): CommandExecutor {
        return CommandExecutor(this, outputReader,errorReader)
    }

    /**
     * run command
     *
     * @param output printed output
     * @param error  printed error
     */
    fun run(output: StringBuffer, error: StringBuffer): CommandExecutor {
        return run({output.append(it)}, {error.append(it)})
    }

    /**
     * run command
     *
     * @param output printed output (include error)
     */
    fun run(output: StringBuffer): CommandExecutor {
        return run { output.append(it) }
    }

    /**
     * capture command's execution output
     *
     * @param timeout max wait time (milli-seconds)
     * @return output
     */
    fun captureOutput(timeout: Long = -1): List<String> {
        val lines = ArrayList<String>()
        run { line -> lines.add(line) }.waitFor(timeout)
        return lines
    }

}

private const val SINGLE = "\'"
private const val DOUBLE = "\""
private const val NONE   = ""
private val       SPACE  = listOf(" ","\t","\r","\n").joinToString("")
