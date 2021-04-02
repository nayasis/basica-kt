package com.github.nayasis.kotlin.basica.expression

import com.github.nayasis.kotlin.basica.reflection.Reflector
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier
import java.time.LocalDateTime

internal class ExpressionCoreTest {

    @Test
    fun simple() {
        assertEquals(21, run<Any>(" 3 * 7") as Int)
        assertTrue(run<Any>("name == 'nayasis' && age == 40 && address == empty", param()) as Boolean)
        assertFalse(run<Any>("name == 'nayasis' && age == 40 && address != empty", param()) as Boolean)
    }

    @Test
    fun contains() {
        assertTrue(run<Any>("['nayasis','jake'].contains(name)", param()) as Boolean)
    }

    @Test
    fun like() {
        assertTrue(run<Any>("name.matches('.+?sis$')", param()) as Boolean)
    }

    @Test
    fun nvl() {
        assertEquals("default", run("nvl(address,'default')", param()))
        assertEquals("default", run("nvl(address,'default')", param()))
        assertEquals("", run("nvl(address)", param()))
    }

    @Test
    fun useKotlinMethod() {

        assertEquals("default", run("Validator.nvl(address,'default')",param()) )
        assertEquals(true, run("Strings.isDate('2021-01-01')") )
        val date: LocalDateTime? = run("LocalDateTimes.toLocalDateTime('2021-01-01')")
        assertEquals("2021-01-01T00:00", date.toString() )

        println( run("""Reflector.toMap("{'A':1,'B':'name'}",false)""") )

    }

    @Test
    fun typecast() {
        assertTrue(run<Any>("1 == '1'") as Boolean)
        assertTrue(run<Any>("1 + (2 * 3) == '7'") as Boolean)
        assertTrue(run<Any>("1 + 'a' == '1a'") as Boolean)
        assertFalse(run<Any>("1 + '2' == '3'") as Boolean)
        assertTrue(run<Any>("1 + (int)'2' == '3'") as Boolean)
    }

    private fun <T> run(expression: String, param: Any?): T? {
        val exp = ExpressionCore.compile(expression)
        return ExpressionCore.run(exp, param)
    }

    private fun <T> run(expression: String): T? {
        return run(expression, null)
    }

    private fun param(): Person {
        return Person(
            name = "nayasis",
            age = 40,
            job = "engineer"
        )
    }

}

data class Person(
    val name: String = "",
    val age: Int = 0,
    val job: String = "",
    val address: String = "",
    val child: List<Person> = ArrayList(),
)