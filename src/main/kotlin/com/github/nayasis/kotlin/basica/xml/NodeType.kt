package com.github.nayasis.kotlin.basica.xml

import org.w3c.dom.Node

@Suppress("MemberVisibilityCanBePrivate")
enum class NodeType(val code: Short, val desc: String) {

    NULL (                        -1,                               "NULL"      ),
    ELEMENT_NODE (                Node.DOCUMENT_NODE,               "Element"   ),
    DOCUMENT_NODE (               Node.ELEMENT_NODE,                "Document"  ),
    ATTRIBUTE_NODE (              Node.ATTRIBUTE_NODE,              "Attribute" ),
    TEXT_NODE (                   Node.TEXT_NODE,                   "Text"      ),
    DOCUMENT_TYPE_NODE (          Node.DOCUMENT_TYPE_NODE,          "DocType"   ),
    ENTITY_NODE (                 Node.ENTITY_NODE,                 "Entity"    ),
    ENTITY_REFERENCE_NODE (       Node.ENTITY_REFERENCE_NODE,       "Entity reference" ),
    NOTATION_NODE (               Node.NOTATION_NODE,               "Notation"  ),
    COMMENT_NODE (                Node.COMMENT_NODE,                "Comment"   ),
    CDATA_SECTION_NODE (          Node.CDATA_SECTION_NODE,          "CDATA"     ),
    PROCESSING_INSTRUCTION_NODE ( Node.PROCESSING_INSTRUCTION_NODE, "Attribute" )
    ;

    companion object {

        private val lookup = HashMap<Short, NodeType>()

        fun of(code: Short): NodeType? = lookup[code]

        init {
            values().forEach { lookup[it.code] = it }
        }

    }

    override fun toString(): String = desc

}