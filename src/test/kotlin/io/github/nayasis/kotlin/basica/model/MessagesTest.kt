package io.github.nayasis.kotlin.basica.model

import io.github.nayasis.kotlin.basica.core.io.Paths
import io.github.nayasis.kotlin.basica.core.io.div
import io.github.nayasis.kotlin.basica.core.string.bind
import io.github.nayasis.kotlin.basica.core.string.message
import io.github.nayasis.kotlin.basica.model.Messages.Companion.loadMessages
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.util.*
import kotlin.io.path.writeText

private val EMPTY = Locale("", "")

internal class MessagesTest: StringSpec({

    beforeTest {
        Messages.clear()
        Messages.defaultLocale  = Locale.getDefault()
        Messages.fallbackLocale = Locale.ENGLISH
    }

    "use default locale when locale is not given" {

        "/message/**.prop".loadMessages()

        Messages.defaultLocale = Locale.KOREAN
        Messages["err.session.expired"] shouldBe "세션이 종료되었습니다."
        "err.session.expired".message() shouldBe "세션이 종료되었습니다."

        Messages.defaultLocale = Locale.ENGLISH
        Messages["err.session.expired"] shouldBe "Session is expired."

    }

    "load from file" {

        val path = Paths.applicationRoot / "build/resources/test/message/message.en.prop"

        path.loadMessages()

        Messages["err.session.expired"] shouldBe "Session is expired."
        Messages["notExistCode"] shouldBe "notExistCode"

    }

    "load from resource" {

        "/message/**.prop".loadMessages()

        Messages["err.session.expired", Locale.ENGLISH] shouldBe "Session is expired."
        Messages["err.session.expired", Locale.UK]      shouldBe "Session is expired."
        Messages["err.session.expired", Locale.KOREAN]  shouldBe "세션이 종료되었습니다."

    }

    "parameter binding" {

        Messages["test"] = "{}는 누구입니다."

        Messages["test"].bind("정화수") shouldBe "정화수는 누구입니다."
        Messages["test"].bind("정화종") shouldBe "정화종은 누구입니다."

    }

    "code having space" {

        "message/*".loadMessages()

        "message with space".message() shouldBe "띄어쓰기가 포함된 메세지"

    }

    "use fallback locale when requested locale is missing" {

        "/message/**.prop".loadMessages()

        Messages["err.session.expired", Locale.JAPANESE] shouldBe "Session is expired."

        Messages.fallbackLocale = Locale.KOREAN
        Messages["err.session.expired", Locale.JAPANESE] shouldBe "세션이 종료되었습니다."

    }

    "prefer empty locale to fallback locale" {

        Messages[EMPTY, "greeting"]          = "hi"
        Messages[Locale.ENGLISH, "greeting"] = "hello"

        Messages["greeting", Locale.JAPANESE] shouldBe "hi"
        Messages["greeting", Locale.ENGLISH]  shouldBe "hello"

    }

    "use first registered message when fallback locale is also missing" {

        Messages[Locale.KOREAN, "greeting"]  = "안녕하세요"
        Messages[Locale.CHINESE, "greeting"] = "你好"

        Messages["greeting", Locale.JAPANESE] shouldBe "안녕하세요"

    }

    "load files in order of language priority" {

        val dir = Files.createTempDirectory("messages")
        try {
            (dir / "a.xx.prop").writeText("greeting = xx")
            (dir / "b.ko.prop").writeText("greeting = ko")
            (dir / "c.zh.prop").writeText("greeting = zh")

            dir.loadMessages()

            Messages["greeting", Locale.JAPANESE] shouldBe "zh"
        } finally {
            dir.toFile().deleteRecursively()
        }

    }

})