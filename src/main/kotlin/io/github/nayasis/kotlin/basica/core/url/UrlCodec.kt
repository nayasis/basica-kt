package io.github.nayasis.kotlin.basica.core.url

import java.io.CharArrayWriter
import java.nio.charset.Charset
import java.util.*

private val exclusive = BitSet(256).apply {
    ('a'..'z').forEach{set(it.code)}
    ('A'..'Z').forEach{set(it.code)}
    ('0'..'9').forEach{set(it.code)}
    listOf('-','_','.','*').forEach {set(it.code)}
}
private const val caseDiff = 'a' - 'A'

/**
 * URL codec implements 'www-form-urlencoded' encoding scheme (misleadingly known as URL encoding)
 */
class URLCodec{

    /**
     * translates a string into {@code application/x-www-form-urlencoded} format
     * @param string String to be translated
     * @param charset Charset   supported charset
     * @param legacyMode Boolean legacy encoding mode ( turn ' ' to '%20' )
     * @return the translated string
     */
    fun encode(string: String, charset: Charset = Charsets.UTF_8, legacyMode: Boolean = true): String {

        var changed = false
        val out     = StringBuilder(string.length)
        val writer  = CharArrayWriter()

        var i = 0
        while (i < string.length) {
            var c = string[i]
            if (exclusive[c.code]) {
                out.append(c)
                i++
            } else if( !legacyMode && c == ' ' ) {
                changed = true
                out.append('+')
                i++
            } else {
                // convert to external encoding before hex conversion
                do {
                    writer.write(c.code)
                    if (c.code in 0xD800..0xDBFF) {
                        if (i + 1 < string.length) {
                            val d = string[i + 1].code
                            if (d in 0xDC00..0xDFFF) {
                                writer.write(d)
                                i++
                            }
                        }
                    }
                    i++
                } while (i < string.length && !exclusive[string[i].code.also { c = Char(it) }])
                writer.flush()
                val bytes = String(writer.toCharArray()).toByteArray(charset)
                writer.reset()
                for( byte in bytes ) {
                    out.append('%')
                        .append(toHex(byte.toInt() shr 4 and 0xF))
                        .append(toHex(byte.toInt() and 0xF))
                }
                changed = true
            }
        }

        return if (changed) out.toString() else string

    }

    private fun toHex( digit: Int ): Char {
        return Character.forDigit(digit, 16).let {
            if( Character.isLetter(it) ) it - caseDiff else it
        }
    }

    /**
     * decode {@code application/x-www-form-urlencoded} string
     * @param string String to be translated
     * @param charset Charset   supported charset
     * @param legacyMode Boolean legacy encoding mode ( turn '%20' to ' ' )
     * @return the newly decoded string
     */
    fun decode(string: String, charset: Charset = Charsets.UTF_8, legacyMode: Boolean = true): String {

        var changed = false
        val length  = string.length
        val sb      = StringBuilder(if (length > 500) length / 2 else length)
        var bytes: ByteArray? = null

        var i = 0
        while (i < length) {
            var c = string[i]
            if(c == '%') {
                try {
                    // (numChars-i)/3 is an upper bound for the number of remaining bytes
                    if (bytes == null) bytes = ByteArray((length - i) / 3)
                    var pos = 0
                    while (i + 2 < length && c == '%' ) {
                        val v = string.substring(i + 1, i + 3).toInt(16)
                        if (v < 0) throw IllegalArgumentException("URLDecoder: Illegal hex characters in escape (%) pattern - negative value")
                        bytes[pos++] = v.toByte()
                        i += 3
                        if (i < length) c = string[i]
                    }
                    // A trailing, incomplete byte encoding such as "%x" will cause an exception to be thrown
                    if (i < length && c == '%') throw IllegalArgumentException("URLDecoder: Incomplete trailing escape (%) pattern")
                    sb.append(String(bytes, 0, pos, charset))
                } catch (e: NumberFormatException) {
                    throw IllegalArgumentException("URLDecoder: Illegal hex characters in escape (%) pattern - ${e.message}")
                }
                changed = true
            } else if(! legacyMode && c == '+') {
                sb.append(' ')
                i++
                changed = true
            } else {
                sb.append(c)
                i++
            }
        }

        return if (changed) sb.toString() else string

    }

}