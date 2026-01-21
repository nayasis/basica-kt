package io.github.nayasis.kotlin.basica.model.dataframe.helper.exporter

import io.github.nayasis.kotlin.basica.model.dataframe.DataFrame
import java.io.OutputStream


/**
 * ODS exporter
 */
class OdsExporter(
    private val dataframe: DataFrame,
    private val sheetName: String = "Sheet1",
    private val showLabel: Boolean = true,
    startIndex: Int? = null,
): DataFrameExporter() {

    private val exporter = OdsMultiExporter(
        sheets = linkedMapOf(sheetName to dataframe),
        showLabel = showLabel,
        startIndex = startIndex,
    )

    override fun export(outputStream: OutputStream) {
        exporter.export(outputStream)
    }

}