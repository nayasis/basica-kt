package com.github.nayasis.kotlin.basica.core

import java.net.URL
import kotlin.reflect.KClass

fun Class<*>.extends( klass: Class<*> ): Boolean {
    return this.isAssignableFrom(klass) || klass.isAssignableFrom(this)
}