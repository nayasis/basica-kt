package com.github.nayasis.kotlin.basica.xml

import com.github.nayasis.kotlin.basica.core.klass.Classes
import com.github.nayasis.kotlin.basica.core.path.inputStream
import mu.KotlinLogging
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.traversal.DocumentTraversal
import org.w3c.dom.traversal.NodeFilter
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

    fun read(xml: String, ignoreDtd: Boolean = true): Element {

//        val registry = DOMImplementationRegistry.newInstance()
//        val dom = registry.getDOMImplementation("LS") as DOMImplementationLS
//        val builder = dom.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null)
//
//        builder.filter = XmlInputFilter()
//
//
//
//        val input = dom.createLSInput()
//        input.stringData = xml
//
//        return builder.parse(input).documentElement

        return read(ByteArrayInputStream(xml.toByteArray()), ignoreDtd)

    }
//        read(ByteArrayInputStream(xml.toByteArray()), ignoreDtd)

    fun read(url: URL, ignoreDtd: Boolean = true): Element =
        read(Classes.getResourceStream(url), ignoreDtd)

    fun read(inputStream: InputStream, ignoreDtd: Boolean = true): Element =
        InputStreamReader(inputStream, StandardCharsets.UTF_8.toString()).use { reader ->

            val doc = getBuilder(ignoreDtd).parse(InputSource(reader)).apply { xmlStandalone = true }
            val treeWalker = (doc as DocumentTraversal).createTreeWalker(doc, NodeFilter.SHOW_TEXT, null, false)
            val textNodes = ArrayList<Node>()
            while( true ) {
                val node = treeWalker.nextNode() ?: break
                textNodes.add(node)
            }

            for( node in textNodes ) {
                if( node.textContent.trim().isNotEmpty() ) continue
                val prev = node.previousSibling
                val next = node.nextSibling
                when {
                    prev == null -> node.remove()
                    next == null -> node.remove()
                    prev.isComment() || prev.isElement() -> node.remove()
                    next.isComment() || next.isElement() -> node.remove()
                }
            }

            return doc.documentElement
        }

    fun createNew(rootTagName: String, ignoreDtd: Boolean = true): Element {
        val doc = getBuilder(ignoreDtd).newDocument().apply { xmlStandalone = true }
        val root = doc.createElement(rootTagName)
        doc.appendChild(root)
        return root
    }



}