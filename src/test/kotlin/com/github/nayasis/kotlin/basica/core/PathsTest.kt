package com.github.nayasis.kotlin.basica.core

import mu.KotlinLogging
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.Serializable
import kotlin.io.path.useLines

private val log = KotlinLogging.logger {}

internal class PathsTest {

    val TEST_DIR = userHome() / "basica-test"

    @BeforeEach
    fun makeTemp() {
        TEST_DIR.makeDir()
    }

    @AfterEach
    fun clearTemp() {
        TEST_DIR.delete()
    }

    @Test
    fun filename() {
        assertEquals( "c", "/a/b/c".toPath().name )
        assertEquals( "c.txt", "/a/b/c.txt".toPath().name )
    }

    @Test
    fun invariantSeparators() {
        assertEquals( "c:/documents/merong", "c:\\documents\\merong\\".toPath().invariantSeparators )
    }

    @Test
    fun glob() {
        "c:\\NIDE\\workspace\\ByteMatrix\\crawler\\".toPath().find("*",0) {
            log.debug { it }
        }
    }

    @Test
    fun exists() {
        log.debug { "a".toPath().exists() }
        log.debug { "a".toPath().isFile() }
    }

    @Test
    fun convertFromString() {
        assertEquals( "a", Path("a       ").pathString )
    }

    @Test
    fun makeFile() {
        (TEST_DIR/"merong.txt").makeFile()
    }

    @Test
    fun objectWriter() {

        val person = Person("nayasis",45)

        val path = TEST_DIR / "person"
        path.writeObject(person)

        val written = path.readObject<Person>()

        log.debug { written }

        assertEquals( person.name, written!!.name )
        assertEquals( person.age, written!!.age )

        path.useLines {  }

    }

    @Test
    fun `read & write`() {

        val path = TEST_DIR / "file.txt"

        path.writeLines(listOf("1","23","456","7890"))

        var cnt = 0

        path.readLines {
            cnt++
            log.debug { it }
        }

        assertEquals( 4, cnt )

        assertEquals("""
            1
            23
            456
            7890
        """.trimIndent(), path.readLines())

    }

}

data class Person (
    val name: String,
    val age: Int
): Serializable