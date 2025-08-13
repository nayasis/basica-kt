/*
 * Copyright 2002-2016 the original author or authors.
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
package io.github.nayasis.kotlin.basica.core.resource.resolver

import io.github.nayasis.kotlin.basica.core.resource.loader.ResourceLoader
import io.github.nayasis.kotlin.basica.core.resource.type.interfaces.Resource

/**
 * A resolution strategy for protocol-specific resource handles.
 *
 *
 * Used as an SPI for [DefaultResourceLoader], allowing for
 * custom protocols to be handled without subclassing the loader
 * implementation (or application context implementation).
 *
 * @author Juergen Hoeller
 * @since 4.3
 * @see DefaultResourceLoader.addProtocolResolver
 */
fun interface ProtocolResolver {
    /**
     * Resolve the given location against the given resource loader
     * if this implementation's protocol matches.
     * @param location the user-specified resource location
     * @param resourceLoader the associated resource loader
     * @return a corresponding `Resource` handle if the given location
     * matches this resolver's protocol, or `null` otherwise
     */
    fun resolve(location: String?, resourceLoader: ResourceLoader?): Resource?
}