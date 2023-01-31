package com.github.nayasis.kotlin.basica.core.character

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class CharactersTest {

    @Test
    fun `convert full-width to half-width`() {

        assertEquals('2','２'.toHalfWidth())
        assertEquals('2','2'.toHalfWidth())
        assertEquals('한','한'.toHalfWidth())
        assertEquals('ア','ア'.toHalfWidth())
        assertEquals('ガ','ガ'.toHalfWidth())

        assertEquals('A','Ａ'.toHalfWidth())
        assertEquals('Z','Ｚ'.toHalfWidth())
        assertEquals('a','ａ'.toHalfWidth())
        assertEquals('z','ｚ'.toHalfWidth())
        assertEquals('0','０'.toHalfWidth())
        assertEquals('9','９'.toHalfWidth())
        assertEquals('!','！'.toHalfWidth())
        assertEquals('~','～'.toHalfWidth())
        assertEquals('･','・'.toHalfWidth())
        assertEquals('｡','。'.toHalfWidth())
        assertEquals('｢','「'.toHalfWidth())
        assertEquals('｣','」'.toHalfWidth())
        assertEquals(' ','　'.toHalfWidth())

        listOf(
            "ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺ",
            "ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ",
            "０１２３４５６７８９",
            "！＃＄％＆（）＊＋，－．／：；＜＝＞？＠［］＾＿｀｛｜｝”’￥～",
            "・ー、。・「」",
            "　",
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
            "abcdefghijklmnopqrstuvwxyz",
            "0123456789",
            "!#$%&()*+,-./:;<=>?@[]^_`{|}\"'\\~",
            "･ｰ､｡･｢｣",
            " ",
        ).joinToString("").forEach { c ->
            val halfC = c.toHalfWidth()
            println("[$c]\t${c.code.toString(16)} (${c.code}) -> [$halfC]\t${halfC.code.toString(16)} (${halfC.code})")
        }

    }

}