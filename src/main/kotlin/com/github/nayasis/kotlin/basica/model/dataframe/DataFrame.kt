package com.github.nayasis.kotlin.basica.model.dataframe

import com.github.nayasis.kotlin.basica.reflection.Reflector
import java.io.Serializable

class DataFrame(
    private val body: Columns = Columns()
): Serializable, Cloneable, Iterable<Map<String, Any?>> {

    val lastIndex: Int?
        get() = body.values.mapNotNull { it.lastIndex }.maxOrNull()

    val firstIndex: Int?
        get() = body.values.mapNotNull { it.firstIndex }.minOrNull()

    val size: Int
        get() = lastIndex?.inc()?.minus( firstIndex ?: 0 ) ?: 0

    fun isEmpty(): Boolean {
        return size == 0
    }

    fun isRowEmpty(row: Int): Boolean {
        return body.values.all { it[row] == null }
    }

    fun setLabel(key: String, label: String) {
        body.setLabel(key, label)
    }

    fun getLabel(key: String): String {
        return body.getLabel(key)
    }

    val keys: Set<String>
        get() = body.keys

    val values: Collection<Column>
        get() = body.values

    val labels: List<String>
        get() = body.keys.map { body.getLabel(it) }

    fun addKey(key: String) {
        if (body[key] == null) {
            body[key] = Column()
        }
    }

    fun removeKey(key: String) {
        body.remove(key)
    }

    fun getColumn(key: String): Column {
        return body[key] ?: throw NoSuchElementException("No column found for key $key")
    }

    fun getColumn(index: Int): Column {
        return body.getColumnBy(index) ?: throw NoSuchElementException("No column found for index[$index]")
    }

    fun getData(row: Int, col: Int): Any? {
        val key = body.getKeyBy(col) ?: throw NoSuchElementException("No column found for index[$col]")
        return body[key]?.get(row)
    }

    fun getData(row: Int, key: String): Any? {
        return body[key]?.get(row)
    }

    fun setData(row: Int, col: Int, value: Any?) {
        val key = body.getKeyBy(col) ?: throw NoSuchElementException("No column found for index[$col]")
        body[key]?.set(row, value)
    }

    fun setData(row: Int, key: String, value: Any?) {
        addKey(key)
        body[key]!![row] = value
    }

    fun removeData(row: Int, col: Int) {
        val key = body.getKeyBy(col) ?: throw NoSuchElementException("No column found for index[$col]")
        body[key]?.remove(row)
    }

    fun removeData(row: Int, key: String) {
        (body[key]?: throw NoSuchElementException("No column found for key $key")).remove(row)
    }

    fun setRow(row: Int, value: Any?) {
        when(value) {
            null -> {
                body.forEach { (_, column) ->
                    column.set(row, null)
                }
            }
            is Map<*, *> -> {
                for ((key, v) in value) {
                    setData(row, "$key", v)
                }
            }
            is Collection<*> -> {
                var start = row
                value.forEach { setRow(start++, it) }
            }
            is Array<*> -> {
                var start = row
                value.forEach { setRow(start++, it) }
            }
            is CharSequence -> {
                val json = value.toString()
                try {
                    setRow(row, Reflector.toObject<ArrayList<Map<String, Any?>>>(json))
                } catch (e: Exception) {
                    setRow(row, Reflector.toMap(json))
                }
            }
            else -> setRow(row, Reflector.toMap(value))
        }
    }

    fun addRow(value: Any?) {
        setRow(size, value)
    }

    fun addRows(values: Iterable<Any?>) {
        var index = size
        values.forEach { setRow(index++, it) }
    }

    fun getRow(row: Int): Map<String, Any?> {
        return body.mapValues { (_, column) -> column[row] }
    }

    fun removeRow(row: Int) {
        body.forEach { (_, column) ->
            column.remove(row)
        }
    }

    fun clear() {
        body.clear()
    }

    override fun toString(): String {
        return toString(showHeader = true)
    }

    fun toString(
        showHeader: Boolean = true,
        showIndex: Boolean = false,
        showLabel: Boolean = true,
        startRow: Int = firstIndex ?: 0,
        endRow: Int = lastIndex ?: 0,
        maxColumnWidth: Int = 50,
    ): String {
        return DataframePrinter(
            this,
            showHeader,
            showIndex,
            showLabel,
            startRow,
            endRow,
            maxColumnWidth.toDouble(),
        ).toString()
    }

    fun head(
        n: Int = 10,
        showHeader: Boolean = true,
        showIndex: Boolean = false,
        showLabel: Boolean = true,
        maxColumnWidth: Int = 50,
    ): String {
        return toString(
            showHeader = showHeader,
            showIndex = showIndex,
            showLabel = showLabel,
            startRow = firstIndex ?: 0,
            endRow = (firstIndex ?: 0) + n - 1,
            maxColumnWidth = maxColumnWidth
        )
   }

    fun tail(
        n: Int = 10,
        showHeader: Boolean = true,
        showIndex: Boolean = false,
        showLabel: Boolean = true,
        maxColumnWidth: Int = 50,
    ): String {
        return toString(
            showHeader = showHeader,
            showIndex = showIndex,
            showLabel = showLabel,
            startRow = (lastIndex ?: 0) - n + 1,
            endRow = lastIndex ?: 0,
            maxColumnWidth = maxColumnWidth
        )
    }

    public override fun clone(): DataFrame {
        return DataFrame(body = body.clone())
    }

    override fun iterator(): Iterator<Map<String, Any?>> {
        return object: Iterator<Map<String,Any?>> {
            private val size = this@DataFrame.size
            private var i    = firstIndex ?: 0
            private val end  = lastIndex ?: -1
            override fun hasNext(): Boolean = i <= end
            override fun next(): Map<String,Any?> = getRow(i++)
        }
    }

//    // ===== Export Methods =====
//
//    /**
//     * DataFrame을 CSV 파일로 내보냅니다.
//     */
//    fun exportToCsv(filePath: Path, delimiter: Char = ',', hasQuotes: Boolean = true) {
//        CsvExporter(delimiter, hasQuotes).export(this, filePath)
//    }
//
//    /**
//     * DataFrame을 CSV 형식의 문자열로 내보냅니다.
//     */
//    fun exportToCsvString(delimiter: Char = ',', hasQuotes: Boolean = true): String {
//        return CsvExporter(delimiter, hasQuotes).exportToString(this)
//    }
//
//    /**
//     * DataFrame을 JSON 파일로 내보냅니다.
//     */
//    fun exportToJson(filePath: Path, prettyPrint: Boolean = false) {
//        JsonExporter(prettyPrint).export(this, filePath)
//    }
//
//    /**
//     * DataFrame을 JSON 형식의 문자열로 내보냅니다.
//     */
//    fun exportToJsonString(prettyPrint: Boolean = false): String {
//        return JsonExporter(prettyPrint).exportToString(this)
//    }
//
//    /**
//     * DataFrame을 XLSX 파일로 내보냅니다.
//     */
//    fun exportToXlsx(filePath: Path, sheetName: String = "Sheet1") {
//        XlsxExporter(sheetName).export(this, filePath)
//    }
//
//    /**
//     * DataFrame을 ODS 파일로 내보냅니다.
//     */
//    fun exportToOds(filePath: Path, sheetName: String = "Sheet1") {
//        OdsExporter(sheetName).export(this, filePath)
//    }
//
//    /**
//     * DataFrame을 OutputStream으로 내보냅니다.
//     */
//    fun exportToStream(outputStream: OutputStream, format: String, options: Map<String, Any> = emptyMap()) {
//        when (format.lowercase()) {
//            "csv" -> {
//                val delimiter = options["delimiter"] as? Char ?: ','
//                val hasQuotes = options["hasQuotes"] as? Boolean ?: true
//                CsvExporter(delimiter, hasQuotes).export(this, outputStream)
//            }
//            "json" -> {
//                val prettyPrint = options["prettyPrint"] as? Boolean ?: false
//                JsonExporter(prettyPrint).export(this, outputStream)
//            }
//            "xlsx" -> {
//                val sheetName = options["sheetName"] as? String ?: "Sheet1"
//                XlsxExporter(sheetName).export(this, outputStream)
//            }
//            "ods" -> {
//                val sheetName = options["sheetName"] as? String ?: "Sheet1"
//                OdsExporter(sheetName).export(this, outputStream)
//            }
//            else -> throw IllegalArgumentException("Unsupported format: $format")
//        }
//    }
//
//    // ===== Import Methods =====
//
//    companion object {
//        /**
//         * CSV 파일에서 DataFrame을 가져옵니다.
//         */
//        fun fromCsv(filePath: Path, delimiter: Char = ',', hasQuotes: Boolean = true): DataFrame {
//            return CsvImporter(delimiter, hasQuotes).import(filePath)
//        }
//
//        /**
//         * CSV 문자열에서 DataFrame을 가져옵니다.
//         */
//        fun fromCsvString(content: String, delimiter: Char = ',', hasQuotes: Boolean = true): DataFrame {
//            return CsvImporter(delimiter, hasQuotes).importFromString(content)
//        }
//
//        /**
//         * JSON 파일에서 DataFrame을 가져옵니다.
//         */
//        fun fromJson(filePath: Path): DataFrame {
//            return JsonImporter().import(filePath)
//        }
//
//        /**
//         * JSON 문자열에서 DataFrame을 가져옵니다.
//         */
//        fun fromJsonString(content: String): DataFrame {
//            return JsonImporter().importFromString(content)
//        }
//
//        /**
//         * XLSX 파일에서 DataFrame을 가져옵니다.
//         */
//        fun fromXlsx(filePath: Path, sheetIndex: Int = 0, hasHeader: Boolean = true): DataFrame {
//            return XlsxImporter(sheetIndex, hasHeader).import(filePath)
//        }
//
//        /**
//         * ODS 파일에서 DataFrame을 가져옵니다.
//         */
//        fun fromOds(filePath: Path, sheetIndex: Int = 0, hasHeader: Boolean = true): DataFrame {
//            return OdsImporter(sheetIndex, hasHeader).import(filePath)
//        }
//
//        /**
//         * InputStream에서 DataFrame을 가져옵니다.
//         */
//        fun fromStream(inputStream: java.io.InputStream, format: String, options: Map<String, Any> = emptyMap()): DataFrame {
//            return when (format.lowercase()) {
//                "csv" -> {
//                    val delimiter = options["delimiter"] as? Char ?: ','
//                    val hasQuotes = options["hasQuotes"] as? Boolean ?: true
//                    CsvImporter(delimiter, hasQuotes).import(inputStream)
//                }
//                "json" -> {
//                    JsonImporter().import(inputStream)
//                }
//                "xlsx" -> {
//                    val sheetIndex = options["sheetIndex"] as? Int ?: 0
//                    val hasHeader = options["hasHeader"] as? Boolean ?: true
//                    XlsxImporter(sheetIndex, hasHeader).import(inputStream)
//                }
//                "ods" -> {
//                    val sheetIndex = options["sheetIndex"] as? Int ?: 0
//                    val hasHeader = options["hasHeader"] as? Boolean ?: true
//                    OdsImporter(sheetIndex, hasHeader).import(inputStream)
//                }
//                else -> throw IllegalArgumentException("Unsupported format: $format")
//            }
//        }
//    }

}