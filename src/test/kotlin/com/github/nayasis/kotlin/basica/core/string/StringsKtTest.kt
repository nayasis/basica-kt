package com.github.nayasis.kotlin.basica.core.string

import com.github.nayasis.basica.base.Strings
import com.github.nayasis.kotlin.basica.core.localdate.toLocalDateTime
import mu.KotlinLogging
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

private val log = KotlinLogging.logger {}

internal class StringsKtTest {

    @Test
    fun `glob test`() {

        println( ".".glob("*.kt").joinToString("\n") )
        Assertions.assertTrue( ".".glob("*.kt").size > 1 )
        println("----------")
        println( "".glob("*.kt").joinToString("\n") )
        Assertions.assertTrue( "".glob("*.kt").size > 1 )

    }

    @Test
    fun isDate() {

        "2021-01-01".toLocalDateTime()

        Assertions.assertTrue( "2021-01-01".isDate() )
        Assertions.assertFalse( "2021-01-33".isDate() )
    }

    @Test
    fun toNumber() {
        Assertions.assertEquals(1, "1".toNumber())
        Assertions.assertEquals(1.2, "1.2".toNumber(Double::class))
        Assertions.assertEquals(0.0, "nayasis".toNumber(Double::class))
        Assertions.assertEquals(0L, "nayasis".toNumber(Long::class))
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

        Assertions.assertEquals(2, captured.size)
        Assertions.assertEquals(listOf("Merong", "Nayasis"), captured)

        val refids = "< Ref id=\"refOrigin2\" />"
            .capture("(?i)< *?ref +?id *?= *?['\"](.*?)['\"] *?\\/>".toPattern())

        Assertions.assertEquals("[refOrigin2]", refids.toString())


    }

}