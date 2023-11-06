package com.github.nayasis.kotlin.basica.model

import com.github.nayasis.kotlin.basica.core.io.Paths
import com.github.nayasis.kotlin.basica.core.io.div
import com.github.nayasis.kotlin.basica.core.string.bind
import com.github.nayasis.kotlin.basica.core.string.message
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.*

internal class MessagesTest: StringSpec({

    beforeTest {
        Messages.clear()
    }

    "load from file" {

        val path = Paths.applicationRoot / "build/resources/test/message/message.en.prop"

        Messages.loadFromFile(path)

        Messages["err.session.expired"] shouldBe "Session is expired."
        Messages["notExistCode"] shouldBe "notExistCode"

    }

    "load from resource" {

        Messages.loadFromResource("/message/**.prop")

        Messages[Locale.ENGLISH, "err.session.expired"] shouldBe "Session is expired."
        Messages[Locale.UK, "err.session.expired"] shouldBe "Session is expired."
        Messages[Locale.KOREAN, "err.session.expired"] shouldBe "세션이 종료되었습니다."

    }

    "parameter binding" {

        Messages["test"] = "{}는 누구입니다."

        Messages["test"].bind("정화수") shouldBe "정화수는 누구입니다."
        Messages["test"].bind("정화종") shouldBe "정화종은 누구입니다."

    }

    "code having space" {

        Messages.loadFromResource("message/*")

        "message with space".message() shouldBe "띄어쓰기가 포함된 메세지"

    }

})