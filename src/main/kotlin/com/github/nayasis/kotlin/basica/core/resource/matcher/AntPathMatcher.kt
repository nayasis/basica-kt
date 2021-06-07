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
package com.github.nayasis.kotlin.basica.core.resource.matcher

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

/**
 * [PathMatcher] implementation for Ant-style path patterns.
 *
 *
 * Part of this mapping code has been kindly borrowed from [Apache Ant](http://ant.apache.org).
 *
 *
 * The mapping matches URLs using the following rules:<br></br>
 *
 *  * `?` matches one character
 *  * `*` matches zero or more characters
 *  * `**` matches zero or more *directories* in a path
 *  * `{spring:[a-z]+}` matches the regexp `[a-z]+` as a path variable named "spring"
 *
 *
 * <h3>Examples</h3>
 *
 *  * `com/t?st.jsp`  matches `com/test.jsp` but also
 * `com/tast.jsp` or `com/txst.jsp`
 *  * `com/ *.jsp`  matches all `.jsp` files in the
 * `com` directory
 *  * `com/&#42;&#42;/test.jsp`  matches all `test.jsp`
 * files underneath the `com` path
 *  * `org/springframework/&#42;&#42;/ *.jsp`  matches all
 * `.jsp` files underneath the `org/springframework` path
 *  * `org/&#42;&#42;/servlet/bla.jsp`  matches
 * `org/springframework/servlet/bla.jsp` but also
 * `org/springframework/testing/servlet/bla.jsp` and `org/servlet/bla.jsp`
 *  * `com/{filename:\\w+}.jsp` will match `com/test.jsp` and assign the value `test`
 * to the `filename` variable
 *
 *
 *
 * **Note:** a pattern and a path must both be absolute or must
 * both be relative in order for the two to match. Therefore it is recommended
 * that users of this implementation to sanitize patterns in order to prefix
 * them with "/" as it makes sense in the context in which they're used.
 *
 * @author Alef Arendsen
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 16.07.2003
 */
class AntPathMatcher: PathMatcher {
    private var pathSeparator: String
    private var pathSeparatorPatternCache: PathSeparatorPatternCache
    private var caseSensitive = true
    private var trimTokens = false

    @Volatile
    private var cachePatterns: Boolean? = null
    private val tokenizedPatternCache: MutableMap<String, Array<String?>?> = ConcurrentHashMap(256)

    private val stringMatcherCache: MutableMap<String?, AntPathStringMatcher> = ConcurrentHashMap(256)

    /**
     * Create a new instance with the [.DEFAULT_PATH_SEPARATOR].
     */
    constructor() {
        pathSeparator = DEFAULT_PATH_SEPARATOR
        pathSeparatorPatternCache = PathSeparatorPatternCache(DEFAULT_PATH_SEPARATOR)
    }

    /**
     * A convenient, alternative constructor to use with a custom path separator.
     * @param pathSeparator the path separator to use, must not be `null`.
     * @since 4.1
     */
    constructor(pathSeparator: String) {
        this.pathSeparator = pathSeparator
        pathSeparatorPatternCache = PathSeparatorPatternCache(pathSeparator)
    }

    /**
     * Set the path separator to use for pattern parsing.
     *
     * Default is "/", as in Ant.
     */
    fun setPathSeparator(pathSeparator: String?) {
        this.pathSeparator = pathSeparator
            ?: DEFAULT_PATH_SEPARATOR
        pathSeparatorPatternCache = PathSeparatorPatternCache(this.pathSeparator)
    }

    /**
     * Specify whether to perform pattern matching in a case-sensitive fashion.
     *
     * Default is `true`. Switch this to `false` for case-insensitive matching.
     * @since 4.2
     */
    fun setCaseSensitive(caseSensitive: Boolean) {
        this.caseSensitive = caseSensitive
    }

    /**
     * Specify whether to trim tokenized paths and patterns.
     *
     * Default is `false`.
     */
    fun setTrimTokens(trimTokens: Boolean) {
        this.trimTokens = trimTokens
    }

    /**
     * Specify whether to cache parsed pattern metadata for patterns passed
     * into this matcher's [.match] method. A value of `true`
     * activates an unlimited pattern cache; a value of `false` turns
     * the pattern cache off completely.
     *
     * Default is for the cache to be on, but with the variant to automatically
     * turn it off when encountering too many patterns to cache at runtime
     * (the threshold is 65536), assuming that arbitrary permutations of patterns
     * are coming in, with little chance for encountering a recurring pattern.
     * @since 4.0.1
     * @see .getStringMatcher
     */
    fun setCachePatterns(cachePatterns: Boolean) {
        this.cachePatterns = cachePatterns
    }

    private fun deactivatePatternCache() {
        cachePatterns = false
        tokenizedPatternCache.clear()
        stringMatcherCache.clear()
    }

    override fun isPattern(path: String): Boolean {
        return path.indexOf('*') != -1 || path.indexOf('?') != -1
    }

    override fun match(pattern: String, path: String): Boolean {
        return doMatch(pattern, path, true, null)
    }

    override fun matchStart(pattern: String, path: String): Boolean {
        return doMatch(pattern, path, false, null)
    }

    /**
     * Actually match the given `path` against the given `pattern`.
     * @param pattern the pattern to match against
     * @param path the path String to test
     * @param fullMatch whether a full pattern match is required (else a pattern match
     * as far as the given base path goes is sufficient)
     * @return `true` if the supplied `path` matched, `false` if it didn't
     */
    protected fun doMatch(
        pattern: String, path: String, fullMatch: Boolean,
        uriTemplateVariables: MutableMap<String, String>?
    ): Boolean {
        if (path.startsWith(pathSeparator) != pattern.startsWith(pathSeparator)) {
            return false
        }
        val pattDirs = tokenizePattern(pattern)
        if (fullMatch && caseSensitive && !isPotentialMatch(path, pattDirs)) {
            return false
        }
        val pathDirs = tokenizePath(path)
        var pattIdxStart = 0
        var pattIdxEnd = pattDirs!!.size - 1
        var pathIdxStart = 0
        var pathIdxEnd = pathDirs.size - 1

        // Match all elements up to the first **
        while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
            val pattDir = pattDirs[pattIdxStart]
            if ("**" == pattDir) {
                break
            }
            if (!matchStrings(pattDir, pathDirs[pathIdxStart], uriTemplateVariables)) {
                return false
            }
            pattIdxStart++
            pathIdxStart++
        }
        if (pathIdxStart > pathIdxEnd) {
            // Path is exhausted, only match if rest of pattern is * or **'s
            if (pattIdxStart > pattIdxEnd) {
                return pattern.endsWith(pathSeparator) == path.endsWith(pathSeparator)
            }
            if (!fullMatch) {
                return true
            }
            if (pattIdxStart == pattIdxEnd && pattDirs[pattIdxStart] == "*" && path.endsWith(pathSeparator)) {
                return true
            }
            for (i in pattIdxStart..pattIdxEnd) {
                if (pattDirs[i] != "**") {
                    return false
                }
            }
            return true
        } else if (pattIdxStart > pattIdxEnd) {
            // String not exhausted, but pattern is. Failure.
            return false
        } else if (!fullMatch && "**" == pattDirs[pattIdxStart]) {
            // Path start definitely matches due to "**" part in pattern.
            return true
        }

        // up to last '**'
        while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
            val pattDir = pattDirs[pattIdxEnd]
            if (pattDir == "**") {
                break
            }
            if (!matchStrings(pattDir, pathDirs[pathIdxEnd], uriTemplateVariables)) {
                return false
            }
            pattIdxEnd--
            pathIdxEnd--
        }
        if (pathIdxStart > pathIdxEnd) {
            // String is exhausted
            for (i in pattIdxStart..pattIdxEnd) {
                if (pattDirs[i] != "**") {
                    return false
                }
            }
            return true
        }
        while (pattIdxStart != pattIdxEnd && pathIdxStart <= pathIdxEnd) {
            var patIdxTmp = -1
            for (i in pattIdxStart + 1..pattIdxEnd) {
                if (pattDirs[i] == "**") {
                    patIdxTmp = i
                    break
                }
            }
            if (patIdxTmp == pattIdxStart + 1) {
                // '**/**' situation, so skip one
                pattIdxStart++
                continue
            }
            // Find the pattern between padIdxStart & padIdxTmp in str between
            // strIdxStart & strIdxEnd
            val patLength = patIdxTmp - pattIdxStart - 1
            val strLength = pathIdxEnd - pathIdxStart + 1
            var foundIdx = -1
            strLoop@ for (i in 0..strLength - patLength) {
                for (j in 0 until patLength) {
                    val subPat = pattDirs[pattIdxStart + j + 1]
                    val subStr = pathDirs[pathIdxStart + i + j]
                    if (!matchStrings(subPat, subStr, uriTemplateVariables)) {
                        continue@strLoop
                    }
                }
                foundIdx = pathIdxStart + i
                break
            }
            if (foundIdx == -1) {
                return false
            }
            pattIdxStart = patIdxTmp
            pathIdxStart = foundIdx + patLength
        }
        for (i in pattIdxStart..pattIdxEnd) {
            if (pattDirs[i] != "**") {
                return false
            }
        }
        return true
    }

    private fun isPotentialMatch(path: String, pattDirs: Array<String?>?): Boolean {
        if (!trimTokens) {
            var pos = 0
            for (pattDir in pattDirs!!) {
                var skipped = skipSeparator(path, pos, pathSeparator)
                pos += skipped
                skipped = skipSegment(path, pos, pattDir)
                if (skipped < pattDir!!.length) {
                    return skipped > 0 || pattDir.length > 0 && isWildcardChar(pattDir[0])
                }
                pos += skipped
            }
        }
        return true
    }

    private fun skipSegment(path: String, pos: Int, prefix: String?): Int {
        var skipped = 0
        for (i in 0 until prefix!!.length) {
            val c = prefix[i]
            if (isWildcardChar(c)) {
                return skipped
            }
            val currPos = pos + skipped
            if (currPos >= path.length) {
                return 0
            }
            if (c == path[currPos]) {
                skipped++
            }
        }
        return skipped
    }

    private fun skipSeparator(path: String, pos: Int, separator: String): Int {
        var skipped = 0
        while (path.startsWith(separator, pos + skipped)) {
            skipped += separator.length
        }
        return skipped
    }

    private fun isWildcardChar(c: Char): Boolean {
        for (candidate in WILDCARD_CHARS) {
            if (c == candidate) {
                return true
            }
        }
        return false
    }

    /**
     * Tokenize the given path pattern into parts, based on this matcher's settings.
     *
     * Performs caching based on [.setCachePatterns], delegating to
     * [.tokenizePath] for the actual tokenization algorithm.
     * @param pattern the pattern to tokenize
     * @return the tokenized pattern parts
     */
    protected fun tokenizePattern(pattern: String): Array<String?>? {
        var tokenized: Array<String?>? = null
        val cachePatterns = cachePatterns
        if (cachePatterns == null || cachePatterns) {
            tokenized = tokenizedPatternCache[pattern]
        }
        if (tokenized == null) {
            tokenized = tokenizePath(pattern)
            if (cachePatterns == null && tokenizedPatternCache.size >= CACHE_TURNOFF_THRESHOLD) {
                // Try to adapt to the runtime situation that we're encountering:
                // There are obviously too many different patterns coming in here...
                // So let's turn off the cache since the patterns are unlikely to be reoccurring.
                deactivatePatternCache()
                return tokenized
            }
            if (cachePatterns == null || cachePatterns) {
                tokenizedPatternCache[pattern] = tokenized
            }
        }
        return tokenized
    }

    /**
     * Tokenize the given path String into parts, based on this matcher's settings.
     * @param path the path to tokenize
     * @return the tokenized path parts
     */
    protected fun tokenizePath(path: String?): Array<String?> {
        return tokenizeToStringArray(path, pathSeparator, trimTokens, true)
    }

    /**
     * Test whether or not a string matches against a pattern.
     * @param pattern the pattern to match against (never `null`)
     * @param str the String which must be matched against the pattern (never `null`)
     * @return `true` if the string matches against the pattern, or `false` otherwise
     */
    private fun matchStrings(
        pattern: String?, str: String?,
        uriTemplateVariables: MutableMap<String, String>?
    ): Boolean {
        return getStringMatcher(pattern).matchStrings(str, uriTemplateVariables)
    }

    /**
     * Build or retrieve an [AntPathStringMatcher] for the given pattern.
     *
     * The default implementation checks this AntPathMatcher's internal cache
     * (see [.setCachePatterns]), creating a new AntPathStringMatcher instance
     * if no cached copy is found.
     *
     * When encountering too many patterns to cache at runtime (the threshold is 65536),
     * it turns the default cache off, assuming that arbitrary permutations of patterns
     * are coming in, with little chance for encountering a recurring pattern.
     *
     * This method may be overridden to implement a custom cache strategy.
     * @param pattern the pattern to match against (never `null`)
     * @return a corresponding AntPathStringMatcher (never `null`)
     * @see .setCachePatterns
     */
    protected fun getStringMatcher(pattern: String?): AntPathStringMatcher {
        var matcher: AntPathStringMatcher? = null
        val cachePatterns = cachePatterns
        if (cachePatterns == null || cachePatterns) {
            matcher = stringMatcherCache[pattern]
        }
        if (matcher == null) {
            matcher = AntPathStringMatcher(pattern, caseSensitive)
            if (cachePatterns == null && stringMatcherCache.size >= CACHE_TURNOFF_THRESHOLD) {
                // Try to adapt to the runtime situation that we're encountering:
                // There are obviously too many different patterns coming in here...
                // So let's turn off the cache since the patterns are unlikely to be reoccurring.
                deactivatePatternCache()
                return matcher
            }
            if (cachePatterns == null || cachePatterns) {
                stringMatcherCache[pattern] = matcher
            }
        }
        return matcher
    }

    /**
     * Given a pattern and a full path, determine the pattern-mapped part.
     *
     *For example:
     *  * '`/docs/cvs/commit.html`' and '`/docs/cvs/commit.html` -> ''
     *  * '`/docs/ *`' and '`/docs/cvs/commit` -> '`cvs/commit`'
     *  * '`/docs/cvs/ *.html`' and '`/docs/cvs/commit.html` -> '`commit.html`'
     *  * '`/docs/ **`' and '`/docs/cvs/commit` -> '`cvs/commit`'
     *  * '`/docs/ **\/ *.html`' and '`/docs/cvs/commit.html` -> '`cvs/commit.html`'
     *  * '`/ *.html`' and '`/docs/cvs/commit.html` -> '`docs/cvs/commit.html`'
     *  * '`*.html`' and '`/docs/cvs/commit.html` -> '`/docs/cvs/commit.html`'
     *  * '`*`' and '`/docs/cvs/commit.html` -> '`/docs/cvs/commit.html`'
     *
     * Assumes that [.match] returns `true` for '`pattern`' and '`path`', but
     * does **not** enforce this.
     */
    override fun extractPathWithinPattern(pattern: String, path: String): String {
        val patternParts = tokenizeToStringArray(pattern, pathSeparator, trimTokens, true)
        val pathParts = tokenizeToStringArray(path, pathSeparator, trimTokens, true)
        val builder = StringBuilder()
        var pathStarted = false
        var segment = 0
        while (segment < patternParts.size) {
            val patternPart = patternParts[segment]
            if (patternPart!!.indexOf('*') > -1 || patternPart.indexOf('?') > -1) {
                while (segment < pathParts.size) {
                    if (pathStarted || segment == 0 && !pattern.startsWith(pathSeparator)) {
                        builder.append(pathSeparator)
                    }
                    builder.append(pathParts[segment])
                    pathStarted = true
                    segment++
                }
            }
            segment++
        }
        return builder.toString()
    }

    override fun extractUriTemplateVariables(pattern: String, path: String): Map<String, String> {
        val variables: MutableMap<String, String> = LinkedHashMap()
        doMatch(pattern, path, true, variables).let { check(it){
            "Pattern \"$pattern\" is not a match for \"$path\""
        }}
        return variables
    }

    /**
     * Combine two patterns into a new pattern.
     *
     * This implementation simply concatenates the two patterns, unless
     * the first pattern contains a file extension match (e.g., `*.html`).
     * In that case, the second pattern will be merged into the first. Otherwise,
     * an `IllegalArgumentException` will be thrown.
     * <h3>Examples</h3>
     * <table border="1">
     * <tr><th>Pattern 1</th><th>Pattern 2</th><th>Result</th></tr>
     * <tr><td>`null`</td><td>`null`</td><td>&nbsp;</td></tr>
     * <tr><td>/hotels</td><td>`null`</td><td>/hotels</td></tr>
     * <tr><td>`null`</td><td>/hotels</td><td>/hotels</td></tr>
     * <tr><td>/hotels</td><td>/bookings</td><td>/hotels/bookings</td></tr>
     * <tr><td>/hotels</td><td>bookings</td><td>/hotels/bookings</td></tr>
     * <tr><td>/hotels/ *</td><td>/bookings</td><td>/hotels/bookings</td></tr>
     * <tr><td>/hotels/&#42;&#42;</td><td>/bookings</td><td>/hotels/&#42;&#42;/bookings</td></tr>
     * <tr><td>/hotels</td><td>{hotel}</td><td>/hotels/{hotel}</td></tr>
     * <tr><td>/hotels/ *</td><td>{hotel}</td><td>/hotels/{hotel}</td></tr>
     * <tr><td>/hotels/&#42;&#42;</td><td>{hotel}</td><td>/hotels/&#42;&#42;/{hotel}</td></tr>
     * <tr><td>/ *.html</td><td>/hotels.html</td><td>/hotels.html</td></tr>
     * <tr><td>/ *.html</td><td>/hotels</td><td>/hotels.html</td></tr>
     * <tr><td>/ *.html</td><td>/ *.txt</td><td>`IllegalArgumentException`</td></tr>
    </table> *
     * @param pattern1 the first pattern
     * @param pattern2 the second pattern
     * @return the combination of the two patterns
     * @throws IllegalArgumentException if the two patterns cannot be combined
     */
    override fun combine(pattern1: String, pattern2: String): String {
        if ( pattern1.isEmpty() && pattern2.isEmpty() ) {
            return ""
        }
        if ( pattern1.isEmpty() ) {
            return pattern2
        }
        if ( pattern2.isEmpty() ) {
            return pattern1
        }
        val pattern1ContainsUriVar = pattern1.indexOf('{') != -1
        if (pattern1 != pattern2 && !pattern1ContainsUriVar && match(pattern1, pattern2)) {
            // /* + /hotel -> /hotel ; "/*.*" + "/*.html" -> /*.html
            // However /user + /user -> /usr/user ; /{foo} + /bar -> /{foo}/bar
            return pattern2
        }

        // /hotels/* + /booking -> /hotels/booking
        // /hotels/* + booking -> /hotels/booking
        if (pattern1.endsWith(pathSeparatorPatternCache.endsOnWildCard)) {
            return concat(pattern1.substring(0, pattern1.length - 2), pattern2)
        }

        // /hotels/** + /booking -> /hotels/**/booking
        // /hotels/** + booking -> /hotels/**/booking
        if (pattern1.endsWith(pathSeparatorPatternCache.endsOnDoubleWildCard)) {
            return concat(pattern1, pattern2)
        }
        val starDotPos1 = pattern1.indexOf("*.")
        if (pattern1ContainsUriVar || starDotPos1 == -1 || pathSeparator == ".") {
            // simply concatenate the two patterns
            return concat(pattern1, pattern2)
        }
        val ext1 = pattern1.substring(starDotPos1 + 1)
        val dotPos2 = pattern2.indexOf('.')
        val file2 = if (dotPos2 == -1) pattern2 else pattern2.substring(0, dotPos2)
        val ext2 = if (dotPos2 == -1) "" else pattern2.substring(dotPos2)
        val ext1All = ext1 == ".*" || ext1.isEmpty()
        val ext2All = ext2 == ".*" || ext2.isEmpty()
        require(!(!ext1All && !ext2All)) { "Cannot combine patterns: $pattern1 vs $pattern2" }
        val ext = if (ext1All) ext2 else ext1
        return file2 + ext

    }

    private fun concat(path1: String, path2: String): String {
        val path1EndsWithSeparator = path1.endsWith(pathSeparator)
        val path2StartsWithSeparator = path2.startsWith(pathSeparator)
        return if (path1EndsWithSeparator && path2StartsWithSeparator) {
            path1 + path2.substring(1)
        } else if (path1EndsWithSeparator || path2StartsWithSeparator) {
            path1 + path2
        } else {
            path1 + pathSeparator + path2
        }
    }

    /**
     * Given a full path, returns a [Comparator] suitable for sorting patterns in order of
     * explicitness.
     *
     * This`Comparator` will [sort][List.sort]
     * a list so that more specific patterns (without uri templates or wild cards) come before
     * generic patterns. So given a list with the following patterns:
     *
     *  1. `/hotels/new`
     *  1. `/hotels/{hotel}`  1. `/hotels/ *`
     *
     * the returned comparator will sort this list so that the order will be as indicated.
     *
     * The full path given as parameter is used to test for exact matches. So when the given path
     * is `/hotels/2`, the pattern `/hotels/2` will be sorted before `/hotels/1`.
     * @param path the full path to use for comparison
     * @return a comparator capable of sorting patterns in order of explicitness
     */
    override fun getPatternComparator(path: String): Comparator<String> {
        return AntPatternComparator(path)
    }

    /**
     * Tests whether or not a string matches against a pattern via a [Pattern].
     *
     * The pattern may contain special characters: '*' means zero or more characters; '?' means one and
     * only one character; '{' and '}' indicate a URI template pattern. For example <tt>/users/{user}</tt>.
     */
    protected class AntPathStringMatcher @JvmOverloads constructor(pattern: String?, caseSensitive: Boolean = true) {

        private val pattern: Pattern
        private val variableNames: MutableList<String> = LinkedList()
        private fun quote(s: String?, start: Int, end: Int): String {
            return if (start == end) "" else Pattern.quote(s!!.substring(start, end))
        }

        /**
         * Main entry point.
         * @return `true` if the string matches against the pattern, or `false` otherwise.
         */
        fun matchStrings(str: String?, uriTemplateVariables: MutableMap<String, String>?): Boolean {
            val matcher = pattern.matcher(str)
            return if (matcher.matches()) {
                if (uriTemplateVariables != null) {
                    // SPR-8455
                    require(variableNames.size == matcher.groupCount()) {
                        "The number of capturing groups in the pattern segment $pattern" +
                        " does not match the number of URI template variables it defines, " +
                        "which can occur if capturing groups are used in a URI template regex. " +
                        "Use non-capturing groups instead."
                    }
                    for (i in 1..matcher.groupCount()) {
                        val name = variableNames[i - 1]
                        val value = matcher.group(i)
                        uriTemplateVariables[name] = value
                    }
                }
                true
            } else {
                false
            }
        }

        companion object {

        }

        init {
            val patternBuilder = StringBuilder()
            val matcher = GLOB_PATTERN.matcher(pattern)
            var end = 0
            while (matcher.find()) {
                patternBuilder.append(quote(pattern, end, matcher.start()))
                val match = matcher.group()
                if ("?" == match) {
                    patternBuilder.append('.')
                } else if ("*" == match) {
                    patternBuilder.append(".*")
                } else if (match.startsWith("{") && match.endsWith("}")) {
                    val colonIdx = match.indexOf(':')
                    if (colonIdx == -1) {
                        patternBuilder.append(DEFAULT_VARIABLE_PATTERN)
                        variableNames.add(matcher.group(1))
                    } else {
                        val variablePattern = match.substring(colonIdx + 1, match.length - 1)
                        patternBuilder.append('(')
                        patternBuilder.append(variablePattern)
                        patternBuilder.append(')')
                        val variableName = match.substring(1, colonIdx)
                        variableNames.add(variableName)
                    }
                }
                end = matcher.end()
            }
            patternBuilder.append(quote(pattern, end, pattern!!.length))
            this.pattern = if (caseSensitive) Pattern.compile(patternBuilder.toString()) else Pattern.compile(
                patternBuilder.toString(),
                Pattern.CASE_INSENSITIVE
            )
        }
    }

    /**
     * The default [Comparator] implementation returned by
     * [.getPatternComparator].
     *
     * In order, the most "generic" pattern is determined by the following:
     *
     *  * if it's null or a capture all pattern (i.e. it is equal to "/ **")
     *  * if the other pattern is an actual match
     *  * if it's a catch-all pattern (i.e. it ends with "**"
     *  * if it's got more "*" than the other pattern
     *  * if it's got more "{foo}" than the other pattern
     *  * if it's shorter than the other pattern
     *
     */
    protected class AntPatternComparator(private val path: String): Comparator<String> {

        /**
         * Compare two patterns to determine which should match first, i.e. which
         * is the most specific regarding the current path.
         * @return a negative integer, zero, or a positive integer as pattern1 is
         * more specific, equally specific, or less specific than pattern2.
         */
        override fun compare(pattern1: String, pattern2: String): Int {
            val info1 = PatternInfo(pattern1)
            val info2 = PatternInfo(pattern2)
            if (info1.isLeastSpecific && info2.isLeastSpecific) {
                return 0
            } else if (info1.isLeastSpecific) {
                return 1
            } else if (info2.isLeastSpecific) {
                return -1
            }
            val pattern1EqualsPath = pattern1 == path
            val pattern2EqualsPath = pattern2 == path
            if (pattern1EqualsPath && pattern2EqualsPath) {
                return 0
            } else if (pattern1EqualsPath) {
                return -1
            } else if (pattern2EqualsPath) {
                return 1
            }
            if (info1.isPrefixPattern && info2.doubleWildcards == 0) {
                return 1
            } else if (info2.isPrefixPattern && info1.doubleWildcards == 0) {
                return -1
            }
            if (info1.totalCount != info2.totalCount) {
                return info1.totalCount - info2.totalCount
            }
            if (info1.getLength() != info2.getLength()) {
                return info2.getLength() - info1.getLength()
            }
            if (info1.singleWildcards < info2.singleWildcards) {
                return -1
            } else if (info2.singleWildcards < info1.singleWildcards) {
                return 1
            }
            if (info1.uriVars < info2.uriVars) {
                return -1
            } else if (info2.uriVars < info1.uriVars) {
                return 1
            }
            return 0
        }

        /**
         * Value class that holds information about the pattern, e.g. number of
         * occurrences of "*", "**", and "{" pattern elements.
         */
        private class PatternInfo(private val pattern: String?) {
            var uriVars = 0
                private set
            var singleWildcards = 0
                private set
            var doubleWildcards = 0
                private set
            private var catchAllPattern = false
            var isPrefixPattern = false
            private var length: Int? = null
            protected fun initCounters() {
                var pos = 0
                if (pattern != null) {
                    while (pos < pattern.length) {
                        if (pattern[pos] == '{') {
                            uriVars++
                            pos++
                        } else if (pattern[pos] == '*') {
                            if (pos + 1 < pattern.length && pattern[pos + 1] == '*') {
                                doubleWildcards++
                                pos += 2
                            } else if (pos > 0 && pattern.substring(pos - 1) != ".*") {
                                singleWildcards++
                                pos++
                            } else {
                                pos++
                            }
                        } else {
                            pos++
                        }
                    }
                }
            }

            val isLeastSpecific: Boolean
                get() = pattern == null || catchAllPattern
            val totalCount: Int
                get() = uriVars + singleWildcards + 2 * doubleWildcards

            /**
             * Returns the length of the given pattern, where template variables are considered to be 1 long.
             */
            fun getLength(): Int {
                if (length == null) {
                    length = if (pattern != null) VARIABLE_PATTERN.matcher(
                        pattern
                    ).replaceAll("#").length else 0
                }
                return length!!
            }

            init {
                if (pattern != null) {
                    initCounters()
                    catchAllPattern = pattern == "/**"
                    isPrefixPattern = !catchAllPattern && pattern.endsWith("/**")
                }
                if (uriVars == 0) {
                    length = if (pattern != null) pattern.length else 0
                }
            }
        }
    }

    /**
     * A simple cache for patterns that depend on the configured path separator.
     */
    private class PathSeparatorPatternCache(pathSeparator: String) {
        val endsOnWildCard: String
        val endsOnDoubleWildCard: String

        init {
            endsOnWildCard = "$pathSeparator*"
            endsOnDoubleWildCard = "$pathSeparator**"
        }
    }

    companion object {

        private fun tokenizeToStringArray(
            str: String?, delimiters: String, trimTokens: Boolean, ignoreEmptyTokens: Boolean
        ): Array<String?> {
            if (str == null) {
                return arrayOfNulls(0)
            }
            val st = StringTokenizer(str, delimiters)
            val tokens: MutableList<String?> = ArrayList()
            while (st.hasMoreTokens()) {
                var token = st.nextToken()
                if (trimTokens) {
                    token = token.trim { it <= ' ' }
                }
                if (!ignoreEmptyTokens || token.length > 0) {
                    tokens.add(token)
                }
            }
            return tokens.toTypedArray()
        }

        fun toStringArray(collection: Collection<String>): Array<String> {
            return collection.toTypedArray()
        }
    }
}

private val GLOB_PATTERN = Pattern.compile("\\?|\\*|\\{((?:\\{[^/]+?\\}|[^/{}]|\\\\[{}])+?)\\}")
private const val DEFAULT_VARIABLE_PATTERN = "(.*)"

/** Default path separator: "/".  */
const val DEFAULT_PATH_SEPARATOR = "/"

private const val CACHE_TURNOFF_THRESHOLD = 65536
private val VARIABLE_PATTERN = Pattern.compile("\\{[^/]+?\\}")
private val WILDCARD_CHARS = charArrayOf('*', '?', '{')