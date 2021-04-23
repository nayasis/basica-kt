package com.github.nayasis.kotlin.basica.expression

import com.github.nayasis.kotlin.basica.reflection.Reflector
import org.mvel2.CompileException
import org.mvel2.MVEL
import org.mvel2.ParserContext
import java.io.Serializable

@Suppress("MemberVisibilityCanBePrivate")
class MvelHandler { companion object {

    val ctx = ParserContext().apply{
        addPackageImport("com.github.nayasis.kotlin.basica.core")
        addImport(Reflector::class.java)
        classLoader.loadClass("com.github.nayasis.kotlin.basica.core.Validator").methods.forEach {
            if( it.name == "nvl" ) {
                addImport( it.name, it )
            }
        }
    }

    /**
     * compile expression
     *
     * @param expression    MVEL expression
     * @return compiled code
     * @throws CompileException if compile error occurs.
     */
    @Throws(CompileException::class)
    fun compile(expression: String?): Serializable = MVEL.compileExpression(expression, ctx)

    /**
     * run compiled expression
     *
     * @param expression    compiled expression
     * @param param         parameter
     * @param <T>           return type
     * @return execution result
    </T> */
    fun <T:Any> run(expression: Serializable?, param: Any?): T? = MVEL.executeExpression(expression, param) as T

    /**
     * run compiled expression
     *
     * @param expression    compiled expression
     * @param <T>           return type
     * @return execution result
    */
    fun <T:Any> run(expression: Serializable?): T? = run(expression, null)

}}