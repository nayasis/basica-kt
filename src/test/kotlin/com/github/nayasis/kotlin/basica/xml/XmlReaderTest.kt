package com.github.nayasis.kotlin.basica.xml

import com.github.nayasis.kotlin.basica.core.klass.Classes
import mu.KotlinLogging
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.w3c.dom.Node
import java.lang.StringBuilder

private val logger = KotlinLogging.logger {}

internal class XmlReaderTest {

    @Test
    fun `basic parse`() {

//        val path = rootPath(this::class).resolve("xml/grammar.xml")
        val path = Classes.getResource("/xml/grammar.xml")
        val doc = XmlReader().read(path!!)
        doc.setDocType("merong","http://struts.apache.org/dtds/struts-2.0.dtd")

        logger.debug{ "\n${doc}" }
        logger.debug{ "\n${doc.toString(true)}" }

        assertTrue(doc.toString(true).isNotEmpty())
        assertEquals("merong", doc.docType?.publicId)
        assertEquals("http://struts.apache.org/dtds/struts-2.0.dtd", doc.docType?.systemId)

    }

    @Test
    fun `node parse`() {

        val doc = XmlReader().read(sampleTreeXml)

        logger.debug { doc.toString(true) }

        var node = doc.findNode("//row")
        assertTrue( node != null )

        logger.debug { node?.toString(true) }

        node = doc.findNode("//not-exist")
        assertTrue( node == null )

        logger.debug { node?.toString(true) }

    }

    @Test
    fun `xPath test`() {

        val doc = XmlReader().read(sampleTreeXml)

        var node = doc.findNode("//*[@id='c2']")

        logger.debug { node?.attributes() }

        assertEquals("<col2 id=\"c2\" val=\"val2\">Value2</col2>", node?.toString(false)?.trim())
        assertEquals("{id=id=\"c2\", val=val=\"val2\"}", node?.attributes().toString())
        assertEquals("{id=c2, val=val2}", node?.attrs().toString())

        val child01 = doc.findNode("./row[2]/col2")
        val child02 = doc.findNode("//col2[2]")

        assertEquals("<col2 id=\"c4\">Value4</col2>", child01?.toString(false)?.trim())
        assertEquals("c4", child01?.attr("id"))
        assertEquals(null, child02?.toString(false)?.trim())
        assertEquals(null, child02?.attr("id"))

        assertEquals(3, doc.elementsByTagName("row").size )

        val node2 = doc.elementsByTagName("row").first()

        assertEquals(0, node2.findNodes("//col").size)
        assertEquals(1, node2.findNodes("./row").size)
        assertEquals(1, node2.findNodes("./*[contains(local-name(), 'col')]").size)
        assertEquals(2, node2.findNodes(".//*[contains(local-name(), 'col')]").size)

        val node3 = doc.findNode("./row")?.findNode(".//col2")

        assertEquals("col2", node3?.nodeName)
        assertEquals("Value2", node3?.value)
        assertEquals("c2", node3?.attr("id"))
        assertEquals("val2", node3?.attr("val"))
        assertEquals("{id=c2, val=val2}", node3?.attrs().toString())

    }

    @Test
    fun `set docType`() {

        val docValidFromFile = XmlReader().read(Classes.getResource("/xml/grammar.xml")!!)
        docValidFromFile.setDocType("merong","http://struts.apache.org/dtds/struts-2.0.dtd")

        val docValidFromString = XmlReader().read("<tag><nested>hello</nested></tag>")
        docValidFromString.setDocType("-//Apache Software Foundation//DTD Struts Configuration 2.0//EN","http://struts.apache.org/dtds/struts-2.0.dtd")

    }

    @Test
    fun `print contents`() {

        val doc = XmlReader().read(sampleSqlXml)

        logger.debug { "\n${doc.toString(true,tabSize = 2)}" }
//        logger.debug { "\n${doc.toString(true,tabSize = 4)}" }

        assertEquals("sqlMap", doc.nodeName)

        val inserts = doc.getElementsByTagName("insert")

        assertEquals(1, inserts.length)

    }

    @Test
    fun `load sql`() {

        val doc = XmlReader().read(sampleSqlXml)

        logger.debug { doc.toSimpleString() }
        logger.debug { makeSql(doc) }

    }

    private fun makeSql(node: Node, depth: Int = 0): String {
        val sb = StringBuilder().append("\t".repeat(depth))
        if( node.isText() || node.isCData() || node.isComment() ) {
            sb.append(node.value)
        } else {
            sb.append(node.toSimpleString())
        }
        for( child in node.children() ) {
            sb.append( makeSql(child, depth + 1) )
        }
        return sb.toString()
    }

    @Test
    fun `create document`() {

        val body = "<node>merong</node>"

        val doc = XmlReader().createNew("root")
        assertEquals(0,doc.children().size)
        doc.appendFromXml(body)
        assertEquals(1,doc.children().size)
        doc.appendFromXml(body)
        assertEquals(2,doc.children().size)
        doc.appendFromXml(body)
        assertEquals(3,doc.children().size)

    }

    @Test
    fun `pretty print`() {

        val doc1 = XmlReader().read("<a><b><c/><d>text D</d><e value='0'/></b></a>")

        assertEquals("""
            <a>
              <b>
                <c/>
                <d>text D</d>
                <e value="0"/>
              </b>
            </a>
        """.trimIndent(), doc1.toString(tabSize = 2).trim() )

        assertEquals("""
            <a>
                <b>
                    <c/>
                    <d>text D</d>
                    <e value="0"/>
                </b>
            </a>
        """.trimIndent(), doc1.toString(tabSize = 4).trim() )

        val doc2 = XmlReader().read(sampleTreeXml)

        assertEquals("""
            <root>
              <row>
                <col1 id="c1">Value1</col1>
                <row>
                  <col2 id="c2" val="val2">Value2</col2>
                </row>
              </row>
              <row>
                <col1 id="c3">Value3</col1>
                <col2 id="c4">Value4</col2>
              </row>
            </root>
        """.trimIndent(), doc2.toString(tabSize = 2).trim() )

        assertEquals("""
            <root>
                <row>
                    <col1 id="c1">Value1</col1>
                    <row>
                        <col2 id="c2" val="val2">Value2</col2>
                    </row>
                </row>
                <row>
                    <col1 id="c3">Value3</col1>
                    <col2 id="c4">Value4</col2>
                </row>
            </root>
        """.trimIndent(), doc2.toString(tabSize = 4).trim() )

        // mixture format is hard to indent (text node itself is member of XML format.)
        val doc3 = XmlReader().read(sampleSqlXml)
        logger.debug { "\n${doc3.toString(true,tabSize = 2)}" }
        logger.debug { "\n${doc3.toString(true,tabSize = 4)}" }

    }

    private val sampleTreeXml = """
        <root>
          <row>
                <col1 id="c1">Value1</col1>
                <row>
                    <col2 id="c2" val="val2">Value2</col2>
                </row>
            </row>
            <row>
                <col1 id="c3">Value3</col1>
                <col2 id="c4">Value4</col2>
            </row>
        </root>
        """.trimIndent()


    private val sampleSqlXml = """
        <?xml version="1.0" encoding="UTF-8" standalone="no"?>
        <!DOCTYPE sqlMap PUBLIC "-//iBATIS.com//DTD SQL Map 2.0//EN" "http://www.ibatis.com/dtd/sql-map-2.dtd">
        <sqlMap namespace="Cash">
            <insert id="chargePoint" parameterClass="java.util.Map">
                <!-- Merong Merong -->
                <selectKey keyProperty="POINT_ID" resultClass="java.lang.String">
                    <![CDATA[
                        SELECT 'PO' || LPAD (SEQ_OD_POINT_HST.NEXTVAL, 18, '0') AS POINT_ID
                        FROM DUAL
                    ]]>
                </selectKey>  
              INSERT /* patch_20121120 PurchasePoc, Cash_SqlMap.xml, chargePoint, KimEungjin, 2012-11-12 */
              INTO TST_OD_POINT_HST
                    (POINT_ID
                   , SVC_CD
                   , ORDER_NO
                   , MBR_NO
                   , OCCR_DT
                   , OCCR_TM
                   , EXTN_PLAN_DT
                   , EXTN_PLAN_TM
                   , OCCR_ST_CD
                   , PROC_TYPE
                   , OCCR_AMT
                   , AVAIL_AMT
                    )
             VALUES (#POINT_ID#
                   , 'OR003101' /*SVC_CD(Tstore)*/         
                   , #PRCHS_ID#
                   , #MBR_NO#
                   , TO_CHAR(SYSDATE, 'YYYYMMDD')
                   , TO_CHAR(SYSDATE, 'HH24MISS')
                   <isEqual property="CARD_TYPE" compareValue="OR002902">
                   , TO_CHAR(ADD_MONTHS(SYSDATE, 12*5), 'YYYYMMDD')
                   , TO_CHAR(ADD_MONTHS(SYSDATE, 12*5), 'HH24MISS')
                   </isEqual>
                   <isEqual >15 AABB</isEqual>
                   <isNotEqual property="CARD_TYPE" compareValue="OR002902">
                   , TO_CHAR(ADD_MONTHS(SYSDATE, 12*1), 'YYYYMMDD')
                   , TO_CHAR(ADD_MONTHS(SYSDATE, 12*1), 'HH24MISS')
                   </isNotEqual>  
                   , 'OR003201' /*OCCR_STAT(발생)*/
                   , 'OR003311' /*OP_TYPE(Gift등록)*/
                   , #AMT#
                   , #AMT#
                    )
            </insert>
        </sqlMap>
        """.trimIndent()

}