package com.github.nayasis.kotlin.basica.core

import com.github.nayasis.kotlin.basica.reflection.Reflector

fun Map<*,*>.flattenKeys(): Map<String,Any?> = Reflector.flattenKeys(this)

fun Map<*,*>.unflattenKeys(): Map<String,Any?> = Reflector.unflattenKeys(this)

