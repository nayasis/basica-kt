package com.github.nayasis.kotlin.basica.core.validator

import com.github.nayasis.kotlin.basica.core.klass.isEnum
import com.github.nayasis.kotlin.basica.core.klass.isPrimitive
import com.github.nayasis.kotlin.basica.core.number.cast
import com.github.nayasis.kotlin.basica.core.string.toNumber
import com.github.nayasis.kotlin.basica.reflection.Reflector
import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.Path
import java.time.temporal.Temporal
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

val Any?.isPojo: Boolean
    get() {
        if( this == null ) return false
        val klass = this::class
        return when {
            klass.isPrimitive -> false
            klass.java.isAnnotation -> false
            klass.isEnum -> false
            klass.isValue -> false
            klass.isData -> true
            this is Boolean -> false
            this is CharSequence -> false
            this is Char -> false
            this is Number -> false
            this is Date -> false
            this is Calendar -> false
            this is Temporal -> false
            this is URI -> false
            this is URL -> false
            this is File -> false
            this is Path -> false
            else -> true
        }
    }

@Suppress("UNCHECKED_CAST")
fun <T: Any> Any?.cast(typeClass: KClass<T>, ignoreError: Boolean = true): T? {
    return when {
        this == null -> null
        this::class.isSubclassOf(typeClass) -> this as T
        typeClass == String::class -> this.toString() as T
        (this is CharSequence || this is Char) && typeClass.isSubclassOf(Number::class) ->
            this.toString().toNumber(typeClass as KClass<Number>) as T
        this is Number ->
            this.cast(typeClass as KClass<Number>) as T
        else -> runCatching {
            Reflector.toObject(this, typeClass)
        }.getOrElse { e ->
            if(!ignoreError) null else throw ClassCastException("Value($this) cannot be cast to ${typeClass.simpleName}")
        }
    }
}

inline fun <reified T: Any> Any?.cast(ignoreError: Boolean = true): T? {
    return this.cast(T::class, ignoreError)
}