package com.github.nayasis.kotlin.basica.model

import java.io.Serializable
import java.util.*
import kotlin.collections.LinkedHashMap

@Suppress("MayBeConstant")
class NGrid: Serializable, Cloneable {

    companion object {
        private val serialVersionUID = 4570402963506233952L
    }

    private val header = LinkedHashMap<String,Any?>()
    private val body   = TreeMap<Long,Map<String,Any?>>()




}