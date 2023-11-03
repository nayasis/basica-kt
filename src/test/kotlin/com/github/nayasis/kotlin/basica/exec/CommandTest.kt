package com.github.nayasis.kotlin.basica.exec

import com.github.nayasis.kotlin.basica.core.string.wrap
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

internal class CommandTest: StringSpec({

    "basic" {
        Command("a b c   d e ").let {
            "$it" shouldBe "a b c d e"
            it.command.size shouldBe 5
        }
        Command("a b\t c \n\r\t   d\t e ").let {
            "$it" shouldBe "a b c d e"
            it.command.size shouldBe 5
        }
        Command("a \"b c  d\"  e").let {
            "$it" shouldBe "a \"b c  d\" e"
            it.command.size shouldBe 3
        }
    }

    "remove quote" {
        val cli = Command("\"c:\\windows\\notepad.exe\"")
        cli.command[0] shouldBe "c:\\windows\\notepad.exe".wrap()
    }

    "parse" {
        val cli = Command("\"c:\\run.exe\" 'merong \" is parameter' \"oh ' no!\"")
        cli.command[0] shouldBe "c:\\run.exe".wrap()
        cli.command[1] shouldBe "merong \" is parameter".wrap("'")
        cli.command[2] shouldBe "oh ' no!".wrap()
    }

    "append" {
        val cli = Command("run")
        cli.append("'merong.txt'")
        cli.toString() shouldBe "run 'merong.txt'"
    }

})