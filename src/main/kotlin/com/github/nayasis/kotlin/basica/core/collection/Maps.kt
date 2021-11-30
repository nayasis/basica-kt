package com.github.nayasis.kotlin.basica.core.collection

import com.github.nayasis.kotlin.basica.core.string.toUrlParam
import com.github.nayasis.kotlin.basica.expression.MvelExpression
import com.github.nayasis.kotlin.basica.model.NGrid
import com.github.nayasis.kotlin.basica.reflection.Merger
import com.github.nayasis.kotlin.basica.reflection.Reflector
import java.nio.charset.Charset

fun Map<*,*>.flattenKeys(): Map<String,Any?> = Reflector.flattenKeys(this)

fun Map<*,*>.unflattenKeys(): Map<String,Any?> = Reflector.unflattenKeys(this)

fun Map<*,*>.toJson(pretty: Boolean = false, ignoreNull: Boolean = true, view: Class<*>? = null): String = Reflector.toJson(this,pretty,ignoreNull,view)

inline fun <reified T> Map<*,*>.toObject(ignoreNull: Boolean = true): T = Reflector.toObject(this,ignoreNull)

fun Map<*,*>.merge(other: Map<*,*>?, skipEmpty: Boolean = true): Map<*,*> = Merger().merge(other,this, skipEmpty)

@Suppress("UNCHECKED_CAST")
fun <V> Map<*,*>.get(expression: MvelExpression? ): V? {
    return when(expression) {
        null -> null
        else -> try {
            expression.run<Any>(this) as V?
        } catch (e: Exception) {
            null
        }
    }
}

fun <V> Map<*,*>.getOrElse(expression: MvelExpression?): V? = get(expression)

fun <V> Map<*,*>.getOrDefault(expression: MvelExpression?, default: V): V = get(expression) ?: default

fun <V> Map<*,*>.getByExpr(mvelExpression: String?, default: V? = null): V? = getOrElse(MvelExpression(mvelExpression)) ?: default

fun Map<*,*>.toString(showType: Boolean, rowcount:Int = 500): String {
    val grid = NGrid()
    forEach { (key,value) ->
        grid.addData("key", key)
        if(showType)
            grid.addData("type",value?.let{it::class.simpleName})
        grid.addData("value", value)
    }
    return grid.toString(showHeader=false, rowcount=rowcount)
}

fun Map<*,*>.toUrlParam(charset: Charset = Charsets.UTF_8): String {
    return this.map { "${it.key.toString().toUrlParam(charset)}=${it.value.toString().toUrlParam(charset)}" }.joinToString("&")
}