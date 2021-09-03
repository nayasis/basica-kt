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

    constructor(cli: String? = null) {
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

        val curr = StringBuilder()
        var inQuote = NONE

        for( token in cli.tokenize("${SINGLE}${DOUBLE}${SPACE.joinToString("")}", true) ) {
            if( inQuote != NONE || token !in SPACE)
                curr.append(token)
            when(inQuote) {
                SINGLE -> if(token == "\'") inQuote = NONE
                DOUBLE -> if(token == "\"") inQuote = NONE
                NONE -> {
                    when (token) {
                        SINGLE -> inQuote = SINGLE
                        DOUBLE -> inQuote = DOUBLE
                        in SPACE -> {
                            if(curr.isNotEmpty()) {
                                command.add(curr.toString())
                                curr.clear()
                            }
                        }
                    }
                }
            }
        }

        if(curr.isNotEmpty())
            command.add(curr.toString())

    }

    override fun toString(): String = command.joinToString(" ")

}

private const val SINGLE = "\'"
private const val DOUBLE = "\""
private const val NONE   = ""
private val       SPACE  = listOf(" ","\t","\r","\n")
