package com.github.nayasis.kotlin.basica.core.localdate

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.ZoneId
import java.time.ZonedDateTime

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

    "ZonedDateTime format and toString" {
        val zonedDateTime = ZonedDateTime.of(2025, 7, 10, 15, 45, 30, 0, ZoneId.of("Asia/Seoul"))
        
        // 기본 format 테스트
        zonedDateTime.toString() shouldBe "2025-07-10T15:45:30+09:00"
        
        // 사용자 정의 format 테스트
        zonedDateTime.format("yyyy-MM-dd") shouldBe "2025-07-10"
        zonedDateTime.format("HH:mm:ss") shouldBe "15:45:30"
        zonedDateTime.format("yyyy-MM-dd HH:mm:ss") shouldBe "2025-07-10 15:45:30"
        zonedDateTime.format("yyyy-MM-dd'T'HH:mm:ssXXX") shouldBe "2025-07-10T15:45:30+09:00"
        
        // toString with format 테스트
        zonedDateTime.toString("yyyy-MM-dd") shouldBe "2025-07-10"
        zonedDateTime.toString("HH:mm:ss") shouldBe "15:45:30"
        zonedDateTime.toString("yyyy-MM-dd HH:mm:ss") shouldBe "2025-07-10 15:45:30"
        zonedDateTime.toString("yyyy-MM-dd'T'HH:mm:ssXXX") shouldBe "2025-07-10T15:45:30+09:00"
        
        // 다른 timezone 테스트
        val utcDateTime = ZonedDateTime.of(2025, 7, 10, 15, 45, 30, 0, ZoneId.of("UTC"))
        utcDateTime.toString() shouldBe "2025-07-10T15:45:30Z"
        
        val estDateTime = ZonedDateTime.of(2025, 7, 10, 15, 45, 30, 0, ZoneId.of("America/New_York"))
        estDateTime.toString() shouldBe "2025-07-10T15:45:30-04:00"
    }

    "ZonedDateTime with different timezones" {
        val seoulTime = ZonedDateTime.of(2025, 7, 10, 15, 45, 30, 0, ZoneId.of("Asia/Seoul"))
        val tokyoTime = ZonedDateTime.of(2025, 7, 10, 15, 45, 30, 0, ZoneId.of("Asia/Tokyo"))
        val newYorkTime = ZonedDateTime.of(2025, 7, 10, 15, 45, 30, 0, ZoneId.of("America/New_York"))
        
        // 각 timezone의 기본 format
        seoulTime.toString() shouldBe "2025-07-10T15:45:30+09:00"
        tokyoTime.toString() shouldBe "2025-07-10T15:45:30+09:00"
        newYorkTime.toString() shouldBe "2025-07-10T15:45:30-04:00"
        
        // 사용자 정의 format으로 timezone 정보 제거
        seoulTime.format("yyyy-MM-dd HH:mm:ss") shouldBe "2025-07-10 15:45:30"
        tokyoTime.format("yyyy-MM-dd HH:mm:ss") shouldBe "2025-07-10 15:45:30"
        newYorkTime.format("yyyy-MM-dd HH:mm:ss") shouldBe "2025-07-10 15:45:30"
    }

    "ZonedDateTime edge cases" {
        val midnight = ZonedDateTime.of(2025, 7, 10, 0, 0, 0, 0, ZoneId.of("Asia/Seoul"))
        val noon = ZonedDateTime.of(2025, 7, 10, 12, 0, 0, 0, ZoneId.of("Asia/Seoul"))
        val endOfDay = ZonedDateTime.of(2025, 7, 10, 23, 59, 59, 999999999, ZoneId.of("Asia/Seoul"))
        
        midnight.toString() shouldBe "2025-07-10T00:00:00+09:00"
        noon.toString() shouldBe "2025-07-10T12:00:00+09:00"
        endOfDay.toString() shouldBe "2025-07-10T23:59:59.999999999+09:00"
        
        midnight.format("HH:mm:ss") shouldBe "00:00:00"
        noon.format("HH:mm:ss") shouldBe "12:00:00"
        endOfDay.format("HH:mm:ss") shouldBe "23:59:59"
    }
})