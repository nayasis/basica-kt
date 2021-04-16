package com.github.nayasis.kotlin.basica.model

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.lang.RuntimeException
import java.util.*
import kotlin.collections.HashMap

internal class NGridTest {

    @Test
    fun addRow() {

        val body = TreeMap<Int,HashMap<Any?,Any?>>()

        try {
            println( "last key : ${body.lastKey()}")
            throw RuntimeException("must be raised")
        } catch (e: NoSuchElementException) {}

        body[-1] = HashMap()
        println( "last key : ${body.lastKey()}")

        body[1] = HashMap()
        println( "last key : ${body.lastKey()}")

        body[7] = HashMap()
        println( "last key : ${body.lastKey()}")

    }

    @Test
    fun array() {
        val arr = charArrayOf('a','b','c')
        println(arr)
        println( arr[6] )
    }

}