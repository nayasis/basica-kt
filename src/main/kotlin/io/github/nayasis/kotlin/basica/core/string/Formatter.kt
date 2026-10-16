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
 * pronunciation markup in binding value ({{value|pronunciation}}, escaped by backslash as \{{value|pronunciation}})
 */
private val PATTERN_PRONUNCIATION = "(\\\\?)\\{\\{([^{}|]*)\\|([^{}]*)}}".toRegex()

/**
 * marker of binding value in buffer to protect it from escape remover
 */
private const val VALUE_MARK_START = '\uE000'
private const val VALUE_MARK_END   = '\uE001'
private val PATTERN_VALUE_MARK = "$VALUE_MARK_START(\\d+)$VALUE_MARK_END".toRegex()

/**
 * endings following copula '이' whose '이' is omitted after character without jongsung (사과이다 -> 사과다, 사과이고 -> 사과고, ...)
 *
 * '이' followed by other korean character is kept as it is (이자, 이상, 이하, 이내, ...)
 */
private const val COPULA_ENDINGS = "다며고면지니므기든나랑야여시라란래냐요죠네군구거던더잖세셨"

/**
 * jongsung of korean pronunciation of digit (영,일,이,삼,사,오,육,칠,팔,구)
 */
private val DIGIT_JONGSUNG = charArrayOf('ㅇ','ㄹ',NULL_CHAR,'ㅁ',NULL_CHAR,NULL_CHAR,'ㄱ','ㄹ','ㄹ',NULL_CHAR)

/**
 * condition of josa followed by non korean or another josa (ex. 과의, 은요, 으로부터)
 */
private fun notKoreanOr(josa: String) = { c: Char? -> c == null || ! c.isKorean() || c in josa }

private val NOT_KOREAN = notKoreanOr("")

/**
 * korean josa changed by jongsung of preceding word
 *
 * @param withJongsung      form used after jongsung
 * @param withoutJongsung   form used after no jongsung
 * @param aliases           other forms written in template (ex. "을(를)", "(으)로")
 * @param rieulAsVowel      treat jongsung 'ㄹ' as no jongsung (only for 으로/로)
 * @param followedBy        condition of character following josa in template
 */
private class Josa(
    val withJongsung: String,
    val withoutJongsung: String,
    aliases: List<String> = emptyList(),
    val rieulAsVowel: Boolean = false,
    val followedBy: (Char?) -> Boolean = { true },
) {
    private val forms = (listOf(withJongsung, withoutJongsung) + aliases).filter { it.isNotEmpty() }.sortedByDescending { it.length }

    /**
     * return form of josa matched at index of source
     */
    fun match(source: String, index: Int): String? =
        forms.firstOrNull { source.startsWith(it, index) && followedBy(source.getOrNull(index + it.length)) }

    fun select(jongsung: Char): String =
        if(jongsung == NULL_CHAR || (rieulAsVowel && jongsung == 'ㄹ')) withoutJongsung else withJongsung
}

/**
 * josa candidates in order of matching priority (longer and more specific first)
 *
 * josa written in one character form without jongsung (ex. "나", "다", "란") is matched only when it is not followed by korean
 * to avoid changing other words (ex. 나라, 다음, 란에)
 */
private val JOSAS = listOf(
    // contraction of copula
    Josa("이에요", "예요", listOf("(이)에요", "에요")),
    Josa("이었", "였", listOf("(이)었", "었")),
    Josa("이어서", "여서", listOf("(이)어서")),
    Josa("이어도", "여도", listOf("(이)어도")),
    Josa("이어야", "여야", listOf("(이)어야")),
    // josa starting with '이' after jongsung
    Josa("이야말로", "야말로"),
    Josa("이라고", "라고"),
    Josa("이라는", "라는"),
    Josa("이라도", "라도"),
    Josa("이라면", "라면"),
    Josa("이라서", "라서"),
    Josa("이나마", "나마"),
    Josa("이든지", "든지"),
    Josa("이든가", "든가"),
    Josa("이시여", "시여"),
    Josa("이지만", "지만"),
    Josa("이니까", "니까"),
    Josa("이므로", "므로"),
    Josa("이세요", "세요"),
    Josa("이셨", "셨"),
    Josa("이랑", "랑"),
    Josa("이라", "라", followedBy = NOT_KOREAN),
    Josa("이란", "란", followedBy = NOT_KOREAN),
    Josa("이나", "나", followedBy = NOT_KOREAN),
    Josa("이든", "든", followedBy = NOT_KOREAN),
    Josa("이여", "여", followedBy = NOT_KOREAN),
    Josa("이요", "요", followedBy = NOT_KOREAN),
    Josa("이다", "다", followedBy = NOT_KOREAN),
    Josa("이며", "며", followedBy = NOT_KOREAN),
    Josa("이고", "고", followedBy = NOT_KOREAN),
    Josa("이면", "면", followedBy = NOT_KOREAN),
    Josa("이지", "지", followedBy = NOT_KOREAN),
    Josa("이니", "니", followedBy = NOT_KOREAN),
    Josa("이냐", "냐", followedBy = NOT_KOREAN),
    Josa("이", "", listOf("(이)"), followedBy = { it != null && it in COPULA_ENDINGS }),
    // josa alternated by jongsung
    Josa("으로", "로", listOf("(으)로"), rieulAsVowel = true, followedBy = notKoreanOr("서써부는도만의요")),
    Josa("은", "는", listOf("은(는)", "는(은)"), followedBy = notKoreanOr("요")),
    Josa("을", "를", listOf("을(를)", "를(을)"), followedBy = notKoreanOr("요")),
    Josa("과", "와", listOf("과(와)", "와(과)"), followedBy = notKoreanOr("의도는만")),
    Josa("아", "야", listOf("아(야)", "야(아)"), followedBy = NOT_KOREAN),
    Josa("이", "가", listOf("이(가)", "가(이)"), followedBy = NOT_KOREAN),
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
     * korean josa following markup is modified by pronunciation of binding value.
     * pronunciation can be given in value as `{{value|pronunciation}}` (ex. "{{James|제임스}}" is printed as "James")
     * value can be written in any language, and pronunciation must be written in hangul (ex. "{{東京|도쿄}}", "{{München|뮌헨}}")
     * value without pronunciation markup is guessed only when it ends with hangul, digit or english alphabet.
     * to print markup as it is, escape it with backslash (ex. "\{{James|제임스}}" is printed as "{{James|제임스}}")
     *
     * @receiver template
     * @param parameter binding parameter
     * @param modifyKorean if true modify josa following parameter binding markup by rule of korean.
     *                     (if false, binding value including pronunciation markup is printed as it is)
     * @return formatted string
     */
    fun bind(format: String, vararg parameter: Any?, modifyKorean: Boolean = true): String =
        bind(pattern=PATTERN_BASIC, format=format, binder= DEFAULT_BINDER, modifyKorean=modifyKorean, parameter=parameter, )

    fun bind(pattern: ExtractPattern, format: String, binder: (key: BindingKey, param: Map<String,*>) -> String?, modifyKorean: Boolean, vararg parameter: Any?,): String {

        val source  = format.also { if(it.isEmpty()) return it }
        val matcher = pattern.pattern.matcher(source)
        val params  = toParam(*parameter)
        val buffer  = StringBuilder()
        val values  = mutableListOf<String?>()

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
            val value = binder(key,params)

            buffer.append(VALUE_MARK_START).append(values.size).append(VALUE_MARK_END)

            index++
            cursor = end

            if(modifyKorean) {
                values.add(value?.resolvePronunciation(display = true))
                cursor += modifyKorean(value?.resolvePronunciation(display = false),cursor,buffer,source)
            } else {
                values.add(value)
            }

        }

        buffer.append(source.substring(cursor))

        return pattern.escapeRemover(buffer.toString()).replace(PATTERN_VALUE_MARK) { "${values[it.groupValues[1].toInt()]}" }

    }

    /**
     * resolve pronunciation markup in binding value
     *
     * @param display if true, resolve to value to display. if false, resolve to pronunciation.
     */
    private fun String.resolvePronunciation(display: Boolean): String = replace(PATTERN_PRONUNCIATION) {
        val (escape, value, pronunciation) = it.destructured
        when {
            escape.isNotEmpty() -> it.value.substring(1)
            display             -> value
            else                -> pronunciation
        }
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