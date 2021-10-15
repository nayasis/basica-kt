package com.github.nayasis.kotlin.basica.exec

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class CommandTest {

    @Test
    fun basic() {

        Command("a b c   d e ").let {
            assertEquals("a b c d e", "$it")
            assertEquals(5, it.command.size)
        }

        Command("a b\t c \n\r\t   d\t e ").let {
            assertEquals("a b c d e", "$it")
            assertEquals(5, it.command.size)
        }

        Command("a \"b c  d\"  e").let {
            assertEquals("a \"b c  d\" e", "$it")
            assertEquals(3, it.command.size)
        }

    }

    @Test
    fun removeQuote() {
        val cli = Command("\"c:\\windows\\notepad.exe\"")
        assertEquals("\"c:\\windows\\notepad.exe\"", cli.command[0] )
    }

    @Test
    fun parse() {
        val cli = Command("\"c:\\run.exe\" 'merong \" is parameter' \"oh ' no!\"")
        assertEquals("\"c:\\run.exe\"",cli.command[0])
        assertEquals("'merong \" is parameter'",cli.command[1])
        assertEquals("\"oh ' no!\"",cli.command[2])
    }

    @Test
    fun append() {

        val cli = Command("run")
        cli.append("'merong.txt'")

        assertEquals("run 'merong.txt'", cli.toString() )

    }

}