package io.github.nayasis.kotlin.basica.core.io

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path

class ProgressIOTest: StringSpec({

    @Suppress("LocalVariableName")
    val TEST_DIR = Paths.userHome / "progress-io-test"
    @Suppress("LocalVariableName")
    val DUMMY_CONTENTS = "A".repeat(10000)

    fun dummy(path: Path): Path {
        path.parent.makeDir()
        path.writeText(DUMMY_CONTENTS)
        return path
    }

    fun dummy(subDir: String): Path {
        return dummy(TEST_DIR / subDir)
    }

    beforeAny {
        TEST_DIR.makeDir()
    }

    afterAny {
        TEST_DIR.delete()
    }

    "copy file" {
        val source = dummy("source.txt")
        val target = TEST_DIR / "target.txt"
        var progress = false

        ProgressIO.copyFile(source,target) { read, done, total ->
            progress = true
            println("read: $read, done: $done, total: $total")
        }

        source.fileSize shouldBe target.fileSize
        progress shouldBe true
    }
    "move file" {
        val source = dummy("source.txt")
        val target = TEST_DIR / "target.txt"
        var progress = false

        val prevSourceSize = source.fileSize

        target.notExists() shouldBe true

        ProgressIO.moveFile(source,target) { read, done, total ->
            progress = true
            println("read: $read, done: $done, total: $total")
        }

        source.notExists() shouldBe true
        target.exists() shouldBe true
        target.fileSize shouldBe prevSourceSize
        progress shouldBe true
    }
    "move same file" {
        val source = dummy("source.txt")
        val target = TEST_DIR / "source.txt"
        shouldThrow<FileAlreadyExistsException> {
            ProgressIO.moveFile(source,target) { read, done, total ->
                println("read: $read, done: $done, total: $total")
            }
        }
    }
    "copy directory" {
        val source = TEST_DIR / "source"
        val target = TEST_DIR / "target"
        var progress = false

        dummy(source / "test1.txt")
        dummy(source / "test2.txt")

        ProgressIO.copyDirectory(source, target) { index, file, read, done ->
            progress = true
            println("i: $index, file: ${file.name}, read: $read, done: $done")
        }

        target.exists() shouldBe true
        progress shouldBe true

        val sourceStat = source.statistics
        val targetStat = target.statistics

        println(targetStat)
        targetStat shouldBe sourceStat
    }
    "move directory" {
        val source = TEST_DIR / "source"
        val target = TEST_DIR / "target"
        var progress = false

        dummy(source / "test1.txt")
        dummy(source / "test2.txt")

        val sourceStat = source.statistics

        target.notExists() shouldBe true

        ProgressIO.moveDirectory(source, target) { index, file, read, done ->
            progress = true
            println("i: $index, file: ${file.name}, read: $read, done: $done")
        }

        source.notExists() shouldBe true
        target.exists() shouldBe true
        progress shouldBe true

        val targetStat = target.statistics

        println(targetStat)
        targetStat shouldBe sourceStat
    }
})