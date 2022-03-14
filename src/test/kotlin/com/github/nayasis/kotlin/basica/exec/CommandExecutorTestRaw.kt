package com.github.nayasis.kotlin.basica.exec

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.lang.Thread.sleep
import kotlin.concurrent.thread

fun main(arg: Array<String>) {
    CommandExecutorTestRaw().buildChd()
}

@Disabled("exclude by platform dependency")
class CommandExecutorTestRaw {

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

}