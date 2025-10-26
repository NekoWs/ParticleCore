package work.nekow.particlecore.math

import org.joml.Vector2d
import kotlin.math.cos
import kotlin.math.sin

class FourierUtils {
    data class FourierConfig(
        val terms: List<FourierTerm>,
        val duration: Double, val
        timeStep: Double,
        val scale: Double
    )
    companion object {
        val caches = HashMap<FourierConfig, List<Vector2d>>()

        /**
         * 计算傅里叶位置
         * @param terms 傅里叶项
         * @param time 时间
         */
        fun computeFourier(terms: List<FourierTerm>, time: Double, scale: Double): Vector2d {
            var x = 0.0
            var y = 0.0
            for (term in terms) {
                val angle = term.rotate.toDouble() + term.speed.toDouble() * time
                x += term.radius.toDouble() * cos(angle) * scale
                y += term.radius.toDouble() * sin(angle) * scale
            }
            return Vector2d(x, y)
        }

        /**
         * 计算傅里叶级数坐标列表
         * @param terms 傅里叶项
         * @param duration 最大时间
         * @param timeStep 时间跨度
         */
        fun fourierPoints(terms: List<FourierTerm>, duration: Double, timeStep: Double, scale: Double = 1.0): List<Vector2d> {
            val config = FourierConfig(terms, duration, timeStep, scale)
            if (caches.containsKey(config)) {
                return caches[config]!!
            }
            val out = mutableListOf<Vector2d>()
            var totalTime = 0.0
            while (totalTime < duration) {
                val pos = computeFourier(terms, totalTime,  scale)
                out.add(pos)
                totalTime += timeStep
            }
            if (caches.size > 512) {
                caches.remove(caches.keys.first())
            }
            caches[config] = out
            return out
        }
    }
}