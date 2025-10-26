package work.nekow.particlecore.math

import com.ezylang.evalex.Expression
import com.ezylang.evalex.data.EvaluationValue
import com.ezylang.evalex.functions.AbstractFunction
import com.ezylang.evalex.functions.FunctionParameter
import com.ezylang.evalex.parser.Token
import kotlin.math.pow

@FunctionParameter(name = "a")
@FunctionParameter(name = "b")
class PowFunction : AbstractFunction() {
    override fun evaluate(
        expression: Expression,
        functionToken: Token,
        vararg parameterValues: EvaluationValue
    ): EvaluationValue? {
        val a = parameterValues[0].numberValue.toDouble()
        val b = parameterValues[1].numberValue.toDouble()
        return EvaluationValue.numberValue(a.pow(b).toBigDecimal())
    }
}