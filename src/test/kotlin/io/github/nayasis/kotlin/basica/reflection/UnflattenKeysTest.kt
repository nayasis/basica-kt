package io.github.nayasis.kotlin.basica.reflection

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UnflattenKeysTest {

    @Test
    fun `restores map keys containing regex metacharacters literally`() {
        for(key in listOf("gpuplugin(advanced)", "gpu+plugin", "gpu*plugin", "gpu?plugin",
            "gpu\$plugin", "gpu^plugin", "gpu|plugin", "gpu\\plugin", "gpu{plugin}", "gpu(plugin")) {
            assertEquals(mapOf(key to mapOf("enabled" to true)),
                Reflector.unflattenKeys(mapOf("$key.enabled" to true)), key)
        }
    }

    @Test
    fun `round trips nested map keys and arrays with special characters`() {
        val source = linkedMapOf(
            "gpuplugin(advanced)" to linkedMapOf(
                "renderer+fix" to listOf(mapOf("enabled?" to true), mapOf("enabled?" to false)),
                "mode\$" to "auto",
            ),
            "plain" to mapOf("value" to 12),
            "profiles(test)" to listOf("default", "custom"),
        )
        assertEquals(source, Reflector.unflattenKeys(Reflector.flattenKeys(source)))
    }

    @Test
    fun `restores multiple PCSX advanced GPU options under one tab`() {
        val flat = linkedMapOf(
            "gpuplugin(advanced).pcsx_rearmed_gpu_peops_fix_0" to "enabled",
            "gpuplugin(advanced).pcsx_rearmed_gpu_unai_blending" to "disabled",
            "run.core" to "pcsx_rearmed_libretro",
        )
        val restored = Reflector.unflattenKeys(flat)
        assertEquals(mapOf(
            "pcsx_rearmed_gpu_peops_fix_0" to "enabled",
            "pcsx_rearmed_gpu_unai_blending" to "disabled",
        ), restored["gpuplugin(advanced)"])
        assertEquals(flat, Reflector.flattenKeys(restored))
    }
}
