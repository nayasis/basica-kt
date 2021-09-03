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

}