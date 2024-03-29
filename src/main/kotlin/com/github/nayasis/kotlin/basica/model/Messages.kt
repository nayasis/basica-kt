@file:Suppress("MemberVisibilityCanBePrivate")

package com.github.nayasis.kotlin.basica.model

import com.github.nayasis.kotlin.basica.core.extension.ifEmpty
import com.github.nayasis.kotlin.basica.core.klass.Classes
import com.github.nayasis.kotlin.basica.core.io.toUrl
import com.github.nayasis.kotlin.basica.core.string.extractLowers
import com.github.nayasis.kotlin.basica.core.string.extractUppers
import com.github.nayasis.kotlin.basica.core.string.toUrl
import com.github.nayasis.kotlin.basica.core.url.toFile
import com.github.nayasis.kotlin.basica.core.validator.nvl
import java.io.File
import java.net.URL
import java.nio.file.Path
import java.util.*

private val NULL_LOCALE = Locale("", "")

/**
 * Message utility based on message code for I18N
 */
class Messages { companion object {

    private val pool = Hashtable<String,Hashtable<Locale, String>>()

    /**
     * get message corresponding code
     *
     * @param locale    locale
     * @param code      message code
     */
    operator fun get(locale: Locale?, code: String?): String {
        return getMessage(code, locale) ?: ""
    }

    /**
     * get default locale's message corresponding to code.
     *
     * @param code      message code
     */
    operator fun get(code: String?): String {
        return get(Locale.getDefault(), code)
    }

    /**
     * put message code
     *
     * @param locale    locale (if null, use system's default)
     * @param code      message code
     * @param message   message
     */
    operator fun set(locale: Locale?, code: String?, message: Any?) {
        val cd = nvl(code).trim()
        if( ! pool.containsKey(cd) )
            pool[cd] = Hashtable<Locale, String>()
        val messages = pool[cd]!!
        messages[locale.ifEmpty{Locale.getDefault()}] = "$message".trim()
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
     * get message from repository
     *
     * @param code      message code
     * @param locale    locale
     * @return message corresponding to code
     */
    @Suppress("NAME_SHADOWING")
    private fun getMessage(code: String?, locale: Locale?): String? {
        var locale    = locale.ifEmpty { Locale.getDefault() }
        val cd        = nvl(code).trim().also { if(it.isEmpty()|| pool.isEmpty) return it }
        val messages  = pool[code].also { if(it.isNullOrEmpty()) return cd }!!
        var localeKey = locale
        if ( !messages.containsKey(localeKey) ) {
            localeKey = Locale(locale.language)
            if ( !messages.containsKey(localeKey) )
                localeKey = if(!messages.containsKey(NULL_LOCALE))
                    messages.keys.iterator().next() else NULL_LOCALE
        }
        return messages[localeKey]
    }

    /**
     * load message file to memory
     *
     * @param resourcePath message file or resource path
     */
    fun loadFromResource(resourcePath: String?) {
        if( resourcePath.isNullOrEmpty() ) return
        Classes.findResources(resourcePath).forEach{ loadFromURL(it) }
    }

    /**
     * load message file to memory
     *
     * @param file message file or resource path
     */
    fun loadFromFile(file: String?) {
        if( file.isNullOrEmpty() ) return
        loadFromURL(file.toUrl())
    }

    /**
     * load message file to memory
     *
     * @param file message file or resource path
     */
    fun loadFromFile(file: Path?) {
        if( file == null ) return
        loadFromURL(file.toUrl())
    }

    /**
     * load message file to memory
     *
     * @param file message file or resource path
     */
    fun loadFromFile(file: File?) {
        if( file == null ) return
        loadFromURL(file.toUrl())
    }

    /**
     * load message file to memory
     *
     * @param url URL path of message resource
     */
    fun loadFromURL(url: URL) {
        val locale = getLocaleFrom(url)
        val properties = NProperties(url)
        for (key in properties.keys.map {"$it"} ) {
            if (!pool.containsKey(key))
                pool[key] = Hashtable()
            val messages = pool[key]!!
            messages[locale] = properties.getProperty(key)
        }
    }

    /**
     * clear  message pool
     */
    fun clear() = pool.clear()

    private fun getLocaleFrom(url: URL): Locale {
        val words = url.toFile().nameWithoutExtension.split(".")
            .also { if(it.size <=1) return NULL_LOCALE  }
        val last     = words.last()
        val country  = last.extractUppers()
        var language = last.extractLowers().ifEmpty { Locale.getDefault().language }
        return Locale(language, country)
    }

    /**
     * get all messages
     *
     * @param locale    locale to extract message.
     * @return messages in pool
     */
    fun getAll(locale: Locale = Locale.getDefault()): Map<String, String?> {
        return mutableMapOf<String, String?>().apply {
            for( code in pool.keys ) {
                this[code] = getMessage(code,locale)
            }
        }
    }

}}