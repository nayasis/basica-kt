package io.github.nayasis.kotlin.basica.core.validator

import io.github.nayasis.kotlin.basica.core.klass.isEnum
import io.github.nayasis.kotlin.basica.core.klass.isPrimitive
import io.github.nayasis.kotlin.basica.core.localdate.*
import io.github.nayasis.kotlin.basica.core.number.cast
import io.github.nayasis.kotlin.basica.core.string.toNumber
import io.github.nayasis.kotlin.basica.reflection.Reflector
import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.Path
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.temporal.Temporal
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

val Any?.isPojo: Boolean
    get() = when {
        this == null -> false
        this::class.isPrimitive -> false
        this::class.java.isAnnotation -> false
        this::class.isEnum -> false
        this::class.isValue -> false
        this::class.isData -> true
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

@Suppress("UNCHECKED_CAST")
fun <T: Any> Any.cast(typeClass: KClass<T>): T {
    return when {
        typeClass.isInstance(this) -> this as T
        typeClass == String::class -> this.toString() as T
        (this is String || this is CharSequence || this is Char) ->
            when {
                typeClass.isSubclassOf(Number::class) ->
                    this.toString().toNumber(typeClass as KClass<Number>) as T
                typeClass.isSubclassOf(LocalDate::class) ->
                    this.toString().toLocalDate() as T
                typeClass.isSubclassOf(LocalDateTime::class) ->
                    this.toString().toLocalDateTime() as T
                typeClass.isSubclassOf(ZonedDateTime::class) ->
                    this.toString().toZonedDateTime() as T
                typeClass.isSubclassOf(Date::class) ->
                    this.toString().toDate() as T
                typeClass.isSubclassOf(Calendar::class) ->
                    this.toString().toCalendar() as T
                else -> null
            }
        this is Number ->
            this.cast(typeClass as KClass<Number>) as T
        else -> null
    } ?: runCatching {
        Reflector.toObject(this, typeClass)
    }.getOrElse { e ->
        throw ClassCastException("Value($this) cannot be cast to ${typeClass.simpleName}")
    }
}

inline fun <reified T: Any> Any.cast(): T {
    return this.cast(T::class)
}

fun <T: Any> Any.castNullable(typeClass: KClass<T>): T? {
    return runCatching { this.cast(typeClass) }.getOrNull()
}

inline fun <reified T: Any> Any.castNullable(): T? {
    return this.castNullable(T::class)
}