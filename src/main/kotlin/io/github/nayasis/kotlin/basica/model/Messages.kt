@file:Suppress("MemberVisibilityCanBePrivate")

package io.github.nayasis.kotlin.basica.model

import io.github.nayasis.kotlin.basica.core.extension.ifEmpty
import io.github.nayasis.kotlin.basica.core.extension.ifNull
import io.github.nayasis.kotlin.basica.core.extension.isNotEmpty
import io.github.nayasis.kotlin.basica.core.io.exists
import io.github.nayasis.kotlin.basica.core.io.findFiles
import io.github.nayasis.kotlin.basica.core.io.isDirectory
import io.github.nayasis.kotlin.basica.core.io.isFile
import io.github.nayasis.kotlin.basica.core.io.toUrl
import io.github.nayasis.kotlin.basica.core.klass.Classes
import io.github.nayasis.kotlin.basica.core.string.extractLowers
import io.github.nayasis.kotlin.basica.core.string.extractUppers
import io.github.nayasis.kotlin.basica.core.string.toFile
import io.github.nayasis.kotlin.basica.core.string.toPath
import io.github.nayasis.kotlin.basica.core.url.toFile
import java.io.File
import java.net.URL
import java.nio.file.Path
import java.util.*

private val EMPTY_LOCALE = Locale("", "")

/**
 * Message utility based on message code for I18N
 */
open class Messages { companion object {

    private val pool = Hashtable<String,Hashtable<Locale, String>>()

    /**
     * get message corresponding code
     *
     * @param locale    locale
     * @param code      message code
     */
    operator fun get(code: String?, locale: Locale = Locale.getDefault()): String {
        return getMessage(code, locale) ?: ""
    }

    /**
     * put message code
     *
     * @param locale    locale (if null, use system's default)
     * @param code      message code
     * @param message   message
     */
    operator fun set(locale: Locale?, code: String?, message: Any?) {
        val cd = code?.trim() ?: ""
        if( ! pool.containsKey(cd) )
            pool[cd] = Hashtable<Locale, String>()
        val messages = pool[cd]!!
        messages[locale.ifNull{Locale.getDefault()}] = "$message"
    }

    /**
     * put message code
     *
     * @param code      message code
     * @param message   message
     */
    operator fun set(code: String?, message: Any?) {
        set(null, code, message)
    }

    /**
     * get messages from repository
     *
     * @param code      message code
     * @param locale    locale
     * @return message corresponding to code
     */
    private fun getMessage(code: String?, locale: Locale): String? {
        val cd        = code?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val messages  = pool[cd] ?: return cd
        return messages[locale] ?: messages[Locale(locale.language)] ?: messages[EMPTY_LOCALE] ?: messages.values.first()
    }

    /**
     * clear message pool
     */
    fun clear() = pool.clear()

    private fun getLocaleFrom(url: URL): Locale {
        val words = url.toFile().nameWithoutExtension.split(".")
            .also { if(it.size <=1) return EMPTY_LOCALE  }
        val last     = words.last()
        val country  = last.extractUppers()
        val language = last.extractLowers().ifEmpty { Locale.getDefault().language }
        return Locale(language, country)
    }

    /**
     * get all messages
     *
     * @param locale    locale to extract messages
     * @return messages in pool
     */
    fun getAll(locale: Locale = Locale.getDefault()): Map<String, String?> {
        return mutableMapOf<String, String?>().apply {
            for( code in pool.keys ) {
                this[code] = getMessage(code,locale)
            }
        }
    }

    fun URL.loadMessages() {
        val locale = getLocaleFrom(this)
        val properties = NProperties(this)
        for (key in properties.keys.map {"$it"} ) {
            if (!pool.containsKey(key))
                pool[key] = Hashtable()
            val messages = pool[key]!!
            messages[locale] = properties.getProperty(key)
        }
    }

    fun File.loadMessages() {
        if(this.isFile) {
            this.toUrl().loadMessages()
        } else if(this.isDirectory) {
            this.toPath().findFiles().forEach { it.toUrl().loadMessages() }
        }
    }

    fun Path.loadMessages() {
        if(this.isFile()) {
            this.toUrl().loadMessages()
        } else if(this.isDirectory()) {
            this.findFiles().forEach { it.toUrl().loadMessages() }
        }
    }

    fun String.loadMessages() {
        Classes.findResources(this).toList().takeIf { it.isNotEmpty() }?.forEach { it.loadMessages() } ?:
        runCatching { this.toPath() }.getOrNull()?.takeIf { it.exists() }?.loadMessages() ?:
        runCatching { this.toFile() }.getOrNull()?.takeIf { it.exists() }?.loadMessages()
    }

}}