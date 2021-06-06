package com.github.nayasis.kotlin.basica.core.string.binder

import com.github.nayasis.basica.base.Strings
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import javax.script.ScriptEngineManager

internal class ParameterBinderTest {

    val binder = ParameterBinder()

    @Test
    fun changeHangulJosa() {

        assertEquals("카드템플릿을 등록합니다.", binder.bind("{}를 등록합니다.", "카드템플릿"))
        assertEquals("카드를 등록합니다.", binder.bind("{}를 등록합니다.", "카드"))
        assertEquals("카드는 등록됩니다.", binder.bind("{}는 등록됩니다.", "카드"))
        assertEquals("카드템플릿은 등록됩니다.", binder.bind("{}는 등록됩니다.", "카드템플릿"))
        assertEquals("카드가 등록됩니다.", binder.bind("{}가 등록됩니다.", "카드"))
        assertEquals("카드템플릿이 등록됩니다.", binder.bind("{}가 등록됩니다.", "카드템플릿"))

    }

    @Test
    fun skip() {

        assertEquals( "{}", binder.bind("{{}}") )
        assertEquals( "12", binder.bind("1{}2") )

    }

}