package work.nekow.particlecore.client.particle

import com.ezylang.evalex.Expression
import com.ezylang.evalex.data.EvaluationValue
import net.minecraft.util.math.Vec3d
import work.nekow.particlecore.math.FunctionSolver.Companion.evaluateCond
import work.nekow.particlecore.math.FunctionSolver.Companion.parseExpressionWithCache
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

data class ParticleEnv(
    val expression: String,
    val velocity: Vec3d,
    var offset: Vec3d,
    var ticks: Int,
    var center: Vec3d,
    val arguments: HashMap<String, EvaluationValue> = HashMap(),
    val age: Int
) {
    val exps: List<Pair<Expression?, List<Pair<String, Expression>>>> by lazy {
        if (expression.isEmpty()) emptyList() else parseExpressionWithCache(expression)
    }

    val results = HashMap<Int, ParticleEnvData>()

    init {
        offset = offset.scale(SCALE_PRECISION)
        center = center.scale(SCALE_PRECISION)

        val data = EnvData(expression, arguments, age)
        caches[data]?.let { cachedResults ->
            results.putAll(cachedResults)
        } ?: run {
            initParticleData()
            if (caches.size > MAX_CACHE_SIZE) {
                caches.remove(caches.keys.first())
            }
            caches[data] = results
        }
    }

    fun initParticleData() {
        var abstractData = ParticleEnvData(
            position = Vec3d.ZERO,
            velocity = velocity
        )
        // 预计算常量值
        val valuesMap = HashMap<String, EvaluationValue>(32)  // 预分配大小

        for (t in 0 until age) {
            // 复用map对象减少分配
            getValues(valuesMap, abstractData, t)

            val data = abstractData.clone()
            // 使用序列进行惰性求值
            evaluateCond(exps, valuesMap, arguments).forEach { (prefix, expr) ->
                try {
                    val value = expr.withValues(arguments)
                        .withValues(valuesMap)
                        .evaluate()
                    updateData(prefix, value, data, arguments)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            data.position = data.position.add(data.velocity)

            results[t] = data
            abstractData = data
        }
    }

    fun current(): ParticleEnvData {
        return results[ticks] ?: ParticleEnvData()
    }

    fun next(): ParticleEnvData {
        return current().also { ticks++ }
    }

    companion object {
        private val caches = Collections.synchronizedMap(
            object : LinkedHashMap<EnvData, HashMap<Int, ParticleEnvData>>(16, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<EnvData, HashMap<Int, ParticleEnvData>>): Boolean {
                    return size > MAX_CACHE_SIZE
                }
            }
        )

        const val MAX_CACHE_SIZE = 20480
        const val SCALE_PRECISION = 5

        /**
         * 将计算结果应用于 data
         *
         * @param prefix 修改键
         * @param value 值
         * @param data 粒子数据
         * @param arguments 变量
         */
        fun updateData(
            prefix: String,
            value: EvaluationValue,
            data: ParticleEnvData,
            arguments: HashMap<String, EvaluationValue>
        ) {
            when (prefix) {
                "vx", "vy", "vz" -> {
                    val velocity = data.velocity
                    data.velocity = when (prefix) {
                        "vx" -> Vec3d(value.numberValue.toDouble(), velocity.y, velocity.z)
                        "vy" -> Vec3d(velocity.x, value.numberValue.toDouble(), velocity.z)
                        else -> Vec3d(velocity.x, velocity.y, value.numberValue.toDouble())
                    }
                }
                "cr" -> data.red = value.numberValue.toFloat() / 255
                "cg" -> data.green = value.numberValue.toFloat() / 255
                "cb" -> data.blue = value.numberValue.toFloat() / 255
                "ca" -> data.alpha = value.numberValue.toFloat() / 255
                "angle" -> data.angle = value.numberValue.toFloat()
                "light" -> data.light = value.numberValue.toInt()
                "gravity" -> data.gravity = value.numberValue.toFloat()
                "scale" -> data.scale = value.numberValue.toFloat()
                else -> arguments[prefix] = value
            }
            data.prefix.add(prefix)
        }

        /**
         * 获取用于计算的粒子变量
         *
         * @param data 粒子数据
         * @param ticks 刻
         * @return 粒子变量
         */
        fun getValues(
            map: HashMap<String, EvaluationValue>,
            data: ParticleEnvData,
            ticks: Int
        ) {
            map.clear()

            // 添加动态值
            map["vx"] = numberValue(data.velocity.x)
            map["vy"] = numberValue(data.velocity.y)
            map["vz"] = numberValue(data.velocity.z)

            map["x"] = numberValue(data.position.x)
            map["y"] = numberValue(data.position.y)
            map["z"] = numberValue(data.position.z)

            map["cr"] = numberValue(data.red.toDouble())
            map["cg"] = numberValue(data.green.toDouble())
            map["cb"] = numberValue(data.blue.toDouble())
            map["ca"] = numberValue(data.alpha.toDouble())

            map["angle"] = numberValue(data.angle.toDouble())
            map["light"] = numberValue(data.light.toDouble())
            map["gravity"] = numberValue(data.gravity.toDouble())
            map["scale"] = numberValue(data.scale.toDouble())
            map["t"] = numberValue(ticks.toDouble())
        }

        fun numberValue(value: Number): EvaluationValue {
            return EvaluationValue.numberValue(BigDecimal.valueOf(value.toDouble()))
        }
    }
    fun Vec3d.scale(scale: Int): Vec3d {
        return Vec3d(
            BigDecimal.valueOf(x).setScale(scale, RoundingMode.HALF_UP).toDouble(),
            BigDecimal.valueOf(y).setScale(scale, RoundingMode.HALF_UP).toDouble(),
            BigDecimal.valueOf(z).setScale(scale, RoundingMode.HALF_UP).toDouble()
        )
    }
}