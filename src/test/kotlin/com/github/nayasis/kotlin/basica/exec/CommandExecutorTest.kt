package com.github.nayasis.kotlin.basica.exec

import mu.KotlinLogging
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.lang.Thread.sleep
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger {}

@Disabled("exclude by platform dependency")
internal class CommandExecutorTest {

    @Test
    fun notepad() {
        Command("cmd /c start notepad").run()
    }

    @Test
    fun excelAsync() {
        Command("cmd.exe /c start excel.exe").run().waitFor()
        println(">> Done!!")
    }

    @Test
    fun excelSync() {
        Command("cmd.exe /c start /wait excel.exe").run().waitFor()
        println(">> Done!!")
    }

    @Test
    fun runExcel() {
        Command("c:\\Program Files (x86)\\Microsoft Office\\Office15\\EXCEL.EXE").run().waitFor()
        println(">> Done!!")
    }

    @Test
    fun preventDoubleExecution() {
        assertThrows(IllegalAccessException::class.java) {
            val path = "c:\\Program Files (x86)\\Microsoft Office\\Office15\\EXCEL.EXE"
            Command(path).run()
            Command(path).run()
        }
    }

    @Test
    fun readDir() {
        val out = StringBuffer()
        Command("cmd /c c: && cd \"c:\\Windows\" && dir").run(out).waitFor()
        println(out)
        assertTrue(out.isNotEmpty())
    }

    @Test
    fun communication() {

        val out = StringBuffer()
        val executor = Command("cmd").run(out)

        executor.sendCommand("c:")
        executor.sendCommand("cd c:\\Users")
        executor.sendCommand("dir")
        executor.sendCommand("exit")
        executor.waitFor()

        println(out)
        assertTrue(out.isNotEmpty())

    }

    @Test
    fun readDirByReader() {

        val out = StringBuffer()

        Command("cmd /c c: && cd \"c:\\Windows\" && dir").run({ out.append(it) }).waitFor()

        println(out)
        assertTrue(out.isNotEmpty())

    }

    @Test
    fun printOutputOnMame() {

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

    @Test
    fun openEmail() {
        val addr = "mailto:nayasis@gmail.com?subject=merong"
        Command("explorer \"${addr}\"").run().waitFor()
    }


    @Test
    fun buildChd() {

        val cd = "d:/download/test/chd"
        val command = "${cd}/chdman.exe createcd -f -i ${cd}/disc.cue -o ${cd}/disc.chd"

        thread(true) {
            Command(command).run({txt -> print(txt)},{txt -> print(txt)}).waitFor()
//            CommandExecutor().runOnSystemOut(command).waitFor()
        }

        sleep(10_000)

    }

    @Test
    fun runNetworkExe() {
        val cmd = Command("\\\\NAS2\\emul\\_tool\\3DS\\decrypter\\3ds_decrypt_v4.exe", "\\\\NAS2\\emul\\_tool\\3DS\\decrypter")
        cmd.run() { println(it) }
    }

}