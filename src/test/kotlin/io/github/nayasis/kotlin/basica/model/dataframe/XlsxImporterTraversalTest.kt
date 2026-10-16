package io.github.nayasis.kotlin.basica.model.dataframe

import io.github.nayasis.kotlin.basica.model.dataframe.helper.importer.XlsxImporter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.system.measureTimeMillis

class XlsxImporterTraversalTest {

    @Test
    fun `preserves rich strings dates booleans sparse rows and duplicate headers`() {
        val result = XlsxImporter(sheetName = "Data").import(fixture().inputStream())
        assertEquals(listOf("name", "amount", "date", "time", "enabled", "text", "literal", "name_1"), result.keys.toList())
        assertEquals(null, result.getData(0, "name"))
        assertEquals("Hello & World", result.getData(1, "name"))
        assertEquals(1500.0, result.getData(1, "amount"))
        assertEquals(LocalDate.of(2020, 1, 1), result.getData(1, "date"))
        assertEquals(LocalDateTime.of(2020, 1, 1, 12, 0), result.getData(1, "time"))
        assertEquals(true, result.getData(1, "enabled"))
        assertEquals("001", result.getData(1, "text"))
        assertEquals("0x4000", result.getData(1, "literal"))
        assertEquals(4, XlsxImporter(sheetName = "Data", lastColumnIndex = 3).import(fixture().inputStream()).keys.size)
    }

    @Test
    fun `supports namespace-prefixed workbook styles shared strings and cells`() {
        val result = XlsxImporter(sheetName = "Data").import(fixture(prefixed = true).inputStream())
        assertEquals("Hello & World", result.getData(1, "name"))
        assertEquals(LocalDate.of(2020, 1, 1), result.getData(1, "date"))
        assertEquals(LocalDateTime.of(2020, 1, 1, 12, 0), result.getData(1, "time"))
    }

    @Test
    fun `supports inline rich text and headerless sparse coordinates`() {
        val result = XlsxImporter(firstRowAsHeader = false).import(fixture(inline = true).inputStream())
        assertEquals("name", result.getData(0, 0))
        assertEquals("Hello & World", result.getData(2, 0))
        assertEquals(1500.0, result.getData(2, 1))
    }

    @Test
    fun `excludes phonetic hints from shared and inline strings by default`() {
        for (prefixed in listOf(false, true)) {
            val result = XlsxImporter(firstRowAsHeader = false).import(phoneticFixture(prefixed).inputStream())
            for (column in 0..5) {
                assertEquals(if (column % 3 == 2) "Plain & text" else "東京", result.getData(0, column))
            }
        }
    }

    @Test
    fun `appends phonetic hints on one new line only when present and enabled`() {
        for (prefixed in listOf(false, true)) {
            val importers = listOf(
                XlsxImporter(firstRowAsHeader = false, includePhonetics = true),
                XlsxImporter(sheetName = "Data", firstRowAsHeader = false, includePhonetics = true),
            )
            for (importer in importers) {
                val result = importer.import(phoneticFixture(prefixed).inputStream())
                for (column in 0..5) {
                    assertEquals(if (column % 3 == 2) "Plain & text" else "東京\nトウキョウ", result.getData(0, column))
                }
            }
        }
    }

    @Test
    fun `applies phonetic option to headers`() {
        val input = phoneticFixture()
        assertEquals("東京", XlsxImporter().import(input.inputStream()).keys.first())
        assertEquals("東京\nトウキョウ", XlsxImporter(includePhonetics = true).import(input.inputStream()).keys.first())
    }

    @Test
    fun `forwards phonetic option through fromXlsx overloads`(@TempDir directory: Path) {
        val path = directory.resolve("phonetics.xlsx")
        Files.write(path, phoneticFixture())
        assertEquals("東京", DataFrame().fromXlsx(path, firstRowAsHeader = false).getData(0, 0))
        assertEquals("東京", DataFrame().fromXlsx(path, sheetName = "Data", firstRowAsHeader = false).getData(0, 0))
        assertEquals("東京\nトウキョウ", DataFrame().fromXlsx(path, firstRowAsHeader = false, includePhonetics = true).getData(0, 0))
        assertEquals("東京\nトウキョウ", DataFrame().fromXlsx(path, sheetName = "Data", firstRowAsHeader = false, includePhonetics = true).getData(0, 0))
    }

    @Test
    fun `imports a wide worksheet without per-cell XPath evaluation`() {
        val rows = 100
        val columns = 120
        val xml = buildString {
            append("<worksheet xmlns=\"$NS\"><sheetData>")
            repeat(rows) { row ->
                append("<row r=\"${row + 1}\">")
                repeat(columns) { column -> append("<c r=\"${address(column)}${row + 1}\" t=\"s\"><v>0</v></c>") }
                append("</row>")
            }
            append("</sheetData></worksheet>")
        }
        val input = archive(mapOf("xl/sharedStrings.xml" to "<sst xmlns=\"$NS\"><si><t>value</t></si></sst>", "xl/worksheets/sheet1.xml" to xml))
        lateinit var result: DataFrame
        val elapsed = measureTimeMillis { result = XlsxImporter(firstRowAsHeader = false).import(input.inputStream()) }
        assertEquals(rows, result.size)
        assertEquals(columns, result.keys.size)
        assertEquals("value", result.getData(rows - 1, columns - 1))
        println("XLSX-TRAVERSAL: rows=$rows, columns=$columns, elapsedMs=$elapsed")
    }

    private fun fixture(prefixed: Boolean = false, inline: Boolean = false): ByteArray {
        val value = if(inline) "<c r=\"A5\" t=\"inlineStr\"><is><r><t>Hello </t></r><r><t>&amp; World</t></r></is></c>"
            else "<c r=\"A5\" t=\"s\"><v>0</v></c>"
        val entries = linkedMapOf(
            "xl/workbook.xml" to "<workbook xmlns=\"$NS\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Data\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>",
            "xl/_rels/workbook.xml.rels" to "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Target=\"worksheets/sheet1.xml\"/></Relationships>",
            "xl/styles.xml" to "<styleSheet xmlns=\"$NS\"><numFmts><numFmt numFmtId=\"164\" formatCode=\"yyyy-mm-dd\"/><numFmt numFmtId=\"165\" formatCode=\"yyyy-mm-dd hh:mm:ss\"/></numFmts><cellXfs><xf numFmtId=\"0\"/><xf numFmtId=\"164\"/><xf numFmtId=\"165\"/></cellXfs></styleSheet>",
            "xl/sharedStrings.xml" to "<sst xmlns=\"$NS\"><si><r><t>Hello </t></r><r><t>&amp; World</t></r></si><si><t>0x4000</t></si></sst>",
            "xl/worksheets/sheet1.xml" to """
                <worksheet xmlns="$NS"><sheetData>
                  <row r="3"><c r="A3" t="str"><v>name</v></c><c r="B3" t="str"><v>amount</v></c><c r="C3" t="str"><v>date</v></c><c r="D3" t="str"><v>time</v></c><c r="E3" t="str"><v>enabled</v></c><c r="F3" t="str"><v>text</v></c><c r="G3" t="str"><v>literal</v></c><c r="H3" t="str"><v>name</v></c></row>
                  <row r="5">$value<c r="B5"><v>1.5e3</v></c><c r="C5" s="1"><v>43831</v></c><c r="D5" s="2"><v>43831.5</v></c><c r="E5" t="b"><v>1</v></c><c r="F5" t="str"><v>001</v></c><c r="G5" t="s"><v>1</v></c><c r="H5"/></row>
                </sheetData></worksheet>
            """.trimIndent(),
        )
        return archive(entries.mapValues { (_, xml) ->
            if(!prefixed) xml else xml.replace("xmlns=", "xmlns:x=")
                .replace(Regex("<(/?)([A-Za-z][A-Za-z0-9]*)(?=[\\s/>])"), "<$1x:$2")
        })
    }

    private fun archive(entries: Map<String, String>): ByteArray = ByteArrayOutputStream().also { output ->
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, xml) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(xml.toByteArray())
                zip.closeEntry()
            }
        }
    }.toByteArray()

    private fun phoneticFixture(prefixed: Boolean = false): ByteArray {
        val hints = "<rPh sb=\"0\" eb=\"1\"><t>トウ</t></rPh><rPh sb=\"1\" eb=\"2\"><t>キョウ</t></rPh>"
        val strings = listOf("<t>東京</t>$hints", "<r><t>東</t></r><r><t>京</t></r>$hints", "<t>Plain &amp; text</t>")
        val entries = mapOf(
            "xl/workbook.xml" to "<workbook xmlns=\"$NS\"><sheets><sheet name=\"Data\" sheetId=\"1\"/></sheets></workbook>",
            "xl/sharedStrings.xml" to "<sst xmlns=\"$NS\">${strings.joinToString("") { "<si>$it</si>" }}</sst>",
            "xl/worksheets/sheet1.xml" to "<worksheet xmlns=\"$NS\"><sheetData><row r=\"1\">" +
                strings.indices.joinToString("") { "<c r=\"${address(it)}1\" t=\"s\"><v>$it</v></c>" } +
                strings.mapIndexed { index, text -> "<c r=\"${address(index + 3)}1\" t=\"inlineStr\"><is>$text</is></c>" }.joinToString("") +
                "</row></sheetData></worksheet>",
        )
        return archive(entries.mapValues { (_, xml) ->
            if (!prefixed) xml else xml.replace("xmlns=", "xmlns:x=")
                .replace(Regex("<(/?)([A-Za-z][A-Za-z0-9]*)(?=[\\s/>])"), "<$1x:$2")
        })
    }

    private fun address(column: Int): String = if(column < 26) ('A' + column).toString()
        else address(column / 26 - 1) + ('A' + column % 26)

    companion object {
        private const val NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    }
}
