package com.github.nayasis.kotlin.basica.model.dataframe.helper

import org.w3c.dom.Document
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

fun ZipOutputStream.write(doc: Document) {
    TransformerFactory.newInstance().newTransformer().apply {
        setOutputProperty(OutputKeys.INDENT, "yes")
        setOutputProperty(OutputKeys.ENCODING, "UTF-8")
        setOutputProperty(OutputKeys.STANDALONE, "yes")
    }.transform(DOMSource(doc), StreamResult(this))
}

fun ZipOutputStream.writeEntry(name: String, content: String) {
    putNextEntry(ZipEntry(name))
    write(content.toByteArray())
    closeEntry()
}

fun ZipOutputStream.writeEntry(entry: ZipEntry, content: String) {
    putNextEntry(entry)
    write(content.toByteArray())
    closeEntry()
}

fun ZipOutputStream.writeEntry(name: String, content: Document) {
    putNextEntry(ZipEntry(name))
    write(content)
    closeEntry()
}