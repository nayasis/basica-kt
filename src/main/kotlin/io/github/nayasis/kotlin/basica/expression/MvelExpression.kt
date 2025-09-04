package io.github.nayasis.kotlin.basica.expression

import java.io.Serializable

/**
 * MVEL expression wrapper
 */
class MvelExpression{

    private var original: String
    var compiled: Serializable

    /**
     * obtains an instance of `Expression`
     *
     * @param expression MVEL expression language
     * @see [MVEL language guide](http://mvel.documentnode.com/.basic-syntax)
     */
    constructor(expression: String) {
        original = expression.trim()
        compiled = MvelHandler.compile(original)
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