package io.github.nayasis.kotlin.basica.reflection.serializer

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import io.github.nayasis.kotlin.basica.core.localdate.format
import io.github.nayasis.kotlin.basica.core.localdate.toCalendar
import io.github.nayasis.kotlin.basica.core.localdate.toDate
import io.github.nayasis.kotlin.basica.core.localdate.toZonedDateTime
import java.time.ZonedDateTime
import java.util.*

class DateSerializer: JsonSerializer<Date>() {
    override fun serialize(value: Date, generator: JsonGenerator, provider: SerializerProvider) {
        provider.defaultSerializeValue( value.format(), generator )
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

class CalendarSerializer: JsonSerializer<Calendar>() {
    override fun serialize(value: Calendar, generator: JsonGenerator, provider: SerializerProvider) {
        provider.defaultSerializeValue( value.toZonedDateTime().format(), generator )
    }
}

class CalendarDeserializer: JsonDeserializer<Calendar>() {
    override fun deserialize(jp: JsonParser, ctx: DeserializationContext): Calendar {
        return try {
            jp.valueAsString.toCalendar()
        } catch (e: JsonParseException) {
            Date(jp.longValue).toCalendar()
        }
    }
}

class ZonedDateTimeSerializer: JsonSerializer<ZonedDateTime>() {
    override fun serialize(value: ZonedDateTime, generator: JsonGenerator, provider: SerializerProvider) {
        provider.defaultSerializeValue(value.format(), generator)
    }
}

class ZonedDateTimeDeserializer: JsonDeserializer<ZonedDateTime>() {
    override fun deserialize(jp: JsonParser, ctx: DeserializationContext): ZonedDateTime {
        return try {
            jp.valueAsString.toZonedDateTime()
        } catch (e: JsonParseException) {
            throw IllegalArgumentException("Cannot parse ZonedDateTime: ${jp.valueAsString}")
        }
    }
}