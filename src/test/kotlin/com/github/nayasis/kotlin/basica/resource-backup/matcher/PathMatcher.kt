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
package com.github.nayasis.kotlin.basica.`resource-backup`.matcher

interface PathMatcher {
    /**
     * Does the given `path` represent a pattern that can be matched
     * by an implementation of this interface?
     *
     * If the return value is `false`, then the [.match]
     * method does not have to be used because direct equality comparisons
     * on the static path Strings will lead to the same result.
     * @param path the path String to check
     * @return `true` if the given `path` represents a pattern
     */
    fun isPattern(path: String?): Boolean

    /**
     * Match the given `path` against the given `pattern`,
     * according to this PathMatcher's matching strategy.
     * @param pattern the pattern to match against
     * @param path the path String to test
     * @return `true` if the supplied `path` matched,
     * `false` if it didn't
     */
    fun match(pattern: String?, path: String?): Boolean

    /**
     * Match the given `path` against the corresponding part of the given
     * `pattern`, according to this PathMatcher's matching strategy.
     *
     * Determines whether the pattern at least matches as far as the given base
     * path goes, assuming that a full path may then match as well.
     * @param pattern the pattern to match against
     * @param path the path String to test
     * @return `true` if the supplied `path` matched,
     * `false` if it didn't
     */
    fun matchStart(pattern: String?, path: String?): Boolean

    /**
     * Given a pattern and a full path, determine the pattern-mapped part.
     *
     * This method is supposed to find out which part of the path is matched
     * dynamically through an actual pattern, that is, it strips off a statically
     * defined leading path from the given full path, returning only the actually
     * pattern-matched part of the path.
     *
     * For example: For "myroot/ *.html" as pattern and "myroot/myfile.html"
     * as full path, this method should return "myfile.html". The detailed
     * determination rules are specified to this PathMatcher's matching strategy.
     *
     * A simple implementation may return the given full path as-is in case
     * of an actual pattern, and the empty String in case of the pattern not
     * containing any dynamic parts (i.e. the `pattern` parameter being
     * a static path that wouldn't qualify as an actual [pattern][.isPattern]).
     * A sophisticated implementation will differentiate between the static parts
     * and the dynamic parts of the given path pattern.
     * @param pattern the path pattern
     * @param path the full path to introspect
     * @return the pattern-mapped part of the given `path`
     * (never `null`)
     */
    fun extractPathWithinPattern(pattern: String?, path: String?): String?

    /**
     * Given a pattern and a full path, extract the URI template variables. URI template
     * variables are expressed through curly brackets ('{' and '}').
     *
     * For example: For pattern "/hotels/{hotel}" and path "/hotels/1", this method will
     * return a map containing "hotel"->"1".
     * @param pattern the path pattern, possibly containing URI templates
     * @param path the full path to extract template variables from
     * @return a map, containing variable names as keys; variables values as values
     */
    fun extractUriTemplateVariables(pattern: String?, path: String?): Map<String?, String?>?

    /**
     * Given a full path, returns a [Comparator] suitable for sorting patterns
     * in order of explicitness for that path.
     *
     * The full algorithm used depends on the underlying implementation,
     * but generally, the returned `Comparator` will
     * [sort][java.util.List.sort]
     * a list so that more specific patterns come before generic patterns.
     * @param path the full path to use for comparison
     * @return a comparator capable of sorting patterns in order of explicitness
     */
    fun getPatternComparator(path: String?): Comparator<String?>?

    /**
     * Combines two patterns into a new pattern that is returned.
     *
     * The full algorithm used for combining the two pattern depends on the underlying implementation.
     * @param pattern1 the first pattern
     * @param pattern2 the second pattern
     * @return the combination of the two patterns
     * @throws IllegalArgumentException when the two patterns cannot be combined
     */
    fun combine(pattern1: String?, pattern2: String?): String?
}