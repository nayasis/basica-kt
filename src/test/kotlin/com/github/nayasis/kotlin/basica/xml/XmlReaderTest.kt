package com.github.nayasis.kotlin.basica.xml

import com.github.nayasis.kotlin.basica.core.klass.Classes
import com.github.nayasis.kotlin.basica.core.string.toResource
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.w3c.dom.Node
import java.lang.StringBuilder

private val logger = KotlinLogging.logger {}

internal class XmlReaderTest: StringSpec({

    "basic parse" {
        val path = "/xml/grammar.xml".toResource()!!
        val doc = XmlReader.read(path).apply {
            setDocType("merong","http://struts.apache.org/dtds/struts-2.0.dtd")
        }.also { logger.debug{ "\n${it.toString(true)}" } }

        doc.toString(true).isNotEmpty() shouldBe true
        doc.docType?.publicId shouldBe "merong"
        doc.docType?.systemId shouldBe "http://struts.apache.org/dtds/struts-2.0.dtd"
    }

    "parse node" {

        val doc = XmlReader.read(sampleTreeXml)
            .also { logger.debug{ "\n${it.toString(true)}" } }

        var node = doc.findNode("//row")
            ?.also { logger.debug{ "\n${it.toString(true)}" } }

        node shouldNotBe null

        node = doc.findNode("//not-exist")
            ?.also { logger.debug{ "\n${it.toString(true)}" } }

        node shouldBe null

    }

    "xPath test" {

        val doc = XmlReader.read(sampleTreeXml)

        val node = doc.findNode("//*[@id='c2']")
            ?.also { logger.debug { it.attributes() } }

        node?.toString(false)?.trim() shouldBe "<col2 id=\"c2\" val=\"val2\">Value2</col2>"
        node?.attributes().toString() shouldBe "{id=id=\"c2\", val=val=\"val2\"}"
        node?.attrs().toString() shouldBe "{id=c2, val=val2}"

        val child01 = doc.findNode("./row[2]/col2")
        val child02 = doc.findNode("//col2[2]")

        child01?.toString(false)?.trim() shouldBe "<col2 id=\"c4\">Value4</col2>"
        child01?.attr("id") shouldBe "c4"
        child02?.toString(false)?.trim() shouldBe null
        child02?.attr("id") shouldBe null
        doc.elementsByTagName("row").size shouldBe 3

        val node2 = doc.elementsByTagName("row").first()

        node2.findNodes("//col").size shouldBe 0
        node2.findNodes("./row").size shouldBe 1
        node2.findNodes("./*[contains(local-name(), 'col')]").size shouldBe 1
        node2.findNodes(".//*[contains(local-name(), 'col')]").size shouldBe 2

        val node3 = doc.findNode("./row")?.findNode(".//col2")

        node3?.nodeName shouldBe "col2"
        node3?.value shouldBe "Value2"
        node3?.attr("id") shouldBe "c2"
        node3?.attr("val") shouldBe "val2"
        node3?.attrs().toString() shouldBe "{id=c2, val=val2}"

    }

    "set docType" {

        val docFromFile = "/xml/grammar.xml".toResource()?.let { XmlReader.read(it) }?.apply {
            setDocType("merong","http://struts.apache.org/dtds/struts-2.0.dtd")
        }?.also { logger.debug{ "\n${it.toString(true)}" } }

        docFromFile?.docType?.publicId shouldBe "merong"
        docFromFile?.docType?.systemId shouldBe "http://struts.apache.org/dtds/struts-2.0.dtd"

        val docFromString = "<tag><nested>hello</nested></tag>".let { XmlReader.read(it) }.apply {
            setDocType("-//Apache Software Foundation//DTD Struts Configuration 2.0//EN","http://struts.apache.org/dtds/struts-2.0.dtd")
        }.also { logger.debug{ "\n${it.toString(true)}" } }

        docFromString?.docType?.publicId shouldBe "-//Apache Software Foundation//DTD Struts Configuration 2.0//EN"
        docFromString?.docType?.systemId shouldBe "http://struts.apache.org/dtds/struts-2.0.dtd"

    }

    "print contents" {
        val doc = XmlReader.read(sampleSqlXml)
            .also { logger.debug{ "\n${it.toString(true, tabSize = 2)}" } }
        doc.nodeName shouldBe "sqlMap"

        val inserts = doc.getElementsByTagName("insert")
        inserts.length shouldBe 1
    }

    "simple string" {
        val doc = XmlReader.read(sampleSqlXml)
        doc.toSimpleString() shouldBe "<sqlMap namespace=\"Cash\"/>"
    }

    "create document" {
        val body = "<node>merong</node>"
        val doc = XmlReader.createNew("root")
        doc.children().size shouldBe 0

        doc.appendFromXml(body)
        doc.children().size shouldBe 1

        doc.appendFromXml(body)
        doc.children().size shouldBe 2

        doc.appendFromXml(body)
        doc.children().size shouldBe 3
    }

    "pretty print" {

        val doc1 = XmlReader.read("<a><b><c/><d>text D</d><e value='0'/></b></a>")

        doc1.toString(tabSize = 2).trim() shouldBe """
            <a>
              <b>
                <c/>
                <d>text D</d>
                <e value="0"/>
              </b>
            </a>
        """.trimIndent()

        doc1.toString(tabSize = 4).trim() shouldBe """
            <a>
                <b>
                    <c/>
                    <d>text D</d>
                    <e value="0"/>
                </b>
            </a>
        """.trimIndent()

        val doc2 = XmlReader.read(sampleTreeXml)

        doc2.toString(tabSize = 2).trim() shouldBe """
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

        doc2.toString(tabSize = 4).trim() shouldBe """
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

        // mixture format is hard to indent (text node itself is member of XML format.)
        XmlReader.read(sampleSqlXml).also { doc ->
            logger.debug { "\n${doc.toString(true,tabSize = 2)}" }
            logger.debug { "\n${doc.toString(true,tabSize = 4)}" }
        }

    }

})

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