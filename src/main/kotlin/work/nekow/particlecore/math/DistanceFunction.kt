package work.nekow.particlecore.math

import com.ezylang.evalex.Expression
import com.ezylang.evalex.data.EvaluationValue
import com.ezylang.evalex.functions.AbstractFunction
import com.ezylang.evalex.functions.FunctionParameter
import com.ezylang.evalex.parser.Token
import net.minecraft.util.math.Vec3d

@FunctionParameter(name = "x1")
@FunctionParameter(name = "y1")
@FunctionParameter(name = "z1")
@FunctionParameter(name = "x2")
@FunctionParameter(name = "y2")
@FunctionParameter(name = "z2")
class DistanceFunction : AbstractFunction() {
    override fun evaluate(
        expression: Expression,
        functionToken: Token,
        vararg parameterValues: EvaluationValue
    ): EvaluationValue? {
        val x1 = parameterValues[0].numberValue.toDouble()
        val y1 = parameterValues[1].numberValue.toDouble()
        val z1 = parameterValues[2].numberValue.toDouble()
        val x2 = parameterValues[3].numberValue.toDouble()
        val y2 = parameterValues[4].numberValue.toDouble()
        val z2 = parameterValues[5].numberValue.toDouble()
        val vec1 = Vec3d(x1, y1, z1)
        val vec2 = Vec3d(x2, y2, z2)
        return EvaluationValue.numberValue(vec1.distanceTo(vec2).toBigDecimal())
    }
}