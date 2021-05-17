/*
 * Copyright 2002-2007 the original author or authors.
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
package com.github.nayasis.kotlin.basica.core.resource.resolver

import com.github.nayasis.basica.resource.loader.ResourceLoader
import com.github.nayasis.basica.resource.type.interfaces.Resource
import java.io.IOException

interface ResourcePatternResolver: ResourceLoader {
    /**
     * Resolve the given location pattern into Resource objects.
     *
     * Overlapping resource entries that point to the same physical
     * resource should be avoided, as far as possible. The result should
     * have set semantics.
     * @param locationPattern the location pattern to resolve
     * @return the corresponding Resource objects
     * @throws IOException in case of I/O errors
     */
    @Throws(IOException::class)
    fun getResources(locationPattern: String?): Set<Resource?>?
}