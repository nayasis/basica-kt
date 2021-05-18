/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.nayasis.kotlin.basica.core.resource.util

import com.github.nayasis.basica.exception.unchecked.BaseRuntimeException
import com.github.nayasis.basica.resource.util.VfsUtils
import com.github.nayasis.kotlin.basica.core.klass.getValue
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.Field
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.net.URI
import java.net.URL

/**
 * Utility for detecting and accessing JBoss VFS in the classpath.
 *
 *
 * As of Spring 4.0, this class supports VFS 3.x on JBoss AS 6+ (package
 * `org.jboss.vfs`) and is in particular compatible with JBoss AS 7 and
 * WildFly 8.
 *
 *
 * Thanks go to Marius Bogoevici for the initial patch.
 * **Note:** This is an internal class and should not be used outside the framework.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 3.0.3
 */
object VfsUtils {

    private var VFS_METHOD_GET_ROOT_URL: Method
    private var VFS_METHOD_GET_ROOT_URI: Method
    private var VIRTUAL_FILE_METHOD_EXISTS: Method
    private var VIRTUAL_FILE_METHOD_GET_INPUT_STREAM: Method
    private var VIRTUAL_FILE_METHOD_GET_SIZE: Method
    private var VIRTUAL_FILE_METHOD_GET_LAST_MODIFIED: Method
    private var VIRTUAL_FILE_METHOD_TO_URL: Method
    private var VIRTUAL_FILE_METHOD_TO_URI: Method
    private var VIRTUAL_FILE_METHOD_GET_NAME: Method
    private var VIRTUAL_FILE_METHOD_GET_PATH_NAME: Method
    private var VIRTUAL_FILE_METHOD_GET_CHILD: Method
    private var VIRTUAL_FILE_VISITOR_INTERFACE: Class<*>
    private var VIRTUAL_FILE_METHOD_VISIT: Method
    private var VISITOR_ATTRIBUTES_FIELD_RECURSE: Field
    private var GET_PHYSICAL_FILE: Method

    init {
        val loader = VfsUtils::class.java.classLoader
        val vfsPackage = "org.jboss.vfs"
        try {
            val vfsClass: Class<*> = loader.loadClass("${vfsPackage}.VFS")

            VFS_METHOD_GET_ROOT_URL = vfsClass.getMethod("getChild", URL::class.java)
            VFS_METHOD_GET_ROOT_URI = vfsClass.getMethod("getChild", URI::class.java)

            val virtualFile: Class<*> = loader.loadClass("${vfsPackage}.VirtualFile")
            VIRTUAL_FILE_METHOD_EXISTS = virtualFile.getMethod("exists")
            VIRTUAL_FILE_METHOD_GET_INPUT_STREAM = virtualFile.getMethod("openStream")
            VIRTUAL_FILE_METHOD_GET_SIZE = virtualFile.getMethod("getSize")
            VIRTUAL_FILE_METHOD_GET_LAST_MODIFIED = virtualFile.getMethod("getLastModified")
            VIRTUAL_FILE_METHOD_TO_URI = virtualFile.getMethod("toURI")
            VIRTUAL_FILE_METHOD_TO_URL = virtualFile.getMethod("toURL")
            VIRTUAL_FILE_METHOD_GET_NAME = virtualFile.getMethod("getName")
            VIRTUAL_FILE_METHOD_GET_PATH_NAME = virtualFile.getMethod("getPathName")
            GET_PHYSICAL_FILE = virtualFile.getMethod("getPhysicalFile")
            VIRTUAL_FILE_METHOD_GET_CHILD = virtualFile.getMethod("getChild", String::class.java)
            VIRTUAL_FILE_VISITOR_INTERFACE = loader.loadClass("${vfsPackage}.VirtualFileVisitor")
            VIRTUAL_FILE_METHOD_VISIT = virtualFile.getMethod("visit",VIRTUAL_FILE_VISITOR_INTERFACE)

            val visitorAttributesClass: Class<*> = loader.loadClass("${vfsPackage}.VisitorAttributes")
            VISITOR_ATTRIBUTES_FIELD_RECURSE = visitorAttributesClass.getField("RECURSE")
        } catch (ex: Throwable) {
            throw IllegalStateException("Could not detect JBoss VFS infrastructure", ex)
        }
    }

    @Throws(IOException::class)
    private fun invokeMethod(method: Method?, target: Any?, vararg args: Any?): Any {
        return try {
            method!!.invoke(target, *args)
        } catch (e: InvocationTargetException) {
            when (e.targetException) {
                is IOException -> throw e.targetException
                else -> throw BaseRuntimeException(e)
            }
        } catch (e: Exception) {
            throw BaseRuntimeException(e)
        }
    }

    fun exists(vfsResource: Any?): Boolean {
        return try {
            invokeMethod(VIRTUAL_FILE_METHOD_EXISTS, vfsResource) as Boolean
        } catch (ex: IOException) {
            false
        }
    }

    fun isReadable(vfsResource: Any?): Boolean {
        return try {
            invokeMethod(VIRTUAL_FILE_METHOD_GET_SIZE, vfsResource) as Long > 0
        } catch (ex: IOException) {
            false
        }
    }

    @Throws(IOException::class)
    fun getSize(vfsResource: Any?): Long {
        return invokeMethod(VIRTUAL_FILE_METHOD_GET_SIZE, vfsResource) as Long
    }

    @Throws(IOException::class)
    fun getLastModified(vfsResource: Any?): Long {
        return invokeMethod(VIRTUAL_FILE_METHOD_GET_LAST_MODIFIED, vfsResource) as Long
    }

    @Throws(IOException::class)
    fun getInputStream(vfsResource: Any?): InputStream {
        return invokeMethod(VIRTUAL_FILE_METHOD_GET_INPUT_STREAM, vfsResource) as InputStream
    }

    @Throws(IOException::class)
    fun getURL(vfsResource: Any?): URL {
        return invokeMethod(VIRTUAL_FILE_METHOD_TO_URL, vfsResource) as URL
    }

    @Throws(IOException::class)
    fun getURI(vfsResource: Any?): URI {
        return invokeMethod(VIRTUAL_FILE_METHOD_TO_URI, vfsResource) as URI
    }

    fun getName(vfsResource: Any?): String {
        return try {
            invokeMethod(VIRTUAL_FILE_METHOD_GET_NAME, vfsResource) as String
        } catch (ex: IOException) {
            throw IllegalStateException("Cannot get resource name", ex)
        }
    }

    @Throws(IOException::class)
    fun getRelative(url: URL?): Any {
        return invokeMethod(VFS_METHOD_GET_ROOT_URL, null, url)
    }

    @Throws(IOException::class)
    fun getChild(vfsResource: Any?, path: String?): Any {
        return invokeMethod(VIRTUAL_FILE_METHOD_GET_CHILD, vfsResource, path)
    }

    @Throws(IOException::class)
    fun getFile(vfsResource: Any?): File {
        return invokeMethod(GET_PHYSICAL_FILE, vfsResource) as File
    }

    @Throws(IOException::class)
    fun getRoot(url: URI?): Any {
        return invokeMethod(VFS_METHOD_GET_ROOT_URI, null, url)
    }

    @Throws(IOException::class)
    fun getRoot(url: URL?): Any {
        return invokeMethod(VFS_METHOD_GET_ROOT_URL, null, url)
    }

    val visitorAttributes: Any
        get() = VISITOR_ATTRIBUTES_FIELD_RECURSE.getValue(null)

    fun getPath(resource: Any?): String =
        VIRTUAL_FILE_METHOD_GET_PATH_NAME.invoke(resource)?.toString() ?: ""

    @Throws(IOException::class)
    fun visit(resource: Any?, visitor: InvocationHandler?) {
        val visitorProxy = Proxy.newProxyInstance(
            VIRTUAL_FILE_VISITOR_INTERFACE!!.classLoader, arrayOf<Class<*>?>(VIRTUAL_FILE_VISITOR_INTERFACE), visitor
        )
        invokeMethod(VIRTUAL_FILE_METHOD_VISIT, resource, visitorProxy)
    }

}