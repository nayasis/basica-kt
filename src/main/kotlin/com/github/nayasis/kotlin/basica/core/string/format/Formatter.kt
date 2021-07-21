package com.github.nayasis.kotlin.basica.core.string.format

import com.github.nayasis.kotlin.basica.core.character.hasHangulJongsung
import com.github.nayasis.kotlin.basica.core.character.isKorean
import com.github.nayasis.kotlin.basica.core.klass.isImmutable
import com.github.nayasis.kotlin.basica.core.validator.nvl
import com.github.nayasis.kotlin.basica.reflection.Reflector
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
     * @param modifyKorean if true modify first outer character of parameter binding markup by rule of korean.
     * @return formatted string
     */
    fun bindSimple(format: String, vararg parameter: Any?, modifyKorean: Boolean = true): String =
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
            val value = binder(key,params)

            buffer.append(value)

            index++
            cursor = end

            if(modifyKorean) {
                if(modifyKorean(value,cursor,buffer,source) )
                    cursor++
            }

        }

        buffer.append(source.substring(cursor))

        return pattern.escapeRemover(buffer.toString())

    }

    private fun modifyKorean(param: String?, cursor: Int, buffer: StringBuilder, origin: String): Boolean {
        if( param.isNullOrEmpty() || cursor >= origin.length ) return false
        if( param.last().isKorean() ) {
            val hasJongsung = param.last().hasHangulJongsung()
            when(origin[cursor]) {
                '은','는' -> buffer.append( if(hasJongsung) '은' else '는' )
                '이','가' -> buffer.append( if(hasJongsung) '이' else '가' )
                '을','를' -> buffer.append( if(hasJongsung) '을' else '를' )
                else -> return false
            }
            return true
        }
        return false
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