package com.github.nayasis.kotlin.basica.reflection.helper.mapper

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule

open class NObjectMapper: ObjectMapper {

    var skipJsonIgnore: Boolean
        set(value) {
            when(value) {
                true -> setAnnotationIntrospector(SkipJsonIgnoreInspector())
                else -> setAnnotationIntrospector(null)
            }
            field = value
        }

    var ignoreNull: Boolean
        set(value) {
            setSerializationInclusion(if (value) JsonInclude.Include.NON_NULL else JsonInclude.Include.ALWAYS)
            field = value
        }

    var sortable: Boolean
        set(value) {
            configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, value)
            configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, value)
            field = value
        }

    constructor(
        skipJsonIgnore: Boolean = false,
        ignoreNull: Boolean     = false,
        sortable: Boolean       = false,
    ) {
        init()
        custom()
        this.skipJsonIgnore = skipJsonIgnore
        this.ignoreNull     = ignoreNull
        this.sortable       = sortable
    }

    private fun init() {

        configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
        configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        configure(SerializationFeature.WRITE_CHAR_ARRAYS_AS_JSON_ARRAYS, true)
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
        configure(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS, true)
        configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
        configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false).registerModule(JavaTimeModule())

        // only convert by Class' field.
        setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
        setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)

        // Prevent error when pojo with @JsonFilter annotation is parsed.
        setFilterProvider(SimpleFilterProvider().setFailOnUnknownId(false))

    }

    open fun custom() {

    }

}