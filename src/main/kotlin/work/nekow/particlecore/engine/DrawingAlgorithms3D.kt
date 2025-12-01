package work.nekow.particlecore.engine

import work.nekow.particlecore.engine.FillAlgorithms3D.Companion.fillTriangles
import work.nekow.particlecore.engine.FillAlgorithms3D.Companion.triangulatePlanarPolygon
import work.nekow.particlecore.engine.utils.Connection
import work.nekow.particlecore.engine.utils.ConnectionType
import work.nekow.particlecore.engine.utils.Point3D
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.pow

class DrawingAlgorithms3D {
    companion object {
        /**
         * 直线插值
         *
         * @param start 起点
         * @param end 终点
         * @param stepSize 点之间最大距离
         */
        fun interpolateLine(
            start: Point3D,
            end: Point3D,
            stepSize: Double
        ): List<Point3D> {
            val distance = start.distanceTo(end)
            val steps = max(2, ceil(distance / stepSize).toInt())

            return (0 until steps).map { i ->
                val t = i.toDouble() / (steps - 1)
                start * (1 - t) + end * t
            }
        }

        /**
         * 多段线插值
         *
         * @param points 点列表
         * @param stepSize 点之间最大长度
         * @param closed 是否闭合
         */
        fun interpolatePolyline(
            points: List<Point3D>,
            stepSize: Double,
            closed: Boolean = false
        ): List<Point3D> {
            val result = mutableListOf<Point3D>()

            val segments = if (closed) {
                points.indices.map { i ->
                    points[i] to points[(i + 1) % points.size]
                }
            } else {
                points.windowed(2) { (p1, p2) -> p1 to p2 }
            }

            segments.forEach { (p1, p2) ->
                val segmentPoints = interpolateLine(p1, p2, stepSize)
                result.addAll(segmentPoints.dropLast(1)) // 避免重复点
            }

            if (points.isNotEmpty()) {
                result.add(points.last())
            }

            return result
        }

        /**
         * 贝塞尔曲线
         *
         * @param controlPoints 控制点列表
         * @param stepSize 点之间最大距离
         */
        fun bezierCurve(
            controlPoints: List<Point3D>,
            stepSize: Double
        ): List<Point3D> {
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
            controlPoints: List<Point3D>,
            t: Double
        ): Point3D {
            var result = Point3D(0.0, 0.0, 0.0)
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

        /**
         * B样条曲线
         *
         * @param controlPoints 控制点
         * @param degree 数量
         * @param stepSize 点之间最大长度
         */
        fun bSplineCurve3D(
            controlPoints: List<Point3D>,
            degree: Int = 3,
            stepSize: Double
        ): List<Point3D> {
            val n = controlPoints.size - 1
            val m = n + degree + 1

            // 生成均匀节点向量
            val knots = DoubleArray(m + 1) { i ->
                when {
                    i <= degree -> 0.0
                    i >= m - degree -> 1.0
                    else -> (i - degree).toDouble() / (n - degree + 1)
                }
            }

            val steps = ceil(1.0 / stepSize).toInt()
            return (0..steps).map { step ->
                val t = step.toDouble() / steps
                bSplinePoint(controlPoints, knots, degree, t)
            }
        }

        private fun bSplinePoint(
            controlPoints: List<Point3D>,
            knots: DoubleArray,
            degree: Int,
            t: Double
        ): Point3D {
            val n = controlPoints.size
            val basis = DoubleArray(n)

            // 找到 t 所在的区间
            var span = degree
            while (span < n && t >= knots[span + 1]) {
                span++
            }

            basis[span] = 1.0

            for (k in 1..degree) {
                for (i in span - k..span) {
                    val left = knots[i + k] - knots[i]
                    val right = knots[i + k + 1] - knots[i + 1]

                    val leftCoeff = if (left != 0.0) (t - knots[i]) / left else 0.0
                    val rightCoeff = if (right != 0.0) (knots[i + k + 1] - t) / right else 0.0

                    basis[i] = leftCoeff * basis[i] + rightCoeff * basis[i + 1]
                }
            }

            var result = Point3D(0.0, 0.0, 0.0)
            for (i in 0 until n) {
                result += controlPoints[i] * basis[i]
            }

            return result
        }

        // 根据连接类型生成点
        fun generatePointsFromConnection(
            connection: Connection,
            stepSize: Double
        ): List<Point3D> = when (connection.type) {
            ConnectionType.LINE ->
                interpolatePolyline(connection.points, stepSize, connection.isClosed)

            ConnectionType.BEZIER_CURVE -> {
                val controlPoints = connection.controlPoints ?: connection.points
                bezierCurve(controlPoints, stepSize)
            }

            ConnectionType.BSPLINE_CURVE -> {
                val controlPoints = connection.controlPoints ?: connection.points
                bSplineCurve3D(controlPoints, stepSize = stepSize)
            }

            ConnectionType.PLANAR_SURFACE ->
                generatePlanarSurfacePoints(connection.points, stepSize)
        }

        private fun generatePlanarSurfacePoints(
            points: List<Point3D>,
            stepSize: Double
        ): List<Point3D> {
            if (points.size < 3) return emptyList()

            // 计算三角剖分并填充
            val triangles = triangulatePlanarPolygon(points)
            return fillTriangles(triangles, stepSize)
        }
    }
}