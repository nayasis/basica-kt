package com.github.nayasis.kotlin.basica.core.localdate

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

internal class LocalDatesKtTest: StringSpec({

    "toLocalDateTime" {
        "1977".toLocalDate().toString() shouldBe "1977-01-01"
        "1977-01".toLocalDate().toString() shouldBe "1977-01-01"
        "1977-01-22".toLocalDate().toString() shouldBe "1977-01-22"
        "1977-01-22T23:59:59.999".toLocalDateTime().toString() shouldBe "1977-01-22T23:59:59.999"
        "1977-01-22T23:59:59.999".toLocalDate().toString() shouldBe "1977-01-22"
    }

    "atStartOfMonth" {
        val current = "2020-12-22 13:42:59".toLocalDateTime()
        current.atStartOfMonth().format("YYYY-MM-DD") shouldBe "2020-12-01"
        current.atEndOfMonth().format("YYYY-MM-DD") shouldBe "2020-12-31"
        current.atStartOfDay().toString("YYYY-MM-DD HH:MI:SS") shouldBe "2020-12-22 00:00:00"
        current.atEndOfDay().toString("YYYY-MM-DD HH:MI:SS") shouldBe "2020-12-22 23:59:59"
    }

    "offset" {
        "2020-01-01 13:00:00 Z+0300".toLocalDateTime().format() shouldBe "2020-01-01T16:00:00"
        "2020-01-01 13:00:00 Z+03:00".toLocalDateTime().format() shouldBe "2020-01-01T16:00:00"
        "2020-01-01 13:00:00 +03:00".toLocalDateTime().format() shouldBe "2020-01-01T16:00:00"
        "2020-01-01 13:00:00 +0300".toLocalDateTime().format() shouldBe "2020-01-01T16:00:00"
    }

    "oldTime" {
        "0423-01-01".toDate().format("YYYY-MM-DD") shouldBe "0423-01-01"
        "0423-01-01".toDate().time shouldBe -48818622472000
        "0423-01-01".toLocalDate().format("YYYY-MM-DD") shouldBe "0423-01-01"
        "0423-01-01".toLocalDate().toLong()shouldBe -48818622472000
        "0423-01-01".toLocalDateTime().format("YYYY-MM-DD") shouldBe "0423-01-01"
        "0423-01-01".toLocalDateTime().toLong() shouldBe -48818622472000
    }
})