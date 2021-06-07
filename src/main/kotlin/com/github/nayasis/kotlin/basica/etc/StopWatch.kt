package com.github.nayasis.kotlin.basica.etc

import com.github.nayasis.kotlin.basica.core.collection.sumByLong
import com.github.nayasis.kotlin.basica.core.math.round
import com.github.nayasis.kotlin.basica.model.NGrid
import java.io.Serializable

/**
 * Stop Watch
 */
class StopWatch: Serializable {

    private var start = 0L
    private val logs  = ArrayList<Log>()

    var task: String = ""
        get() = field

    var enable: Boolean = true

    constructor(task: String = "") {
        tick(task)
    }

    fun elapsedNanos(): Long = if( start == 0L ) 0L else System.nanoTime() - start
    fun elapsedMilis(): Long = elapsedNanos() / 1_000_000
    fun elapsedSeconds(): Double = round(elapsedNanos() / 1_000_000_000.0, 3 )

    fun stop(): StopWatch {
        addLog()
        task = ""
        start = 0
        return this
    }

    private fun addLog() {
        if (enable && start != 0L)
            logs.add(Log(task, elapsedMilis()))
    }

    fun tick(task: String = ""): StopWatch {
        addLog()
        this.task = task
        this.start = System.nanoTime()
        return this
    }

    fun reset() {
        start = System.nanoTime()
        logs.clear()
    }

    override fun toString(): String {
        if( ! enable ) return ""

        val total = logs.sumByLong { it.milis }

        var remainPercent = 100.0

        val last = logs.size - 1
        for( i in 0..last ) {
            val log = logs[i]
            if (i == last) {
                log.percent = remainPercent
            } else {
                log.percent = round(log.milis.toDouble() / total * 100,1)
                remainPercent -= log.percent
            }
        }

        val grid = NGrid()

        logs.forEach {
            grid.addData( "Task", it.task )
            grid.addData( "ms", it.milis )
            grid.addData( "%", "%.1f".format(it.percent) )
        }

        grid.addData("Task", "Total")
        grid.addData("ms", "%6d".format(total))
        grid.addData("%", "" )

        return grid.toString(true)

    }

}

private data class Log(
    var task: String,
    var milis: Long = 0,
    var percent: Double = 0.0
): Serializable
