package com.github.nayasis.kotlin.basica.model.dataframe.helper.exporter

import com.github.nayasis.kotlin.basica.core.io.notExists
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

abstract class DataFrameExporter {

    fun export(filePath: Path) {
        val parent = filePath.parent
        if (parent.notExists()) {
            Files.createDirectories(parent)
        }
        Files.newOutputStream(filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).use { stream ->
            export(stream)
        }
    }

    abstract fun export(outputStream: OutputStream)

}