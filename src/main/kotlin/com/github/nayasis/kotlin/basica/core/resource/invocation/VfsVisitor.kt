package com.github.nayasis.kotlin.basica.core.resource.invocation

import com.github.nayasis.basica.resource.matcher.PathMatcher
import com.github.nayasis.basica.resource.type.VfsResource
import com.github.nayasis.basica.resource.type.interfaces.Resource
import com.github.nayasis.basica.resource.util.VfsUtils
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method

class VfsVisitor(rootPath: String, private val subPattern: String, private val pathMatcher: PathMatcher):
    InvocationHandler {
    private val rootPath: String
    private val resources: MutableSet<Resource> = LinkedHashSet()
    @Throws(Throwable::class)
    override fun invoke(proxy: Any, method: Method, args: Array<Any>): Any {
        val methodName = method.name
        if (Any::class.java == method.declaringClass) {
            if (methodName == "equals") {
                // Only consider equal when proxies are identical.
                return proxy === args[0]
            } else if (methodName == "hashCode") {
                return System.identityHashCode(proxy)
            }
        } else if ("getAttributes" == methodName) {
            return attributes
        } else if ("visit" == methodName) {
            visit(args[0])
            return null
        } else if ("toString" == methodName) {
            return toString()
        }
        throw IllegalStateException("Unexpected method invocation: $method")
    }

    fun visit(vfsResource: Any?) {
        if (pathMatcher.match(
                subPattern,
                VfsUtils.getPath(vfsResource).substring(rootPath.length)
            )
        ) {
            resources.add(VfsResource(vfsResource))
        }
    }

    val attributes: Any
        get() = VfsUtils.getVisitorAttributes()

    fun getResources(): Set<Resource> {
        return resources
    }

    fun size(): Int {
        return resources.size
    }

    override fun toString(): String {
        return "sub-pattern: " + subPattern + ", resources: " + resources
    }

    init {
        this.rootPath = if (rootPath.isEmpty() || rootPath.endsWith("/")) rootPath else "$rootPath/"
    }
}