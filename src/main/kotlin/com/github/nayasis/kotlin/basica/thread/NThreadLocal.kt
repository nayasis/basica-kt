@file:JvmName("NThreadLocal")

package com.github.nayasis.kotlin.basica.thread

import java.util.*

class NThreadLocal { companion object {

    private val pool = Hashtable<String,Hashtable<String,Any?>>()

    private val threadLocal: MutableMap<String,Any?>
        get() {
            synchronized(this) {
                ThreadRoot.key.let {
                    if (!pool.containsKey(it))
                        pool[it] = Hashtable()
                    return pool[it]!!
                }
            }
        }

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: String): T? = threadLocal[key] as T?

    operator fun set(key: String, value: Any?) {
        threadLocal[key] = value
    }

    fun remove(key: String) = threadLocal.remove(key)

    fun containsKey(key: String): Boolean = threadLocal.containsKey(key)

    fun keySet(): Set<String> = threadLocal.keys

    fun values(): Collection<Any?> = threadLocal.values

    fun clear() {
        pool.remove(ThreadRoot.key)
        ThreadRoot.remove()
    }

    fun pool(): MutableMap<String,Any?> {
        return threadLocal
    }

}}


class ThreadRoot { companion object {

    private val root = object : InheritableThreadLocal<String>() {
        override fun initialValue(): String = UUID.randomUUID().toString()
        override fun childValue(parentValue: String): String =
            if(parentValue.isNullOrEmpty()) initialValue() else parentValue
    }

    val key: String
        get() = root.get()

    fun remove() = root.remove()

}}