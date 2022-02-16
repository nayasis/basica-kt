package com.github.nayasis.kotlin.basica.core.extention

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ExtensionsKtTest {

    @Test
    fun ifNotNull() {

        var a: String? = "A"

        assertEquals(1234, a.ifNotNull { 1234 })
        assertEquals(1234, a.ifNotEmpty { 1234 })

        a = null

        assertEquals(null, a.ifNotNull { "IS NOT EMPTY" })
        assertEquals(null, a.ifNotEmpty { "IS NOT EMPTY" })

    }

    @Test
    fun ifNotEmptyNull() {

        var a = mutableListOf(1,2,3,4)

        assertEquals(10, a.ifNotEmpty { it.sum() })

        a.clear()

        assertEquals(null, a.ifNotEmpty { it.sum() })

    }

}