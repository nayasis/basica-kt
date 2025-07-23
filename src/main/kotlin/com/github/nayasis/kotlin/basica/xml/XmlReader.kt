package com.github.nayasis.kotlin.basica.xml

import com.github.nayasis.kotlin.basica.core.klass.Classes
import com.github.nayasis.kotlin.basica.core.io.inputStream
import io.github.oshai.kotlinlogging.KotlinLogging
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

private val logger = KotlinLogging.logger {}

/**
 * XML document reader
 */
class XmlReader {

    companion object {
        private fun getBuilder(ignoreDtd: Boolean = true): DocumentBuilder {
            val factory = DocumentBuilderFactory.newInstance().apply { if (ignoreDtd) {
                setFeature("http://xml.org/sax/features/namespaces", false);
                setFeature("http://xml.org/sax/features/validation", false);
                setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false)
                setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            }}
            return factory.newDocumentBuilder()
        }

        fun read(path: Path, charset: Charset = StandardCharsets.UTF_8, ignoreDtd: Boolean = true): Element =
            read(path.inputStream(), charset, ignoreDtd)

        fun read(file: File, charset: Charset = StandardCharsets.UTF_8, ignoreDtd: Boolean = true): Element =
            read(file.inputStream(), charset, ignoreDtd)

        fun read(xml: String, charset: Charset = StandardCharsets.UTF_8, ignoreDtd: Boolean = true): Element =
            read(ByteArrayInputStream(xml.toByteArray()), charset, ignoreDtd)

        fun read(url: URL, charset: Charset = StandardCharsets.UTF_8, ignoreDtd: Boolean = true): Element =
            read(Classes.getResourceStream(url), charset, ignoreDtd)

        fun read(inputStream: InputStream, charset: Charset = StandardCharsets.UTF_8, ignoreDtd: Boolean = true): Element =
            InputStreamReader(inputStream, charset.toString()).use { reader ->
                return getBuilder(ignoreDtd).parse(InputSource(reader)).apply { xmlStandalone = true }
                    .documentElement
            }

        fun read(inputStreamReader: InputStreamReader, ignoreDtd: Boolean = true): Element =
            inputStreamReader.use { reader ->
                return getBuilder(ignoreDtd).parse(InputSource(reader)).apply { xmlStandalone = true }
                    .documentElement
            }

        fun createDocument(rootTagName: String, ignoreDtd: Boolean = true): Element {
            val doc = createDocument(ignoreDtd)
            return doc.appendElement(rootTagName)
        }

        fun createDocument(ignoreDtd: Boolean = true): Document =
            getBuilder(ignoreDtd).newDocument().apply { xmlStandalone = true }
    }

}