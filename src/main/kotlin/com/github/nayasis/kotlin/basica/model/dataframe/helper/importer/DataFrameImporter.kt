package com.github.nayasis.kotlin.basica.model.dataframe.helper.importer

import com.github.nayasis.kotlin.basica.model.dataframe.DataFrame
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

abstract class DataFrameImporter {

    open fun import(filePath: Path): DataFrame {
        Files.newInputStream(filePath, StandardOpenOption.READ).use { stream ->
            return import(stream)
        }
    }

    abstract fun import(inputStream: InputStream): DataFrame
} 