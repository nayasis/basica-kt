package com.github.nayasis.kotlin.basica.core.string

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.io.File

internal class FormatterTest: StringSpec({

    val binder = Formatter()

    "change Hangul Josa" {
        binder.bind("{}를 등록합니다.", "카드템플릿") shouldBe "카드템플릿을 등록합니다."
        binder.bind("{}를 등록합니다.", "카드") shouldBe "카드를 등록합니다."
        binder.bind("{}는 등록됩니다.", "카드") shouldBe "카드는 등록됩니다."
        binder.bind("{}는 등록됩니다.", "카드템플릿") shouldBe "카드템플릿은 등록됩니다."
        binder.bind("{}가 등록됩니다.", "카드") shouldBe "카드가 등록됩니다."
        binder.bind("{}가 등록됩니다.", "카드템플릿") shouldBe "카드템플릿이 등록됩니다."
    }

    "skip" {
        binder.bind("{{}}") shouldBe "{}"
        binder.bind("1{}2") shouldBe "12"
    }

    "complex" {
        val format = "{aa} {} {{merong}} is {a} and {{{b}"
        val param = mapOf<String,Any>( "aa" to "nayasis", "b" to "end")
        binder.bind(format, param, 2) shouldBe "nayasis 2 {merong} is  and {end"
    }

    "no parameter" {
        val format = "badCredentials"
        binder.bind(format) shouldBe format
    }

    "format" {
        val parameter = "{'name':'abc', 'age':2}".toMap()
        binder.bind("PRE {{age}} POST", parameter) shouldBe "PRE {age} POST"
        binder.bind("{}{}", "001", "001") shouldBe "001001"
        binder.bind("items : (count:{})\n{}", 3) shouldBe "items : (count:3)\n"
        binder.bind("{ name : {}, age : {} }", "merong", 2) shouldBe "{ name : merong, age : 2 }"
        binder.bind("{}\n{}\n{}", "5K", "Util", "desc") shouldBe "5K\nUtil\ndesc"
        binder.bind("PRE {age} POST", parameter) shouldBe "PRE 2 POST"
        binder.bind("{name} PRE {age} POST", parameter) shouldBe "abc PRE 2 POST"
        binder.bind("{name} PRE {age:%3d} POST", parameter) shouldBe "abc PRE   2 POST"
    }

    "format from Bean" {
        val bean = Pojo(1,2,"abcd")
        binder.bind("{a} is {b} or {c}", bean) shouldBe "1 is 2 or abcd"
    }

    "format from map" {
        val bean = "{'a':1, 'b':2, 'c':'abcd'}".toMap()
        binder.bind("{a} is {b} or {c}", bean) shouldBe "1 is 2 or abcd"
    }

    "file parameter" {
        val f = File("salamander")
        binder.bind("it is {:%15s}", f) shouldBe "it is      salamander"
    }

})

data class Pojo (
    val a: Int = 0,
    val b: Int = 0,
    val c: String? = null,
)