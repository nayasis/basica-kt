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
 * languages in order of common use, used to sort message files for loading
 */
private val LANGUAGE_PRIORITY = listOf("en", "zh", "es", "fr", "de", "ja", "ko", "pt", "ru", "it", "ar", "hi")

/**
 * Message utility based on message code for I18N
 */
open class Messages { companion object {

    private val pool = Hashtable<String,MutableMap<Locale, String>>()

    /**
     * locale used when locale is not given
     */
    var defaultLocale: Locale = Locale.getDefault()

    /**
     * locale used when no message matches the requested locale
     */
    var fallbackLocale: Locale = Locale.ENGLISH

    /**
     * get message corresponding code
     *
     * @param locale    locale
     * @param code      message code
     */
    operator fun get(code: String?, locale: Locale = defaultLocale): String {
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
        put(cd, locale.ifNull{Locale.getDefault()}, "$message")
    }

    private fun put(code: String, locale: Locale, message: String) {
        pool.computeIfAbsent(code) { LinkedHashMap() }[locale] = message
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
        return messages.find(locale) ?: messages[EMPTY_LOCALE] ?: messages.find(fallbackLocale) ?: messages.values.first()
    }

    private fun Map<Locale,String>.find(locale: Locale): String? =
        this[locale] ?: this[Locale(locale.language)]

    /**
     * sort message files to load them in a fixed order regardless of classpath or file system
     *
     * 1. file without locale
     * 2. file with locale in order of [LANGUAGE_PRIORITY] (other languages go last)
     * 3. file name
     */
    private fun List<URL>.sortedForLoading(): List<URL> =
        sortedWith(compareBy({ getLocaleFrom(it).loadingRank() }, { it.path.substringAfterLast('/') }, { it.toString() }))

    private fun Locale.loadingRank(): Int {
        if(this == EMPTY_LOCALE) return 0
        val rank = LANGUAGE_PRIORITY.indexOf(language)
        return if(rank >= 0) rank + 1 else LANGUAGE_PRIORITY.size + 1
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
    fun getAll(locale: Locale = defaultLocale): Map<String, String?> {
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
            put(key, locale, properties.getProperty(key))
        }
    }

    fun File.loadMessages() {
        if(this.isFile) {
            this.toUrl().loadMessages()
        } else if(this.isDirectory) {
            this.toPath().loadMessages()
        }
    }

    fun Path.loadMessages() {
        if(this.isFile()) {
            this.toUrl().loadMessages()
        } else if(this.isDirectory()) {
            this.findFiles().map { it.toUrl() }.sortedForLoading().forEach { it.loadMessages() }
        }
    }

    fun String.loadMessages() {
        Classes.findResources(this).takeIf { it.isNotEmpty() }?.sortedForLoading()?.forEach { it.loadMessages() } ?:
        runCatching { this.toPath() }.getOrNull()?.takeIf { it.exists() }?.loadMessages() ?:
        runCatching { this.toFile() }.getOrNull()?.takeIf { it.exists() }?.loadMessages()
    }

}}