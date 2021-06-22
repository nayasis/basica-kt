package com.github.nayasis.kotlin.basica.thread

import mu.KotlinLogging
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.lang.Thread.sleep

private val log = KotlinLogging.logger {}

internal class NThreadLocalTest {

    @Test
    @Throws(InterruptedException::class)
    fun test() {

        val result = HashMap<String, Int?>()

        val threadA = makeTestThread("A", 5, result)
        val threadB = makeTestThread("A", 10, result)

        threadA.start()
        threadB.start()

        sleep(5000)
        assertEquals(10, result["A-10"] as Int)
        assertEquals(5, result["A-5"] as Int)

        log.debug(result.toString())

    }

    private fun makeTestThread(key: String, count: Int, result: MutableMap<String, Int?>): Thread {
        return Thread {
            NThreadLocal[key] = 0
            val child = Thread {
                for (i in 0 until count) {
                    var value : Int = NThreadLocal[key]!!
                    NThreadLocal[key] = ++value
                    log.debug{"key : $key, count : $count, value : $value"}
                    try {
                        sleep(10)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
                result["$key-$count"] = NThreadLocal[key]
            }
            child.start()
        }
    }

}