package io.github.nayasis.kotlin.basica.model.dataframe

import io.github.nayasis.kotlin.basica.model.dataframe.helper.exporter.*
import io.github.nayasis.kotlin.basica.model.dataframe.helper.importer.*
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.nio.file.Path

/**
 * Loads DataFrame from CSV file.
 *
 * @param filePath CSV file path
 * @param charset character encoding
 * @param delimiter delimiter character
 * @param firstRowAsHeader whether to include header
 * @return DataFrame object
 */
fun DataFrame.fromCsv(
    filePath: Path,
    charset: Charset = Charsets.UTF_8,
    delimiter: Char = ',',
    firstRowAsHeader: Boolean = true
): DataFrame {
    return CsvImporter(delimiter, firstRowAsHeader, charset).import(filePath)
}

/**
 * Loads DataFrame from CSV InputStream.
 * @param inputStream CSV input stream
 * @param charset character encoding
 * @param delimiter delimiter character
 * @param firstRowAsHeader whether to include header
 * @return DataFrame object
 */
fun DataFrame.fromCsv(
    inputStream: InputStream,
    charset: Charset = Charsets.UTF_8,
    delimiter: Char = ',',
    firstRowAsHeader: Boolean = true
): DataFrame {
    return CsvImporter(delimiter, firstRowAsHeader, charset).import(inputStream)
}

/**
 * Saves DataFrame to CSV file.
 * @param filePath file path to save
 * @param charset character encoding
 * @param delimiter delimiter character
 * @param includeHeader whether to include header
 */
fun DataFrame.toCsv(
    filePath: Path,
    charset: Charset = Charsets.UTF_8,
    delimiter: Char = ',',
    includeHeader: Boolean = true
) {
    CsvExporter(this, delimiter.toString(), includeHeader, charset).export(filePath)
}

/**
 * Saves DataFrame to CSV OutputStream.
 * @param outputStream CSV output stream
 * @param charset character encoding
 * @param delimiter delimiter character
 * @param includeHeader whether to include header
 */
fun DataFrame.toCsv(
    outputStream: OutputStream,
    charset: Charset = Charsets.UTF_8,
    delimiter: Char = ',',
    includeHeader: Boolean = true
) {
    CsvExporter(this, delimiter.toString(), includeHeader, charset).export(outputStream)
}

// ==================== JSON Extension Functions ====================

/**
 * Loads DataFrame from JSON file.
 * @param filePath JSON file path
 * @param charset character encoding
 * @return DataFrame object
 */
fun DataFrame.fromJson(
    filePath: Path,
): DataFrame {
    return JsonImporter().import(filePath)
}

/**
 * Loads DataFrame from JSON InputStream.
 * @param inputStream JSON input stream
 * @param charset character encoding
 * @return DataFrame object
 */
fun DataFrame.fromJson(
    inputStream: InputStream,
): DataFrame {
    return JsonImporter().import(inputStream)
}

/**
 * Saves DataFrame to JSON file.
 * @param filePath file path to save
 * @param charset character encoding
 * @param prettyPrint whether to format output nicely
 */
fun DataFrame.toJson(
    filePath: Path,
    charset: Charset = Charsets.UTF_8,
    prettyPrint: Boolean = true
) {
    JsonExporter(this, prettyPrint).export(filePath)
}

/**
 * Saves DataFrame to JSON OutputStream.
 * @param outputStream JSON output stream
 * @param charset character encoding
 * @param prettyPrint whether to format output nicely
 */
fun DataFrame.toJson(
    outputStream: OutputStream,
    charset: Charset = Charsets.UTF_8,
    prettyPrint: Boolean = true
) {
    JsonExporter(this, prettyPrint).export(outputStream)
}

// ==================== Excel Extension Functions ====================

/**
 * Loads DataFrame from Excel file (XLSX).
 * @param filePath Excel file path
 * @param sheetIndex sheet index
 * @param firstRowAsHeader whether to include header
 * @return DataFrame object
 */
fun DataFrame.fromXlsx(
    filePath: Path,
    sheetIndex: Int = 0,
    firstRowAsHeader: Boolean = true
): DataFrame {
    return XlsxImporter(sheetIndex, firstRowAsHeader).import(filePath)
}

/**
 * Saves DataFrame to Excel file (XLSX).
 *
 * @param filePath file path to save
 * @param sheetName sheet name (default: "Sheet1")
 */
fun DataFrame.toXlsx(
    filePath: Path,
    sheetName: String = "Sheet1"
) {
    XlsxExporter(this, sheetName).export(filePath)
}



// ==================== ODS Extension Functions ====================

/**
 * Loads DataFrame from ODS file.
 *
 * @param filePath ODS file path
 * @param sheetIndex sheet index
 * @param firstRowAsHeader whether to include header
 * @return DataFrame object
 */
fun DataFrame.fromOds(
    filePath: Path,
    sheetIndex: Int = 0,
    firstRowAsHeader: Boolean = true
): DataFrame {
    return OdsImporter(sheetIndex, firstRowAsHeader).import(filePath)
}

/**
 * Loads DataFrame from ODS InputStream.
 *
 * @param inputStream ODS input stream
 * @param sheetIndex sheet index
 * @param firstRowAsHeader whether to include header
 * @return DataFrame object
 */
fun DataFrame.fromOds(
    inputStream: InputStream,
    sheetIndex: Int = 0,
    firstRowAsHeader: Boolean = true
): DataFrame {
    return OdsImporter(sheetIndex, firstRowAsHeader).import(inputStream)
}

/**
 * Saves DataFrame to ODS file.
 *
 * @param filePath file path to save
 * @param sheetName sheet name (default: "Sheet1")
 */
fun DataFrame.toOds(
    filePath: Path,
    sheetName: String = "Sheet1"
) {
    OdsExporter(this, sheetName).export(filePath)
}

/**
 * Saves DataFrame to ODS file.
 *
 * @param outputStream ODS output stream
 * @param sheetName sheet name (default: "Sheet1")
 */
fun DataFrame.toOds(
    outputStream: OutputStream,
    sheetName: String = "Sheet1"
) {
    OdsExporter(this, sheetName).export(outputStream)
}

// ==================== HTML Extension Functions ====================

/**
 * Saves DataFrame to HTML file.
 * @param filePath file path to save
 * @param title HTML title (default: "DataFrame")
 */
fun DataFrame.toHtml(
    filePath: Path,
    title: String = "DataFrame"
) {
    HtmlXlsExporter(this, title).export(filePath)
}

/**
 * Saves DataFrame to HTML OutputStream.
 * @param outputStream HTML output stream
 * @param title HTML title (default: "DataFrame")
 */
fun DataFrame.toHtml(
    outputStream: OutputStream,
    title: String = "DataFrame"
) {
    HtmlXlsExporter(this, title).export(outputStream)
}