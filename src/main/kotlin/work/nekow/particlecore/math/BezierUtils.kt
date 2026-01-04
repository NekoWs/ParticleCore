package work.nekow.particlecore.math

import work.nekow.particlecore.canvas.utils.Point3d
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.pow

class BezierUtils {
    companion object {
        /**
         * 贝塞尔曲线
         *
         * @param controlPoints 控制点列表
         * @param stepSize 精度
         */
        fun bezierCurve(
            controlPoints: List<Point3d>,
            stepSize: Double
        ): List<Point3d> {
            if (controlPoints.size < 2) return emptyList()

            // 估算曲线长度
            val estimatedLength = controlPoints
                .windowed(2)
                .sumOf { (p1, p2) -> p1.distanceTo(p2) }

            val steps = max(10, ceil(estimatedLength / stepSize).toInt())

            return (0..steps).map { i ->
                val t = i.toDouble() / steps
                bezierPoint(controlPoints, t)
            }
        }

        private fun bezierPoint(
            controlPoints: List<Point3d>,
            t: Double
        ): Point3d {
            var result = Point3d(0.0, 0.0, 0.0)
            val n = controlPoints.size - 1

            for (i in controlPoints.indices) {
                val binomial = combination(n, i)
                val weight = binomial * t.pow(i) * (1 - t).pow(n - i)
                result += controlPoints[i] * weight
            }

            return result
        }

        private fun combination(n: Int, k: Int): Double {
            if (k !in 0..n) return 0.0
            var result = 1.0
            for (i in 1..k) {
                result = result * (n - k + i) / i
            }
            return result
        }
    }
}