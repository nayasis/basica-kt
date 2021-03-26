package com.github.nayasis.kotlin.basica.reflection.serializer

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.github.nayasis.kotlin.basica.core.toDate
import com.github.nayasis.kotlin.basica.core.toStr
import java.util.*

class DateSerializer: JsonSerializer<Date>() {
    override fun serialize(value: Date, generator: JsonGenerator, provider: SerializerProvider) {
        provider.defaultSerializeValue( value.toStr(), generator )
    }
}

class DateDeserializer: JsonDeserializer<Date>() {
    override fun deserialize(jp: JsonParser, ctx: DeserializationContext): Date {
        return try {
            Date(jp.longValue)
        } catch (e: JsonParseException) {
            jp.valueAsString.toDate()
        }
    }
}