package io.github.nayasis.kotlin.basica.core.io

import io.github.nayasis.kotlin.basica.core.string.toPath
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.Serializable

private val logger = KotlinLogging.logger {}


class PathsTest: StringSpec({

    @Suppress("PrivatePropertyName", "LocalVariableName")
    val TEST_DIR = Paths.userHome / "basica-file-test"

    beforeAny {
        TEST_DIR.makeDir()
    }

    afterAny {
        TEST_DIR.delete()
    }

    "filename" {
        "/a/b/c".toPath().name shouldBe "c"
        "/a/b/c.txt".toPath().name shouldBe "c.txt"
    }
    "glob" {
        val homeDir = Paths.userHome.invariantPath
        var count = 0
        homeDir.toPath().findToStream("*",0).forEach {
            logger.debug { it }
            count++
        }
        count shouldBeGreaterThan 0
    }
    "exists" {
        val path = "a".toPath().also { println(it) }
        path.exists() shouldBe false
        path.isFile() shouldBe false
    }
    "path from string" {
        Path("a       ").pathString shouldBe "a"
    }
    "make file" {
        (TEST_DIR/"merong.txt").makeFile()
    }
    "write object" {
        val person = Person("nayasis",45)
        val path = TEST_DIR / "person"

        path.writeObject(person)

        val written = path.readObject<Person>().also { println(it) }

        written?.name shouldBe person.name
        written?.age shouldBe person.age
    }
    "read and write" {
        val path = TEST_DIR / "file.txt"
        path.writeLines(listOf("1","23","456","7890"))

        var cnt = 0
        path.readLines {
            cnt++
            logger.debug { it }
        }
        cnt shouldBe 4

        path.readLines() shouldBe """
            1
            23
            456
            7890
        """.trimIndent()

        path.delete()

        path.writer().use {
            it.append("merong")
            it.append("-nayasis")
        }

        path.readText() shouldBe "merong-nayasis"

        path.appender().use {
            it.append("-0666")
        }

        path.readText() shouldBe "merong-nayasis-0666"
    }
    "read lines" {
        val path = TEST_DIR + "/build/resources/test/xml/Grammar.xml"
        path.writeText("AAA")
        val txt = path.readLines().also { println(it) }
        txt.isEmpty() shouldBe false
    }
    "invariant path" {
        "c:\\documents\\merong\\".toPath().invariantPath shouldBe "c:/documents/merong"
        "\\\\NAS\\Game & Watch - Zelda".toPath().invariantPath shouldBe  "//NAS/Game & Watch - Zelda"
        "a\\".toPath().invariantPath shouldBe "a"
        "\\".toPath().invariantPath shouldBe "/"
    }
    "relative path" {
        val relative = "\\\\NAS\\emul\\ArcadeMame\\Game & Watch - Zelda".toPath().toRelative("//NAS/emul/ArcadeMame")
        relative.toString() shouldBe "Game & Watch - Zelda"
    }
    "normalize" {
        val root = "/root/bin/".toPath()
        root.resolve(".././temp").normalize().invariantPath shouldBe "/root/temp"
        root.resolve("./temp").normalize().invariantPath    shouldBe "/root/bin/temp"
        root.resolve("temp").normalize().invariantPath      shouldBe "/root/bin/temp"
        root.resolve("/./temp").normalize().invariantPath   shouldBe "/temp"
    }
    "copy" {
        val root = TEST_DIR / "copy"
        val src = root / "src"
        val trg = root / "trg"
        val file = src / "sample.txt"

        file.writeText("merong")
        trg.makeDir()

        file.copy(trg)
        (root + "/trg/sample.txt").isFile() shouldBe true

        src.copy(trg)
        (root + "/trg/src").isDirectory() shouldBe true
        (root + "/trg/src/sample.txt").isFile() shouldBe true

        src.copy(root / "trg2")
        (root + "/trg2").isDirectory() shouldBe true
        (root + "/trg2/sample.txt").isFile() shouldBe true

        file.copy(root / "sample2.txt")
        (root + "/sample2.txt").isFile() shouldBe true

        file.copy( root + "/new/child/clone.txt")
        (root + "/new/child/clone.txt").isFile() shouldBe true
    }
    "move dir" {
        val src = TEST_DIR / "src"
        val trg = TEST_DIR / "trg"

        (src / "sample.txt").writeText("merong")

        // existed dir !!
        trg.makeDir()

        val moved = src.move(trg)

        src.notExists() shouldBe true
        moved.exists() shouldBe true
        (trg + "/src/sample.txt").isFile() shouldBe true
        moved shouldBe trg + "/src"

    }
    "move dir not exist" {
        val src = TEST_DIR / "src"
        val trg = TEST_DIR / "trg"

        val file = src / "sample.txt"
        file.writeText("merong")

        val moved = src.move(trg)

        src.notExists() shouldBe true
        moved.exists() shouldBe true
        (trg + "/sample.txt").isFile() shouldBe true
        moved shouldBe trg

    }
    "move file" {
        val src = TEST_DIR / "src"
        val trg = TEST_DIR / "trg"
        val existDir = (TEST_DIR / "existed").also { it.makeDir() }

        val file1 = (src / "sample1.txt").also { it.writeText("merong 1") }
        val file2 = (src / "sample2.txt").also { it.writeText("merong 2") }
        val file3 = (src / "sample3.txt").also { it.writeText("merong 3") }

        val moved1 = file1.move(trg + "/sample.txt")
        val moved2 = file2.move(trg + "/children/sample2.txt")
        val moved3 = file3.move(existDir)

        moved1 shouldBe trg + "/sample.txt"
        moved2 shouldBe trg + "/children/sample2.txt"
        moved3 shouldBe existDir + "/sample3.txt"

        moved1.isFile() shouldBe true
        moved2.isFile() shouldBe true
        moved3.isFile() shouldBe true
    }
    "symbolic link".config(false) {
        val src = TEST_DIR + "/src/sample.txt"
        val trg = TEST_DIR + "/trg/sample.txt"

        src.writeText("merong")
        src.makeSymbolicLink(trg)

        trg.exists() shouldBe true
        trg.isSymbolicLink() shouldBe true
        trg.readText() shouldBe "merong"
    }
    "hard link".config(false) {
        val src = TEST_DIR + "/src/sample.txt"
        val trg = TEST_DIR + "/trg/sample.txt"

        src.writeText("merong")
        src.makeHardLink(trg)

        trg.exists() shouldBe true
        trg.readText() shouldBe "merong"
    }
    "last path" {
        Paths.applicationRoot.let {
            println( "${it}")
            println( "${it.first()}")
            println( "${it.last()}")
        }
        "merong/11".toPath().let {
            it.invariantPath shouldBe "merong/11"
            "${it.first()}" shouldBe "merong"
            "${it.last()}" shouldBe "11"
        }
    }
    "isCommonPrefix" {
        listOf(
            "\\\\NAS2\\game\\pc\\_backup\\Fight\\",
            "\\\\NAS2\\game\\pc\\_backup\\",
            "\\\\NAS2\\game\\pc\\_backup\\Adult\\"
        ).map{it.toPath()}.isCommonPrefix(
            "//NAS2/game/pc/_backup".toPath()
        ) shouldBe true

        listOf(
            "\\\\NAS2\\game\\pc\\_backup\\Fight\\",
            "\\\\NAS2\\game\\pc\\_backup\\",
            "\\\\NAS1\\game\\pc\\_backup\\Adult\\"
        ).map{it.toPath()}.isCommonPrefix(
            "//NAS2/game/pc/_backup".toPath()
        ) shouldBe false

        listOf(
            "c:/NAS2/game/pc/_backup/Fight",
            "c:/NAS2/game/pc/_backup/merong.txt",
            "c:/NAS1/game/pc/_backup/Adult"
        ).map{it.toPath()}.isCommonPrefix(
            "c:/".toPath()
        ) shouldBe true
    }
    "findLongestPrefix" {
        listOf(
            "\\\\NAS2\\game\\pc\\_backup\\Fight\\",
            "\\\\NAS2\\game\\pc\\_backup\\",
            "\\\\NAS2\\game\\pc\\_backup\\Adult\\"
        ).map{it.toPath()}.findLongestPrefix() shouldBe "//NAS2/game/pc/_backup".toPath()

        listOf(
            "\\\\NAS2\\game\\pc\\_backup\\Fight\\",
            "\\\\NAS2\\game\\pc\\_backup\\",
            "\\\\NAS1\\game\\pc\\_backup\\Adult\\"
        ).map{it.toPath()}.findLongestPrefix() shouldBe null

        listOf(
            "c:/NAS2/game/pc/_backup/Fight",
            "c:/NAS2/game/pc/_backup/merong.txt",
            "c:/NAS1/game/pc/_backup/Adult"
        ).map{it.toPath()}.findLongestPrefix() shouldBe "c:/".toPath()
    }
    "get name" {
        "c:/test/dir v0.9.22.0".toPath().let {
            "${it.fileName}" shouldBe "dir v0.9.22.0"
            it.name shouldBe "dir v0.9.22.0"
            it.nameWithoutExtension shouldBe "dir v0.9.22"
        }
    }
    "get name without extension" {
        "c:/test/file v0.9.22.0 .txt".toPath().let {
            it.name shouldBe "file v0.9.22.0 .txt"
            it.nameWithoutExtension shouldBe "file v0.9.22.0 "
            it.pathWithoutExtension shouldBe "c:\\test\\file v0.9.22.0 "
            it.invariantPathWithoutExtension shouldBe "c:/test/file v0.9.22.0 "
        }
    }
    "get extension" {
        "e:\\download\\Hypseus.Singe.v2.8.2a.win64".toPath().let {
            it.extension shouldBe "win64"
        }
    }
})

data class Person (
    val name: String,
    val age: Int
): Serializable