@file:JvmName("Nodes")

package com.github.nayasis.kotlin.basica.xml

import com.github.nayasis.kotlin.basica.core.extension.ifNotEmpty
import com.github.nayasis.kotlin.basica.core.extension.ifNull
import com.github.nayasis.kotlin.basica.xml.NodeType.*
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.w3c.dom.Attr
import org.w3c.dom.Document
import org.w3c.dom.DocumentType
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.w3c.dom.traversal.DocumentTraversal
import org.w3c.dom.traversal.NodeFilter
import org.w3c.dom.traversal.TreeWalker
import java.io.StringWriter
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import kotlin.coroutines.CoroutineContext

private val parserXpath = XPathFactory.newInstance().newXPath()

fun Node?.isEmpty(): Boolean =
    this == null || NodeType.of(nodeType) == NULL

fun Node?.isDocument(): Boolean =
    this != null && NodeType.of(nodeType) == DOCUMENT_NODE

fun Node?.isElement(): Boolean =
    this != null && NodeType.of(nodeType) in listOf(DOCUMENT_NODE, ELEMENT_NODE)

fun Node?.isText(): Boolean =
    this != null && NodeType.of(nodeType) == TEXT_NODE

fun Node?.isCData(): Boolean =
    this != null && NodeType.of(nodeType) == CDATA_SECTION_NODE

fun Node?.isComment(): Boolean =
    this != null && NodeType.of(nodeType) == COMMENT_NODE

fun Node?.isAttribute(): Boolean =
    this != null && NodeType.of(nodeType) == ATTRIBUTE_NODE

fun Node?.isNewline(): Boolean =
    this != null && NodeType.of(nodeType) == TEXT_NODE && this.textContent == "\n"

fun Node.rename(tagName: String): Node =
    ownerDocument.renameNode(this, this.namespaceURI, tagName )

fun Node.appendFromXml(xml: String, charset: Charset = StandardCharsets.UTF_8, ignoreDtd: Boolean = true): Node {
    val doc = XmlReader.read(xml, charset, ignoreDtd)
    val children = ownerDocument.importNode( doc, true)
    return this.appendChild(children)
}

fun Node.appendTextNode(text: String?): Node =
    this.appendChild(ownerDocument.createTextNode(text?:""))

fun Node.appendElement(tagName: String): Element {
    return (ownerDocument ?: this as Document).createElement(tagName).also {
        this.appendChild(it)
    }
}

fun Node.appendComment(comment: String?): Node =
    this.appendChild(ownerDocument.createComment(comment?:""))

fun Node.appendTo(parent: Node): Node {
    return parent.appendChild(this)
}

fun Node.remove(): Node {
    this.parentNode?.removeChild(this)
    return this
}

fun Node.removeAttr(key: String, ignoreCase: Boolean = false): Attr? {
    if( this !is Element || attributes == null ) return null
    return if(ignoreCase) {
        val attr = attributes().filterKeys { it.equals(key,ignoreCase = true) }.map { it }.firstOrNull()
        if( attr == null ) null else
            attributes.removeNamedItem(attr.key) as Attr?
    } else {
        attributes.removeNamedItem(key) as Attr?
    }
}

fun Node.setAttr(key: String, value: String?): Boolean {
    if( this !is Element ) return false
    this.setAttribute(key,value)
    return true
}

fun Node.hasAttr(key: String, ignoreCase: Boolean = false): Boolean =
    attribute(key,ignoreCase) != null

fun Node.attr(key: String, ignoreCase: Boolean = false): String? {
    val attrs = this.attrs()
    return if( ignoreCase ) {
        attrs.filterKeys { it.equals(key,ignoreCase = true) }.map { it.value }.firstOrNull()
    } else {
        attrs[key]
    }
}

fun Node.attribute(key: String, ignoreCase: Boolean = false): Attr? {
    val attrs = this.attributes()
    return if( ignoreCase ) {
        attrs.filterKeys { it.equals(key,ignoreCase = true) }.map { it.value }.firstOrNull()
    } else {
        attrs[key]
    }
}

fun Node.attributes(): Map<String,Attr> {
    val map = LinkedHashMap<String,Attr>()
    for( i in 0 until attributes.length) {
        val attr = attributes.item(i) as Attr
        map[attr.nodeName] = attr
    }
    return map
}

fun Node.attrs(): Map<String,String> {
    val map = LinkedHashMap<String,String>()
    if( attributes == null ) return map
    for( i in 0 until attributes.length) {
        val attr = attributes.item(i) as Attr
        map[attr.nodeName] = attr.value
    }
    return map
}

fun Node.removeEmptyTextNodes(): Node {

    val walker = document.getTreeWalker(NodeFilter.SHOW_TEXT)
    val textNodes = ArrayList<Node>()
    while(true) {
        val node = walker.nextNode() ?: break
        textNodes.add(node)
    }

    for(node in textNodes) {
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

    return this

}

val Node.xpath: String
    get() {
        if( this.isEmpty() ) return ""
        val parent = this.parentNode
        return if( parent.isEmpty() ) "" else "${parent.xpath}/${this.nodeName}"
    }

var Node.value: String?
    get() {
        return this.nodeValue.ifNull { this.textContent }
    }
    set(value) {
        this.nodeValue = value
    }

fun Node.removeChildren() =
    children().forEach { this.removeChild(it) }

fun Node.removeChild(index: Int): Node? {
    return try {
        this.removeChild( this.childNodes.item(index) )
    } catch (e: Throwable) {
        null
    }
}

fun Node.childrenByTagName(tagName: String): List<Node> =
    this.findNodes(".//$tagName")

fun Node.childrenById(id: String): List<Node> =
    this.childrenBy("id",id)

fun Node.childrenBy(attr: String, value: String): List<Node> =
    this.findNodes(".//*[@$attr='$value']")

fun Node.children(): List<Node> =
    this.childNodes.toList()

fun Node.hasChildren(): Boolean =
    this.hasChildNodes()

fun Node.elements(): List<Element> =
    children().filterIsInstance<Element>()

fun Node.elementsByTagName(tagName: String): List<Element> =
    this.childrenByTagName(tagName).filterIsInstance<Element>()

fun Node.toSimpleString(): String {
    val attr = attrs().map { "${it.key}=\"${it.value}\"" }.joinToString(" ")
    return if( attr.isEmpty() ) "<${this.nodeName}/>" else "<${this.nodeName} $attr/>"
}

/**
 * find nodes using xpath expression.
 *
 *```
 *| Expression | Description                                                                           |
 *|------------|---------------------------------------------------------------------------------------|
 *| nodename   | Selects all nodes have [nodename]                                                     |
 *| /          | Selects from the root node                                                            |
 *| //         | Selects nodes from the current node that match the selection no matter where they are |
 *| .          | Selects the current node                                                              |
 *| ..         | Selects the parent of the current node                                                |
 *| @          | Selects attributes                                                                    |
 *```
 *
 * Example
 *
 * ```
 * | Expression                    | Result                                                                                      |
 * |-------------------------------|---------------------------------------------------------------------------------------------|
 * | employee                      | Selects all nodes with the name "employee"                                                  |
 * | employees/employee            | Selects all employee elements that are children of "employees"                              |
 * | //employee                    | Selects all book elements no matter where they are in the document                          |
 * | /employees/employee[1]        | Selects the first employee element that is the child of the employees element.              |
 * | /employees/employee[last()]   | Selects the last employee element that is the child of the employees element                |
 * | /employees/employee[last()-1] | Selects the last but one employee element that is the child of the employees element        |
 * | //employee[@type='admin']     | Selects all the employee elements that have an attribute named type with a value of 'admin' |
 * | //\*[@id='c2']                 | Selects all elements that have an attribute named id with a value of 'c2'                   |
 *```
 *
 * @param xpath xPath Expression
 * @return matched nodes
 */
fun Node.findNodes(xpath: String): List<Node> =
    (parserXpath.evaluate(xpath, this, XPathConstants.NODESET) as NodeList).toList()

/**
 * find node using Xpath expression.
 *
 *```
 *| Expression | Description                                                                           |
 *|------------|---------------------------------------------------------------------------------------|
 *| nodename   | Selects all nodes have [nodename]                                                     |
 *| /          | Selects from the root node                                                            |
 *| //         | Selects nodes from the current node that match the selection no matter where they are |
 *| .          | Selects the current node                                                              |
 *| ..         | Selects the parent of the current node                                                |
 *| @          | Selects attributes                                                                    |
 *```
 *
 * Example
 *
 * ```
 * | Expression                    | Result                                                                                      |
 * |-------------------------------|---------------------------------------------------------------------------------------------|
 * | employee                      | Selects all nodes with the name "employee"                                                  |
 * | employees/employee            | Selects all employee elements that are children of "employees"                              |
 * | //employee                    | Selects all book elements no matter where they are in the document                          |
 * | /employees/employee[1]        | Selects the first employee element that is the child of the employees element.              |
 * | /employees/employee[last()]   | Selects the last employee element that is the child of the employees element                |
 * | /employees/employee[last()-1] | Selects the last but one employee element that is the child of the employees element        |
 * | //employee[@type='admin']     | Selects all the employee elements that have an attribute named type with a value of 'admin' |
 * | //\*[@id='c2']                 | Selects all elements that have an attribute named id with a value of 'c2'                   |
 *```
 *
 * @param xpath xPath Expression
 * @return matched node
 */
fun Node.findNode(xpath: String): Node? =
    parserXpath.evaluate(xpath, this, XPathConstants.NODE) as Node?

fun Node.toString(pretty: Boolean = true, indent: Int = 2): String {
    if( this.isEmpty() ) return ""
    
    if (!pretty) {
        val transformer = TransformerFactory.newInstance().newTransformer().apply {
            setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
            setOutputProperty(OutputKeys.STANDALONE, "yes")
        }
        return StringWriter().use {
            transformer.transform(DOMSource(this), StreamResult(it))
            it.toString()
        }
    }

    val sb    = StringBuilder()
    val tab   = " ".repeat(indent)
    var depth = 0

    fun appendIndent() {
        if (depth > 0) {
            if (sb.isNotEmpty() && sb.last() != '\n') {
                sb.append("\n")
            }
            sb.append(tab.repeat(depth))
        }
    }

    fun serializeNode(node: Node) {
        when (node.nodeType) {
            Node.ELEMENT_NODE -> {
                appendIndent()
                sb.append("<").append(node.nodeName)
                
                // Add attributes
                val attrs = node.attributes
                for (i in 0 until attrs.length) {
                    val attr = attrs.item(i) as Attr
                    sb.append(" ").append(attr.nodeName).append("=\"").append(attr.value).append("\"")
                }

                if (!node.hasChildNodes()) {
                    sb.append("/>")
                    return
                }
                
                sb.append(">")
                depth++
                
                // Process child nodes
                val children = node.childNodes
                var hasElementChild = false
                var hasTextChild = false
                for (i in 0 until children.length) {
                    val child = children.item(i)
                    when (child.nodeType) {
                        Node.ELEMENT_NODE -> {
                            hasElementChild = true
                            serializeNode(child)
                        }
                        Node.TEXT_NODE -> {
                            val text = child.textContent.trim()
                            if (text.isNotEmpty()) {
                                hasTextChild = true
                                sb.append(text)
                            }
                        }
                    }
                }
                
                depth--
                if (hasElementChild && !hasTextChild) {
                    appendIndent()
                }
                sb.append("</").append(node.nodeName).append(">")
                if (hasElementChild && !hasTextChild) {
                    sb.append("\n")
                }
            }
            Node.TEXT_NODE -> {
                node.textContent.trim().ifNotEmpty { sb.append(it) }
            }
            else -> {}
        }
    }

    serializeNode(this)
    return sb.toString()
}

/**
 * set doctype
 *
 * @param publicId external subset public identifier
 * @param systemId external subset system identifier
 * @return self
 */
fun Node.setDocType(publicId: String, systemId: String): Node {
    document.doctype?.remove()
    val qualifiedName = document.documentElement.nodeName
    val newDoctype = document.implementation.createDocumentType(qualifiedName,publicId,systemId)
    document.appendChild(newDoctype)
    return this
}

val Node.docType: DocumentType?
    get() = if(this is Document) this.doctype else document.docType

val Node.document: Document
    get() = this as? Document ?: this.ownerDocument

fun Node.getTreeWalker(whatToShow: Int = NodeFilter.SHOW_ALL, filter: NodeFilter? = null, entityReferenceExpansion: Boolean = false ): TreeWalker =
    (document as DocumentTraversal).createTreeWalker(this,whatToShow,filter,entityReferenceExpansion)

fun NodeList.iterator(): Iterator<Node> {
    val e = this
    return object : Iterator<Node> {
        private var index = 0
        override fun next(): Node {
            if( ! hasNext() ) throw NoSuchElementException()
            return e.item(index++)
        }

        override fun hasNext(): Boolean {
            return index < e.length
        }
    }
}

fun NodeList.firstOrNull(): Node? = if( this.length > 0 ) this.item(0) else null

fun NodeList.toList(): List<Node> {
    val nodes = ArrayList<Node>()
    for( i in 0 until this.length) {
        nodes.add(this.item(i))
    }
    return nodes
}