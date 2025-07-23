package com.github.nayasis.kotlin.basica.core.string

import com.github.nayasis.kotlin.basica.core.collection.toUrlParam
import com.github.nayasis.kotlin.basica.core.extension.isNotEmpty
import com.github.nayasis.kotlin.basica.core.localdate.format
import com.github.nayasis.kotlin.basica.core.localdate.toLocalTime
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.Serializable
import java.net.URLEncoder
import java.time.LocalTime

private val log = KotlinLogging.logger {}

internal class StringsTest: StringSpec({

    "glob test" {
        ".".glob("*.kt")
            .also { println(it.joinToString("\n")) }
            .size shouldBeGreaterThan 1
        println("----------")
        "".glob("*.kt")
            .also { println(it.joinToString("\n")) }
            .size shouldBeGreaterThan 1
    }

    "is date" {
        "2021-01-01".isDate() shouldBe true
        "2021-01-33".isDate() shouldBe false
    }

    "toNumber" {
        "1".toNumber<Int>() shouldBe 1
        "1.2".toNumber<Double>() shouldBe 1.2
        "1.2".toNumber(Double::class) shouldBe 1.2
        "nayasis".toNumber<Double>() shouldBe 0.0
        "nayasis".toNumber<Long>() shouldBe 0L
    }

    "bind parameter" {
        val param = """
            { "name":"nayasis", "age":10 }
        """.toMap()
        "{name} is {age} years old.".bind(param) shouldBe "nayasis is 10 years old."
    }

    "capture regex" {
        val captured = "jdbc:sqlite:./target/test-classes/localDb/#{Merong_babo}#{Nayasis_babo}SimpleLauncherHelloWorld.db"
            .capture("#\\{(.+?)_(.+?)}".toRegex())
        captured.size shouldBe 4
        captured shouldBe listOf("Merong", "babo", "Nayasis", "babo")
        val refids = "< Ref id=\"refOrigin2\" />"
            .capture("(?i)< *?ref +?id *?= *?['\"](.*?)['\"] *?\\/>".toRegex())
        refids.toString() shouldBe "[refOrigin2]"
    }

    "capture patterns" {
        val captured = "jdbc:sqlite:./target/test-classes/localDb/#{Merong_babo}#{Nayasis_babo}SimpleLauncherHelloWorld.db"
            .capture("#\\{(.+?)_(.+?)}".toPattern())
        captured.size shouldBe 4
        captured shouldBe listOf("Merong", "babo", "Nayasis", "babo")
        val refids = "< Ref id=\"refOrigin2\" />"
            .capture("(?i)< *?ref +?id *?= *?['\"](.*?)['\"] *?\\/>".toPattern())
        refids.toString() shouldBe "[refOrigin2]"
    }

    "encode & decode" {
        val vo = Dummy("nayasis", 45 ).also { println(">> origin: $it") }
        val encoded = vo.encodeBase64().also { println(">> encoded: $it") }
        val decoded = encoded.decodeBase64<Dummy>().also { println(">> decoded: $it") }
        decoded.name shouldBe vo.name
        decoded.age shouldBe vo.age
    }

    "split & tokenize" {
        val txt = """
            A
            B

            C
            D
        """.trimIndent()
        txt.split("\n").joinToString(",") shouldBe "A,B,,C,D"
        txt.tokenize("\n").joinToString(",") shouldBe "A,B,C,D"
    }

    "url encoding & decoding" {
        val param = "abcd =&1234원스토어韓國"
        param.urlEncode().urlDecode() shouldBe param
        param.urlEncode() shouldBe URLEncoder.encode(param,Charsets.UTF_8.name()).replace("+","%20")
        param.urlEncode(legacyMode = false) shouldBe URLEncoder.encode(param,Charsets.UTF_8.name())
    }

    "map parameter url encoding & decoding" {
        val param = mapOf( 1 to "원스토어", "ab& _e" to 3 )

        param.toUrlParam() shouldBe "1=%EC%9B%90%EC%8A%A4%ED%86%A0%EC%96%B4&ab%26%20_e=3"
        "1=%EC%9B%90%EC%8A%A4%ED%86%A0%EC%96%B4&ab%26%20_e=3".toMapFromUrlParam().toString() shouldBe "{1=원스토어, ab& _e=3}"

        "a&&&ab%26%20_e=3".toMapFromUrlParam().toString() shouldBe "{a=null, ab& _e=3}"
    }

    "mask" {
        val word = "010ABCD1234"
        word.mask("") shouldBe ""
        word.mask("###_####_####") shouldBe "010_ABCD_1234"
        word.mask("###_####_###") shouldBe "010_ABCD_123"
        word.mask("###-####-###") shouldBe "010-ABCD-123"
        word.mask("###-****-####") shouldBe "010-****-1234"
        word.mask("\\*###_####_***") shouldBe "*010_ABCD_***"
        word.mask("###_####_###\\*") shouldBe "010_ABCD_123*"
        word.mask("***-#**#-***\\") shouldBe "***-A**D-***"
        word.mask("###-****-####", pass = '*', hide = '#') shouldBe "###-ABCD-####"
    }

    "unmask" {
        "010_ABCD_1234".unmask("###_####_####") shouldBe "010ABCD1234"
        "010_ABCD_1234".unmask("###_####_###") shouldBe "010ABCD123"
        "010-ABCD-123".unmask("###-####-###") shouldBe "010ABCD123"
        "010-****-1234".unmask("###-****-####") shouldBe "010****1234"
        "*010_ABCD_***".unmask("\\*###_####_***") shouldBe "010ABCD***"
        "010_ABCD_123*".unmask("###_####_###\\*") shouldBe "010ABCD123"
        "***-A**D-***".unmask("***-#**#-***\\") shouldBe "***A**D***"
        "###-ABCD-####".unmask("###-****-####", pass = '*', hide = '#') shouldBe "###ABCD####"
    }

    "isMasked" {
        "010_ABCD_1234".isMasked("###_####_####") shouldBe true
        "010_ABCD_1234".isMasked("###_####_###") shouldBe false
        "010_ABCD_123".isMasked("###_####_###") shouldBe true
        "010_ABCD_123".isMasked("###_####_####") shouldBe true
        "010-ABCD-123".isMasked("###-####-###") shouldBe true
        "010-****-1234".isMasked("###-****-####") shouldBe true
        "*010_ABCD_***".isMasked("\\*###_####_***") shouldBe true
        "010_ABCD_123*".isMasked("###_####_###\\*") shouldBe true
        "***-A**D-***".isMasked("***-#**#-***\\") shouldBe true
        "###-ABCD-####".isMasked("###-****-####", pass = '*', hide = '#') shouldBe true
        "".isMasked("") shouldBe true
        "AAA".isMasked("") shouldBe false
        "".isMasked("#*#", fullMasked = false) shouldBe true
        "".isMasked("#*#", fullMasked = true) shouldBe false
        "010_ABCD_123".isMasked("###_####_####", fullMasked = false)shouldBe true
        "010_ABCD_123".isMasked("###_####_####", fullMasked = true)shouldBe false
    }

    "similarity" {
        "".similarity("") shouldBe 1.0
        "".similarity("A") shouldBe 0.0
        assertEquals(1.0, "".similarity(""))
        assertEquals(0.0, "".similarity("A"))
        "ABCDEFG".similarity("CDEF").let { 0.5 < it && it < 0.6 } shouldBe true
    }

    "find resources" {
        "/message/*.prop".toResources()
            .also { println(it) }
            .isNotEmpty() shouldBe true
        "message/message.en.prop".toResource()
            .also { println(it) }
            .isNotEmpty() shouldBe true
    }

    "capitalize" {
        "capitalize".toCapitalize() shouldBe "Capitalize"
        "merong".toCapitalize() shouldBe "Merong"
        "사람".toCapitalize() shouldBe "사람"
    }

    "isNumeric" {
        "1.2".isNumeric() shouldBe true
        "${Int.MAX_VALUE}".isNumeric() shouldBe true
        "${Int.MIN_VALUE}".isNumeric() shouldBe true
        "${Long.MAX_VALUE}".isNumeric() shouldBe true
        "${Long.MIN_VALUE}".isNumeric() shouldBe true
        "${Float.MAX_VALUE}".isNumeric() shouldBe true
        "${Float.MAX_VALUE}".isNumeric() shouldBe true
        "${Double.MAX_VALUE}".isNumeric() shouldBe true
        "${Double.MAX_VALUE}".isNumeric() shouldBe true
        "${Short.MAX_VALUE}".isNumeric() shouldBe true
        "${Short.MAX_VALUE}".isNumeric() shouldBe true
        "${Byte.MAX_VALUE}".isNumeric() shouldBe true
        "${Byte.MAX_VALUE}".isNumeric() shouldBe true
        "5.67892E+04".isNumeric() shouldBe true
        "5.67892e+04".isNumeric() shouldBe true
        "1.23456E-05".isNumeric() shouldBe true
        "1.23456e-05".isNumeric() shouldBe true
        "1.2A".isNumeric() shouldBe false
        "1.2.2".isNumeric() shouldBe false
        "5.67892+04".isNumeric() shouldBe false
        "1.23456-05".isNumeric() shouldBe false
    }

    "LocalTime to String #1" {
        val time = LocalTime.of(12, 23, 42 )
        shouldThrow<IllegalArgumentException> {
            println(time.format("MM:HI"))
        }
        time.format("MI:HH") shouldBe "23:12"
        time.format() shouldBe "12:23:42"
        time.format("FFF") shouldBe "000"
        time.format("SSS", native = true) shouldBe "000"
        time.format("SS", native = true) shouldBe "00"
    }

    "LocalTime to String #2" {
        val time = "12:23:42".toLocalTime("HH:MI:SS").also { println(">> time: $it") }
        time.format("MI:HH") shouldBe "23:12"
        time.format() shouldBe "12:23:42"
        time.format("FFF") shouldBe "000"
        time.format("SSS", native = true) shouldBe "000"
        time.format("SS", native = true) shouldBe "00"
    }

    "wrap" {
        "1234".wrap() shouldBe """
            "1234"
        """.trimIndent().trim()
        "12\"34".wrap() shouldBe """
            "12\"34"
        """.trimIndent().trim()
        "1234".wrap("'") shouldBe """
            '1234'
        """.trimIndent().trim()
        "12\"34".wrap("'") shouldBe """
            '12"34'
        """.trimIndent().trim()
        "12'34".wrap("'") shouldBe """
            '12\'34'
        """.trimIndent().trim()
    }

    "camel" {
        "camelCase".toCamel() shouldBe "camelCase"
        "camel_case".toCamel() shouldBe "camelCase"
        "CamelCase".toCamel() shouldBe "camelCase"
        "Camel_Case".toCamel() shouldBe "camelCase"
        "camel-case".toCamel() shouldBe "camelCase"
        "Camel-Case".toCamel() shouldBe "camelCase"
        "camel case".toCamel() shouldBe "camelCase"
        "Camel Case".toCamel() shouldBe "camelCase"
    }

    "snake" {
        "snake_case".toSnake() shouldBe "snake_case"
        "snakeCase".toSnake() shouldBe "snake_case"
        "SnakeCase".toSnake() shouldBe "snake_case"
        "Snake_Case".toSnake() shouldBe "snake_case"
        "snake-case".toSnake() shouldBe "snake_case"
        "Snake-Case".toSnake() shouldBe "snake_case"
        "snake case".toSnake() shouldBe "snake_case"
        "Snake Case".toSnake() shouldBe "snake_case"
    }

})

data class Dummy(
    val name: String,
    val age: Int,
): Serializable