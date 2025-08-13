package io.github.nayasis.kotlin.basica.model.dataframe.helper.exporter

import io.github.nayasis.kotlin.basica.core.localdate.format
import io.github.nayasis.kotlin.basica.core.localdate.toDate
import io.github.nayasis.kotlin.basica.core.localdate.toLocalDateTime
import io.github.nayasis.kotlin.basica.model.dataframe.DataFrame
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

/**
 * XLS (Excel Spreadsheet) Exporter
 *
 * Generates real binary XLS (BIFF8) format without any external library.
 * Supports basic string, number, and date types.
 */
class XlsExporter(
    private val dataframe: DataFrame,
    private val sheetName: String = "Sheet1",
    private val showLabel: Boolean = true,
    startIndex: Int? = null,
) : DataFrameExporter() {

    private val first: Int = startIndex?.takeIf { it >= 0 } ?: dataframe.firstIndex ?: 0
    private val last: Int  = dataframe.lastIndex ?: -1

    override fun export(outputStream: OutputStream) {
        outputStream.use { out ->
            val writer = BiffWriter(out)
            // Workbook Globals
            writer.writeBOF(0x0005) // BOF: Workbook
            writer.writeWINDOW1()
            writer.writeBOUNDSHEET(sheetName)
            writer.writeEOF()
            // Worksheet
            writer.writeBOF(0x0010) // BOF: Worksheet
            writer.writeDIMENSIONS(rowCount = (last - first + 2), colCount = dataframe.keys.size)
            // Header row
            writer.writeRow(0, dataframe.keys.mapIndexed { idx, key ->
                if (showLabel) dataframe.getLabel(key) else key
            })
            // Data rows
            for ((rowIdx, i) in (first..last).withIndex()) {
                val row = dataframe.keys.map { key -> dataframe.getData(i, key) }
                writer.writeRow(rowIdx + 1, row)
            }
            writer.writeEOF()
        }
    }

    private class BiffWriter(val out: OutputStream) {

        init {
            writeBOF()
        }

        private fun writeBOF() {
            // BOF: BIFF8, Worksheet
            writeRecord(0x0809, byteArrayOf(
                0x08, 0x00, // BIFF8
                0x05, 0x00, // Worksheet
                0x00, 0x00, 0x00, 0x00, // Build ID, Year
                0x00, 0x00, 0x00, 0x00  // File history flags, lowest Excel version
            ))
        }

        fun writeEOF() {
            writeRecord(0x000A, byteArrayOf())
        }

        fun writeCell(row: Int, col: Int, value: Any?) {
            when (value) {
                is Number -> writeNumber(row, col, value.toDouble())
                is LocalDate -> writeNumber(row, col, toExcelSerial(value))
                is LocalDateTime -> writeNumber(row, col, toExcelSerial(value))
                is Date -> writeNumber(row, col, toExcelSerial(value.toLocalDateTime()))
                is Calendar -> writeNumber(row, col, toExcelSerial(value.toDate().toLocalDateTime()))
                is ZonedDateTime -> writeLabel(row, col, value.format())
                null -> writeLabel(row, col, "")
                else -> writeLabel(row, col, value.toString())
            }
        }

        fun writeRow(row: Int, values: List<Any?>) {
            for ((col, value) in values.withIndex()) {
                writeCell(row, col, value)
            }
        }

        private fun writeNumber(row: Int, col: Int, value: Double) {
            val buf = ByteBuffer.allocate(14).order(ByteOrder.LITTLE_ENDIAN).apply {
                putShort(row.toShort())
                putShort(col.toShort())
                putShort(0) // XF index
            }
            buf.putDouble(value)
            writeRecord(0x0203, buf.array())
        }

        private fun writeLabel(row: Int, col: Int, value: String) {
            val bytes = value.toByteArray(Charsets.UTF_8)
            val safeLen = safeUtf8Length(bytes, 255)
            val buf = ByteBuffer.allocate(7 + safeLen).order(ByteOrder.LITTLE_ENDIAN).apply {
                putShort(row.toShort())
                putShort(col.toShort())
                putShort(0) // XF index
            }
            buf.put(safeLen.toByte())
            buf.put(bytes, 0, safeLen)
            writeRecord(0x0204, buf.array())
        }

        // Safely cut UTF-8 string to max bytes without breaking multibyte characters
        private fun safeUtf8Length(bytes: ByteArray, max: Int): Int {
            if (bytes.size <= max) return bytes.size
            var len = max
            while (len > 0 && (bytes[len - 1].toInt() and 0xC0) == 0x80) {
                len-- // If in the middle of a multibyte character, cut before it
            }
            return len
        }

        private fun writeRecord(type: Int, data: ByteArray) {
            val header = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).apply {
                putShort(type.toShort())
                putShort(data.size.toShort())
            }
            out.write(header.array())
            out.write(data)
        }

        fun writeBOF(type: Int) {
            // type: 0x0005 = workbook, 0x0010 = worksheet
            writeRecord(0x0809, byteArrayOf(
                0x08, 0x00, // BIFF8
                type.toByte(), 0x00,
                0x00, 0x00, 0x00, 0x00, // Build ID, Year
                0x00, 0x00, 0x00, 0x00  // File history flags, lowest Excel version
            ))
        }
        fun writeWINDOW1() {
            // Default window settings
            val buf = ByteBuffer.allocate(18).order(ByteOrder.LITTLE_ENDIAN)
            buf.putShort(0x258) // horiz pos
            buf.putShort(0x1C4) // vert pos
            buf.putShort(0x3B1) // width
            buf.putShort(0x1D8) // height
            buf.putShort(0) // option flags
            buf.putShort(0) // active sheet
            buf.putShort(1) // first visible tab
            buf.putShort(1) // selected tab count
            buf.putShort(0x258) // tab width
            writeRecord(0x003D, buf.array())
        }
        fun writeBOUNDSHEET(sheetName: String) {
            val nameBytes = sheetName.toByteArray(Charsets.UTF_8)
            val len = nameBytes.size.coerceAtMost(31)
            val buf = ByteBuffer.allocate(8 + len).order(ByteOrder.LITTLE_ENDIAN)
            buf.putInt(0x00000000) // sheet offset (0 for now)
            buf.put(0x00) // sheet type/visibility
            buf.put(len.toByte())
            buf.put(0x00) // compressed unicode
            buf.put(nameBytes, 0, len)
            writeRecord(0x0085, buf.array())
        }
        fun writeDIMENSIONS(rowCount: Int, colCount: Int) {
            val buf = ByteBuffer.allocate(10).order(ByteOrder.LITTLE_ENDIAN)
            buf.putInt(0) // first row
            buf.putInt(rowCount) // last row (exclusive)
            buf.putShort(colCount.toShort()) // first col, last col (exclusive)
            buf.putShort(0) // reserved
            writeRecord(0x0200, buf.array())
        }

        // Convert to Excel serial date value
        private fun toExcelSerial(date: LocalDate): Double {
            return date.toEpochDay() + 25569.0 // based on 1899-12-30
        }

        private fun toExcelSerial(date: LocalDateTime): Double {
            val base = toExcelSerial(date.toLocalDate())
            val seconds = date.toLocalTime().toSecondOfDay()
            return base + seconds / 86400.0
        }

    }
} 