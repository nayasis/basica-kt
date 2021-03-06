/*
 * Copyright 2002-2018 the original author or authors.
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
package com.github.nayasis.kotlin.basica.core.resource.loader

import com.github.nayasis.kotlin.basica.core.resource.type.interfaces.Resource

interface ResourceLoader {
    /**
     * Return a Resource handle for the specified resource location.
     *
     * The handle should always be a reusable resource descriptor,
     * allowing for multiple [Resource.getInputStream] calls.
     *
     *
     *  * Must support fully qualified URLs, e.g. "file:C:/test.dat".
     *  * Must support classpath pseudo-URLs, e.g. "classpath:test.dat".
     *  * Should support relative file paths, e.g. "WEB-INF/test.dat".
     * (This will be implementation-specific, typically provided by an
     * ApplicationContext implementation.)
     *
     *
     * Note that a Resource handle does not imply an existing resource;
     * you need to invoke [Resource.exists] to check for existence.
     * @param location the resource location
     * @return a corresponding Resource handle (never `null`)
     * @see Resource.exists
     * @see Resource.getInputStream
     */
    fun getResource(location: String): Resource

    /**
     * Expose the ClassLoader used by this ResourceLoader.
     *
     * Clients which need to access the ClassLoader directly can do so
     * in a uniform manner with the ResourceLoader, rather than relying
     * on the thread context ClassLoader.
     * @return the ClassLoader
     * (only `null` if even the system ClassLoader isn't accessible)
     */
    fun getClassLoader(): ClassLoader?
}