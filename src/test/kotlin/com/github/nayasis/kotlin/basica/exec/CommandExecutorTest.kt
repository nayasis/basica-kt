package com.github.nayasis.kotlin.basica.exec

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.lang.Thread.sleep
import kotlin.concurrent.thread

@Disabled("exclude by platform dependency")
internal class CommandExecutorTest {

    @Test
    fun notepad() {
        CommandExecutor().run("cmd /c start notepad")
    }

    @Test
    fun excelAsync() {
        CommandExecutor().run("cmd.exe /c start excel.exe").waitFor()
        println(">> Done!!")
    }

    @Test
    fun excelSync() {
        CommandExecutor().run("cmd.exe /c start /wait excel.exe").waitFor()
        println(">> Done!!")
    }

    @Test
    fun runExcel() {
        CommandExecutor().run("c:\\Program Files (x86)\\Microsoft Office\\Office15\\EXCEL.EXE").waitFor()
        println(">> Done!!")
    }

    @Test
    fun preventDoubleExecution() {
        assertThrows(IllegalAccessException::class.java) {
            val executor = CommandExecutor()
            val path = "c:\\Program Files (x86)\\Microsoft Office\\Office15\\EXCEL.EXE"
            executor.run(path)
            executor.run(path)
        }
    }

    @Test
    fun readDir() {
        val out = StringBuffer()
        CommandExecutor().run("cmd /c c: && cd \"c:\\Windows\" && dir",out).waitFor()
        println(out)
        assertTrue(out.isNotEmpty())
    }

    @Test
    fun redirectOutputToSystemOut() {
        val command = Command().apply {
            this.command.addAll(listOf("cmd", "/c", "c:", "&&", "cd", "c:\\Windows", "&&", "dir"))
        }
        CommandExecutor().runOnSystemOut(command).waitFor()
    }

    @Test
    fun communication() {

        val out = StringBuffer()
        val executor = CommandExecutor().run("cmd",out)

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

        CommandExecutor().run("cmd /c c: && cd \"c:\\Windows\" && dir") { out.append(it) }.waitFor()

        println(out)
        assertTrue(out.isNotEmpty())

    }

    @Test
    fun openEmail() {
        val addr = "mailto:nayasis@gmail.com?subject=merong"
        CommandExecutor().run("explorer \"${addr}\"").waitFor()
    }


    @Test
    fun buildChd() {

        val cd = "d:/download/test/chd"
        val command = "${cd}/chdman.exe createcd -f -i ${cd}/disc.cue -o ${cd}/disc.chd"

        thread(true) {
            CommandExecutor().run(command,{txt -> print(txt)},{txt -> print(txt)}).waitFor()
//            CommandExecutor().runOnSystemOut(command).waitFor()
        }

        sleep(10_000)

    }

}