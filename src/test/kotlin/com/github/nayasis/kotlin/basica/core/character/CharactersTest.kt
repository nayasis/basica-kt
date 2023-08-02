package com.github.nayasis.kotlin.basica.core.character

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CharactersTest: StringSpec({

    "convert full-width to half-width" {

        '２'.toHalfWidth() shouldBe '2'
        '2'.toHalfWidth() shouldBe '2'
        '한'.toHalfWidth() shouldBe '한'
        'ア'.toHalfWidth() shouldBe 'ア'
        'ガ'.toHalfWidth() shouldBe 'ガ'

        'Ａ'.toHalfWidth() shouldBe 'A'
        'Ｚ'.toHalfWidth() shouldBe 'Z'
        'ａ'.toHalfWidth() shouldBe 'a'
        'ｚ'.toHalfWidth() shouldBe 'z'
        '０'.toHalfWidth() shouldBe '0'
        '９'.toHalfWidth() shouldBe '9'
        '！'.toHalfWidth() shouldBe '!'
        '～'.toHalfWidth() shouldBe '~'
        '・'.toHalfWidth() shouldBe '･'
        '。'.toHalfWidth() shouldBe '｡'
        '「'.toHalfWidth() shouldBe '｢'
        '」'.toHalfWidth() shouldBe '｣'
        '　'.toHalfWidth() shouldBe ' '

        fun toCodeTable(c: Char): String {
            return "[$c]\t${c.code.toString(16)} (${c.code})"
        }

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
            println("${toCodeTable(c)} -> ${toCodeTable(c.toHalfWidth())}")
        }
    }

})