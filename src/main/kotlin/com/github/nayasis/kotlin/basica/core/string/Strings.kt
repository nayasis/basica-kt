@file:JvmName("Strings")

package com.github.nayasis.kotlin.basica.core.string

import com.github.nayasis.kotlin.basica.core.character.Characters
import com.github.nayasis.kotlin.basica.core.character.fontwidth
import com.github.nayasis.kotlin.basica.core.character.isCJK
import com.github.nayasis.kotlin.basica.core.extention.then
import com.github.nayasis.kotlin.basica.core.localdate.toLocalDateTime
import com.github.nayasis.kotlin.basica.core.number.cast
import com.github.nayasis.kotlin.basica.core.path.*
import com.github.nayasis.kotlin.basica.core.string.format.Formatter
import com.github.nayasis.kotlin.basica.model.Messages
import com.github.nayasis.kotlin.basica.reflection.Reflector
import mu.KotlinLogging
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStreamReader
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.math.BigDecimal
import java.math.BigInteger
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets.ISO_8859_1
import java.nio.file.Path
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.collections.ArrayList
import kotlin.math.min
import kotlin.math.round
import kotlin.reflect.KClass

private val log = KotlinLogging.logger {}

private val REGEX_CAMEL = "(_[a-zA-Z])".toPattern()
private val REGEX_SNAKE = "([A-Z])".toPattern()
private val FORMATTER   = Formatter()

fun String.message(locale: Locale? = null): String = Messages[locale, this]

fun String.toPath(): Path = Path(this)

fun String.toDir(filecheck: Boolean = true): Path? {
    val path = this.toPath()
    return when {
        ! filecheck -> path.parent
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

fun String.invariantSeparators(): String {
    return if ( FOLDER_SEPARATOR != '/' ) this.replace(FOLDER_SEPARATOR, '/') else this
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

fun String?.isDate(format: String): Boolean {
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

fun String?.isDate(): Boolean = isDate("")

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

fun String?.dpadEnd(length: Int, padChar: Char = ' ' ): String {
    val repeat = length - this.displayLength
    return when {
        repeat > 0 -> {
            var sb = StringBuilder()
            sb.append(this?:"")
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
            length += c.fontwidth()
        return round(length).toInt()
    }

fun String?.toCamel(): String {
    if( this.isNullOrEmpty() ) return ""
    var sb = StringBuffer()
    val matcher = REGEX_CAMEL.matcher(this.lowercase())
    while(matcher.find()) {
        var r = matcher.group().substring(1)
        matcher.appendReplacement(sb, if(matcher.start() == 0) r else r.uppercase())
    }
    matcher.appendTail(sb)
    return sb.toString()
}

fun String?.toSnake(): String {
    if( this.isNullOrEmpty() ) return ""
    var sb = StringBuffer()
    val matcher = REGEX_SNAKE.matcher(this.lowercase())
    while(matcher.find()) {
        if(matcher.start() == 0) continue
        var r = matcher.group()
        matcher.appendReplacement(sb, "_${r.lowercase()}")
    }
    matcher.appendTail(sb)
    return sb.toString()
}

fun String?.escape(): String {
    if(this.isNullOrEmpty()) return ""
    var sb = StringBuilder()
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

fun String?.unescape(): String {
    if(this.isNullOrEmpty()) return ""
    var sb = StringBuffer()
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

private fun unescapeUnicodeChar(escaped: String): String? {
    val hex = escaped.substring(2).toInt(16)
    return hex.toChar().toString()
}

private fun unescapeChar(escaped: String): String? {
    return when (escaped[0]) {
        'b' -> "\b"
        't' -> "\t"
        'n' -> "\n"
        'r' -> "\r"
        else -> escaped
    }
}

/**
 * add \ character before Regular Expression Keywords ([](){}.*+?$^|#\)
 *
 * @return escaped pattern string
 */
fun String?.escapeRegex(): String {
    if( this == null ) return ""
    val buf = StringBuilder()
    val chars = "[](){}.*+?\$^|#\\".toCharArray()
    for( c in this ) {
        if( c in chars )
            buf.append('\\')
        buf.append(c)
    }
    return buf.toString()
}

fun String?.toSingleSpace(includeLineBreaker:Boolean = false): String =
    if (this.isNullOrEmpty()) "" else this.replace(((includeLineBreaker) then "[ \t]+" ?: "[ \t\n\r]+").toRegex(), " ").trim()

fun String?.toSingleEnter(): String =
    if( this.isNullOrEmpty() ) "" else this.replace(" *[\n\r]".toRegex(), "\n").replace( "[ \n\r]+".toRegex(), "\n" )

fun String?.extractDigit(): String = if( this.isNullOrEmpty() ) "" else this.replace( "[^0-9]".toRegex(), "" )
fun String?.extractUppers(): String = if( this.isNullOrEmpty() ) "" else this.replace( "[^A-Z]".toRegex(), "" )
fun String?.extractLowers(): String = if( this.isNullOrEmpty() ) "" else this.replace( "[^a-z]".toRegex(), "" )


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
fun String?.compress(): String {
    if( this.isNullOrEmpty() ) return ""
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
fun String?.decompress(): String {
    if( this.isNullOrEmpty() ) return ""
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
 * clear XSS(cross site script) pattern in text
 *
 * @return secured string
 */
fun String?.clearXss(): String {
    if( this.isNullOrEmpty() ) return ""
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
fun String?.restoreXss(): String {
    if( this.isNullOrEmpty() ) return ""
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
        when(type) {
            BigDecimal::class -> BigDecimal.ZERO
            BigInteger::class -> BigInteger.ZERO
            else              -> 0.cast(type)
        } as T
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
fun String?.bind(vararg parameter: Any?, modifyKorean: Boolean = true): String {
    return when {
        this.isNullOrEmpty() -> ""
        else -> FORMATTER.bindSimple(this, *parameter, modifyKorean = modifyKorean)
    }
}

/**
 * encode object to text
 *
  * @return encoded text
 */
fun Any?.encodeBase64(): String {
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
inline fun <reified T> String?.decodeBase64(): T? {
    if( this == null ) return null
    val bytes = Base64.getDecoder().decode(this)
    ByteArrayInputStream(bytes).use {
        ObjectInputStream(it).use { instream ->
            return instream.readObject() as T?
        }
    }
}

fun String?.ifBlank(fn:() -> String): String {
    return if(this.isNullOrBlank()) fn() else this
}

fun String?.ifNotBlank(fn: (String) -> Unit) {
    if(!this.isNullOrBlank()) fn(this)
}