package com.github.nayasis.kotlin.basica.expression

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class MvelHandlerTest {

    @Test
    fun simple() {
        assertEquals(21, run<Any>(" 3 * 7") as Int)
        assertTrue(run<Boolean>("name == 'nayasis' && age == 40 && address == empty", Person()) == true)
        assertTrue(run<Boolean>("name == 'nayasis' && age == 40 && address != empty", Person()) == false)
    }

    @Test
    fun contains() {
        assertTrue(run<Boolean>("['nayasis','jake'].contains(name)", Person()) == true)
    }

    @Test
    fun like() {
        assertTrue(run<Boolean>("name.matches('.+?sis$')", Person()) == true)
    }

    @Test
    fun nvl() {
        assertEquals("default", run("nvl(address,'default')", Person()))
        assertEquals("default", run("nvl(address,'default')", Person()))
        assertEquals("", run("nvl(address)", Person()))
    }

    @Test
    fun useKotlinMethod() {

        assertEquals("default", run("Validator.nvl(address,'default')",Person()) )
        assertEquals(true, run("Strings.isDate('2021-01-01')") )
        val date: LocalDateTime? = run("LocalDateTimes.toLocalDateTime('2021-01-01')")
        assertEquals("2021-01-01T00:00", date.toString() )

        println( run("""Reflector.toMap("{'A':1,'B':'name'}",false)""") )

    }

    @Test
    fun typecast() {
        assertTrue(run<Boolean>("1 == '1'") == true)
        assertTrue(run<Boolean>("1 + (2 * 3) == '7'") == true)
        assertTrue(run<Boolean>("1 + 'a' == '1a'") == true)
        assertTrue(run<Boolean>("1 + '2' == '3'") == false)
        assertTrue(run<Boolean>("1 + (int)'2' == '3'") == true)
    }

    private fun <T> run(expression: String, param: Any? = null): T? {
        val exp = MvelHandler.compile(expression)
        return MvelHandler.run(exp, param)!!
    }

}

data class Person(
    val name: String = "nayasis",
    val age: Int = 40,
    val job: String = "engineer",
    val address: String = "",
    val child: List<Person> = ArrayList(),
)