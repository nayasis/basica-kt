package io.github.nayasis.kotlin.basica.core.extension

import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ExtensionsTest: StringSpec({
    "then" {
        ((1 == 1) then "A" ?: "B") shouldBe "A"
        ((1 == 2) then "A" ?: "B") shouldBe "B"
    }
    "if not null" {

        var a: String? = "A"

        a.ifNotNull { 1234 } shouldBe 1234
        a.ifNotEmpty { 1234 } shouldBe 1234

        a = null
        a.ifNotNull { "IS NOT EMPTY" } shouldBe null
        a.ifNotEmpty { "IS NOT EMPTY" } shouldBe null

    }
    "if not empty" {
        val a = mutableListOf(1,2,3,4)
        a.ifNotEmpty { it.sum() } shouldBe 10
        a.clear()
        a.ifNotEmpty { it.sum() } shouldBe null
    }
})

class AnnotationExtensionsTest: AnnotationSpec() {
    @Test
    fun `return in lambda`() {
        "A".ifEmpty { "B" }.plus("A") shouldBe "AA"
        "".ifEmpty { return }
        throw Exception("must not run this code")
    }
    @Test
    fun `ifTrue`() {
        var a = 0
        val b = true.ifTrue { a = 1 }

        a shouldBe 1
        b shouldBe true

        true.ifTrue { return }
        throw Exception("must not run this code")
    }
    @Test
    fun `ifFalse`() {
        var a = 0
        val b = false.ifFalse { a = 1 }

        a shouldBe 1
        b shouldBe false

        false.ifFalse { return }
        throw Exception("must not run this code")
    }
}