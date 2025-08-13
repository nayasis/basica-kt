package io.github.nayasis.kotlin.basica.expression

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

internal class MvelHandlerTest: StringSpec({

    "simple" {
        run<Int>("3 * 7") shouldBe 21
        run<Boolean>("name == 'nayasis' && age == 40 && address == empty", Person()) shouldBe true
        run<Boolean>("name == 'nayasis' && age == 40 && address != empty", Person()) shouldBe false
    }

    "contains" {
        run<Boolean>("['nayasis','jake'].contains(name)", Person()) shouldBe true
    }

    "like" {
        run<Boolean>("name.matches('.+?sis$')", Person()) shouldBe true
    }

    "nvl" {
        run<String>("nvl(address,'default')", Person()) shouldBe "default"
        run<String>("nvl(address)", Person()) shouldBe ""
    }

    "use KotlinMethod" {

        run<String>("Validator.nvl(address,'default')",Person()) shouldBe "default"
        run<Boolean>("Strings.isDate('2021-01-01')") shouldBe true

        val date: LocalDateTime? = run("LocalDateTimes.toLocalDateTime('2021-01-01')")
        date.toString() shouldBe "2021-01-01T00:00"

        println( run<String>("""Reflector.toMap("{'A':1,'B':'name'}",false)""") )

        run<Map<String,Any>>("""
            Reflector.toMap("{'A':1,'B':'name'}",false)
        """.trimIndent()).toString() shouldBe "{A=1, B=name}"
    }

    "typecast" {
        run<Boolean>("1 == '1'") shouldBe true
        run<Boolean>("1 + (2 * 3) == '7'") shouldBe true
        run<Boolean>("1 + 'a' == '1a'") shouldBe true
        run<Boolean>("1 + '2' == '3'") shouldBe false
        run<Boolean>("1 + (int)'2' == '3'") shouldBe true
    }

})

private fun <T> run(expression: String, param: Any? = null): T {
    val exp = MvelHandler.compile(expression)
    return MvelHandler.run(exp, param)
}

data class Person(
    val name: String = "nayasis",
    val age: Int = 40,
    val job: String = "engineer",
    val address: String = "",
    val child: List<Person> = ArrayList(),
)