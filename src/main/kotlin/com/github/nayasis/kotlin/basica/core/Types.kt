package com.github.nayasis.kotlin.basica.core

import kotlin.reflect.KClass

class Types { companion object {

    fun isPojo(any: Any?): Boolean {
        if( any == null ) return false

        val klass = any.javaClass

        if( klass.isPrimitive ) return false
        if( klass.isAnnotation ) return false
        if( klass.isEnum ) return false

        if( any is CharSequence ) return false
        if( any is Char ) return false

        return true

    }

}}