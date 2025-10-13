package io.github.nayasis.kotlin.basica.expression

import java.io.Serializable

/**
 * MVEL expression wrapper
 */
class MvelExpression{

    private var original: String
    private var compiled: Serializable

    /**
     * obtains an instance of `Expression`
     *
     * @param expression MVEL expression language
     * @param strict     if true, do not modify the expression to handle hyphenated property access
     * @see [MVEL language guide](http://mvel.documentnode.com/.basic-syntax)
     */
    constructor(expression: String, strict: Boolean = false) {
        original = expression.trim()
        compiled = MvelHandler.compile( if(strict) original else preprocess(original))
    }

    /**
     * Preprocess expression to handle hyphenated property access
     * Converts dot notation with hyphens to bracket notation
     * e.g., "second.minus-key" -> "second['minus-key']"
     * Handles nested properties like "second.minus-key.third-key-value"
     */
    private fun preprocess(expression: String): String {
        if (expression.isEmpty()) return expression
        
        var rs = expression
        
        // Process from right to left to handle nested properties correctly
        // This regex matches any property that contains hyphens, including those after bracket notation
        val hyphenChecker = """(\w+|\[[^]]+])\.([a-zA-Z_][a-zA-Z0-9_-]*-[a-zA-Z0-9_-]*)""".toRegex()
        
        // Keep processing until no more hyphenated properties are found
        var previous: String
        do {
            previous = rs
            rs = hyphenChecker.replace(rs) { matched ->
                val (obj, prop) = matched.destructured
                "$obj['$prop']"
            }
        } while (rs != previous)
        
        return rs
    }

    /**
     * run expression
     * @param param  parameter
     * @return execution result
    */
    fun <T> get(param: Any? = null): T? = MvelHandler.run(compiled, param)

    /**
     * run expression
     * @param param  parameter
     * @return execution result
     */
    fun run(param: Any? = null) = MvelHandler.run<Any>(compiled, param)

    /**
     * test expression
     * @param param  parameter
     * @return execution result
     */
    fun test(param: Any? = null): Boolean = get(param) ?: false

    override fun toString(): String {
        return original
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MvelExpression
        return original == other.original
    }

    override fun hashCode(): Int {
        return original.hashCode()
    }

}