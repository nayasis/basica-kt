@file:Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")

package com.github.nayasis.kotlin.basica.reflection

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.JsonParser
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
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.github.nayasis.kotlin.basica.core.isEmpty
import com.github.nayasis.kotlin.basica.core.nvl
import com.github.nayasis.kotlin.basica.reflection.serializer.DateDeserializer
import com.github.nayasis.kotlin.basica.reflection.serializer.DateSerializer
import java.beans.Transient
import java.io.File
import java.io.InputStream
import java.io.Reader
import java.net.URL
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.KClass

@Suppress("DuplicatedCode")
class Reflector { companion object {

    private val mapper = createJsonMapper()
    private val nullMapper = createJsonMapper(ignoreNull = false)

    private fun createJsonMapper(
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

            addModule(
                SimpleModule("${javaClass.simpleName},ignore-nul:${ignoreNull}").apply {
                    addSerializer(Date::class, DateSerializer())
                    addDeserializer( Date::class, DateDeserializer() )
                })
            addModule(kotlinModule{})

        }

    }

    private fun mapper(ignoreNull: Boolean = true) : ObjectMapper = if( ignoreNull ) mapper else nullMapper

    fun toJson( obj: Any?, pretty: Boolean = false, ignoreNull: Boolean = true, view: Class<*>? = null ): String {
        return when (obj) {
            null -> ""
            else -> {
                val writer = mapper(ignoreNull).let{ if (pretty) it.writerWithDefaultPrettyPrinter() else it.writer() }
                view?.let { writer.withView(it) }
                writer.writeValueAsString(obj)
            }
        }
    }

    fun isJson(string: String?): Boolean {
        return try {
            mapper.readTree(string); true
        } catch (e: Exception) {
            false
        }
    }

    inline fun <reified T> toObject(src: Any?, ignoreNull: Boolean = true): T {
        val mapper  = mapper(ignoreNull)
        val typeref = jacksonTypeRef<T>()
        return when (src) {
            null            -> mapper.readValue("", typeref)
            is CharSequence -> mapper.readValue(nvl(src.toString(),""), typeref)
            is File         -> mapper.readValue(src, typeref)
            is URL          -> mapper.readValue(src, typeref)
            is Reader       -> mapper.readValue(src, typeref)
            is InputStream  -> mapper.readValue(src, typeref)
            is ByteArray    -> mapper.readValue(src, typeref)
            else            -> mapper.convertValue(src,typeref)
        }
    }

    fun <T:Any> toObject(src: Any?, typeClass: KClass<T>, ignoreNull: Boolean = true): T {
        val mapper  = mapper(ignoreNull)
        val typeref = typeClass.java
        return when (src) {
            null            -> mapper.readValue("", typeref)
            is CharSequence -> mapper.readValue(nvl(src.toString(),""), typeref)
            is File         -> mapper.readValue(src, typeref)
            is URL          -> mapper.readValue(src, typeref)
            is Reader       -> mapper.readValue(src, typeref)
            is InputStream  -> mapper.readValue(src, typeref)
            is ByteArray    -> mapper.readValue(src, typeref)
            else            -> mapper.convertValue(src,typeref)
        }
    }

    fun toMap(src: Any?, ignoreNull: Boolean = true): Map<String,Any?> = toObject(src,ignoreNull)

    fun flattenKeys(obj: Any?): Map<String,Any?> {
        var map = HashMap<String,Any?>().also{ if(isEmpty(obj)) return it}
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

    fun unflattenKeys( obj: Any? ): Map<String,Any?> {
        var map = HashMap<String,Any?>().also{ if(isEmpty(obj)) return it}
        toMap(obj).forEach{ (key, value) -> unflattenKeys(key,value,map)}
        return map
    }

    private fun unflattenKeys(jsonPath: String, value: Any?, result: MutableMap<String,Any?>) {

        val path    = jsonPath.replaceFirst("""\[.*?]""".toRegex(), "").replaceFirst("""\..*?$""".toRegex(), "")
        var index   = jsonPath.replaceFirst("""^($path)\[(.*?)](.*?)$""".toRegex(), "$2").let{ if(it==jsonPath) "" else it}
        val isArray = index.isNotEmpty()

        val currPath = "$path${if(isArray)"[$index]" else ""}"
        val isKey    = currPath == jsonPath

        if (isKey) {
            if (isArray) {
                setValueToList(path, index.toIntOrNull() ?: 0, value, result)
            } else {
                result[path] = value
            }
        } else {
            if (!result.containsKey(path)) {
                result[path] = if (isArray) ArrayList<Any?>() else HashMap<String,Any?>()
            }
            val newVal = if (isArray) {
                val list = result[path] as List<*>
                val idx  = index.toIntOrNull() ?: 0
                if (list.size <= idx || list[idx] == null) {
                    setValueToList(path, idx, HashMap<String,Any?>(), result)
                }
                list[idx] as MutableMap<String,Any?>?
            } else {
                result[path] as MutableMap<String,Any?>?
            }
            newVal?.let {
                val recursivePath = jsonPath.replaceFirst("${currPath.replace("\\[", "\\\\[")}.".toRegex(), "")
                unflattenKeys(recursivePath, value, newVal)
            }
        }

    }

    private fun setValueToList(key: String, idx: Int, value: Any?, json: MutableMap<String,Any?>) {
        if ( !json.containsKey(key) )
            json[key] = ArrayList<Any?>()
        val list = json[key] as MutableList<Any?>
        if (idx >= list.size) {
            for (i in list.size..idx)
                list.add(null)
        }
        list[idx] = value
    }

}}

