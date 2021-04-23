package com.github.nayasis.kotlin.basica.expression

import org.mvel2.compiler.CompiledExpression
import java.io.Serializable

/**
 * MVEL expression wrapper
 */
class MvelExpression {

    private var raw: String? = null
    private var compiled: Serializable? = null

    /**
     * obtains an instance of `Expression`
     *
     * @param expression    MVEL expression language
     * @see [MVEL language guide](http://mvel.documentnode.com/.basic-syntax)
     */
    constructor(expression: String?): this(expression,false)

    /**
     * obtains an instance of `Expression`
     *
     * @param expression    MVEL expression language
     * @param preserve      preserve original expression
     * @see [MVEL language guide](http://mvel.documentnode.com/.basic-syntax)
     */
    constructor(expression: String?, preserve: Boolean) {
        compiled = MvelHandler.compile(expression?.trim())
        if( preserve ) raw = expression
    }

    /**
     * run expression
     * @param param  parameter
     * @return execution result
    */
    fun <T:Any> run(param: Any? = null): T? = MvelHandler.run(compiled, param)

    /**
     * test expression
     * @param param  parameter
     * @return execution result
     */
    fun test(param: Any? = null): Boolean = run(param) ?: false

    override fun toString(): String {
        return when {
            raw != null -> raw!!
            compiled == null -> ""
            else -> {
                try {
                    (compiled as CompiledExpression).firstNode.expr.toString()
                } catch (e: Exception) {
                    compiled.toString()
                }
            }
        }
    }

}