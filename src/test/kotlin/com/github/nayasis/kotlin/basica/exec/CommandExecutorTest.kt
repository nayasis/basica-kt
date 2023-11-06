package com.github.nayasis.kotlin.basica.exec

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.config.TestCaseConfig
import io.kotest.matchers.shouldBe
import mu.KotlinLogging
import org.junit.jupiter.api.Disabled
import java.lang.Thread.sleep
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger {}

@Disabled("exclude by platform dependency")
internal class CommandExecutorTest: StringSpec({

//    "notepad".config(enabled = false) {
    "notepad" {
        Command("cmd /c start notepad").run()
    }

    "excel async" {
        Command("cmd.exe /c start excel.exe").run().waitFor()
        println(">> Done!!")
    }

    "excel sync" {
        Command("cmd.exe /c start /wait excel.exe").run().waitFor()
        println(">> Done!!")
    }

    "run excel" {
        Command("c:\\Program Files (x86)\\Microsoft Office\\Office15\\EXCEL.EXE").run().waitFor()
        println(">> Done!!")
    }

    "prevent double execution" {
        shouldThrow<IllegalAccessException> {
            val path = "c:\\Program Files (x86)\\Microsoft Office\\Office15\\EXCEL.EXE"
            Command(path).run()
            Command(path).run()
        }
    }

    "read dir" {
        val out = StringBuffer()
        Command("cmd /c c: && cd \"c:\\Windows\" && dir").run(out).waitFor()
        println(out)
        out.isNotEmpty() shouldBe true
    }

    "communication" {

        val out = StringBuffer()
        val executor = Command("cmd").run(out)

        executor.sendCommand("c:")
        executor.sendCommand("cd c:\\Users")
        executor.sendCommand("dir")
        executor.sendCommand("exit")
        executor.waitFor()

        println(out)
        out.isNotEmpty() shouldBe true

    }

    "read dir by reader" {

        val out = StringBuffer()
        Command("cmd /c c: && cd \"c:\\Windows\" && dir").run { out.append(it) }.waitFor()

        println(out)
        out.isNotEmpty() shouldBe true

    }

    "print output on MAME" {

        logger.debug{">> start"}

        var capture = false
        var i = 0

        Command("c:\\download\\_temp\\mame\\mame.exe -listxml").run{ it.trimStart().let { line ->
            if( !capture && line.startsWith("<machine ") ) {
                capture = true
            }
            if( capture ) {
                if( line.endsWith("</machine>") ) {
                    capture = false
                    println("${i++}")
                }
            }
        }}.waitFor()

        logger.debug{">> end"}

    }

    "open E-mail" {
        val addr = "mailto:nayasis@gmail.com?subject=merong"
        Command("explorer \"${addr}\"").run().waitFor()
    }


    "build CHD" {

        val cd = "d:/download/test/chd"
        val command = "${cd}/chdman.exe createcd -f -i ${cd}/disc.cue -o ${cd}/disc.chd"

        thread(true) {
            Command(command).run({txt -> print(txt)},{txt -> print(txt)}).waitFor()
//            CommandExecutor().runOnSystemOut(command).waitFor()
        }

        sleep(10_000)

    }

    "run EXE on SMB" {
        val cmd = Command("\\\\NAS2\\emul\\_tool\\3DS\\decrypter\\3ds_decrypt_v4.exe", "\\\\NAS2\\emul\\_tool\\3DS\\decrypter")
        cmd.run() { println(it) }
    }

}) {
    init {
        defaultTestConfig = TestCaseConfig(enabled = false)
    }
}