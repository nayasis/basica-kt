package com.github.nayasis.kotlin.basica.model

import com.github.nayasis.kotlin.basica.core.path.div
import com.github.nayasis.kotlin.basica.core.path.rootPath
import com.github.nayasis.kotlin.basica.core.string.bind
import mu.KotlinLogging
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

private val log = KotlinLogging.logger {}

internal class MessagesTest {

    @BeforeEach
    fun initPool() {
        Messages.clear()
    }

    @Test
    fun `load from file`() {

        val path = rootPath() / "build/resources/test/message/message.en.prop"

        Messages.loadFromFile(path)

        assertEquals("Session is expired.", Messages["err.session.expired"])
        assertEquals("notExistCode", Messages["notExistCode"])

    }

    @Test
    fun `load from resourcefile`() {

        Messages.loadFromResource("/message/**.prop")

        assertEquals("Session is expired.", Messages[Locale.ENGLISH, "err.session.expired"] )
        assertEquals( "Session is expired.", Messages[Locale.UK, "err.session.expired"] )
        assertEquals("세션이 종료되었습니다.", Messages[Locale.KOREAN, "err.session.expired"])

    }

    @Test
    fun `parameter binding`() {

        Messages["test"] = "{}는 누구입니다."

        assertEquals("정화수는 누구입니다.", Messages["test"].bind("정화수"))
        assertEquals("정화종은 누구입니다.", Messages["test"].bind("정화종"))

    }

}