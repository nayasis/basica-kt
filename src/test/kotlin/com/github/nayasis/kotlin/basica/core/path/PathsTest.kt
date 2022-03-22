package com.github.nayasis.kotlin.basica.core.path

import com.github.nayasis.kotlin.basica.core.string.toPath
import mu.KotlinLogging
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.Serializable

private val log = KotlinLogging.logger {}

internal class PathsTest {

    val TEST_DIR = Paths.userHome / "basica-file-test"

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

        val homeDir = Paths.userHome.invariantSeparators

        homeDir.toPath().findToStream("*",0).forEach {
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

        assertEquals( person.name, written?.name )
        assertEquals( person.age, written?.age )

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

        path.delete()

        path.writer().use {
            it.append("merong")
            it.append("-nayasis")
        }

        assertEquals("merong-nayasis", path.readText())

        path.appender().use {
            it.append("-0666")
        }

        assertEquals("merong-nayasis-0666", path.readText())

//        path.writer{ writer -> }


    }

    @Test
    fun readLines() {

        val path = Paths.userHome + "/build/resources/test/xml/Grammar.xml"
            log.debug { path }

        try {
            path.writeText("AAA")

            val txt = path.readLines()
            log.debug { txt }

            assertFalse(txt.isEmpty())
        } finally {
            path.delete()
        }

    }

    @Test
    fun invariantPath() {
        assertEquals( "//NAS/Game & Watch - Zelda", "\\\\NAS\\Game & Watch - Zelda".toPath().invariantSeparators )
        assertEquals( "a", "a\\".toPath().invariantSeparators )
        assertEquals( "/", "\\".toPath().invariantSeparators )
    }

    @Test
    fun relativePath() {
        val relative = "\\\\NAS\\emul\\ArcadeMame\\Game & Watch - Zelda".toPath().toRelative("//NAS/emul/ArcadeMame")
            log.debug { relative }
        assertEquals("Game & Watch - Zelda",relative.toString())
    }

    @Test
    fun normalize() {
        val root = "/root/bin/".toPath()
        assertEquals( "/root/temp", root.resolve(".././temp").normalize().invariantSeparators)
        assertEquals( "/root/bin/temp", root.resolve("./temp").normalize().invariantSeparators)
        assertEquals( "/root/bin/temp", root.resolve("temp").normalize().invariantSeparators)
        assertEquals( "/temp", root.resolve("/./temp").normalize().invariantSeparators)
    }

    @Test
    fun copy() {

        val root = TEST_DIR / "copy"

        val src = root / "src"
        val trg = root / "trg"
        val file = src / "sample.txt"

        file.writeText("merong")
        trg.makeDir()

        file.copy(trg)
        assertTrue( (root + "/trg/sample.txt").isFile() )

        src.copy(trg)
        assertTrue( (root + "/trg/src").isDirectory() )
        assertTrue( (root + "/trg/src/sample.txt").isFile() )

        src.copy(root / "trg2")
        assertTrue( (root + "/trg2").isDirectory() )
        assertTrue( (root + "/trg2/sample.txt").isFile() )

        file.copy(root / "sample2.txt")
        assertTrue( (root + "/sample2.txt").isFile() )

        file.copy( root + "/new/child/clone.txt")
        assertTrue( (root + "/new/child/clone.txt").isFile() )

    }

    @Test
    fun moveDir() {

        val src = TEST_DIR / "src"
        val trg = TEST_DIR / "trg"

        val file = src / "sample.txt"
        file.writeText("merong")

        // existed dir !!
        trg.makeDir()

        var moved = src.move(trg)

        assertTrue(src.notExists())
        assertTrue(moved.exists())
        assertTrue((trg + "/src/sample.txt").isFile())
        assertEquals(trg + "/src", moved)

    }

    @Test
    fun moveDirNotExist() {

        val src = TEST_DIR / "src"
        val trg = TEST_DIR / "trg"

        val file = src / "sample.txt"
        file.writeText("merong")

        val moved = src.move(trg)

        assertTrue(src.notExists())
        assertTrue(moved.exists())
        assertTrue((trg + "/sample.txt").isFile())
        assertEquals(trg, moved)

    }

    @Test
    fun moveFile() {

        val src = TEST_DIR / "src"
        val trg = TEST_DIR / "trg"

        val file1 = src / "sample1.txt"
        val file2 = src / "sample2.txt"

        file1.writeText("merong")
        file2.writeText("merong")

        val moved1 = file1.move(trg + "/sample.txt")
        val moved2 = file2.move(trg + "/children/sample2.txt")

        assertEquals( trg + "/sample.txt", moved1 )
        assertEquals( trg + "/children/sample2.txt", moved2 )

        assertTrue( moved1.isFile() )
        assertTrue( moved2.isFile() )

    }

    @Test
    @Disabled
    fun symbolicLink() {

        val src = TEST_DIR + "/src/sample.txt"
        val trg = TEST_DIR + "/trg/sample.txt"

        src.writeText("merong")
        src.makeSymbolicLink(trg)

        assertTrue(trg.exists())
        assertTrue(trg.isSymbolicLink())
        assertEquals("merong", trg.readText())

    }

    @Test
    @Disabled
    fun hardLink() {

        val src = TEST_DIR + "/src/sample.txt"
        val trg = TEST_DIR + "/trg/sample.txt"

        src.writeText("merong")
        src.makeHardLink(trg)

        assertTrue(trg.exists())
        assertEquals("merong", trg.readText())

    }

    @Test
    fun last() {

        Paths.applicationRoot.let {
            println( "${it}")
            println( "${it.first()}")
            println( "${it.last()}")
        }

        "merong/11".toPath().let {
            println( "${it}")
            println( "${it.first()}")
            println( "${it.last()}")
        }

    }

}

data class Person (
    val name: String,
    val age: Int
): Serializable