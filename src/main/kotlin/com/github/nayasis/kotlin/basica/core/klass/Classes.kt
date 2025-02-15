package com.github.nayasis.kotlin.basica.core.klass

import com.github.nayasis.kotlin.basica.core.resource.PathMatchingResourceLoader
import com.github.nayasis.kotlin.basica.core.resource.util.URL_PREFIX_CLASSPATH
import com.github.nayasis.kotlin.basica.core.url.toFile
import com.github.nayasis.kotlin.basica.etc.error
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.Array
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.regex.Pattern
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.isSubclassOf

private val log = KotlinLogging.logger {}

/** Suffix for array class names: `"[]"`.  */
private const val SUFFIX_ARRAY = "[]"

/** Prefix for internal non-primitive array class names: `"[L"`.  */
private const val PREFIX_NON_PRIMITIVE_ARRAY = "[L"

/** Prefix for internal array class names: `"["`.  */
private const val PREFIX_INTERNAL_ARRAY = "["

/** The package separator character: `'.'`.  */
const val SEPARATOR_PACKAGE = '.'

/** The path separator character: `'/'`.  */
const val SEPARATOR_PATH = '/'

/** The inner class separator character: `'$'`.  */
const val SEPARATOR_INNER_CLASS = '$'

private val IMMUTABLE = setOf(
    Void::class,
    Char::class,
    Boolean::class,
    Byte::class,
    Short::class,
    Int::class,
    Long::class,
    Float::class,
    Double::class,
    BigDecimal::class,
    BigInteger::class,
    LocalDate::class,
    LocalDateTime::class,
    String::class,
    URI::class,
    URL::class,
    UUID::class,
    Pattern::class,
    Class::class,
)

fun Type.isSubclassOf(klass: Class<*>): Boolean {
    return klass.isAssignableFrom(this as Class<*>)
}

fun Class<*>.isSubclassOf(klass: Class<*>): Boolean {
    return klass.isAssignableFrom(this)
}

fun Class<*>.isSubclassOf(type: Type): Boolean {
    return (type as Class<*>).isAssignableFrom(this)
}

val KClass<*>?.isEnum: Boolean
    get() = this?.isSubclassOf(Enum::class) ?: false

val KClass<*>?.isPrimitive: Boolean
    get() = this?.java?.isPrimitive ?: false

val KClass<*>?.isImmutable: Boolean
    get() = IMMUTABLE.contains(this)

fun Class<*>.fields(declaredOnly: Boolean = false): Set<Field> {
    val fields = mutableSetOf<Field>(*this.declaredFields)
    if( ! declaredOnly ) {
        var parent: Class<*>? = this.superclass
        while( parent != null ) {
            fields.addAll(parent.declaredFields)
            parent = parent.superclass
        }
    }
    return fields
}

fun Class<*>.methods(declaredOnly: Boolean = false): Set<Method> {
    val methods = mutableSetOf<Method>(*this.declaredMethods)
    if( ! declaredOnly ) {
        var parent: Class<*>? = this.superclass
        while( parent != null ) {
            methods.addAll(parent.declaredMethods)
            parent = parent.superclass
        }
    }
    return methods
}

fun Class<*>.constructors(): Set<Constructor<*>> {
    return setOf<Constructor<*>>(*declaredConstructors)
}

fun Field.setValue(instance: Any?, value: Any?) {
    handleField(this) {
        if( isStatic ) {
            set(null, value)
        } else {
            set(instance,value)
        }
    }
}

@Suppress("UNCHECKED_CAST")
fun <T> Field.getValue(instance: Any?): T {
    return handleField(this) {
        if (isStatic) {
            get(null) as T
        } else {
            get(instance) as T
        }
    } as T
}

val Field.isStatic: Boolean
    get() = Modifier.isStatic(this.modifiers)

private fun handleField( field: Field, fn: () -> Any?) {
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
        get() =
            runCatching { Thread.currentThread().contextClassLoader }.getOrNull() ?:
            runCatching { Classes::class.java.classLoader }.getOrNull() ?:
            ClassLoader.getSystemClassLoader()

    /**
     * get class for name
     *
     * @param name class name
     * @throws ClassNotFoundException
     */
    fun getClass(name: String): Class<*> {
        val className = name.replace(" ", "").let {
            val invalidIndex = it.indexOf('<')
            if( invalidIndex >= 0 ) {
                it.substring(0,invalidIndex)
            } else {
                it
            }
        }
        return classLoader.loadClass(className)
    }

    fun getClass(type: Type?): Class<*> {
        if (type == null) return Any::class.java
        val genericName = type.typeName.let {
            val start = it.indexOf('<')
            if( start < 0 ) {
                return Any::class.java
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

    /**
     * get class for name.
     * it could understand array class name (ex. "String[]") and inner class's source name (ex. java.lang.Thread.State instead of "java.lang.Thread@State" )
     *
     * @param name            class name
     * @param classLoader    class loader
     * @return    class instance
     * @throws ClassNotFoundException    if class was not found
     * @throws LinkageError    if class file could not be loaded
     */
    fun forName(name: String, classLoader: ClassLoader = this.classLoader ): Class<*> {

        when {
            name.endsWith(SUFFIX_ARRAY) -> name.substring(0, name.length - SUFFIX_ARRAY.length)
            name.startsWith(PREFIX_NON_PRIMITIVE_ARRAY) && name.endsWith(";") -> name.substring(PREFIX_NON_PRIMITIVE_ARRAY.length,name.length - 1)
            name.startsWith(PREFIX_INTERNAL_ARRAY) -> name.substring(PREFIX_INTERNAL_ARRAY.length)
            else -> null
        }?.let {
            val classInArray = forName(it)
            return Array.newInstance(classInArray, 0).javaClass
        }

        return try {
            Class.forName(name, false, classLoader)
        } catch (e: ClassNotFoundException) {
            name.lastIndexOf(SEPARATOR_PACKAGE).let { index ->
                if(index >= 0) {
                    "${name.substring(0,index)}$SEPARATOR_PACKAGE${name.substring(index + 1)}"
                } else null
            }?.let { inner -> runCatching { Class.forName(inner, false, classLoader)}.getOrNull() } ?: throw e
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> createInstance(type: Type) : T {
        return getClass(type).kotlin.createInstance() as T
    }

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> createInstance(name: String) : T {
        return getClass(name).kotlin.createInstance() as T
    }

    fun hasResource(path: String?): Boolean {
        return ! path.isNullOrEmpty() && classLoader.getResource(path.toResourceName) != null
    }

    fun getResourceStream(path: String): InputStream? {
        return classLoader.getResourceAsStream(path.toResourceName)
    }

    fun getResourceStream(url: URL): InputStream {
        return url.openStream()
    }

    fun getResource(path: String): URL? {
        return classLoader.getResource(path.toResourceName)
    }

    /**
     * get generic class from another class.
     *
     * it only works when used in class itself.
     *
     * ```
     * class Test<T> {
     *   fun test() {
     *     // it returns type of **T** exactly.
     *     val generic = Classes.getGenericClass( this.class.java );
     *   }
     *   fun test2() {
     *     val test = new Test<HashMap>();
     *     // it returns **Object.class** only because instance has no information about Generic.
     *     val generic = Classes.getGenericClass( test.class.java );
     *   }
     * }
     * ```
     *
     * @param klass class to inspect
     * @return generic class of klass
     */
    fun getGenericClass(klass: Class<*>?): Class<*>? {
        return if (klass == null) null else try {
            val genericSuperclass = klass.genericSuperclass
            val types = (genericSuperclass as ParameterizedType).actualTypeArguments
            types[0] as Class<*>
        } catch (e: Exception) {
            Any::class.java
        }
    }

    /**
     * find resources
     *
     * @param pattern   path matching pattern (glob expression. if not exists, add all result)
     * <pre>
     * ** : ignore directory variation
     * *  : filename LIKE search
     *
     * 1. **.xml           : all files having "xml" extension below searchDir and it's all sub directories.
     * 2. *.xml            : all files having "xml" extension in searchDir
     * 3. c:\home\*\*.xml  : all files having "xml" extension below 'c:\home\' and it's just 1 depth below directories.
     * 4. c:\home\**\*.xml : all files having "xml" extension below 'c:\home\' and it's all sub directories.
     *
     * 1. *  It matches zero , one or more than one characters. While matching, it will not cross directories boundaries.
     * 2. ** It does the same as * but it crosses the directory boundaries.
     * 3. ?  It matches only one character for the given name.
     * 4. \  It helps to avoid characters to be interpreted as special characters.
     * 5. [] In a set of characters, only single character is matched. If (-) hyphen is used then, it matches a range of characters. Example: [efg] matches "e","f" or "g" . [a-d] matches a range from a to d.
     * 6. {} It helps to matches the group of sub patterns.
     *
     * 1. *.java when given path is java , we will get true by PathMatcher.matches(path).
     * 2. *.* if file contains a dot, pattern will be matched.
     * 3. *.{java,txt} If file is either java or txt, path will be matched.
     * 4. abc.? matches a file which start with abc and it has extension with only single character.
     * </pre>
     * @return found resource names
     */
    fun findResources(vararg pattern: String): List<URL> {
        val urls   = mutableListOf<URL>()
        val loader = PathMatchingResourceLoader()
        for (ptn in pattern) {
            try {
                loader.getResources(URL_PREFIX_CLASSPATH + ptn).forEach { urls.add(it.getURL()) }
            } catch (e: IOException) {
                log.error(e)
            }
        }
        return urls
    }

    fun getRootLocation(klass: KClass<*>): URL {
        return klass.java.protectionDomain.codeSource.location
    }

    fun isRunningInJar(klass: KClass<*>): Boolean {
        return getRootLocation(klass).let {
            when {
                it.protocol.matches("(?i)^(jar|war)$".toRegex()) -> true
                it.toFile().extension.matches("(?i)^(jar|war)\$".toRegex()) -> true
                else -> false
            }
        }
    }

}}

private val String?.toResourceName: String
    get() = if( this.isNullOrEmpty() ) "" else this.replaceFirst("^/".toRegex(), "")