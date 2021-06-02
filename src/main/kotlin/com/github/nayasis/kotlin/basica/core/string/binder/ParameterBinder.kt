package com.github.nayasis.kotlin.basica.core.string.binder

import com.github.nayasis.kotlin.basica.core.character.hasHangulJongsung
import com.github.nayasis.kotlin.basica.core.character.isKorean
import com.github.nayasis.kotlin.basica.core.validator.nvl
import com.github.nayasis.kotlin.basica.reflection.Reflector

const val FORMAT_INDEX = "_{{%d}}"

class ParameterBinder {

    fun <T> bind( pattern: ExtractPattern, format: Any?, parameter: T, binder: (BindingKey,T) -> String, modifyKorean: Boolean ): String {

        val source  = nvl(format).also { if(it.isEmpty()) return it }
        val matcher = pattern.pattern.matcher(source)
        val buffer  = StringBuilder()

        var cursor = 0
        var index  = 0

        while(matcher.find()) {

            val prefix = source.substring(cursor, matcher.start())
            if( pattern.escapable(prefix) )
                continue

            pattern.replacer?.let{ buffer.append(it(prefix)) }

            val key   = BindingKey(matcher.group(1), index)
            val value = binder(key,parameter)

            buffer.append(value)

            index++
            cursor = matcher.end()

            if(modifyKorean) {
                if(modifyKorean(value,cursor,buffer,source) )
                    cursor++
            }

        }

        buffer.append(source.substring(cursor))

        return buffer.toString()

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

    private fun toParam(vararg parameters: Any): Map<String,*> {
        val params = HashMap<String,Any?>()
        if (parameters.size == 1) {
            val value = parameters[0]
            if( value is Map<*,*> ) {
                value.forEach{params[nvl(it.key)] = it.value}
            } else {
                params.putAll(Reflector.toMap(value))
            }
        }
        var index = 0
        for (param in parameters)
            params[FORMAT_INDEX.format(index++)] = param
        return params
    }

}