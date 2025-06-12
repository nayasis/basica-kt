package com.github.nayasis.kotlin.basica.model.dataframe

import com.github.nayasis.kotlin.basica.core.extension.isEmpty
import com.github.nayasis.kotlin.basica.reflection.Reflector
import java.util.*
import java.util.stream.DoubleStream
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.reflect.KClass
import kotlin.streams.toList

open class Column: Cloneable {

    internal val values = HashMap<Int,Any?>()

    private val indices = TreeSet<Int>()

    internal val lastIndex: Int?
        get() = indices.lastOrNull()

    val last: Any? = lastIndex?.let { values[it] }

    val size: Int
        get() = values.size

    fun has(index: Int): Boolean {
        return values.containsKey(index)
    }

    operator fun get(index: Int): Any? {
        return values[index]
    }

    operator fun set(index: Int, value: Any?) {
        values[index] = value
        indices.add(index)
    }

    fun clear() {
        values.clear()
        indices.clear()
    }

    fun remove(index: Int) {
        values.remove(index)
        indices.remove(index)
    }

    private fun toNumerics(): DoubleStream {
        return values.values.parallelStream().filter { it is Number }.mapToDouble { (it as Number).toDouble() }
    }

    fun count(): Long {
        return values.values.parallelStream().filter { it is Number }.count()
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
        if (values.isEmpty() || percentile < 0.0 || percentile > 100.0) return Double.NaN

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
        if (values.isEmpty()) return emptyMap()

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

    fun <T: Any> toList(typeClass: KClass<T>, ignoreError: Boolean = true): List<T?> {
        val list = ArrayList<T?>()
        if(values.isNotEmpty()) {
            for(i in 0 .. lastIndex!!) {
                try {
                    list.add( values[i]?.let { Reflector.toObject(it, typeClass) } )
                } catch (e: Exception) {
                    if( !ignoreError ) {
                        list.add(null)
                    } else {
                        throw e
                    }
                }
            }
        }
        return list
    }

    inline fun <reified T: Any> toList(ignoreError: Boolean = true): List<T?> {
        return toList(T::class, ignoreError)
    }

    public override fun clone(): Column {
        return Column().also { column ->
            this.values.forEach { (index, value) ->
                column[index] = value
            }
            this.indices.forEach { index ->
                column.indices.add(index)
            }
        }
    }

}