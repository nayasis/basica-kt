@file:JvmName("Strings")

package com.github.nayasis.kotlin.basica.core.string

import com.github.nayasis.kotlin.basica.core.character.Characters
import com.github.nayasis.kotlin.basica.core.character.fontWidth
import com.github.nayasis.kotlin.basica.core.character.isCJK
import com.github.nayasis.kotlin.basica.core.extension.ifEmpty
import com.github.nayasis.kotlin.basica.core.extension.isEmpty
import com.github.nayasis.kotlin.basica.core.extension.then
import com.github.nayasis.kotlin.basica.core.io.*
import com.github.nayasis.kotlin.basica.core.io.Paths.Companion.FOLDER_SEPARATOR
import com.github.nayasis.kotlin.basica.core.io.Paths.Companion.FOLDER_SEPARATOR_UNIX
import com.github.nayasis.kotlin.basica.core.klass.Classes
import com.github.nayasis.kotlin.basica.core.localdate.toLocalDateTime
import com.github.nayasis.kotlin.basica.core.number.cast
import com.github.nayasis.kotlin.basica.core.url.URLCodec
import com.github.nayasis.kotlin.basica.model.Messages
import com.github.nayasis.kotlin.basica.reflection.Reflector
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStreamReader
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.ISO_8859_1
import java.nio.file.Path
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.zip.CRC32
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.math.min
import kotlin.math.round
import kotlin.reflect.KClass

private val REGEX_CAMEL         = "[_\\- ][a-zA-Z]".toRegex()
private val REGEX_SNAKE         = "([a-z0-9])([A-Z])".toRegex()
private val REGEX_DELIM         = "[- ]".toRegex()
private val REGEX_SPACE         = "[ \t]+".toRegex()
private val REGEX_SPACE_ENTER   = "[ \t\n\r]+".toRegex()
private val REGEX_LINE_REMAIN   = " *[\n\r]".toRegex()
private val REGEX_LINE          = "[\n\r]+".toRegex()
private val REGEX_EXTRACT_DIGIT = "[^0-9]".toRegex()
private val REGEX_EXTRACT_UPPER = "[^A-Z]".toRegex()
private val REGEX_EXTRACT_LOWER = "[^a-z]".toRegex()

private val FORMATTER   = Formatter()

fun String.message(locale: Locale? = null): String = Messages[locale, this]

fun String.toPath(): Path = Path(this)

fun String.toDir(fileCheck: Boolean = true): Path? {
    val path = this.toPath()
    return when {
        ! fileCheck -> path.parent
        path.isDirectory() -> path
        else -> path.parent
    }
}

fun String.toFile(): File = File(this)

fun String.toUrl(): URL = URL(this)

fun String.isUrl(): Boolean = try {
    URL(this)
    true
} catch (e: MalformedURLException) {
    false
}

fun String.toUri(): URI = URI(this.replace(" ", "%20"))

fun String.toResource(): URL? = Classes.getResource(this)

fun String.toResources(): List<URL> = Classes.findResources(this)

fun String.invariantSeparators(): String {
    return if(FOLDER_SEPARATOR != FOLDER_SEPARATOR_UNIX) this.replace(FOLDER_SEPARATOR, FOLDER_SEPARATOR_UNIX) else this
}

fun String?.glob(glob: String = "*", depth: Int = -1, includeFile: Boolean = true, includeDirectory: Boolean = true ): List<Path> {
    if(this == null) return emptyList()
    return this.toPath().find(glob,depth,includeFile,includeDirectory)
}

fun String?.find(pattern: Pattern?): Boolean {
    if(this.isNullOrEmpty()) return false
    return pattern?.matcher(this)?.find() ?: false
}

fun String?.find(pattern: Regex?): Boolean {
    if(this.isNullOrEmpty()) return false
    return this.find( pattern?.toPattern() )
}

@JvmOverloads
fun String?.isDate(format: String = ""): Boolean {
    return when {
        this.isNullOrEmpty() -> false
        else -> try {
            this.toLocalDateTime(format)
            true
        } catch (e: Exception) {
            false
        }
    }
}

@JvmOverloads
fun String?.dpadStart(length: Int, padChar: Char = ' ' ): String {
    val repeat = length - this.displayLength
    return when {
        repeat > 0 -> {
            var sb = StringBuilder()
            for( n in 1..repeat )
                sb.append(padChar)
            sb.append(this?:"")
            sb.toString()
        }
        else -> this!!
    }
}

@JvmOverloads
fun String?.dpadEnd(length: Int, padChar: Char = ' ' ): String {
    val repeat = length - this.displayLength
    return when {
        repeat > 0 -> {
            val sb = StringBuilder(this ?: "")
            for( n in 1..repeat )
                sb.append(padChar)
            sb.toString()
        }
        else -> this!!
    }
}

/**
 * get display length applying character's font width.
 *
 * if character is CJK, font width can be 0.5 or 2.
 * this method calculate total display length of string value.
 *
 * Full-Width of CJK characters can be set by {@link Characters#fullwidth}.
 *
 * @return total display length
 */
val String?.displayLength : Int
    get() {
        if( this == null ) return 0
        if( ! Characters.isFontWidthModified() ) return this.length
        var length = 0.0
        for( c in this )
            length += c.fontWidth
        return round(length).toInt()
    }

fun String?.displaySubstr(startIndex: Int, length: Int): String {
    if( this.isNullOrEmpty() ) return ""
    val bf = StringBuilder()
    var total = 0.0
    for( i in startIndex until this.length ) {
        val c = this[i]
        total += c.fontWidth
        if( round(total) >= length )
            return bf.toString()
        bf.append(c)
    }
    return bf.toString()
}

fun String.toCamel(): String {
    if (this.isEmpty()) return ""
    return REGEX_CAMEL.replace(this) { matchResult ->
        val r = matchResult.value.substring(1)
        if (matchResult.range.first == 0) r else r.uppercase()
    }.replaceFirstChar { it.lowercaseChar() }
}

fun String.toSnake(): String {
    if (this.isEmpty()) return ""
    val replaced = REGEX_DELIM.replace(this, "_")
    return REGEX_SNAKE.replace(replaced) {
        "${it.groupValues[1]}_${it.groupValues[2]}"
    }.lowercase()
}

fun String.escape(): String {
    if(this.isEmpty()) return ""
    val sb = StringBuilder()
    for( ch in this ) {
        when (ch) {
            '"' -> sb.append("\\\"")
            '\\' -> sb.append("\\\\")
            '\b' -> sb.append("\\b")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            '/' -> sb.append("\\/")
            else -> if (ch in '\u0000'..'\u001F') {
                sb.append("\\u").append( Integer.toHexString(ch.code).padStart(4,'0') )
            } else {
                sb.append(ch)
            }
        }
    }
    return sb.toString()
}

fun String.escapeXml(): String {
    return buildString {
        for (ch in this@escapeXml) {
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(ch)
            }
        }
    }
}

fun String.unescapeXml(): String {
    if(this.isEmpty()) return ""
    val sb = StringBuffer()
    val matcher = "&(amp|lt|gt|quot|apos);".toPattern().matcher(this)
    while(matcher.find()) {
        when(matcher.group(1)) {
            "amp"  -> matcher.appendReplacement(sb, "&")
            "lt"   -> matcher.appendReplacement(sb, "<")
            "gt"   -> matcher.appendReplacement(sb, ">")
            "quot" -> matcher.appendReplacement(sb, "\"")
            "apos" -> matcher.appendReplacement(sb, "'")
        }
    }
    matcher.appendTail(sb)
    return sb.toString()
}


fun String.unescape(): String {
    if(this.isEmpty()) return ""
    val sb = StringBuffer()
    val matcher = "\\\\(b|t|n|f|r|\\\"|\\\'|\\\\)|([u|U][0-9a-fA-F]{4})".toPattern().matcher(this)
    while(matcher.find()) {
        val unescaped: String? = if (matcher.start(1) >= 0) {
            unescapeChar(matcher.group(1))
        } else if (matcher.start(2) >= 0) {
            unescapeUnicodeChar(matcher.group(2))
        } else {
            null
        }
        matcher.appendReplacement(sb, Matcher.quoteReplacement(unescaped))
    }
    return sb.toString()
}

fun String.toCapitalize(locale: Locale = Locale.getDefault()): String {
    return when {
        isEmpty() -> ""
        else -> replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }
}

private fun unescapeUnicodeChar(escaped: String): String {
    val hex = escaped.substring(2).toInt(16)
    return hex.toChar().toString()
}

private fun unescapeChar(escaped: String): String {
    return when (escaped[0]) {
        'b' -> "\b"
        't' -> "\t"
        'n' -> "\n"
        'r' -> "\r"
        else -> escaped
    }
}

@JvmOverloads
fun String.urlEncode(charset: Charset = Charsets.UTF_8, legacyMode: Boolean = true): String =
    if( this.isEmpty() ) "" else URLCodec().encode(this,charset,legacyMode)

@JvmOverloads
fun String.urlDecode(charset: Charset = Charsets.UTF_8, legacyMode: Boolean = true): String =
    if( this.isEmpty() ) "" else URLCodec().decode(this,charset,legacyMode)

@JvmOverloads
fun String?.toMapFromUrlParam(charset: Charset = Charsets.UTF_8 ): Map<String,String?> {
    if(this.isNullOrEmpty()) return emptyMap()
    return this.split("&").mapNotNull {
        val tokens = it.split("=")
        when {
            tokens.isEmpty() -> null
            tokens.size == 1 -> {
                when {
                    tokens[0].isNullOrEmpty() -> null
                    else -> tokens[0].urlDecode(charset) to null
                }
            }
            else -> tokens[0].urlDecode(charset) to tokens[1].urlDecode(charset)
        }
    }.toMap()
}

/**
 * add \ character before Regular Expression Keywords ([](){}.*+?$^|#\)
 *
 * @return escaped pattern string
 */
fun String.escapeRegex(): String {
    if( this.isEmpty() ) return ""
    val buf = StringBuilder()
    val chars = "[](){}.*+?\$^|#\\".toCharArray()
    for( c in this ) {
        if( c in chars )
            buf.append('\\')
        buf.append(c)
    }
    return buf.toString()
}

@JvmOverloads
fun String.toSingleSpace(includeLineBreaker: Boolean = false): String =
    if (this.isEmpty()) "" else this.replace(includeLineBreaker then REGEX_SPACE_ENTER ?: REGEX_SPACE, " ").trim()

fun String.toSingleEnter(): String =
    if( this.isEmpty() ) "" else this.replace(REGEX_LINE_REMAIN, "\n").replace(REGEX_LINE, "\n")

fun String.extractDigit(): String  = if( this.isEmpty() ) "" else this.replace(REGEX_EXTRACT_DIGIT, "")
fun String.extractUppers(): String = if( this.isEmpty() ) "" else this.replace(REGEX_EXTRACT_UPPER, "")
fun String.extractLowers(): String = if( this.isEmpty() ) "" else this.replace(REGEX_EXTRACT_LOWER, "")

@JvmOverloads
fun String.removeSpace(includeLineBreaker: Boolean = false): String =
    if (this.isEmpty()) "" else this.replace(includeLineBreaker then REGEX_SPACE_ENTER ?: REGEX_SPACE, "")

fun String?.tokenize(delimiter: String, returnDelimiter: Boolean = false): List<String> {
    if( this.isNullOrEmpty() ) return emptyList()
    val tokens = ArrayList<String>()
    StringTokenizer(this, delimiter, returnDelimiter).let {
        while( it.hasMoreTokens() ) tokens.add(it.nextToken())
    }
    return tokens
}

/**
 * compress text
 * @return compressed text
 */
fun String.compress(): String {
    if( this.isEmpty() ) return ""
    ByteArrayOutputStream().use { out ->
        GZIPOutputStream(out).use { gzip ->
            gzip.write(this.toByteArray())
            gzip.close()
            return out.toString(ISO_8859_1.name())
        }
    }
}

/**
 * decompress text
 * @return decompressed text
 */
fun String.decompress(): String {
    if( this.isEmpty() ) return ""
    ByteArrayInputStream(this.toByteArray(ISO_8859_1)).use { input ->
        GZIPInputStream(input).use { gzip ->
            BufferedReader(InputStreamReader(gzip)).use { bufferReader ->
                val sb = StringBuilder()
                var line: String?
                while (bufferReader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                return sb.toString()
            }
        }
    }
}

/**
 * extract only captured pattern(wrapped by (..) in regular expression)
 *
 * @param pattern regular expression
 * @return captured string
 */
fun String?.capture(pattern: Pattern): List<String> {
    val captures = ArrayList<String>()
    if( this.isNullOrEmpty() ) return captures
    val matcher = pattern.matcher(this)
    while(matcher.find()) {
        for( i in 1..matcher.groupCount() )
            captures.add(matcher.group(i))
    }
    return captures
}

/**
 * extract only captured pattern(wrapped by (..) in regular expression)
 *
 * @param regex regular expression
 * @return captured string
 */
fun String?.capture(regex: Regex): List<String> {
    val captures = ArrayList<String>()
    if( this.isNullOrEmpty() ) return captures
    var matcher = regex.find(this)
    while(matcher != null) {
        for( i in 1 until matcher.groups.size) {
            captures.add(matcher.groups[i]!!.value)
        }
        matcher = matcher.next()
    }
    return captures
}

/**
 * clear XSS(cross site script) pattern in text
 *
 * @return secured string
 */
fun String.clearXss(): String {
    if( this.isEmpty() ) return ""
    val sb = StringBuilder()
    for (ch in this) {
        when (ch) {
            '<'  -> sb.append("&lt;")
            '>'  -> sb.append("&gt;")
            '"'  -> sb.append("&#34;")
            '\'' -> sb.append("&#39;")
            '('  -> sb.append("&#40;")
            ')'  -> sb.append("&#41;")
            '{'  -> sb.append("&#123;")
            '}'  -> sb.append("&#125;")
            else -> sb.append(ch)
        }
    }
    return sb.toString()
}

/**
 * restore XSS(cross site script) pattern in text
 *
 * @return unsecured string
 */
fun String.restoreXss(): String {
    if( this.isEmpty() ) return ""
    val sb = StringBuilder()
    val chars = this.toCharArray()
    var i = 0
    val end = chars.size - 1
    while (i <= end) {
        if (chars[i] != '&') {
            sb.append(chars[i]); i++
            continue
        }
        val code = String.format(
            "&%c%c%c%c%c",
            chars[min(i + 1, end)],
            chars[min(i + 2, end)],
            chars[min(i + 3, end)],
            chars[min(i + 4, end)],
            chars[min(i + 5, end)]
        )
        when {
            code.startsWith("&lt;")   -> { sb.append('<');  i += 3 }
            code.startsWith("&gt;")   -> { sb.append('>');  i += 3 }
            code.startsWith("&#34;")  -> { sb.append('"');  i += 4 }
            code.startsWith("&#39;")  -> { sb.append('\''); i += 4 }
            code.startsWith("&#40;")  -> { sb.append('(');  i += 4 }
            code.startsWith("&#41;")  -> { sb.append(')');  i += 4 }
            code.startsWith("&#123;") -> { sb.append('{');  i += 5 }
            code.startsWith("&#125;") -> { sb.append('}');  i += 5 }
            else -> sb.append(chars[i])
        }
        i++
    }
    return sb.toString()
}

/**
 * check if CJK(Chinese, Japanese, Korean) character exists in text.
 * @return Boolean
 */
fun String?.hasCjk(): Boolean {
    if( this.isNullOrEmpty() ) return false
    for( c in this )
        if( c.isCJK() ) return true
    return false
}

@Suppress("UNCHECKED_CAST")
fun <T:Number> String?.toNumber(type: KClass<T>): T {
    if( this.isNullOrBlank() ) return 0.cast(type)
    return try {
        when(type) {
            Short::class      -> this.toShort()
            Byte::class       -> this.toByte()
            Int::class        -> this.toInt()
            Long::class       -> this.toLong()
            Float::class      -> this.toFloat()
            Double::class     -> this.toDouble()
            BigDecimal::class -> this.toBigDecimal()
            BigInteger::class -> this.toBigInteger()
            else              -> 0.cast(type)
        } as T
    } catch (e: Exception) {
        try {
            BigDecimal(this).cast(type)
        } catch (e1: Exception) {
            when(type) {
                BigDecimal::class -> BigDecimal.ZERO
                BigInteger::class -> BigInteger.ZERO
                else              -> 0.cast(type)
            } as T
        }
    }
}

@JvmOverloads
fun String?.toBoolean(trueWhenEmpty: Boolean = true): Boolean {
    return this.toYn(trueWhenEmpty) == 'Y'
}

@JvmOverloads
fun Any?.toYn(trueWhenEmpty: Boolean = true): Char {
    return when {
        this.isEmpty() -> if(trueWhenEmpty) 'Y' else 'N'
        this is Boolean -> if(this) 'Y' else 'N'
        else -> {
            val text = "$this".trim()
            return when {
                "y".equals(text,ignoreCase = true) -> 'Y'
                "yes".equals(text,ignoreCase = true) -> 'Y'
                "t".equals(text,ignoreCase = true) -> 'Y'
                "true".equals(text,ignoreCase = true) -> 'Y'
                else -> 'N'
            }
        }
    }
}

inline fun <reified T:Number> String?.toNumber(): T {
    return toNumber(T::class)
}

fun String?.toMap(): Map<String,*> {
    return Reflector.toMap(this)
}

/**
 * return formatted string
 *
 * | markup       | description                 | usage                                                                                   |
 * | :---         | :---                        | :---                                                                                    |
 * | {}           | index based                 | "{}st, {}nd".bind(1, 2) -> "1st, 2nd"                                                   |
 * | {:format}    | index based with format     | "{}st, [{:%3d}]nd".bind(1, 2) -> "1st, [  2]nd"                                         |
 * | {key}        | parameter based             | "{name}:{age}".bind(mapOf<String,Any>("name" to "abc", "age" to 10)) -> "abc:10"        |
 * | {key:format} | parameter based with format | "{name}:{age:%3d}".bind(mapOf<String,Any>("name" to "abc", "age" to 10)) -> "abc:   10" |
 *
 *
 * @receiver template
 * @param parameter binding parameter
 * @param modifyKorean if true modify first outer character of parameter binding markup by rule of korean.
 * @return formatted string
 */
fun String.bind(vararg parameter: Any?, modifyKorean: Boolean = true): String {
    return when {
        this.isEmpty() -> ""
        else -> FORMATTER.bind(this, *parameter, modifyKorean = modifyKorean)
    }
}

/**
 * encode object to text
 *
  * @return encoded text
 */
fun Any.encodeBase64(): String {
    ByteArrayOutputStream().use {
        ObjectOutputStream(it).use { outstream ->
            outstream.writeObject(this)
        }
        return Base64.getEncoder().encodeToString(it.toByteArray())
    }
}

/**
 * decode text to object
 *
 * @return decoded object
 */
inline fun <reified T> String.decodeBase64(): T {
    val bytes = Base64.getDecoder().decode(this)
    ByteArrayInputStream(bytes).use {
        ObjectInputStream(it).use { instream ->
            return instream.readObject() as T
        }
    }
}

fun String.ifBlank(fn:() -> String): String {
    return if(this.isBlank()) fn() else this
}

fun String.ifNotBlank(fn: (String) -> Unit) {
    if(this.isNotBlank()) fn(this)
}

/**
 * mask pattern to original string
 *
 * example :
 * -----------------------------------------------------------
 * | example                               | result          |
 * -----------------------------------------------------------
 * | "010ABCD1234".mask("")                | ""              |
 * | "010ABCD1234".mask("###_####_####")   | "010_ABCD_1234" |
 * | "010ABCD1234".mask("###-####-###")    | "010-ABCD-123"  |
 * | "010ABCD1234".mask("###-****-####")   | "010-****-1234" |
 * | "010ABCD1234".mask("\\*###_####_***") | "*010_ABCD_***" |
 * | "010ABCD1234".mask("###_####_###\\*") | "010_ABCD_123*" |
 * | "010ABCD1234".mask("***-#**#-***\\")  | "***-A**D-***"  |
 * -----------------------------------------------------------
 *
  * @param pattern  mask pattern.
 *                  '#'  : substitute from word.
 *                  '*'  : hide word with '*'
 *                  '\\' : escape pattern
 *  @param pass     pattern character to substitute word
 *  @param hide     pattern character to hide word
 * @return masked string
 */
@JvmOverloads
fun String.mask(pattern: String?, pass: Char = '#', hide: Char = '*' ): String {
    if(this.isEmpty() || pattern.isNullOrEmpty()) return ""
    val sb = StringBuilder()
    var p = 0; var w = 0
    while ( w < this.length && p < pattern.length ) {
        when (pattern[p]) {
            '\\' -> {
                if(p < pattern.length - 1)
                    sb.append(pattern[p+1])
                p++
            }
            pass -> {
                sb.append(this[w])
                w++
            }
            hide -> {
                sb.append(hide)
                w++
            }
            else -> {
                sb.append(pattern[p])
            }
        }
        p++
    }
    return sb.toString()
}

/**
 * unmask original string according to pattern
 *
 * @param pattern mask pattern
 *
 * - #  : substitute from word
 * - *  : hide word with '*'
 * - \\ : escape pattern
 *  @param pass     pattern character to substitute word
 *  @param hide     pattern character to hide word
 * @return unmasked string
 */
@JvmOverloads
fun String.unmask(pattern: String?, pass: Char = '#', hide: Char = '*'): String {
    if(this.isEmpty() || pattern.isNullOrEmpty()) return ""
    val sb = StringBuilder()
    var p = 0; var w = 0
    while ( w < this.length && p < pattern.length ) {
        when (pattern[p]) {
            '\\' -> {
                p++
            }
            pass -> {
                sb.append(this[w])
            }
            hide -> {
                sb.append(hide)
            }
            else -> {}
        }
        p++
        w++
    }
    return sb.toString()
}

/**
 * check original string is masked
 *
 * @param pattern  mask pattern
 *
 * - #  : substitute from word
 * - *  : hide word with '*'
 * - \\ : escape pattern
 *  @param pass     pattern character to substitute word
 *  @param hide     pattern character to hide word
 *  @param fullMasked check original string is fully masked or partially.
 * @return true if original string is masked
 */
@JvmOverloads
fun String?.isMasked(pattern: String?, pass: Char = '#', hide: Char = '*', fullMasked: Boolean = false): Boolean {
    if( this.isNullOrEmpty() &&  pattern.isNullOrEmpty()) return true
    if(fullMasked) {
        if( this.isNullOrEmpty() && !pattern.isNullOrEmpty()) return false
        if(!this.isNullOrEmpty() &&  pattern.isNullOrEmpty()) return false
        if(this!!.length != pattern!!.replace("\\","").length ) return false
    } else {
        if( this.isNullOrEmpty() && !pattern.isNullOrEmpty()) return true
        if(!this.isNullOrEmpty() &&  pattern.isNullOrEmpty()) return false
        if(this!!.length > pattern!!.replace("\\","").length ) return false
    }
    var p = 0; var w = 0
    while ( w < this.length && p < pattern.length ) {
        when (pattern[p]) {
            '\\' -> {
                if(p < pattern.length - 1) {
                    if(pattern[p+1] != this[w]) return false
                }
                p++
            }
            pass -> {}
            hide -> {
                if(this[w] != hide) return false
            }
            else -> {
                if(pattern[p] != this[w]) return false
            }
        }
        p++
        w++
    }
    return true
}

/**
 * get Levenshtein distance
 *
 * @param source
 * @param target
 * @return cost
  * @see [wikipedia](https://en.wikipedia.org/wiki/Levenshtein_distance)
 */
private fun getLavenshteinDistance(source: String, target: String): Int {
    var src = source.lowercase()
    var trg = target.lowercase()
    val costs = IntArray(trg.length + 1)
    for (i in 0..src.length) {
        var lastValue = i
        for (j in 0..trg.length) {
            if (i == 0) {
                costs[j] = j
            } else {
                if (j > 0) {
                    var newValue = costs[j-1]
                    if (src[i-1] != trg[j-1])
                        newValue = min(min(newValue, lastValue),costs[j]) + 1
                    costs[j-1] = lastValue
                    lastValue = newValue
                }
            }
        }
        if (i > 0) costs[trg.length] = lastValue
    }
    return costs[trg.length]
}

/**
 * get similarity between 0 and 1.
 *
 * 0 is non-matched and 1 is perfect-matched
 *
 * @param other target string to compare
 * @return similarity
 */
fun String?.similarity(other: String?): Double {
    var longer  = this.ifEmpty {""}
    var shorter = other.ifEmpty{""}
    if(longer.length < shorter.length)
        longer = shorter.also { shorter = longer }
    return when {
        longer.isEmpty() -> if(shorter.isEmpty()) 1.0 else 0.0
        else -> (longer.length - getLavenshteinDistance(longer, shorter)) / longer.length.toDouble()
    }
}

@JvmOverloads
fun String.isShort(radix: Int = 10): Boolean = toShortOrNull(radix) != null
@JvmOverloads
fun String.isByte(radix: Int = 10): Boolean = toByteOrNull(radix) != null
@JvmOverloads
fun String.isInt(radix: Int = 10): Boolean = toIntOrNull(radix) != null
@JvmOverloads
fun String.isLong(radix: Int = 10): Boolean = toLongOrNull(radix) != null
fun String.isFloat(): Boolean = toFloatOrNull() != null
fun String.isDouble(): Boolean = toDoubleOrNull() != null
fun String.isNumeric(): Boolean = isDouble()

@JvmOverloads
fun String.isBigInteger(radix: Int = 10): Boolean = toBigIntegerOrNull(radix) != null
@JvmOverloads
fun String.isBigDecimal(mathContext: MathContext? = null): Boolean {
    return if(mathContext == null) {
        toBigDecimalOrNull()
    } else {
        toBigDecimalOrNull(mathContext)
    } != null
}

fun String?.add(text: String): String {
    return if(this.isNullOrEmpty()) {
        text
    } else {
        "$this${text}"
    }
}

fun String.wrap(open: String = "\"", close: String = open, escapeChar: Char? = null): String {
    val escapedValue = (escapeChar ?: when {
        open == "\"" && close == "\"" -> '"'
        open == "'" && close == "'" -> '\''
        else -> null
    })?.let { escCh ->
        val new = ArrayList<Char>()
        this.chars().mapToObj { it.toChar() }.forEach {
            if(it == escCh) {
                new.add('\\')
            }
            new.add(it)
        }
        new.joinToString("")
    } ?: this
    return """$open$escapedValue$close"""
}

fun String.loadClass(classLoader: ClassLoader? = null): Class<*> {
    return (classLoader ?: Classes.classLoader).loadClass(this)
}

fun String.getCrc32(charset: Charset = Charsets.UTF_8): Long {
    return CRC32().also { it.update(this.toByteArray(charset)) }.value
}