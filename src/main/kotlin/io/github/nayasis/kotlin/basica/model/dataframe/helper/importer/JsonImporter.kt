package io.github.nayasis.kotlin.basica.model.dataframe.helper.importer

import io.github.nayasis.kotlin.basica.model.dataframe.DataFrame
import io.github.nayasis.kotlin.basica.reflection.Reflector
import java.io.InputStream
import java.io.InputStreamReader

class JsonImporter : DataFrameImporter() {
    override fun import(inputStream: InputStream): DataFrame {
        val dataframe = DataFrame()
        InputStreamReader(inputStream).use { reader ->
            var rowIndex = 0
            var insideArray = false
            var braceCount = 0
            val sb = StringBuilder()
            var c: Int
            while (reader.read().also { c = it } != -1) {
                val ch = c.toChar()
                if (!insideArray) {
                    if (ch == '[') {
                        insideArray = true
                    }
                    continue
                }
                if (ch == '{') {
                    braceCount++
                }
                if (braceCount > 0) {
                    sb.append(ch)
                }
                if (ch == '}') {
                    braceCount--
                    if (braceCount == 0) {
                        val map = Reflector.toMap(sb.toString())
                        dataframe.setRow(rowIndex++, map)
                        sb.clear()
                    }
                }
                // close if array ends
                if (ch == ']') break
            }
        }
        return dataframe
    }

} 