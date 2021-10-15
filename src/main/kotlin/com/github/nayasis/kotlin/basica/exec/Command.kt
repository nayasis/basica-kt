package com.github.nayasis.kotlin.basica.exec

import com.github.nayasis.kotlin.basica.core.path.isFile
import com.github.nayasis.kotlin.basica.core.string.toPath
import com.github.nayasis.kotlin.basica.core.string.tokenize

class Command {

    val command = ArrayList<String>()
    val environment = HashMap<String,String>()
    var workingDirectory: String? = null
    var outputReader: ((String) -> Unit)? = null
    var errorReader: ((String) -> Unit)? = null

    constructor(cli: String? = null, workingDirectory: String? = null, environment: Map<String,String> = HashMap()) {
        this.workingDirectory = workingDirectory
        this.environment.putAll(environment)
        parse(cli)
    }

    fun isEmpty(): Boolean = command.isEmpty()

    @Suppress("MemberVisibilityCanBePrivate")
    fun parse(cli: String?) {

        if(cli.isNullOrEmpty()) return

        command.clear()

        runCatching {
            if(cli.toPath().isFile()) {
                command.add(cli)
                return
            }
        }

        val buf = StringBuilder()

        fun StringBuilder.appendCommand(): Boolean {
            if(isNotEmpty()) {
                command.add(toString())
                clear()
            }
            return true
        }

        var status = NONE

        for( token in cli.tokenize("${SINGLE}${DOUBLE}${SPACE.joinToString("")}", true) ) {
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

    }

    override fun toString(): String = command.joinToString(" ")

}

private const val SINGLE = "\'"
private const val DOUBLE = "\""
private const val NONE   = ""
private val       SPACE  = listOf(" ","\t","\r","\n")
