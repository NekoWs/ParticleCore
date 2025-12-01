package work.nekow.particlecore.client.particle

import com.ezylang.evalex.Expression
import com.ezylang.evalex.data.EvaluationValue
import net.minecraft.util.math.Vec3d
import work.nekow.particlecore.math.FunctionSolver.Companion.evaluateCond
import work.nekow.particlecore.math.FunctionSolver.Companion.parseExpressionWithCache
import java.math.BigDecimal

class FunctionPoints(
    val function: String,
    var range: Pair<Double, Double>,
    val step: Double,
) {
    val exps: List<Pair<Expression?, List<Pair<String, Expression>>>> by lazy {
        if (function.isEmpty()) emptyList() else parseExpressionWithCache(function)
    }
    val points = mutableListOf<Vec3d>()

    init {
        // 修复如果 First 大于 Second 时不会执行的问题
        if (range.first > range.second) {
            range = Pair(range.second, range.first)
        }
        val data = FunctionData(function, step, range)
        caches[data]?.let {
            points.addAll(it)
        } ?: run {
            init()
            if (caches.size > MAX_CACHE_SIZE) {
                caches.clear()
            }
            caches[data] = points
        }
    }

    fun init() {
        val values = mutableMapOf(
            "x" to numberValue(0.0),
            "y" to numberValue(0.0),
            "z" to numberValue(0.0)
        )
        val variables = mutableMapOf<String, EvaluationValue>()
        var t = range.first
        while (t <= range.second) {
            values["t"] = numberValue(t)

            evaluateCond(exps, values, variables).forEach { (prefix, exp) ->
                try {
                    val value = exp.withValues(variables)
                        .withValues(values)
                        .evaluate()
                    when (prefix) {
                        "x" -> values["x"] = value
                        "y" -> values["y"] = value
                        "z" -> values["z"] = value
                        else -> variables[prefix] = value
                    }
                } catch (_: Exception) { /* ignore */ }
            }
            points.add(Vec3d(
                values["x"]!!.numberValue.toDouble(),
                values["y"]!!.numberValue.toDouble(),
                values["z"]!!.numberValue.toDouble(),
            ))
            t += step
        }
    }
    
    fun numberValue(value: Number): EvaluationValue {
        return EvaluationValue.numberValue(BigDecimal.valueOf(value.toDouble()))
    }

    companion object {
        val caches = mutableMapOf<FunctionData, MutableList<Vec3d>>()

        const val MAX_CACHE_SIZE = 20480
    }

    data class FunctionData(
        val function: String,
        val step: Double,
        val range: Pair<Double, Double>
    )
}