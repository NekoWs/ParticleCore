package work.nekow.particlecore.math

import com.ezylang.evalex.Expression
import com.ezylang.evalex.config.ExpressionConfiguration
import com.ezylang.evalex.data.EvaluationValue
import work.nekow.particlecore.utils.Expressions
import java.util.*

class FunctionSolver {
    companion object {
        // 复用配置对象
        private val evalConfig by lazy {
            ExpressionConfiguration.defaultConfiguration()
                .withAdditionalFunctions(
                    AbstractMap.SimpleEntry("distance", DistanceFunction()),
                    AbstractMap.SimpleEntry("pow", PowFunction())
                )
        }

        private val expressionParseCache = Collections.synchronizedMap(
            LinkedHashMap<String, List<Pair<Expression?, List<Pair<String, Expression>>>>>(128)
        )
        fun parseExpressionWithCache(expression: String): List<Pair<Expression?, List<Pair<String, Expression>>>> {
            return expressionParseCache.getOrPut(expression) {
                parseExpression(expression).toList()
            }
        }

        /**
         * 解析算式，返回值为：
         * 列表<
         *  组<条件?, 列表<
         *   组<前缀, 算式>
         *  >
         * >
         * @param exp 算式
         */
        private fun parseExpression(exp: String): ArrayList<Pair<Expression?, ArrayList<Pair<String, Expression>>>> {
            val result = ArrayList<Pair<Expression?, ArrayList<Pair<String, Expression>>>>()
            val exp = Expressions(exp)
            exp.expressions.forEach { (condition, exps) ->
                val cond = condition?.let { Expression(condition, evalConfig)}
                val list = ArrayList<Pair<String, Expression>>()
                exps.forEach {
                    list.add(it.prefix to Expression(it.suffix, evalConfig))
                }
                result.add(cond to list)
            }
            return result
        }

        /**
         * 通过条件计算所有需要执行的表达式
         *
         * @param exps 表达式列表
         * @param values 常量
         * @param arguments 变量
         */
        fun evaluateCond(
            exps: List<Pair<Expression?, List<Pair<String, Expression>>>>,
            values: Map<String, EvaluationValue>,
            arguments: Map<String, EvaluationValue>,
        ): Sequence<Pair<String, Expression>> {
            return exps.asSequence()
                .filter { (cond, _) ->
                    cond?.let {
                        try {
                            it.withValues(arguments)
                                .withValues(values)
                                .evaluate()
                                .booleanValue
                        } catch (_: Exception) {
                            false
                        }
                    } ?: true
                }
                .flatMap { it.second.asSequence() }
        }
    }
}