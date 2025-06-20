package com.github.nayasis.kotlin.basica.reflection

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.introspect.AnnotatedMember
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.addDeserializer
import com.fasterxml.jackson.module.kotlin.addSerializer
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.github.nayasis.kotlin.basica.core.string.escapeRegex
import com.github.nayasis.kotlin.basica.core.validator.isEmpty
import com.github.nayasis.kotlin.basica.reflection.serializer.DateDeserializer
import com.github.nayasis.kotlin.basica.reflection.serializer.DateSerializer
import java.beans.Transient
import java.io.File
import java.io.InputStream
import java.io.Reader
import java.lang.reflect.ParameterizedType
import java.net.URL
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

@Suppress("DuplicatedCode")
class Reflector { companion object {

    private val mapper = createJsonMapper()
    private val nullMapper = createJsonMapper(ignoreNull = false)

    @JvmStatic
    fun createJsonMapper(
        ignoreNull: Boolean     = true,
        sort: Boolean           = false,
        skipJsonIgnore: Boolean = false,
    ): ObjectMapper {
        return jsonMapper {

            configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
            configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            configure(SerializationFeature.WRITE_CHAR_ARRAYS_AS_JSON_ARRAYS, true)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
            configure(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS, true)
            configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)

            // let date to text like "yyyy-mm-dd'T'hh:mi:ss"
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            addModule(JavaTimeModule())
            addModule( SimpleModule(javaClass.simpleName).apply {
                addSerializer(Date::class, DateSerializer())
                addDeserializer(Date::class, DateDeserializer())
            })

            // only convert by Class' field.
            visibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
            visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)

            // Prevent error when pojo with @JsonFilter annotation is parsed.
            filterProvider(SimpleFilterProvider().setFailOnUnknownId(false))

            if(ignoreNull)
                serializationInclusion(Include.NON_NULL)

            if( skipJsonIgnore )
                annotationIntrospector(object: JacksonAnnotationIntrospector() {
                    override fun hasIgnoreMarker(m: AnnotatedMember?): Boolean {
                        val annotation = m?.getAnnotation(Transient::class.java)
                        return annotation?.value ?: false
                    }
                })

            if( sort ) {
                configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            }

            addModule(kotlinModule{})

        }
    }

    private fun mapper(ignoreNull: Boolean = true) : ObjectMapper = if( ignoreNull ) mapper else nullMapper

    @JvmStatic
    fun toJson(obj: Any?, pretty: Boolean = false, ignoreNull: Boolean = true, view: Class<*>? = null): String {
        return when (obj) {
            null -> ""
            else -> {
                val writer = mapper(ignoreNull).let{ if (pretty) it.writerWithDefaultPrettyPrinter() else it.writer() }
                view?.let { writer.withView(it) }
                writer.writeValueAsString(obj)
            }
        }
    }

    @JvmStatic
    fun isJson(string: CharSequence?): Boolean {
        if( string.isNullOrEmpty() ) return false
        return try {
            mapper.readTree(string as? String ?: string.toString()); true
        } catch (e: Exception) {
            false
        }
    }

    @JvmStatic
    fun <T> toObject(src: Any?, ignoreNull: Boolean = true): T {
        val mapper    = mapper(ignoreNull)
        val typeref   = object : TypeReference<T>() {}
        return when (src) {
            null            -> mapper.readValue(emptyJson(typeref), typeref)
            is CharSequence -> mapper.readValue(src.toString().ifEmpty{emptyJson(typeref)}, typeref)
            is File         -> mapper.readValue(src, typeref)
            is URL          -> mapper.readValue(src, typeref)
            is Reader       -> mapper.readValue(src, typeref)
            is InputStream  -> mapper.readValue(src, typeref)
            is ByteArray    -> mapper.readValue(src, typeref)
            else            -> mapper.convertValue(src,typeref)
        }
    }

    @JvmStatic
    fun <T: Any> toObject(src: Any?, typeClass: KClass<T>, ignoreNull: Boolean = true): T {
        val mapper  = mapper(ignoreNull)
        val typeref = typeClass.java
        return when (src) {
            null            -> mapper.readValue(emptyJson(typeClass), typeref)
            is CharSequence -> mapper.readValue(src.toString().let { it.ifEmpty { emptyJson(typeClass) } }, typeref)
            is File         -> mapper.readValue(src, typeref)
            is URL          -> mapper.readValue(src, typeref)
            is Reader       -> mapper.readValue(src, typeref)
            is InputStream  -> mapper.readValue(src, typeref)
            is ByteArray    -> mapper.readValue(src, typeref)
            else            -> mapper.convertValue(src,typeref)
        }
    }

    @JvmStatic
    fun <T> toObject(src: Any?, typeref: TypeReference<T>, ignoreNull: Boolean = true): T {
        val mapper = mapper(ignoreNull)
        return when (src) {
            null            -> mapper.readValue(emptyJson(typeref.type::class), typeref)
            is CharSequence -> mapper.readValue(src.toString().let { it.ifEmpty { emptyJson(typeref.type::class) } }, typeref)
            is File         -> mapper.readValue(src, typeref)
            is URL          -> mapper.readValue(src, typeref)
            is Reader       -> mapper.readValue(src, typeref)
            is InputStream  -> mapper.readValue(src, typeref)
            is ByteArray    -> mapper.readValue(src, typeref)
            else            -> mapper.convertValue(src,typeref)
        }
    }

    @JvmStatic
    fun <T> convert(src: Any?, typeref: TypeReference<T>, ignoreNull: Boolean = true): T {
        val mapper = mapper(ignoreNull)
        return when (src) {
            null            -> mapper.readValue(emptyJson(typeref.type::class), typeref)
            is CharSequence -> mapper.readValue(src.toString().let { it.ifEmpty { emptyJson(typeref.type::class) } }, typeref)
            is File         -> mapper.readValue(src, typeref)
            is URL          -> mapper.readValue(src, typeref)
            is Reader       -> mapper.readValue(src, typeref)
            is InputStream  -> mapper.readValue(src, typeref)
            is ByteArray    -> mapper.readValue(src, typeref)
            else            -> mapper.convertValue(src,typeref)
        }
    }


    @JvmStatic
    fun toMap(src: Any?, ignoreNull: Boolean = true): Map<String,Any?> = toObject(src,ignoreNull)

    @JvmStatic
    fun flattenKeys(obj: Any?): Map<String,Any?> {
        val map = HashMap<String,Any?>().also{ if(isEmpty(obj)) return it}
        flattenKeys("", toMap(obj), map)
        return map
    }

    private fun flattenKeys(path: String, obj: Any?, result: MutableMap<String,Any?>) {
        when (obj) {
            is Map<*,*> -> {
                val prefix = if (path.isEmpty()) "" else "${path}."
                obj.forEach{ key, value -> flattenKeys("$prefix$key", value, result) }
            }
            is Collection<*> ->
                obj.withIndex().forEach { flattenKeys("$path[${it.index}]", it.value, result ) }
            is Array<*> ->
                obj.withIndex().forEach { flattenKeys("$path[${it.index}]", it.value, result ) }
            else -> result[path] = obj
        }
    }

    @JvmStatic
    fun unflattenKeys( obj: Any? ): Map<String,Any?> {
        val map = HashMap<String,Any?>().also{ if(isEmpty(obj)) return it}
        toMap(obj).forEach{ (key, value) -> unflattenKeys(key,value,map)}
        return map
    }

    @Suppress("UNCHECKED_CAST")
    private fun unflattenKeys(pathParent: String, value: Any?, result: MutableMap<String,Any?>) {

        val key     = pathParent.replaceFirst("""\[.*?]""".toRegex(), "").replaceFirst("""\..*?$""".toRegex(), "")
        val index   = pathParent.replaceFirst("""^(${key.escapeRegex()})\[(.*?)](.*?)$""".toRegex(), "$2").let{ if(it==pathParent) "" else it}
        val isArray = index.isNotEmpty()

        val pathCurr = "$key${if(isArray)"[$index]" else ""}"
        val isKey    = pathCurr == pathParent

        if (isKey) {
            if (isArray) {
                setValueToList(key, index.toIntOrNull() ?: 0, value, result)
            } else {
                result[key] = value
            }
        } else {
            if (!result.containsKey(key)) {
                result[key] = if (isArray) ArrayList<Any?>() else HashMap<String,Any?>()
            }
            val newVal = if (isArray) {
                val list = result[key] as List<*>
                val idx  = index.toIntOrNull() ?: 0
                if (list.size <= idx || list[idx] == null) {
                    setValueToList(key, idx, HashMap<String,Any?>(), result)
                }
                list[idx] as MutableMap<String,Any?>?
            } else {
                result[key] as MutableMap<String,Any?>?
            }
            newVal?.let {
                val pathChild = pathParent.replaceFirst("${pathCurr.replace("[", "\\[")}.".toRegex(), "")
                unflattenKeys(pathChild, value, newVal)
            }
        }

    }

    @Suppress("UNCHECKED_CAST")
    private fun setValueToList(key: String, idx: Int, value: Any?, result: MutableMap<String,Any?>) {
        if ( ! result.containsKey(key) )
            result[key] = ArrayList<Any?>()
        val list = result[key] as MutableList<Any?>
        if (idx >= list.size) {
            for (i in list.size..idx)
                list.add(null)
        }
        list[idx] = value
    }

    @JvmStatic
    inline fun <reified T> merge(from: Any?, to: T?, skipEmpty: Boolean = true): T {
        return Merger.merge(from,to,skipEmpty)
    }

    @JvmStatic
    inline fun <reified T> clone(source: T?): T {
        return toObject(toJson(source))
    }

}}

private fun emptyJson(typeref: TypeReference<*>): String {
    return try {
        val klass = (typeref.type as ParameterizedType).rawType as Class<*>
        emptyJson(klass.kotlin)
    } catch (e: Exception) {
        "{}"
    }
}

private fun emptyJson(klass: KClass<*>): String =
    if( klass.isSubclassOf(Collection::class) || klass.isSubclassOf(Array::class) ) "[]" else "{}"

