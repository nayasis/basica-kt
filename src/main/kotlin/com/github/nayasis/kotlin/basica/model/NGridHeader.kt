package com.github.nayasis.kotlin.basica.model

import java.io.Serializable

interface NGridHeader: Serializable, Cloneable {

    fun add(key: Any)
    fun keys(): List<Any>
    fun aliases(): List<String>
    fun size(): Int
    fun containsKey(key: Any): Boolean
    fun setAlias(key: Any, alias: String)
    fun getAlias(key: Any): String
    fun isEmpty(): Boolean

}