package com.github.nayasis.kotlin.basica.core.klass

import org.objenesis.ObjenesisStd
import java.io.InputStream
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import java.net.URL
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.isSubclassOf

fun Class<*>.extends( klass: Class<*> ): Boolean {
    return klass.isAssignableFrom(this)
}

fun KClass<*>.extends(klass:KClass<*>): Boolean {
    return this.isSubclassOf(klass)
}

fun Class<*>.fields(declaredOnly: Boolean = false): Set<Field> {
    val fields = mutableSetOf<Field>(*declaredFields)
    if( ! declaredOnly ) {
        var parent: Class<*>? = this
        while( parent != Any::class && parent != null ) {
            fields.addAll(declaredFields)
        }
    }
    return fields
}

fun Class<*>.methods(declaredOnly: Boolean = false): Set<Method> {
    val methods = mutableSetOf<Method>(*declaredMethods)
    if( ! declaredOnly ) {
        var parent: Class<*>? = this
        while( parent != Any::class && parent != null ) {
            methods.addAll(declaredMethods)
        }
    }
    return methods
}

fun Class<*>.constructors(): Set<Constructor<*>> {
    return setOf<Constructor<*>>(*declaredConstructors)
}

fun Field.setValue(instance: Any?, value: Any?) {
    handleField(this,instance) {
        if( isStatic ) {
            set(null, value)
        } else {
            set(instance,value)
        }
    }
}

fun <T> Field.getValue(instance: Any?): T {
    return handleField(this,instance) {
        if (isStatic) {
            get(null) as T
        } else {
            get(instance) as T
        }
    } as T
}

val Field.isStatic: Boolean
    get() = Modifier.isStatic(this.modifiers)

private fun handleField( field: Field, instance: Any?, fn: () -> Any?) {
    val accessible = field.isAccessible
    if( ! accessible )
        field.isAccessible = true
    try {
        fn()
    } finally {
        if( ! accessible )
            field.isAccessible = false
    }
}

class Classes { companion object{

    val classLoader: ClassLoader
        get() {
            try {
                return Thread.currentThread().contextClassLoader
            } catch (e: Exception) {}
            try {
                return Classes::class.java.classLoader
            } catch (e: Exception) {}
            return ClassLoader.getSystemClassLoader()
        }

    /**
     * get class for name
     *
     * @param name class name
     * @throws ClassNotFoundException
     */
    @Throws(ClassNotFoundException::class)
    fun getClass(name: String): KClass<*> {
        val className = name.replace(" ", "").let {
            val invalidIndex = it.indexOf('<')
            if( invalidIndex >= 0 ) {
                it.substring(0,invalidIndex)
            } else {
                it
            }
        }
        return classLoader.loadClass(className).kotlin
    }

    @Throws(ClassNotFoundException::class)
    fun getClass(type: Type?): KClass<*> {
        if (type == null) return Any::class
        val genericName = type.typeName.let {
            val start = it.indexOf('<')
            if( start < 0 ) {
                return Any::class
            } else {
                it.substring(start + 1, it.length - 1)
            }
        }
        return getClass(genericName.let {
            val start = it.indexOf('<')
            if( start >= 0 ) {
                it.substring(0,start)
            } else {
                it
            }
        })
    }

    fun <T:Any> newInstance(klass: KClass<T>) : T {
        return try {
            klass.createInstance()
        } catch (e: Exception) {
            ObjenesisStd().newInstance(klass.java)
        }
    }

    fun <T:Any> newInstance(type: Type) : T {
        return newInstance(getClass(type) as Class<T>)
    }

    fun <T:Any> newInstance(name: String) : T {
        return newInstance(getClass(name) as Class<T>)
    }

    fun hasResource(path: String?): Boolean {
        return ! path.isNullOrEmpty() && classLoader.getResource(path.toResourceName) != null
    }

    fun getResourceStream(path: String): InputStream {
        return classLoader.getResourceAsStream(path.toResourceName)
    }

    fun getResourceStream(url: URL): InputStream {
        return url.openStream()
    }

    fun getResource(path: String): URL {
        return classLoader.getResource(path.toResourceName)
    }

}}

private val String?.toResourceName: String
    get() = if( this.isNullOrEmpty() ) "" else this.replaceFirst("^/".toRegex(), "")