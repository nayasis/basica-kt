package com.github.nayasis.kotlin.basica.thread

import mu.KotlinLogging
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.lang.Thread.sleep
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {}

internal class NThreadLocalTest {

    private val threadPool = Executors.newWorkStealingPool(10)

    @Test
    fun `run in separated threads`() {

        val result = HashMap<String, Int?>()

        val threadA = makeTestThread("A", 5, result)
        val threadB = makeTestThread("A", 10, result)

        threadPool.execute(threadA)
        threadPool.execute(threadB)

        sleep(1000)

        assertEquals(10, result["A-10"] as Int)
        assertEquals(5, result["A-5"] as Int)
        assertNull(NThreadLocal["A"])

        log.debug(result.toString())

        NThreadLocal.clear()

    }

    @Test
    fun `from main thread`() {

        NThreadLocal["A"] = 0

        val result = HashMap<String, Int?>()

        val threadA = makeTestThread("A", 5, result)
        val threadB = makeTestThread("A", 10, result)

        threadPool.execute(threadA)
        threadPool.execute(threadB)

        sleep(1000)

        log.debug { NThreadLocal["A"] }
        assertNotNull(NThreadLocal["A"])

        NThreadLocal.clear()
        log.debug(result.toString())
        log.debug { NThreadLocal["A"] }
        assertNull(NThreadLocal["A"])

    }

    private fun makeTestThread(key: String, count: Int, result: MutableMap<String, Int?>): Thread {
        return Thread {
            NThreadLocal[key] = 0
            threadPool.execute( Thread {
                for (i in 0 until count) {
                    var value : Int = NThreadLocal[key] ?: 0
                    NThreadLocal[key] = ++value
                    log.debug{"key : $key, count : $count, value : $value"}
                    try {
                        sleep(10)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
                result["$key-$count"] = NThreadLocal[key]
            })
        }
    }

}