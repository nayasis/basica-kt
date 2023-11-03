package com.github.nayasis.kotlin.basica.core.string

import com.github.nayasis.kotlin.basica.core.collection.toUrlParam
import com.github.nayasis.kotlin.basica.core.extension.isNotEmpty
import com.github.nayasis.kotlin.basica.core.localdate.format
import com.github.nayasis.kotlin.basica.core.localdate.toLocalDateTime
import com.github.nayasis.kotlin.basica.core.localdate.toLocalTime
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import mu.KotlinLogging
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.Serializable
import java.net.URLEncoder
import java.time.LocalTime

private val log = KotlinLogging.logger {}

internal class StringsTest: StringSpec({

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
    fun `capture regex`() {

        val captured = "jdbc:sqlite:./target/test-classes/localDb/#{Merong_babo}#{Nayasis_babo}SimpleLauncherHelloWorld.db"
            .capture("#\\{(.+?)_(.+?)}".toRegex())

        assertEquals(4, captured.size)
        assertEquals(listOf("Merong", "babo", "Nayasis", "babo"), captured)

        val refids = "< Ref id=\"refOrigin2\" />"
            .capture("(?i)< *?ref +?id *?= *?['\"](.*?)['\"] *?\\/>".toRegex())

        assertEquals("[refOrigin2]", refids.toString())

    }

    @Test
    fun `capture patterns`() {

        val captured = "jdbc:sqlite:./target/test-classes/localDb/#{Merong_babo}#{Nayasis_babo}SimpleLauncherHelloWorld.db"
            .capture("#\\{(.+?)_(.+?)}".toPattern())

        assertEquals(4, captured.size)
        assertEquals(listOf("Merong", "babo", "Nayasis", "babo"), captured)

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

        assertEquals(param, param.urlEncode().urlDecode() )
        assertEquals(URLEncoder.encode(param,Charsets.UTF_8.name()).replace("+","%20"), param.urlEncode() )
        assertEquals(URLEncoder.encode(param,Charsets.UTF_8.name()), param.urlEncode(legacyMode = false) )

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

    @Test
    fun `mask`() {

        val word = "010ABCD1234"

        assertEquals("", word.mask(""))
        assertEquals("010_ABCD_1234", word.mask("###_####_####"))
        assertEquals("010_ABCD_123", word.mask("###_####_###"))
        assertEquals("010-ABCD-123", word.mask("###-####-###"))
        assertEquals("010-****-1234", word.mask("###-****-####"))
        assertEquals("*010_ABCD_***", word.mask("\\*###_####_***"))
        assertEquals("010_ABCD_123*", word.mask("###_####_###\\*"))
        assertEquals("***-A**D-***", word.mask("***-#**#-***\\"))
        assertEquals("###-ABCD-####", word.mask("###-****-####", pass = '*', hide = '#'))

    }

    @Test
    fun `unmask`() {
        assertEquals("010ABCD1234", "010_ABCD_1234".unmask("###_####_####"))
        assertEquals("010ABCD123", "010_ABCD_1234".unmask("###_####_###"))
        assertEquals("010ABCD123", "010-ABCD-123".unmask("###-####-###"))
        assertEquals("010****1234", "010-****-1234".unmask("###-****-####"))
        assertEquals("010ABCD***", "*010_ABCD_***".unmask("\\*###_####_***"))
        assertEquals("010ABCD123", "010_ABCD_123*".unmask("###_####_###\\*"))
        assertEquals("***A**D***", "***-A**D-***".unmask("***-#**#-***\\"))
        assertEquals("###ABCD####", "###-ABCD-####".unmask("###-****-####", pass = '*', hide = '#'))
    }

    @Test
    fun `isMasked`() {

        assertTrue( "010_ABCD_1234".isMasked("###_####_####"))
        assertFalse( "010_ABCD_1234".isMasked("###_####_###"))
        assertTrue( "010_ABCD_123".isMasked("###_####_###"))
        assertTrue( "010_ABCD_123".isMasked("###_####_####"))
        assertTrue( "010-ABCD-123".isMasked("###-####-###"))
        assertTrue( "010-****-1234".isMasked("###-****-####"))
        assertTrue( "*010_ABCD_***".isMasked("\\*###_####_***"))
        assertTrue( "010_ABCD_123*".isMasked("###_####_###\\*"))
        assertTrue( "***-A**D-***".isMasked("***-#**#-***\\"))
        assertTrue( "###-ABCD-####".isMasked("###-****-####", pass = '*', hide = '#'))

        assertTrue( "".isMasked("") )
        assertFalse( "AAA".isMasked("") )
        assertTrue( "".isMasked("#*#", fullMasked = false) )
        assertFalse( "".isMasked("#*#", fullMasked = true) )
        assertTrue( "010_ABCD_123".isMasked("###_####_####", fullMasked = false))
        assertFalse( "010_ABCD_123".isMasked("###_####_####", fullMasked = true))

    }

    @Test
    fun `similarity`() {
        assertEquals(1.0, "".similarity(""))
        assertEquals(0.0, "".similarity("A"))
        assertTrue( "ABCDEFG".similarity("CDEF").let { 0.5 < it && it < 0.6 } )
    }

    @Test
    fun `find resources`() {

        val resources = "/message/*.prop".toResources()
        println( resources )
        assertTrue(resources.isNotEmpty(), "there are no resources.")

        val resource = "message/message.en.prop".toResource()
        println(resource)
        assertTrue(resource.isNotEmpty(), "there are no resource.")

    }

    @Test
    fun `capitalize`() {
        assertEquals("Capitalize", "capitalize".toCapitalize())
        assertEquals("Merong", "merong".toCapitalize())
        assertEquals("능력자", "능력자".toCapitalize())
    }

    @Test
    fun `isNumeric`() {
        assertTrue("1.2".isNumeric())
        assertTrue("${Int.MAX_VALUE}".isNumeric())
        assertTrue("${Int.MIN_VALUE}".isNumeric())
        assertTrue("${Long.MAX_VALUE}".isNumeric())
        assertTrue("${Long.MIN_VALUE}".isNumeric())
        assertTrue("${Float.MAX_VALUE}".isNumeric())
        assertTrue("${Float.MAX_VALUE}".isNumeric())
        assertTrue("${Double.MAX_VALUE}".isNumeric())
        assertTrue("${Double.MAX_VALUE}".isNumeric())
        assertTrue("${Short.MAX_VALUE}".isNumeric())
        assertTrue("${Short.MAX_VALUE}".isNumeric())
        assertTrue("${Byte.MAX_VALUE}".isNumeric())
        assertTrue("${Byte.MAX_VALUE}".isNumeric())
        assertTrue("5.67892E+04".isNumeric())
        assertTrue("5.67892e+04".isNumeric())
        assertTrue("1.23456E-05".isNumeric())
        assertTrue("1.23456e-05".isNumeric())
        assertFalse("1.2A".isNumeric())
        assertFalse("1.2.2".isNumeric())
        assertFalse("5.67892+04".isNumeric())
        assertFalse("1.23456-05".isNumeric())
    }

    @Test
    fun `LocalTime to String`() {

//        println( "22:40:30.222".toDate("HHMISSFFF"))

//        val time = LocalTime.now()
//
//        val milisecFormat = time.format(DateTimeFormatter.ofPattern("HH:mm:ss.SS"))
//        println(milisecFormat)
//        val nanoFormat    = time.format(DateTimeFormatter.ofPattern("HH:mm:ss.nnnnnnnnn"))
//        println(nanoFormat)

        val time = LocalTime.of(12, 23, 42 )

        assertThrows(IllegalArgumentException::class.java) {
            println(time.format("MM:HI"))
        }

        assertEquals("23:12", time.format("MI:HH"))
        assertEquals("12:23:42", time.format())
        assertEquals("000", time.format("FFF"))
        assertEquals("000", time.format("SSS", native = true))
        assertEquals("00", time.format("SS", native = true))

    }

    @Test
    fun `String to LocalTime`() {
        val time = "12:23:42".toLocalTime("HH:MI:SS")
        println(time)
        assertEquals("23:12", time.format("MI:HH"))
        assertEquals("12:23:42", time.format())
        assertEquals("000", time.format("FFF"))
        assertEquals("000", time.format("SSS", native = true))
        assertEquals("00", time.format("SS", native = true))
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

})

data class Dummy(
    val name: String,
    val age: Int,
): Serializable