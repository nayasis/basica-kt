package com.github.nayasis.kotlin.basica.reflection

import com.github.nayasis.kotlin.basica.reflection.Result.NOTHING
import com.github.nayasis.kotlin.basica.core.validator.isEmpty
import java.lang.IllegalArgumentException

/**
 * Data merger
 */
class Merger {

    companion object {
        inline fun <reified T> merge(from: Any?, to: T?, skipEmpty: Boolean = true): T {
            if( isList(from) xor isList(to))
                throw IllegalArgumentException("can not merge array to non-array")
            return when {
                isEmpty(to) -> Reflector.toObject(from,skipEmpty)
                isEmpty(from) -> Reflector.toObject(to,skipEmpty)
                from is Map<*,*> && to is Map<*,*> -> Reflector.toObject(Merger().merge(from,to,skipEmpty))
                isList(from) || isList(to) -> Reflector.toObject(Merger().merge(toList(from), toList(to),skipEmpty))
                else -> Reflector.toObject(Merger().merge(Reflector.toMap(from), Reflector.toMap(to),skipEmpty))
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun merge(from: Map<*,*>?, to: Map<*,*>?, skipEmpty: Boolean = true ): Map<*,*> {

        val rs = HashMap<Any?,Any?>()

        if( from.isNullOrEmpty() && to.isNullOrEmpty() ) {
            return rs
        } else if( from.isNullOrEmpty() ) {
            return rs.apply { putAll(to as Map<Any?, Any?>) }
        } else if( to.isNullOrEmpty() ) {
            return rs.apply { putAll(from as Map<Any?, Any?>) }
        }

        rs.putAll(to as Map<Any?, Any?>)

        for( key in from.keys ) {

            val fromVal = from[key]
            val toVal   = to[key]

            if( skipEmpty && isEmpty(fromVal) ) continue

            if( ! to.containsKey(key) || toVal == null ) {
                rs[key] = fromVal
            } else if( fromVal is Map<*,*> && toVal is Map<*,*> ) {
                rs[key] = merge(fromVal,toVal)
            } else if( isList(fromVal) || isList(toVal) ) {
                rs[key] = merge(toList(fromVal),toList(toVal))
            } else {
                rs[key] = fromVal
            }

        }

        return rs

    }

    fun merge(from: List<*>?, to: List<*>?, skipEmpty: Boolean = true): MutableList<*> {

        val rs = ArrayList<Any?>()

        if( from.isNullOrEmpty() && to.isNullOrEmpty() ) {
            return rs
        } else if( from.isNullOrEmpty() ) {
            return rs.apply { addAll(to!!) }
        } else if( to.isNullOrEmpty() ) {
            return rs.apply { addAll(from) }
        }

        val iteratorFrom = from.iterator()
        val iteratorTo   = to.iterator()

        while(true) {
            val fromVal = if( iteratorFrom.hasNext() ) iteratorFrom.next() else NOTHING
            val toVal   = if( iteratorTo.hasNext()   ) iteratorTo.next()   else NOTHING
            if( fromVal == NOTHING && toVal == NOTHING ) {
                break
            } else if( fromVal == NOTHING ) {
                rs.add(toVal)
            } else if( toVal == NOTHING ) {
                rs.add(fromVal)
            } else if( skipEmpty && isEmpty(fromVal) ) {
                rs.add(toVal)
            } else {
                if( toVal == null ) {
                    rs.add(fromVal)
                } else if( fromVal is Map<*,*> && toVal is Map<*,*> ) {
                    rs.add(merge(fromVal,toVal))
                } else if( isList(fromVal) || isList(toVal) ){
                    rs.add( merge(toList(fromVal),toList(toVal)) )
                } else {
                    rs.add(fromVal)
                }
            }
        }

        return rs

    }

}

private enum class Result {
    NOTHING
}

@PublishedApi
internal fun isList(value: Any?): Boolean {
    return value is Collection<*> || value is Array<*>
}

@PublishedApi
internal fun toList(value:Any?): MutableList<*> {
    return when(value) {
        null -> ArrayList<Any?>()
        is Collection<*> -> value.toMutableList()
        is Array<*> -> value.toMutableList()
        else -> listOf(value).toMutableList()
    }
}