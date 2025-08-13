package io.github.nayasis.kotlin.basica.core.collection

import io.github.nayasis.kotlin.basica.core.string.urlEncode
import io.github.nayasis.kotlin.basica.expression.MvelExpression
import io.github.nayasis.kotlin.basica.model.NGrid
import io.github.nayasis.kotlin.basica.reflection.Merger
import io.github.nayasis.kotlin.basica.reflection.Reflector
import java.nio.charset.Charset

fun Map<*,*>.flattenKeys(): Map<String,Any?> = Reflector.flattenKeys(this)

fun Map<*,*>.unflattenKeys(): Map<String,Any?> = Reflector.unflattenKeys(this)

fun Map<*,*>.toJson(pretty: Boolean = false, ignoreNull: Boolean = true, view: Class<*>? = null): String = Reflector.toJson(this,pretty,ignoreNull,view)

inline fun <reified T> Map<*,*>.toObject(ignoreNull: Boolean = true): T = Reflector.toObject(this, ignoreNull)

fun Map<*,*>.merge(other: Map<*,*>?, skipEmpty: Boolean = true): MutableMap<*,*> = Merger().merge(other,this, skipEmpty)

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
        grid.addRow("key", key)
        if(showType)
            grid.addRow("type",value?.let{it::class.simpleName})
        grid.addRow("value", value)
    }
    return grid.toString(showHeader=false, maxRowcount=rowcount)
}

fun Map<*,*>.toUrlParam(charset: Charset = Charsets.UTF_8): String {
    return this.map { "${it.key.toString().urlEncode(charset)}=${it.value.toString().urlEncode(charset)}" }.joinToString("&")
}