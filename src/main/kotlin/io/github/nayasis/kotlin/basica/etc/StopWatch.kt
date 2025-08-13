package io.github.nayasis.kotlin.basica.etc

import io.github.nayasis.kotlin.basica.core.collection.sumByDuration
import io.github.nayasis.kotlin.basica.core.number.round
import io.github.nayasis.kotlin.basica.model.NGrid
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit
import kotlin.time.DurationUnit.*

private val logger = KotlinLogging.logger {}

/**
 * Stop Watch
 */
class StopWatch: Serializable {

    private var start = System.nanoTime().nanoseconds

    private val logs = ArrayList<Log>()

    val elapsed: Duration
        get() = System.nanoTime().nanoseconds - start

    val elapsedTotal: Duration
        get() {
            var sum: Duration = ZERO
            logs.forEach { sum += it.elapsed }
            sum += elapsed
            return sum
        }

    fun tick(task: String = "-", block: (() -> Unit)? = null): Duration {
        start = System.nanoTime().nanoseconds
        block?.invoke()
        return elapsed.also {
            logs.add(Log(task, it))
            start = System.nanoTime().nanoseconds
        }
    }

    suspend fun tickSuspendable(task: String = "-", block: suspend () -> Unit): Duration {
        start = System.nanoTime().nanoseconds
        block.invoke()
        return elapsed.also {
            logs.add(Log(task, it))
            start = System.nanoTime().nanoseconds
        }
    }

    fun reset() {
        start = System.nanoTime().nanoseconds
        logs.clear()
    }

    override fun toString(): String {
        return toString(MILLISECONDS)
    }

    fun toString(unit: DurationUnit): String {
        val total = logs.sumByDuration { it.elapsed }.also { logger.debug { ">> total: $it" } }
        var remain = 100.0
        for(i in 0 until logs.size - 1) {
            logs[i].let { log ->
                log.percent = ((log.elapsed / total) * 100).round(1)
                remain -= log.percent
            }
        }
        logs.lastOrNull()?.percent = remain.round(1)

        val grid = NGrid().apply{
            listOf(
                Log::task.name to "Task",
                Log::elapsed.name to getSimpleUnit(unit),
                Log::percent.name to "%"
            ).forEach { header.setAlias(it.first, it.second) }
        }

        logs.forEach {
            grid.addRow(Log::task.name, it.task)
            grid.addRow(Log::elapsed.name, it.elapsed.toLong(unit))
            grid.addRow(Log::percent.name, it.percent)
        }

        grid.addRow(Log::task.name, "Total")
        grid.addRow(Log::elapsed.name, total.toLong(unit))

        return grid.toString(true)

    }

    private fun getSimpleUnit(unit: DurationUnit): String {
        return when(unit) {
            NANOSECONDS -> "ns"
            MICROSECONDS -> "us"
            MILLISECONDS -> "ms"
            SECONDS -> "s"
            MINUTES -> "m"
            HOURS -> "h"
            DAYS -> "day"
        }
    }

}

private data class Log(
    val task: String,
    val elapsed: Duration,
    var percent: Double = 0.0
): Serializable
