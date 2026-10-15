package io.github.nayasis.kotlin.basica.core.resource

import io.github.nayasis.kotlin.basica.core.klass.Classes
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import java.net.URL

private val URL.fileName: String
    get() = path.substringAfterLast('/')

internal class ResourceFinderTest: StringSpec({

    "find resources in directory" {
        Classes.findResources("/message/*.prop").map { it.fileName } shouldContainExactlyInAnyOrder listOf("message.en.prop", "message.ko.prop")
        Classes.findResources("message/*.prop").map { it.fileName } shouldContainExactlyInAnyOrder listOf("message.en.prop", "message.ko.prop")
        Classes.findResources("/message/*").map { it.fileName } shouldContainExactlyInAnyOrder listOf("message.en.prop", "message.ko.prop", "message.en.properties")
        Classes.findResources("/message/*.{prop,properties}").size shouldBe 3
        Classes.findResources("/message/message.??.prop").size shouldBe 2
    }

    "find resources in sub directories" {
        Classes.findResources("/message/**.prop").size shouldBe 2
        Classes.findResources("/**/message.en.prop").map { it.fileName } shouldBe listOf("message.en.prop")
        Classes.findResources("/message/**/*.prop").size shouldBe 2
    }

    "find resources without pattern" {
        Classes.findResources("/message/message.en.prop").map { it.fileName } shouldBe listOf("message.en.prop")
        Classes.findResources("/message/not-exists.prop") shouldBe emptyList()
    }

    "find resources in jar" {
        Classes.findResources("/META-INF/*.kotlin_module").filter { it.protocol == "jar" }.map { it.fileName } shouldContainAll listOf("kotlin-stdlib.kotlin_module")
        Classes.findResources("/kotlin/collections/*.class").filter { it.protocol == "jar" }.isNotEmpty() shouldBe true
    }

    "read found resource" {
        Classes.findResources("/message/*.en.prop").first().openStream().use { it.readBytes().isNotEmpty() } shouldBe true
        Classes.findResources("/META-INF/*.kotlin_module").first { it.protocol == "jar" }.openStream().use { it.readBytes().isNotEmpty() } shouldBe true
    }

})
