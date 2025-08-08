package com.github.nayasis.kotlin.basica.model.dataframe

import com.github.nayasis.kotlin.basica.core.character.Characters
import com.github.nayasis.kotlin.basica.core.extension.ifNull
import com.github.nayasis.kotlin.basica.core.io.Paths
import com.github.nayasis.kotlin.basica.core.io.delete
import com.github.nayasis.kotlin.basica.core.localdate.*
import com.github.nayasis.kotlin.basica.core.validator.cast
import com.github.nayasis.kotlin.basica.model.dataframe.helper.exporter.CsvExporter
import com.github.nayasis.kotlin.basica.model.dataframe.helper.exporter.JsonExporter
import com.github.nayasis.kotlin.basica.model.dataframe.helper.exporter.OdsExporter
import com.github.nayasis.kotlin.basica.model.dataframe.helper.exporter.XlsxExporter
import com.github.nayasis.kotlin.basica.model.dataframe.helper.importer.CsvImporter
import com.github.nayasis.kotlin.basica.model.dataframe.helper.importer.JsonImporter
import com.github.nayasis.kotlin.basica.model.dataframe.helper.importer.OdsImporter
import com.github.nayasis.kotlin.basica.model.dataframe.helper.importer.XlsxImporter
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

private val logger = KotlinLogging.logger {}

internal class XlsxExporterTest : StringSpec({

    val testDir = Paths.userHome.resolve("basica-test/dataframe")

    Characters.fullwidth = 2.0

    afterSpec {
        testDir.delete()
    }

    "create basic XLSX file" {
        val filePath = testDir.resolve("text.xlsx")
        val src = createTestDataframe().also { logger.debug { "\n${it.toString(showIndex = true)}" } }
            .also { XlsxExporter(it).export(filePath) }
        val trg = XlsxImporter().import(filePath).also { logger.debug { "\n${it.toString(showIndex = true)}" } }

        for(row in src.firstIndex.ifNull{0} until src.lastIndex.ifNull{-1}) {
            src.getData(row, 0) shouldBe trg.getData(row, 0)
        }

        src.getData(10, 1) shouldBe trg.getData(10, 1)
        src.getData(11, 1) shouldBe trg.getData(11, 1)
        src.getData(12, 1) shouldBe trg.getData(12, 1)
        src.getData(13, 1) shouldBe trg.getData(13, 1)
        src.getData(14, 1) shouldBe trg.getData(14, 1)
        src.getData(15, 1) shouldBe trg.getData(15, 1)
        src.getData(16, 1) shouldBe trg.getData(16, 1)?.cast<LocalDateTime>()?.toDate()
        src.getData(17, 1) shouldBe trg.getData(17, 1)?.cast<LocalDateTime>()?.toDate()
        src.getData(18, 1)?.cast<Calendar>()?.toLocalDateTime() shouldBe trg.getData(18, 1)
        src.getData(19, 1)?.cast<ZonedDateTime>()?.toLocalDateTime() shouldBe trg.getData(19, 1)?.cast<LocalDateTime>()
    }

    "create basic CSV file" {
        val filePath = testDir.resolve("text.csv")
        val src = createTestDataframe().also { logger.debug { "\n${it.toString(showIndex = true)}" } }
            .also { CsvExporter(it).export(filePath) }
        val trg = CsvImporter().import(filePath).also { logger.debug { "\n${it.toString(showIndex = true)}" } }

        for(row in src.firstIndex.ifNull{0} until src.lastIndex.ifNull{-1}) {
            src.getData(row, 0).testString shouldBe trg.getData(row, 0).testString
            src.getData(row, 1).testString shouldBe trg.getData(row, 1).testString
        }
    }

    "create basic JSON file" {
        val filePath = testDir.resolve("text.json")
        val src = createTestDataframe().also { logger.debug { "\n${it.toString(showIndex = true)}" } }
            .also { JsonExporter(it).export(filePath) }
        val trg = JsonImporter().import(filePath).also { logger.debug { "\n${it.toString(showIndex = true)}" } }

        for(row in src.firstIndex.ifNull{0} until src.lastIndex.ifNull{-1}) {
            src.getData(row, 0) shouldBe trg.getData(row, 0)
        }

        trg.getData(10, 1) shouldBe src.getData(10, 1)
        trg.getData(11, 1) shouldBe src.getData(11, 1)
        trg.getData(12, 1) shouldBe src.getData(12, 1)
        trg.getData(13, 1) shouldBe src.getData(13, 1)
        trg.getData(14, 1).testString shouldBe src.getData(14, 1).testString
        trg.getData(15, 1).testString shouldBe src.getData(15, 1).testString
        trg.getData(16, 1) shouldBe src.getData(16, 1).testString
        trg.getData(17, 1) shouldBe src.getData(17, 1).testString
        trg.getData(18, 1).toString().let { it.substring(0, it.indexOf("+")) } shouldBe src.getData(18, 1).testString
        trg.getData(19, 1).toString().toZonedDateTime() shouldBe src.getData(19, 1)

    }

    "create basic ODS file" {
        val filePath = testDir.resolve("text.ods")
        val src = createTestDataframe().also { logger.debug { "\n${it.toString(showIndex = true)}" } }
            .also { OdsExporter(it).export(filePath) }
        val trg = OdsImporter().import(filePath).also { logger.debug { "\n${it.toString(showIndex = true)}" } }

        for(row in src.firstIndex.ifNull{0} until src.lastIndex.ifNull{-1}) {
            src.getData(row, 0) shouldBe trg.getData(row, 0)
        }

        src.getData(10, 1) shouldBe trg.getData(10, 1)
        src.getData(11, 1) shouldBe trg.getData(11, 1)
        src.getData(12, 1) shouldBe trg.getData(12, 1)
        src.getData(13, 1) shouldBe trg.getData(13, 1)
        src.getData(14, 1) shouldBe trg.getData(14, 1)
        src.getData(15, 1) shouldBe trg.getData(15, 1)
        src.getData(16, 1) shouldBe trg.getData(16, 1)?.cast<LocalDateTime>()?.toDate()
        src.getData(17, 1) shouldBe trg.getData(17, 1)?.cast<LocalDateTime>()?.toDate()
        src.getData(18, 1)?.cast<Calendar>()?.toLocalDateTime() shouldBe trg.getData(18, 1)
        src.getData(19, 1) shouldBe trg.getData(19, 1)
    }

    "no header import" {
        val filePath = testDir.resolve("text.xlsx")
        @Suppress("UnusedVariable")
        val src = createTestDataframe().also { logger.debug { "\n${it.toString(showIndex = true)}" } }
            .also { XlsxExporter(it).export(filePath) }
        val trg = XlsxImporter(firstRowAsHeader = false).import(filePath).also { logger.debug { "\n${it.toString(showIndex = true)}" } }

        trg.keys shouldBe setOf("0","1")
        trg.size shouldBe 20
    }

})

private fun createTestDataframe(): DataFrame {
    return DataFrame().apply {
        setRow(10, mapOf("key" to "controller", "val" to "컨트롤러는 이런 것입니다."))
        setRow(12, mapOf("key" to 1, "val" to 3359))
        setRow(13, mapOf("key" to 2, "val" to 101.23459))
        setRow(14, mapOf("key" to "LocalDate", "val" to "2025-07-10".toLocalDate()))
        setRow(15, mapOf("key" to "날짜시간", "val" to "2025-07-10".toLocalDateTime()))
        setRow(16, mapOf("key" to "date", "val" to "2025-07-10".toDate()))
        setRow(17, mapOf("key" to "날짜시간", "val" to "2025-07-10 11:23:49".toDate()))
        setRow(18, mapOf("key" to "Calendar", "val" to "2025-07-10 14:30:25".toCalendar()))
        setRow(19, mapOf("key" to "ZonedDateTime", "val" to "2025-07-10T15:45:30+09:00".toZonedDateTime()))
        setLabel("key", "이것은 KEY 입니다.")
        setLabel("val", "これは VALUE です")
    }
}

private val Any?.testString: String
    get() = when(this) {
        is LocalDate -> this.format()
        is LocalDateTime -> this.format()
        is ZonedDateTime -> this.format()
        is Date -> this.format()
        is Calendar -> this.toDate().format()
        else -> this.toString()
    }