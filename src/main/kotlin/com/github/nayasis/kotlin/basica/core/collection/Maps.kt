package com.github.nayasis.kotlin.basica.core.collection

import com.github.nayasis.kotlin.basica.expression.MvelExpression
import com.github.nayasis.kotlin.basica.reflection.Merger
import com.github.nayasis.kotlin.basica.reflection.Reflector

fun Map<*,*>.flattenKeys(): Map<String,Any?> = Reflector.flattenKeys(this)

fun Map<*,*>.unflattenKeys(): Map<String,Any?> = Reflector.unflattenKeys(this)

fun Map<*,*>.toJson(pretty: Boolean = false, ignoreNull: Boolean = true, view: Class<*>? = null): String = Reflector.toJson(this,pretty,ignoreNull,view)

inline fun <reified T> Map<*,*>.toObject(ignoreNull: Boolean = true): T = Reflector.toObject(this,ignoreNull)

fun Map<*,*>.merge(other: Map<*,*>?, skipEmpty: Boolean = true): Map<*,*> = Merger().merge(other,this, skipEmpty)

@Suppress("UNCHECKED_CAST")
fun <K,V> MutableMap<K,V>.getOrPut(key: K, default:(key: K) -> V): V {
    if( !this.containsKey(key) )
        this[key] = default(key)
    return get(key) as V
}

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

fun <V> Map<*,*>.getByExpr(mvelExpression: String?, default: V? = null ): V? = getOrElse(MvelExpression(mvelExpression)) ?: default