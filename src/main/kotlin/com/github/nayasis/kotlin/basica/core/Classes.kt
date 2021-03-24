package com.github.nayasis.kotlin.basica.core

fun Class<*>.extends( klass: Class<*> ): Boolean {
    return this.isAssignableFrom(klass) || klass.isAssignableFrom(this)
}