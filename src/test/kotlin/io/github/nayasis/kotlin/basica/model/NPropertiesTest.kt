package io.github.nayasis.kotlin.basica.model

import io.github.nayasis.kotlin.basica.core.io.Paths
import io.github.nayasis.kotlin.basica.core.io.delete
import io.github.nayasis.kotlin.basica.core.io.div
import io.github.nayasis.kotlin.basica.core.io.makeDir
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.nio.charset.StandardCharsets
import java.nio.file.Files

internal class NPropertiesTest: StringSpec({

    val testDir  = Paths.userHome / "basica-nproperties-test"
    val testFile = testDir / "cjk.properties"

    beforeAny {
        testDir.makeDir()
    }

    afterAny {
        testDir.delete()
    }

    fun writeProps(content: Map<String, String>, charset: java.nio.charset.Charset) {
        val body = content.entries.joinToString("\n") { "${it.key}=${it.value}" }
        Files.write(
            testFile,
            body.toByteArray(charset),
            java.nio.file.StandardOpenOption.CREATE,
            java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
            java.nio.file.StandardOpenOption.WRITE,
        )
    }

    "auto-detect CJK properties across charsets" {

        val messages = linkedMapOf(
            "ko"      to "안녕하세요 세계",
            "ja"      to "こんにちは世界",
            "zh"      to "你好，世界",
            "sym"     to "♥★☆∞✓™",
            "uni.ko"  to "\\uC548\\uB155",                      // 안녕
            "uni.ja"  to "\\u3053\\u3093\\u306B\\u3061\\u306F", // こんにちは
            "uni.zh"  to "\\u4F60\\u597D",                      // 你好
            "uni.sym" to "\\u2665\\u2605",                      // ♥★
        )

        listOf(
            StandardCharsets.UTF_8,
            StandardCharsets.UTF_16,   // includes BOM
            StandardCharsets.UTF_16LE,
            StandardCharsets.UTF_16BE,
        ).forEach { charset ->
            writeProps(messages, charset)
            val props = NProperties(testFile.toFile()) // auto-detect charset

            props["ko"]      shouldBe messages["ko"]
            props["ja"]      shouldBe messages["ja"]
            props["zh"]      shouldBe messages["zh"]
            props["sym"]     shouldBe messages["sym"]
            props["uni.ko"]  shouldBe "안녕"
            props["uni.ja"]  shouldBe "こんにちは"
            props["uni.zh"]  shouldBe "你好"
            props["uni.sym"] shouldBe "♥★"
        }
    }

})

