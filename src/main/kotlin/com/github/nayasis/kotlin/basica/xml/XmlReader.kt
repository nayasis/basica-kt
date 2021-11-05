package com.github.nayasis.kotlin.basica.xml

import com.github.nayasis.kotlin.basica.core.klass.Classes
import com.github.nayasis.kotlin.basica.core.path.inputStream
import mu.KotlinLogging
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

private val logger = KotlinLogging.logger {}

/**
 * XML document reader
 */
@Suppress("MemberVisibilityCanBePrivate")
class XmlReader {

    private fun getBuilder(ignoreDtd: Boolean = true): DocumentBuilder {
        val factory = DocumentBuilderFactory.newInstance().apply { if (ignoreDtd) {
            setFeature("http://xml.org/sax/features/namespaces", false);
            setFeature("http://xml.org/sax/features/validation", false);
            setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false)
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        }}
        return factory.newDocumentBuilder()
    }

    fun read(path: Path, ignoreDtd: Boolean = true): Element =
        read(path.inputStream(),ignoreDtd)

    fun read(file: File, ignoreDtd: Boolean = true): Element =
        read(file.inputStream(),ignoreDtd)

    fun read(xml: String, ignoreDtd: Boolean = true): Element =
        read(ByteArrayInputStream(xml.toByteArray()), ignoreDtd)

    fun read(url: URL, ignoreDtd: Boolean = true): Element =
        read(Classes.getResourceStream(url), ignoreDtd)

    fun read(inputStream: InputStream, ignoreDtd: Boolean = true): Element =
        InputStreamReader(inputStream, StandardCharsets.UTF_8.toString()).use { reader ->
            return getBuilder(ignoreDtd).parse(InputSource(reader)).apply { xmlStandalone = true }
                .documentElement
        }

    fun createNew(rootTagName: String, ignoreDtd: Boolean = true): Element {
        val doc = getBuilder(ignoreDtd).newDocument().apply { xmlStandalone = true }
        val root = doc.createElement(rootTagName)
        doc.appendChild(root)
        return root
    }

    fun createDocument(ignoreDtd: Boolean = true): Document =
        getBuilder(ignoreDtd).newDocument().apply { xmlStandalone = true }

}