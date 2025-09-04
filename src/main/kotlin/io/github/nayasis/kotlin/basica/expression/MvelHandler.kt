package io.github.nayasis.kotlin.basica.expression

import io.github.nayasis.kotlin.basica.reflection.Reflector
import org.mvel2.CompileException
import org.mvel2.MVEL
import org.mvel2.ParserContext
import java.io.Serializable

@Suppress("MemberVisibilityCanBePrivate", "UNCHECKED_CAST")
class MvelHandler { companion object {

    val ctx = ParserContext().apply{
        val base = "io.github.nayasis.kotlin.basica.core"
        listOf(
            "character",
            "collection",
            "extension",
            "klass",
            "localdate",
            "math",
            "number",
            "path",
            "resource",
            "string",
            "validator",
        ).map { addPackageImport("$base.$it") }
        addImport(Reflector::class.java)
        classLoader.loadClass("$base.validator.Validator").methods.forEach {
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
    fun compile(expression: String?): Serializable {
        val processedExpression = preprocessExpression(expression)
        return MVEL.compileExpression(processedExpression, ctx)
    }

    /**
     * Preprocess expression to handle hyphenated property access
     * Converts dot notation with hyphens to bracket notation
     * e.g., "second.minus-key" -> "second['minus-key']"
     */
    private fun preprocessExpression(expression: String?): String? {
        if (expression.isNullOrBlank()) return expression
        
        // Regex to match property access with hyphens
        // Matches patterns like: object.property-with-hyphens
        val hyphenPropertyRegex = """(\w+)\.([a-zA-Z_][a-zA-Z0-9_-]*-[a-zA-Z0-9_-]*)""".toRegex()
        
        return hyphenPropertyRegex.replace(expression) { matchResult ->
            val (objectName, propertyName) = matchResult.destructured
            "$objectName['$propertyName']"
        }
    }

    /**
     * run compiled expression
     *
     * @param expression    compiled expression
     * @param param         parameter
     * @param <T>           return type
     * @return execution result
    </T> */
    @Suppress("UNCHECKED_CAST")
    fun <T: Any> run(expression: Serializable?, param: Any?): T? {
        return when {
            param is Map<*,*> -> MVEL.executeExpression(expression, param)
            else -> MVEL.executeExpression(expression, param)
        } as T
    }

    /**
     * run compiled expression
     *
     * @param expression    compiled expression
     * @param <T>           return type
     * @return execution result
    */
    fun <T: Any> run(expression: Serializable?): T? = run(expression, null)

}}