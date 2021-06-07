package com.github.nayasis.kotlin.basica.core.string.format

import com.github.nayasis.kotlin.basica.core.string.toMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

internal class FormatterTest {

    private val binder = Formatter()

    @Test
    fun changeHangulJosa() {

        assertEquals("카드템플릿을 등록합니다.", binder.bindSimple("{}를 등록합니다.", "카드템플릿"))
        assertEquals("카드를 등록합니다.", binder.bindSimple("{}를 등록합니다.", "카드"))
        assertEquals("카드는 등록됩니다.", binder.bindSimple("{}는 등록됩니다.", "카드"))
        assertEquals("카드템플릿은 등록됩니다.", binder.bindSimple("{}는 등록됩니다.", "카드템플릿"))
        assertEquals("카드가 등록됩니다.", binder.bindSimple("{}가 등록됩니다.", "카드"))
        assertEquals("카드템플릿이 등록됩니다.", binder.bindSimple("{}가 등록됩니다.", "카드템플릿"))

    }

    @Test
    fun skip() {

        assertEquals( "{}", binder.bindSimple("{{}}") )
        assertEquals( "12", binder.bindSimple("1{}2") )

    }

    @Test
    fun complex() {

        val format = "{aa} {} {{merong}} is {a} and {{{b}"

        val param = mapOf<String,Any>( "aa" to "nayasis", "b" to "end")

        assertEquals("nayasis 2 {merong} is  and {end", binder.bindSimple(format, param, 2))

    }

    @Test
    fun `no parameter`() {

        val format = "badCredentials"

        assertEquals(format, binder.bindSimple(format) )

    }

    @Test
    fun `format`() {

        val parameter = "{'name':'abc', 'age':2}".toMap()

        assertEquals("PRE {age} POST", binder.bindSimple("PRE {{age}} POST", parameter))

        assertEquals("001001", binder.bindSimple("{}{}", "001", "001"))

        assertEquals("items : (count:3)\n", binder.bindSimple("items : (count:{})\n{}", 3))

        assertEquals("{ name : merong, age : 2 }", binder.bindSimple("{ name : {}, age : {} }", "merong", 2))
        assertEquals("5K\nUtil\ndesc", binder.bindSimple("{}\n{}\n{}", "5K", "Util", "desc"))

        assertEquals("PRE 2 POST", binder.bindSimple("PRE {age} POST", parameter))
        assertEquals("abc PRE 2 POST", binder.bindSimple("{name} PRE {age} POST", parameter))
        assertEquals("abc PRE   2 POST", binder.bindSimple("{name} PRE {age:%3d} POST", parameter))

    }

    @Test
    fun `format from Bean`() {
        val bean = BeanA(1,2,"abcd")
        assertEquals("1 is 2 or abcd", binder.bindSimple("{a} is {b} or {c}", bean))
    }

    @Test
    fun `format from map`() {
        val bean = "{'a':1, 'b':2, 'c':'abcd'}".toMap()
        assertEquals("1 is 2 or abcd", binder.bindSimple("{a} is {b} or {c}", bean))
    }

    @Test
    fun `file parameter`() {
        val f = File("salamander")
        assertEquals("it is      salamander", binder.bindSimple("it is {:%15s}", f))
    }

}

data class BeanA (
    val a: Int = 0,
    val b: Int = 0,
    val c: String? = null,
)