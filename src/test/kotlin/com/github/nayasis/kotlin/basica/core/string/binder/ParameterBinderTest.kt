package com.github.nayasis.kotlin.basica.core.string.binder

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import javax.script.ScriptEngineManager

internal class ParameterBinderTest {

    @Test
    fun template() {

        val engine = ScriptEngineManager().getEngineByExtension("kts")!!
        engine.eval("val x = 3")
        val res = engine.eval("x + 2")
        assertEquals(5, res)

    }

}