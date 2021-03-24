package com.github.nayasis.kotlin.basica.reflection.helper.mapper

import com.fasterxml.jackson.databind.introspect.AnnotatedMember
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector
import java.beans.Transient

class SkipJsonIgnoreInspector: JacksonAnnotationIntrospector() {
    override fun hasIgnoreMarker(m: AnnotatedMember?): Boolean {
        val annotation = m?.getAnnotation(Transient::class.java)
        return annotation?.value ?: false
    }
}