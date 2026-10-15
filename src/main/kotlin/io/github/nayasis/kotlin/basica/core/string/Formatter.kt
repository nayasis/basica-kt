package io.github.nayasis.kotlin.basica.core.string

import io.github.nayasis.kotlin.basica.core.character.NULL_CHAR
import io.github.nayasis.kotlin.basica.core.character.disassembleHangul
import io.github.nayasis.kotlin.basica.core.character.isKorean
import io.github.nayasis.kotlin.basica.core.klass.isImmutable
import io.github.nayasis.kotlin.basica.core.validator.nvl
import io.github.nayasis.kotlin.basica.reflection.Reflector
import java.util.regex.Pattern

const val FORMAT_INDEX = "_{{%d}}"

val ESCAPE_REMOVER  = { origin: String -> origin.replace("{{", "{").replace("}}".toRegex(), "}") }
val ESCAPE_DETECTOR = { origin: String, matchIndex: Int -> when {
    matchIndex <= 0 -> false
    origin[matchIndex-1] != '{' -> false
    matchIndex >= 2 && origin[matchIndex-2] == '{' -> false
    else -> true
}}

val DEFAULT_BINDER = { key: BindingKey, param: Map<String, *> ->
    val value = param[key.name]
    val exist = param.containsKey(key.name)
    if (key.format.isEmpty()) {
        value?.toString() ?: if (exist) null else ""
    } else {
        key.format.format(value)
    }
}

private val PATTERN_BASIC = ExtractPattern("\\{([^\\s{}]*?)}".toPattern(), ESCAPE_REMOVER, ESCAPE_DETECTOR)

/**
 * closing quotes and brackets skipped when detecting korean josa
 */
private const val JOSA_SKIPPABLE = "'\"’”)]>）］」』〉》"

/**
 * endings following copula '이' (이다, 이며, 이고, ...)
 */
private const val COPULA_ENDINGS = "다며고면지니므기자든야냐래라"

/**
 * jongsung of korean pronunciation of digit (영,일,이,삼,사,오,육,칠,팔,구)
 */
private val DIGIT_JONGSUNG = charArrayOf('ㅇ','ㄹ',NULL_CHAR,'ㅁ',NULL_CHAR,NULL_CHAR,'ㄱ','ㄹ','ㄹ',NULL_CHAR)

private class Josa(
    val withJongsung: String,
    val withoutJongsung: String,
    val rieulAsVowel: Boolean = false,
    val followedBy: (Char?) -> Boolean = { true },
) {
    private val forms = listOf(withJongsung, withoutJongsung).filter { it.isNotEmpty() }.sortedByDescending { it.length }

    /**
     * return form of josa matched at index of source
     */
    fun match(source: String, index: Int): String? =
        forms.firstOrNull { source.startsWith(it, index) && followedBy(source.getOrNull(index + it.length)) }

    fun select(jongsung: Char): String =
        if(jongsung == NULL_CHAR || (rieulAsVowel && jongsung == 'ㄹ')) withoutJongsung else withJongsung
}

/**
 * pronunciation markup in binding value ({{value|pronunciation}})
 */
private val PATTERN_PRONUNCIATION = "\\{\\{([^{}|]*)\\|([^{}]*)}}".toRegex()

private val NOT_KOREAN ={ c: Char? -> c == null || ! c.isKorean() }

/**
 * josa candidates in order of matching priority (longer and more specific first)
 */
private val JOSAS = listOf(
    Josa("이라고", "라고"),
    Josa("이라는", "라는"),
    Josa("이라도", "라도"),
    Josa("이라면", "라면"),
    Josa("이라서", "라서"),
    Josa("이어서", "여서"),
    Josa("이어도", "여도"),
    Josa("이어야", "여야"),
    Josa("이에요", "예요"),
    Josa("이었", "였"),
    Josa("이나", "나"),
    Josa("이랑", "랑"),
    Josa("이여", "여", followedBy = NOT_KOREAN),
    Josa("으로", "로", rieulAsVowel = true),
    Josa("이", "", followedBy = { it != null && it in COPULA_ENDINGS }),
    Josa("은", "는"),
    Josa("을", "를"),
    Josa("과", "와"),
    Josa("아", "야", followedBy = NOT_KOREAN),
    Josa("이", "가"),
)

class Formatter {

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
     * korean josa following markup is modified by pronunciation of binding value.
     * pronunciation can be given in value as `{{value|pronunciation}}` (ex. "{{James|제임스}}" is printed as "James")
     *
     * @param modifyKorean if true modify josa following parameter binding markup by rule of korean.
     * @return formatted string
     */
    fun bind(format: String, vararg parameter: Any?, modifyKorean: Boolean = true): String =
        bind(pattern=PATTERN_BASIC, format=format, binder= DEFAULT_BINDER, modifyKorean=modifyKorean, parameter=parameter, )

    fun bind(pattern: ExtractPattern, format: String, binder: (key: BindingKey, param: Map<String,*>) -> String?, modifyKorean: Boolean, vararg parameter: Any?,): String {

        val source  = format.also { if(it.isEmpty()) return it }
        val matcher = pattern.pattern.matcher(source)
        val params  = toParam(*parameter)
        val buffer  = StringBuilder()

        var cursor = 0
        var index  = 0

        while(matcher.find()) {

            val start = matcher.start()
            val end   = matcher.end()

            if( pattern.escapeDetector(source,start) ) {
                buffer.append(source.substring(cursor,end))
                cursor = end
                continue
            }

            buffer.append(source.substring(cursor,matcher.start()))

            val key   = BindingKey(matcher.group(1), index)
            val value         = binder(key,params)
            val display       = value?.replace(PATTERN_PRONUNCIATION) { it.groupValues[1] }
            val pronunciation = value?.replace(PATTERN_PRONUNCIATION) { it.groupValues[2] }

            buffer.append(display)

            index++
            cursor = end

            if(modifyKorean) {
                cursor += modifyKorean(pronunciation,cursor,buffer,source)
            }

        }

        buffer.append(source.substring(cursor))

        return pattern.escapeRemover(buffer.toString())

    }

    /**
     * modify korean josa following parameter binding markup
     *
     * @return count of characters consumed from origin
     */
    private fun modifyKorean(param: String?, cursor: Int, buffer: StringBuilder, origin: String): Int {
        val jongsung = param?.let { lastJongsung(it) } ?: return 0
        var index = cursor
        while( index < origin.length && origin[index] in JOSA_SKIPPABLE ) index++
        for( josa in JOSAS ) {
            val matched = josa.match(origin, index) ?: continue
            buffer.append(origin, cursor, index).append(josa.select(jongsung))
            return index - cursor + matched.length
        }
        return 0
    }

    /**
     * get jongsung of last pronounceable character
     *
     * @return jongsung ([NULL_CHAR] if not exists, null if it can not be resolved)
     */
    private fun lastJongsung(value: String): Char? {
        val trimmed = value.trimEnd { it in JOSA_SKIPPABLE || it == '}' }
        val last    = trimmed.lastOrNull() ?: return null
        return when {
            last in '0'..'9' -> DIGIT_JONGSUNG[last - '0']
            last.isAlphabet() -> englishJongsung(trimmed.takeLastWhile { it.isAlphabet() })
            else -> last.disassembleHangul()?.get(2)
        }
    }

    private fun Char.isAlphabet(): Boolean = this in 'a'..'z' || this in 'A'..'Z'

    /**
     * guess jongsung of english word by korean pronunciation heuristically
     *
     * <pre>
     * IBM -> 아이비엠 (ㅁ), Tom -> 톰 (ㅁ), Apple -> 애플 (ㄹ), Nick -> 닉 (ㄱ), James -> 제임스 (none)
     * </pre>
     */
    private fun englishJongsung(word: String): Char {
        if( word.length == 1 || word.all { it.isUpperCase() } ) {
            return when(word.last().uppercaseChar()) {
                'L','R' -> 'ㄹ'
                'M'     -> 'ㅁ'
                'N'     -> 'ㄴ'
                else    -> NULL_CHAR
            }
        }
        val w          = word.lowercase()
        val last       = w.last()
        val afterVowel = w[w.length - 2] in "aeiou"
        return when {
            w.endsWith("ng")                     -> 'ㅇ'
            w.endsWith("mb") || w.endsWith("me") -> 'ㅁ'
            w.endsWith("ne")                     -> 'ㄴ'
            w.endsWith("le")                     -> 'ㄹ'
            w.endsWith("ck")                     -> 'ㄱ'
            w.endsWith("pp")                     -> 'ㅂ'
            w.endsWith("tt")                     -> 'ㅅ'
            last == 'm'                          -> 'ㅁ'
            last == 'n'                          -> 'ㄴ'
            last == 'l'                          -> 'ㄹ'
            last in "kc" && afterVowel           -> 'ㄱ'
            last in "pb" && afterVowel           -> 'ㅂ'
            last == 't'  && afterVowel           -> 'ㅅ'
            else                                 -> NULL_CHAR
        }
    }

    private fun toParam(vararg parameters: Any?): Map<String,*> {
        val params = HashMap<String,Any?>()
        var index = 0
        for (param in parameters) {
            params[FORMAT_INDEX.format(index++)] = param
            if( param == null ) continue
            if( param is Map<*,*>) {
                param.forEach { params[nvl(it.key)] = it.value }
            } else if (! param::class.isImmutable) {
                try {
                    params.putAll(Reflector.toMap(param))
                } catch (e: Exception) {}
            }
        }
        return params
    }

}

data class ExtractPattern(
    val pattern: Pattern,
    val escapeRemover: (origin: String) -> String = {origin -> origin},
    val escapeDetector: (origin: String, matchIndex: Int) -> Boolean = { _,_ -> false},
)

class BindingKey {

    var name: String = ""
    var format: String = ""

    constructor(capture: String, index: Int ) {
        if( capture.isNotEmpty() ) {
            with(capture.split(":")) {
                name = this.first()
                if( this.size >= 2 ) {
                    format = this[1]
                }
            }
        }
        if( name.isEmpty() )
            name = FORMAT_INDEX.format(index)
    }

    override fun toString(): String = "$name:$format"

}