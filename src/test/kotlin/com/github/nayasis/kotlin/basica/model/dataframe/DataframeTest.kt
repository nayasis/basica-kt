package com.github.nayasis.kotlin.basica.model.dataframe

import com.github.nayasis.kotlin.basica.core.character.Characters
import com.github.nayasis.kotlin.basica.core.localdate.toLocalDateTime
import com.github.nayasis.kotlin.basica.core.localdate.toString
import com.github.nayasis.kotlin.basica.core.number.round
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private val logger = KotlinLogging.logger {}

internal class DataframeTest: StringSpec({

    Characters.fullwidth = 2.0

    "print" {

        val dataframe = DataFrame()

        dataframe.addRow(mapOf("key" to "controller", "val" to "컨트롤러는 이런 것입니다."))
        dataframe.addRow(mapOf("key" to 1, "val" to 3359))

        dataframe.setLabel("key", "이것은 KEY 입니다.")
        dataframe.setLabel("val", "これは VALUE です")

        dataframe.toString() shouldBe """
            +------------------+-------------------------+
            |이것은 KEY 입니다.|これは VALUE です        |
            +------------------+-------------------------+
            |controller        |컨트롤러는 이런 것입니다.|
            |                 1|                     3359|
            +------------------+-------------------------+            
        """.trimIndent().trim()

        dataframe.toString(showHeader = false) shouldBe """
            +----------+-------------------------+
            |controller|컨트롤러는 이런 것입니다.|
            |         1|                     3359|
            +----------+-------------------------+            
        """.trimIndent().trim()

        dataframe.toString(showIndex = true) shouldBe """
            +-----+------------------+-------------------------+
            |index|이것은 KEY 입니다.|これは VALUE です        |
            +-----+------------------+-------------------------+
            |    0|controller        |컨트롤러는 이런 것입니다.|
            |    1|                 1|                     3359|
            +-----+------------------+-------------------------+          
        """.trimIndent().trim()

    }

    "print empty data" {
        val dataframe = DataFrame()
        dataframe.toString() shouldBe """
            +---+
            +---+            
        """.trimIndent().trim()

        dataframe.addRow(mapOf("name" to null))
        dataframe.toString(showIndex = true) shouldBe """
            +-----+----+
            |index|name|
            +-----+----+
            |    0|    |
            +-----+----+            
        """.trimIndent().trim()
    }

    "print empty with header" {
        val dataframe = DataFrame()
        dataframe.addRow(Person("A", 1))
        dataframe.addRow(Person("B", 2))
        dataframe.clear()
        dataframe.toString() shouldBe """
            +----+---+
            |name|age|
            +----+---+
            +----+---+            
        """.trimIndent().trim()
    }

    "print empty object collection" {
        val dataframe = DataFrame()
        dataframe.toString() shouldBe """
            +---+
            +---+            
        """.trimIndent().trim()
    }

    "toListFromColumn" {

        val dataframe = DataFrame().apply {
            addRow(mapOf("key" to "nayasis", "val" to mapOf("name" to "nayasis", "age" to 40)))
            addRow(mapOf("key" to 1, "val" to mapOf("name" to "jake", "age" to 11)))
        }

        dataframe.getColumn("key").toList<String>().toString() shouldBe "[nayasis, 1]"
        shouldThrow<NoSuchElementException> { dataframe.getColumn("value") }
        dataframe.getColumn("val").toList<Person>().toString() shouldBe "[Person(name=nayasis, age=40), Person(name=jake, age=11)]"
        dataframe.getColumn("key").toList<Double>().toString() shouldBe "[0.0, 1.0]"

    }

    "print overflow" {
        val dataframe = DataFrame().apply {
            addRow(Person("우리나라 좋은나라 대한민국",1234567890))
            addRow(Person("우리나라 좋은나라 미국",1234567890))
            addRow(Person("우리나라 좋은나라 오스트레일리아",1234567890))
        }

        dataframe.toString(maxColumnWidth = 20) shouldBe """
            +--------------------+----------+
            |name                |age       |
            +--------------------+----------+
            |우리나라 좋은나라 ..|1234567890|
            |우리나라 좋은나라 ..|1234567890|
            |우리나라 좋은나라 ..|1234567890|
            +--------------------+----------+            
        """.trimIndent().trim()
    }

    "print Vo overflow" {

        val dataframe = DataFrame().apply {
            addRow(mapOf("key" to "A", "value" to ComplexVo("우리나라 좋은나라 대한민국",1234590)))
            addRow(mapOf("key" to "B", "value" to ComplexVo("우리나라 좋은나라 미국",1234212312)))
            addRow(mapOf("key" to "C", "value" to ComplexVo("우리나라 좋은나라 오스트레일리아",12347890)))
        }

        dataframe.toString(maxColumnWidth = 100) shouldBe """
            +---+----------------------------------------------------------------------------------------------------+
            |key|value                                                                                               |
            +---+----------------------------------------------------------------------------------------------------+
            |A  |ComplexVo(name=우리나라 좋은나라 대한민국, age=1234590, birth=2017-04-06 00:00:00.000, address=매.. |
            |B  |ComplexVo(name=우리나라 좋은나라 미국, age=1234212312, birth=2017-04-06 00:00:00.000, address=매우..|
            |C  |ComplexVo(name=우리나라 좋은나라 오스트레일리아, age=12347890, birth=2017-04-06 00:00:00.000, addr..|
            +---+----------------------------------------------------------------------------------------------------+           
        """.trimIndent().trim()

    }

    "ignore carriage return" {
        val dataframe = DataFrame().apply {
            addRow(Person("우리나라 \n좋은나라 대한민국",1234567890))
            addRow(Person("우리나라 \n좋은나라 미국",1234567890))
            addRow(Person("우리나라 \n좋은나라 오스트레일리아",1234567890))
        }
        dataframe.toString(maxColumnWidth = 20, showIndex = true) shouldBe """
            +-----+--------------------+----------+
            |index|name                |age       |
            +-----+--------------------+----------+
            |    0|우리나라 \n좋은나.. |1234567890|
            |    1|우리나라 \n좋은나.. |1234567890|
            |    2|우리나라 \n좋은나.. |1234567890|
            +-----+--------------------+----------+            
        """.trimIndent().trim()
    }

    "control to print label" {

        val data = listOf(
            Person("A",1),
            Person("B",2),
            Person("C",3),
        )

        DataFrame().apply {
            data.forEach { addRow(it) }
        }.toString() shouldBe """
            +----+---+
            |name|age|
            +----+---+
            |A   |  1|
            |B   |  2|
            |C   |  3|
            +----+---+
        """.trimIndent().trim()

        DataFrame().apply {
            setLabel("age", "a")
            setLabel("name", "n")
            data.forEach { addRow(it) }
        }.toString() shouldBe """
            +-+-+
            |n|a|
            +-+-+
            |A|1|
            |B|2|
            |C|3|
            +-+-+
        """.trimIndent().trim()
    }

    "column statistics" {
        val dataframe = DataFrame().apply {
            addRow(mapOf("numbers" to 1, "text" to "A"))
            addRow(mapOf("numbers" to 2, "text" to "B"))
            addRow(mapOf("numbers" to 3, "text" to "C"))
            addRow(mapOf("numbers" to 4, "text" to "D"))
            addRow(mapOf("numbers" to 5, "text" to "E"))
        }

        val numbersColumn = dataframe.getColumn("numbers")
        
        numbersColumn.count() shouldBe 5L
        numbersColumn.min() shouldBe 1.0
        numbersColumn.max() shouldBe 5.0
        numbersColumn.sum() shouldBe 15.0
        numbersColumn.mean() shouldBe 3.0
        numbersColumn.median() shouldBe 3.0
        numbersColumn.std().round(3) shouldBe 1.414
        numbersColumn.percentile(25.0) shouldBe 2.0
        numbersColumn.percentile(75.0) shouldBe 4.0
    }

    "data access methods" {
        val dataframe = DataFrame().apply {
            addRow(mapOf("name" to "Alice", "age" to 25))
            addRow(mapOf("name" to "Bob", "age" to 30))
            addRow(mapOf("name" to "Charlie", "age" to 35))
        }

        dataframe.size shouldBe 3
        dataframe.isEmpty() shouldBe false
        dataframe.keys shouldBe setOf("name", "age")
        
        dataframe.getData(0, "name") shouldBe "Alice"
        dataframe.getData(1, "age") shouldBe 30
        dataframe.getData(0, 0) shouldBe "Alice"
        dataframe.getData(1, 1) shouldBe 30
        
        dataframe.getRow(0) shouldBe mapOf("name" to "Alice", "age" to 25)
        dataframe.getRow(1) shouldBe mapOf("name" to "Bob", "age" to 30)
    }

    "data modification methods" {
        val dataframe = DataFrame().apply {
            addRow(mapOf("name" to "Alice", "age" to 25))
            addRow(mapOf("name" to "Bob", "age" to 30))
        }

        dataframe.setData(0, "age", 26)
        dataframe.getData(0, "age") shouldBe 26

        dataframe.setData(1, 0, "Robert")
        dataframe.getData(1, "name") shouldBe "Robert"

        dataframe.removeData(1, "age")
        dataframe.getData(1, "age") shouldBe null

        dataframe.removeRow(0)
        dataframe.size shouldBe 1
        dataframe.getRow(1) shouldBe mapOf("name" to "Robert", "age" to null)
    }

    "print non-regular row indexed data" {
        val dataframe = DataFrame().apply {
            addRow(mapOf("name" to "Alice", "age" to 25))
            addRow(mapOf("name" to "Bob", "age" to 30))
            addRow(mapOf("name" to "Charlie", "age" to 35))
            setRow( 5, mapOf("name" to "David", "age" to 40))
        }

        dataframe.toString(showIndex=true) shouldBe """
            +-----+-------+---+
            |index|name   |age|
            +-----+-------+---+
            |    0|Alice  | 25|
            |    1|Bob    | 30|
            |    2|Charlie| 35|
            |    3|       |   |
            |    4|       |   |
            |    5|David  | 40|
            +-----+-------+---+            
        """.trimIndent().trim()

        dataframe.removeRow(1)

        dataframe.toString(showIndex=true) shouldBe """
            +-----+-------+---+
            |index|name   |age|
            +-----+-------+---+
            |    0|Alice  | 25|
            |    1|       |   |
            |    2|Charlie| 35|
            |    3|       |   |
            |    4|       |   |
            |    5|David  | 40|
            +-----+-------+---+            
        """.trimIndent().trim()

        dataframe.removeRow(0)

        dataframe.toString(showIndex=true) shouldBe """
            +-----+-------+---+
            |index|name   |age|
            +-----+-------+---+
            |    2|Charlie| 35|
            |    3|       |   |
            |    4|       |   |
            |    5|David  | 40|
            +-----+-------+---+            
        """.trimIndent().trim()


        dataframe.removeRow(5)

        dataframe.toString(showIndex=true) shouldBe """
            +-----+-------+---+
            |index|name   |age|
            +-----+-------+---+
            |    2|Charlie| 35|
            +-----+-------+---+            
        """.trimIndent().trim()

        dataframe.removeRow(2)

        dataframe.toString(showIndex=true) shouldBe """
            +-----+----+---+
            |index|name|age|
            +-----+----+---+
            +-----+----+---+            
        """.trimIndent().trim()

        dataframe.removeRow(100)

        dataframe.toString(showIndex=true) shouldBe """
            +-----+----+---+
            |index|name|age|
            +-----+----+---+
            +-----+----+---+            
        """.trimIndent().trim()

    }

    "iteration" {
        val dataframe = DataFrame().apply {
            addRow(mapOf("name" to "Alice", "age" to 25))
            addRow(mapOf("name" to "Bob", "age" to 30))
            addRow(mapOf("name" to "Charlie", "age" to 35))
        }

        val rows = mutableListOf<Map<String, Any?>>()
        for (row in dataframe) {
            rows.add(row)
        }

        rows.size shouldBe 3
        rows[0] shouldBe mapOf("name" to "Alice", "age" to 25)
        rows[1] shouldBe mapOf("name" to "Bob", "age" to 30)
        rows[2] shouldBe mapOf("name" to "Charlie", "age" to 35)
    }

    "clone" {
        val original = DataFrame().apply {
            addRow(mapOf("name" to "Alice", "age" to 25))
            addRow(mapOf("name" to "Bob", "age" to 30))
        }

        val clone = original.clone()

        clone.size shouldBe original.size
        clone.getRow(0) shouldBe original.getRow(0)
        clone.getRow(1) shouldBe original.getRow(1)

        // Modify the clone and check that the original is not affected
        clone.setData(0, "age", 26)
        clone.getRow(0)["age"] shouldBe 26
        original.getRow(0)["age"] shouldBe 25
    }

    "print tail and head" {
        val dataframe = DataFrame().apply {
            addRow(mapOf("name" to "Alice", "age" to 25))
            addRow(mapOf("name" to "Bob", "age" to 30))
            addRow(mapOf("name" to "Charlie", "age" to 35))
            addRow(mapOf("name" to "David", "age" to 40))
            addRow(mapOf("name" to "Eve", "age" to 45))
        }

        dataframe.head(3) shouldBe """
            +-------+---+
            |name   |age|
            +-------+---+
            |Alice  | 25|
            |Bob    | 30|
            |Charlie| 35|
            +-------+---+            
        """.trimIndent().trim()

        dataframe.tail(2) shouldBe """
            +-----+---+
            |name |age|
            +-----+---+
            |David| 40|
            |Eve  | 45|
            +-----+---+            
        """.trimIndent().trim()
    }

})

data class Person(
    val name: String? = null,
    val age: Int? = null,
)

data class ComplexVo(
    val name: String,
    val age: Int,
    val birth: String = "2020-01-01".toLocalDateTime().minusDays(1000).toString("YYYY-MM-DD HH:MI:SS.FFF"),
    val address: String = "매우매우 긴 주소입니다.",
    val regDt: String = "2021-07-23".toLocalDateTime().toString("YYYY-MM-DD HH:MI:SS.FFF"),
    val updDt: String = "2022-09-25".toLocalDateTime().toString("YYYY-MM-DD HH:MI:SS.FFF"),
) 