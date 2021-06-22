package com.github.nayasis.kotlin.basica.thread

import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.HashMap
import kotlin.concurrent.withLock
import kotlin.reflect.jvm.jvmName

private val EVENT_DELETE = "${ThreadRoot::class.jvmName}.DELETE"

class NThreadLocal {

    companion object {

        private val listeners = mutableListOf<PropertyChangeListener>()
        private val pool      = HashMap<String,HashMap<String,Any?>>()
        private val lock      = ReentrantLock()
        private val condition = lock.newCondition()

        fun addListener(listener: PropertyChangeListener) {
            listeners.add(listener)
        }

        operator fun <T> get(key: String): T? {
            return threadLocal[key] as T?
        }

        private val threadLocal: MutableMap<String,Any?>
            get() {
                lock.withLock {
                    if (!pool.containsKey(ThreadRoot.getKey())) {
                        pool[ThreadRoot.getKey()] = HashMap()
                    }
                    condition.signalAll()
                }
                return pool[ThreadRoot.getKey()]!!
            }

        operator fun set(key: String, value: Any?) {
            threadLocal[key] = value
        }

        fun remove(key: String) {
            threadLocal.remove(key)
        }

        fun containsKey(key: String?): Boolean {
            return threadLocal.containsKey(key)
        }

        fun clear() {
            lock.withLock {
                pool.remove(ThreadRoot.getKey())
                condition.signalAll()
            }
            listeners.forEach {
                it.propertyChange(PropertyChangeEvent(this,EVENT_DELETE,true,true))
            }
        }

        fun keySet(): Set<String> = threadLocal.keys

        fun values(): Collection<Any?> = threadLocal.values

        fun getPool(): Map<String,Map<String,Any?>> = pool

    }

}


class ThreadRoot: PropertyChangeListener {

    companion object {

        private val root = object : InheritableThreadLocal<String>() {
            override fun initialValue(): String = UUID.randomUUID().toString()
            override fun childValue(parentValue: String): String =
                if(parentValue.isNullOrEmpty()) initialValue() else parentValue
        }

        fun getKey(): String = root.get()

    }

    override fun propertyChange(event: PropertyChangeEvent) {
        if( event.propertyName == EVENT_DELETE && event.newValue == true )
            root.remove()
    }

    init {
        NThreadLocal.addListener(ThreadRoot())
    }

}