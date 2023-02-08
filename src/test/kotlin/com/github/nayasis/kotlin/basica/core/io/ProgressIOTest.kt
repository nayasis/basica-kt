package com.github.nayasis.kotlin.basica.core.io

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path

class ProgressIOTest {

    val TEST_DIR = Paths.userHome / "progress-io-test"
    val DUMMY_CONTENTS = "A".repeat(10000)

    @BeforeEach
    fun makeTemp() {
        TEST_DIR.makeDir()
    }

    @AfterEach
    fun clearTemp() {
        TEST_DIR.delete()
    }

    @Test
    fun copyFile() {

        val source = dummy("source.txt")
        val target = TEST_DIR / "target.txt"
        var progress = false

        ProgressIO.copyFile(source,target) { read, done, total ->
            progress = true
            println("read: $read, done: $done, total: $total")
        }

        assertEquals(source.fileSize, target.fileSize)
        assertTrue(progress)

    }

    @Test
    fun moveFile() {

        val source = dummy("source.txt")
        val target = TEST_DIR / "target.txt"
        var progress = false

        val prevSourceSize = source.fileSize

        assertTrue(target.notExists())

        ProgressIO.moveFile(source,target) { read, done, total ->
            progress = true
            println("read: $read, done: $done, total: $total")
        }

        assertTrue(source.notExists())
        assertTrue(target.exists())
        assertEquals(prevSourceSize, target.fileSize)
        assertTrue(progress)

    }

    @Test
    fun `move same file`() {

        val source = dummy("source.txt")
        val target = TEST_DIR / "source.txt"

        assertThrows(FileAlreadyExistsException::class.java) {
            ProgressIO.moveFile(source,target) { read, done, total ->
                println("read: $read, done: $done, total: $total")
            }
        }

    }

    @Test
    fun copyDirectory() {

        val source = TEST_DIR / "source"
        val target = TEST_DIR / "target"
        var progress = false

        dummy(source / "test1.txt")
        dummy(source / "test2.txt")

        ProgressIO.copyDirectory(source, target) { index, file, read, done ->
            progress = true
            println("i: $index, file: ${file.name}, read: $read, done: $done")
        }

        assertTrue(target.exists())
        assertTrue(progress)

        val sourceStat = source.statistics
        val targetStat = target.statistics

        println(targetStat)
        assertEquals(sourceStat, targetStat)

    }

    @Test
    fun moveDirectory() {

        val source = TEST_DIR / "source"
        val target = TEST_DIR / "target"
        var progress = false

        dummy(source / "test1.txt")
        dummy(source / "test2.txt")

        val sourceStat = source.statistics

        assertTrue(target.notExists())

        ProgressIO.moveDirectory(source, target) { index, file, read, done ->
            progress = true
            println("i: $index, file: ${file.name}, read: $read, done: $done")
        }

        assertTrue(source.notExists())
        assertTrue(target.exists())
        assertTrue(progress)

        val targetStat = target.statistics

        println(targetStat)
        assertEquals(sourceStat, targetStat)

    }

    fun dummy(subDir: String): Path {
        return dummy(TEST_DIR / subDir)
    }

    fun dummy(path: Path): Path {
        path.parent.makeDir()
        path.writeText(DUMMY_CONTENTS)
        return path
    }

}