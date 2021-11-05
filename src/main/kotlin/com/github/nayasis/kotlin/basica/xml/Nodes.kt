@file:JvmName("Nodes")

package com.github.nayasis.kotlin.basica.xml

import com.github.nayasis.kotlin.basica.core.extention.ifNull
import com.github.nayasis.kotlin.basica.xml.NodeType.*
import org.w3c.dom.Attr
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.StringWriter
import javax.xml.XMLConstants
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory


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

fun Node.appendFromXml(xml: String, ignoreDtd: Boolean = true): Node {
    val doc = XmlReader().read(xml,ignoreDtd)
    val children = ownerDocument.importNode( doc, true)
    return this.appendChild(children)
}

fun Node.appendTextNode(text: String?): Node =
    this.appendChild(ownerDocument.createTextNode(text?:""))

fun Node.appendElement(tagName: String): Node =
    this.appendChild(ownerDocument.createElement(tagName))

fun Node.appendComment(comment: String?): Node =
    this.appendChild(ownerDocument.createComment(comment?:""))

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
    attributes(key,ignoreCase) != null

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

fun Node.attributes(key: String, ignoreCase: Boolean = false): Attr? {
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

fun Node.removeEmptyNodes(): Node {
    this.findNodes("//text()[normalize-space()='']").forEach {
        it.parentNode.removeChild(it)
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
    toList(this.childNodes)

fun Node.hasChildren(): Boolean =
    this.hasChildNodes()

fun Node.elements(): List<Element> =
    children().filterIsInstance<Element>()

fun Node.elementsByTagName(tagName: String): List<Element> =
    this.childrenByTagName(tagName).filterIsInstance<Element>()

private fun toList(nodeList: NodeList): List<Node> {
    val nodes = ArrayList<Node>()
    for( i in 0 until nodeList.length) {
        nodes.add(nodeList.item(i))
    }
    return nodes
}

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
    toList(parserXpath.evaluate(xpath, this, XPathConstants.NODESET) as NodeList)

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

fun Node.toString(pretty: Boolean = true, tabSize: Int = 2): String {

    if( this.isEmpty() ) return ""

    val factory = TransformerFactory.newInstance().apply {
        setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
        setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "")
        if(pretty) {
            setAttribute("indent-number", tabSize)
        }
    }

    val transformer = factory.newTransformer().apply {
        setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        if(pretty) {
            setOutputProperty(OutputKeys.INDENT, "yes")
        }
        document?.doctype?.let {
            setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, it.publicId)
            setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, it.systemId)
        }
    }

    val out = StringWriter()
    val streamResult = StreamResult(out)

    transformer.transform(DOMSource(document), streamResult)
    return streamResult.writer.toString()

//    this.removeEmptyNodes()

//    val factory = TransformerFactory.newInstance()
//    val transformer = factory.newTransformer().apply {
//        if( pretty ) {
//            setOutputProperty(OutputKeys.INDENT, "yes")
//            setOutputProperty(OutputPropertiesFactory.S_KEY_INDENT_AMOUNT, "8");
//            setOutputProperty("{http://xml.apache.org/xslt}indent-amount", tabSize.toString())
//        }
//    }
//
//    val isDocPrint = this is Document
//    var hasDocType = true
//    if (isDocPrint) {
//        val doctype = (this as Document).doctype
//        if (doctype != null) {
//            try {
//                transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.publicId)
//                transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.systemId)
//            } catch (e: Exception) {
//                hasDocType = false
//            }
//        } else {
//            hasDocType = false
//        }
//    } else {
//        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
//    }
//
//    val target = StreamResult(StringWriter())
//
//    transformer.transform(DOMSource(this), target)
//
//    var result = target.writer.toString()
//
//    if (!printRoot) {
//        val rootName = this.nodeName
//        result = result
//            .replaceFirst("^<$rootName.*?>".toRegex(), "")
//            .replaceFirst("</$rootName>$".toRegex(), "")
//            .replaceFirst("^(\n|\r)*".toRegex(), "")
//            .replaceFirst("(\n|\r)*$".toRegex(), "")
//    }
//
//    if (isDocPrint) {
//        if (!hasDocType)
//            result = result.replaceFirst("^<\\?xml(.*?)>".toRegex(), "<?xml$1>\n")
//    } else {
//        if ( !printRoot && pretty ) {
//            result = result
//                .replaceFirst("^ {$tabSize}".toRegex(), "")
//                .replace( "(\n|\r) {$tabSize}".toRegex(), "\n" )
//        }
//    }
//
//    return result

}

/**
 * set doctype
 *
 * @param publicId external subset public identifier
 * @param systemId external subset system identifier
 * @return self
 */
fun Node.setDocType(publicId: String, systemId: String): Node {
    document.doctype?.let { removeChild(it) }
    val qualifiedName = document.documentElement.nodeName
    val newDoctype = document.implementation.createDocumentType(qualifiedName,publicId,systemId)
    appendChild(newDoctype)
    return this
}

val Node.document: Document
    get() = if(this is Document) this else this.ownerDocument