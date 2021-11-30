package com.github.nayasis.kotlin.basica.core.string

import com.github.nayasis.kotlin.basica.core.collection.toUrlParam
import com.github.nayasis.kotlin.basica.core.localdate.toLocalDateTime
import mu.KotlinLogging
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.Serializable
import java.net.URLEncoder

private val log = KotlinLogging.logger {}

internal class StringsKtTest {

    @Test
    fun `glob test`() {

        println( ".".glob("*.kt").joinToString("\n") )
        assertTrue( ".".glob("*.kt").size > 1 )
        println("----------")
        println( "".glob("*.kt").joinToString("\n") )
        assertTrue( "".glob("*.kt").size > 1 )

    }

    @Test
    fun isDate() {

        "2021-01-01".toLocalDateTime()

        assertTrue( "2021-01-01".isDate() )
        assertFalse( "2021-01-33".isDate() )
    }

    @Test
    fun toNumber() {
        assertEquals(1, "1".toNumber())
        assertEquals(1.2, "1.2".toNumber(Double::class))
        assertEquals(0.0, "nayasis".toNumber(Double::class))
        assertEquals(0L, "nayasis".toNumber(Long::class))
    }

    @Test
    fun `bind parameter`() {

        val param = """
            { "name":"nayasis", "age":10 }
        """.toMap()

        val format = "\${name} is \${age} years old."

        println( format.format(param) )

    }

    @Test
    fun `capture patterns`() {

        val captured = "jdbc:sqlite:./target/test-classes/localDb/#{Merong}#{Nayasis}SimpleLauncherHelloWorld.db"
            .capture("#\\{(.+?)\\}".toPattern())

        assertEquals(2, captured.size)
        assertEquals(listOf("Merong", "Nayasis"), captured)

        val refids = "< Ref id=\"refOrigin2\" />"
            .capture("(?i)< *?ref +?id *?= *?['\"](.*?)['\"] *?\\/>".toPattern())

        assertEquals("[refOrigin2]", refids.toString())

    }

    @Test
    fun `encode & decode`() {

        val dummy = Dummy("nayasis", 45 )

        val text = dummy.encodeBase64()

        log.debug { text }

        val decoded = text.decodeBase64<Dummy>()!!

        log.debug { decoded }

        assertEquals( dummy.name, decoded.name )
        assertEquals( dummy.age, decoded.age )

    }

    @Test
    fun `split and tokenize`() {

        val txt = """
            A
            B

            C
            D
        """.trimIndent()

        assertEquals("A,B,,C,D", txt.split("\n").joinToString(","))
        assertEquals("A,B,C,D", txt.tokenize("\n").joinToString(","))

    }

    @Test
    fun `url encoding & decoding`() {

        val param = "abcd =&1234원스토어韓國"

        assertEquals(param, param.toUrlParam().urlDecode() )
        assertEquals(URLEncoder.encode(param,Charsets.UTF_8.name()).replace("+","%20"), param.toUrlParam() )
        assertEquals(URLEncoder.encode(param,Charsets.UTF_8.name()), param.toUrlParam(legacyMode = false) )

    }

    @Test
    fun `map parameter url encoding & decoding`() {

        val param = mapOf( 1 to "원스토어", "ab& _e" to 3 )
        val urlParam = param.toUrlParam()
        val map = urlParam.toMapFromUrlParam()

        assertEquals("1=%EC%9B%90%EC%8A%A4%ED%86%A0%EC%96%B4&ab%26%20_e=3", urlParam)
        assertEquals("{1=원스토어, ab& _e=3}", map.toString())


        val another = "a&&&ab%26%20_e=3".toMapFromUrlParam()
        assertEquals("{a=null, ab& _e=3}", another.toString())

    }

}

data class Dummy(
    val name: String,
    val age: Int,
): Serializable