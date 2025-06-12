package com.github.nayasis.kotlin.basica.model.dataframe

import com.github.nayasis.kotlin.basica.core.extension.isEmpty
import java.util.TreeSet
import java.util.stream.DoubleStream
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.streams.toList

open class Column {

    private val kv = HashMap<Int,Any?>()

    private val indices = TreeSet<Int>()

    internal val firstIndex: Int?
        get() = indices.firstOrNull()

    internal val lastIndex: Int?
        get() = indices.lastOrNull()

    val first: Any? = firstIndex?.let { kv[it] }

    val last: Any? = lastIndex?.let { kv[it] }

    val size: Int
        get() = kv.size

    fun has(index: Int): Boolean {
        return kv.containsKey(index)
    }

    operator fun get(index: Int): Any? {
        return kv[index]
    }

    operator fun set(index: Int, value: Any?) {
        kv[index] = value
        indices.add(index)
    }

    internal fun remove(index: Int) {
        kv.remove(index)
        indices.remove(index)
    }

    private fun toNumerics(): DoubleStream {
        return kv.values.parallelStream().filter { it is Number }.mapToDouble { (it as Number).toDouble() }
    }

    fun count(): Long {
        return kv.values.parallelStream().filter { it is Number }.count()
    }

    fun min(): Double {
        return toNumerics().min().orElse(Double.NaN)
    }

    fun max(): Double {
        return toNumerics().max().orElse(Double.NaN)
    }

    fun sum(): Double {
        return toNumerics().parallel().reduce { a, b -> a + b }.orElse(Double.NaN)
    }

    fun mean(): Double {
        return toNumerics().parallel().average().orElse(Double.NaN)
    }

    fun median(): Double {
        val sorted = toNumerics().parallel().sorted().toList()
        return when {
            sorted.isEmpty() -> Double.NaN
            sorted.size % 2 == 0 -> (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
            else -> sorted[sorted.size / 2]
        }
    }

    fun std(): Double {
        val stats = toNumerics().parallel().summaryStatistics()
                .also { if(it.isEmpty()) return Double.NaN }

        val avg         = stats.average
        val squaredDiff = toNumerics().parallel().map { (it - avg).pow(2) }
        val variance    = squaredDiff.parallel().sum() / stats.count

        return sqrt(variance)
    }

    fun percentile(percentile: Double): Double {
        if (kv.isEmpty() || percentile < 0.0 || percentile > 100.0) return Double.NaN

        val sorted = toNumerics().parallel().sorted().toList()
                .also { if(it.isEmpty()) return Double.NaN }

        val pos = percentile * (sorted.size - 1) / 100.0

        val lower = floor(pos).toInt()
        val upper = ceil(pos).toInt()

        return when {
            lower == upper -> sorted[lower]
            else -> sorted[lower] + (pos - lower) * (sorted[upper] - sorted[lower])
        }
    }

    fun percentile(vararg percentiles: Double): Map<Double, Double> {
        if (kv.isEmpty()) return emptyMap()

        val sorted = toNumerics().parallel().sorted().toList()
            .also { if(it.isEmpty()) return emptyMap() }

        return percentiles.filter { it in 0.0..100.0 }.sorted().associateWith { percentile ->
            val pos = percentile * (sorted.size - 1) / 100.0

            val lower = floor(pos).toInt()
            val upper = ceil(pos).toInt()

            when {
                lower == upper -> sorted[lower]
                else -> sorted[lower] + (pos - lower) * (sorted[upper] - sorted[lower])
            }
        }
    }

}