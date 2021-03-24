package com.github.nayasis.kotlin.basica.reflection.helper.serializer

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.github.nayasis.basica.exception.unchecked.ParseException
import com.github.nayasis.basica.model.NDate
import com.github.nayasis.kotlin.basica.core.toDate
import java.util.*

class SimpleDateDeserializer: JsonDeserializer<Date>() {
    override fun deserialize(jp: JsonParser, ctx: DeserializationContext): Date {
        return try {
            Date(jp.longValue)
        } catch (e: JsonParseException) {
            val string = jp.valueAsString
            try {
                string.toDate()
                NDate(string, NDate.ISO_8601_FORMAT).toDate()
            } catch (ex: ParseException) {
                NDate(string).toDate()
            }
        }
    }
}