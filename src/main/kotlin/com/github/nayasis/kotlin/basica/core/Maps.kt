package com.github.nayasis.kotlin.basica.core

import com.github.nayasis.kotlin.basica.expression.MvelExpression
import com.github.nayasis.kotlin.basica.reflection.Reflector

fun Map<*,*>.flattenKeys(): Map<String,Any?> = Reflector.flattenKeys(this)

fun Map<*,*>.unflattenKeys(): Map<String,Any?> = Reflector.unflattenKeys(this)

fun Map<*,*>.toJson(pretty: Boolean = false, ignoreNull: Boolean = true, view: Class<*>? = null): String = Reflector.toJson(this,pretty,ignoreNull,view)

inline fun <reified T> Map<*,*>.toObject(ignoreNull: Boolean = true): T = Reflector.toObject(this,ignoreNull)

fun <V> Map<*,*>.get(expression: MvelExpression? ): V? {
    return when(expression) {
        null -> null
        else -> try {
            expression.run<Any>(this) as V
        } catch (e: Exception) {
            null
        }
    }
}

fun <V> Map<*,*>.getOrElse(expression: MvelExpression?, defaultVal: V? = null ): V? = get(expression) ?: defaultVal

fun <V> Map<*,*>.getByExpr(mvelExpression: String?, defaultVal: V? = null ): V? = getOrElse(MvelExpression(mvelExpression),defaultVal)