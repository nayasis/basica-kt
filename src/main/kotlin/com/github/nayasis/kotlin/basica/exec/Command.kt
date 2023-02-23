package com.github.nayasis.kotlin.basica.exec

import com.github.nayasis.kotlin.basica.core.extention.ifNotEmpty
import com.github.nayasis.kotlin.basica.core.io.exists
import com.github.nayasis.kotlin.basica.core.string.toFile
import com.github.nayasis.kotlin.basica.core.string.toPath
import com.github.nayasis.kotlin.basica.core.string.tokenize
import com.github.nayasis.kotlin.basica.etc.Platforms
import java.io.File
import java.nio.file.Path
import java.security.InvalidParameterException

/**
 * command line
 */
class Command {

    val command = ArrayList<String>()
    val environment = HashMap<String,String>()
    var workingDirectory: String? = null
    var charset: String = Platforms.os.charset

    /**
     * default constructor
     *
     * @param cli               command
     * @param workingDirectory  process's working directory
     * @param environment       environment executing process
     * @param charset           supported character-set
     * @constructor
     */
    constructor(
        cli: String? = null,
        workingDirectory: String? = null,
        environment: Map<String,String> = HashMap(),
        charset: String = Platforms.os.charset,
    ) {
        this.workingDirectory = workingDirectory
        this.environment.putAll(environment)
        this.command.clear()
        this.charset = charset
        append(cli)
    }

    fun isEmpty(): Boolean = command.isEmpty()

    fun appendRaw(command: String?): Command {
        command.ifNotEmpty { this.command.add(it) }
        return this
    }

    fun append(path: Path): Command {
        command.add("\"$path\"")
        return this
    }

    fun append(file: File): Command {
        command.add("\"$file\"")
        return this
    }

    fun append(command: String?): Command {
        if(command.isNullOrEmpty()) return this
        runCatching {
            command.toPath().let { path ->
                if(path.exists())
                    return append(path)
            }
        }

        val buf = StringBuilder()
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
                            buf.ifNotEmpty {
                                this.command.add(it.toString())
                                it.clear()
                            }
                        }
                    }
                }
            }
        }
        buf.ifNotEmpty {
            this.command.add(it.toString())
            it.clear()
        }

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
        return CommandExecutor(command = this, outputReader = null, errorReader = null)
    }

    /**
     * run command
     *
     * @param outputReader  output reader (include error)
     */
    fun run(outputReader: ((line: String) -> Unit)): CommandExecutor {
        return CommandExecutor(this, charset, outputReader,null)
    }

    /**
     * run command
     *
     * @param outputReader  output reader
     * @param errorReader   error reader
     */
    fun run(outputReader: ((line: String) -> Unit), errorReader: ((line: String) -> Unit)): CommandExecutor {
        return CommandExecutor(this, charset, outputReader,errorReader)
    }

    /**
     * run command
     *
     * @param output    printed output
     * @param error     printed error
     */
    fun run(output: StringBuffer, error: StringBuffer): CommandExecutor {
        return run( {output.append(it)}, {error.append(it)})
    }

    /**
     * run command
     *
     * @param output    printed output (include error)
     */
    fun run(output: StringBuffer): CommandExecutor {
        return run() { output.append(it) }
    }

    /**
     * capture command's execution output
     *
     * @param timeout max wait time (milli-seconds)
     * @return output
     */
    fun captureOutput(charset: String = Platforms.os.charset, timeout: Long = -1): List<String> {
        val lines = ArrayList<String>()
        run() { line -> lines.add(line) }.waitFor(timeout)
        return lines
    }

}

private const val SINGLE = "\'"
private const val DOUBLE = "\""
private const val NONE   = ""
private val       SPACE  = listOf(" ","\t","\r","\n").joinToString("")
