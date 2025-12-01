package work.nekow.particlecore.canvas

import work.nekow.particlecore.canvas.utils.Connection
import work.nekow.particlecore.canvas.utils.ConnectionType
import work.nekow.particlecore.canvas.utils.Point3D
import work.nekow.particlecore.canvas.utils.PointGroup
import kotlin.math.*

@Suppress("unused")
class DrawingEngine3D {
    private val groups = mutableListOf<PointGroup>()
    private var precision = 0.1

    fun setPrecision(precision: Double) {
        this.precision = max(0.01, precision)
    }

    fun createGroup(
        points: List<Point3D>,
        connections: List<Connection> = emptyList(),
        isClosed: Boolean = false
    ): PointGroup {
        val group = PointGroup(points, connections, isClosed)
        groups.add(group)
        return group
    }

    fun drawLine(start: Point3D, end: Point3D): List<Point3D> =
        DrawingAlgorithms3D.interpolateLine(start, end, precision)

    fun drawPolyline(
        points: List<Point3D>,
        closed: Boolean = false
    ): List<Point3D> =
        DrawingAlgorithms3D.interpolatePolyline(points, precision, closed)

    fun drawBezierCurve(controlPoints: List<Point3D>): List<Point3D> =
        DrawingAlgorithms3D.bezierCurve(controlPoints, precision)

    fun drawBSplineCurve(controlPoints: List<Point3D>): List<Point3D> =
        DrawingAlgorithms3D.bSplineCurve3D(controlPoints, stepSize = precision)

    fun renderGroup(
        group: PointGroup,
        renderEdges: Boolean = true
    ): List<Point3D> {
        val points = mutableListOf<Point3D>()

        if (renderEdges) {
            group.connections.forEach { connection ->
                val connectionPoints = DrawingAlgorithms3D.generatePointsFromConnection(
                    connection,
                    precision
                )
                points.addAll(connectionPoints)
            }
        }

        // 去重
        return points.distinctBy { point ->
            Triple(
                roundToPrecision(point.x),
                roundToPrecision(point.y),
                roundToPrecision(point.z)
            )
        }
    }

    private fun roundToPrecision(value: Double): Double {
        return (value / precision).roundToInt() * precision
    }

    private fun Double.roundTo(precision: Double): Double {
        val factor = 1.0 / precision
        return (this * factor).roundToInt() / factor
    }

    fun exportAllPoints(): List<Point3D> =
        groups.flatMap { renderGroup(it) }

    fun clear() {
        groups.clear()
    }

    companion object {
        // 创建简单几何体
        fun createCube(
            center: Point3D,
            size: Double
        ): PointGroup {
            val half = size / 2
            val vertices = listOf(
                Point3D(center.x - half, center.y - half, center.z - half),
                Point3D(center.x + half, center.y - half, center.z - half),
                Point3D(center.x + half, center.y + half, center.z - half),
                Point3D(center.x - half, center.y + half, center.z - half),
                Point3D(center.x - half, center.y - half, center.z + half),
                Point3D(center.x + half, center.y - half, center.z + half),
                Point3D(center.x + half, center.y + half, center.z + half),
                Point3D(center.x - half, center.y + half, center.z + half)
            )

            val edges = listOf(
                listOf(0, 1), listOf(1, 2), listOf(2, 3), listOf(3, 0), // 底面
                listOf(4, 5), listOf(5, 6), listOf(6, 7), listOf(7, 4), // 顶面
                listOf(0, 4), listOf(1, 5), listOf(2, 6), listOf(3, 7)  // 侧面
            )

            val connections = edges.map { edge ->
                Connection(
                    points = listOf(vertices[edge[0]], vertices[edge[1]]),
                    type = ConnectionType.LINE
                )
            }

            return PointGroup(vertices, connections, isClosed = true)
        }

        // 几何体创建
        fun createSphere(
            center: Point3D,
            radius: Double,
            resolution: Double
        ): PointGroup {
            val points = mutableListOf<Point3D>()
            val connections = mutableListOf<Connection>()

            // 经度和纬度采样
            val thetaSteps = max(10, (2 * PI * radius / resolution).toInt())
            val phiSteps = max(10, (PI * radius / resolution).toInt())

            // 生成顶点
            for (i in 0..thetaSteps) {
                val theta = 2 * PI * i / thetaSteps
                for (j in 0..phiSteps) {
                    val phi = PI * j / phiSteps

                    val x = center.x + radius * sin(phi) * cos(theta)
                    val y = center.y + radius * sin(phi) * sin(theta)
                    val z = center.z + radius * cos(phi)

                    points.add(Point3D(x, y, z))
                }
            }

            // 创建连接（线框）
            val numPointsPerRing = phiSteps + 1

            // 经线连接
            for (i in 0 until thetaSteps) {
                for (j in 0 until phiSteps) {
                    val current = i * numPointsPerRing + j
                    val nextInRing = i * numPointsPerRing + (j + 1)
                    val nextMeridian = ((i + 1) % thetaSteps) * numPointsPerRing + j

                    connections.add(Connection(
                        points = listOf(points[current], points[nextInRing]),
                        type = ConnectionType.LINE
                    ))

                    connections.add(Connection(
                        points = listOf(points[current], points[nextMeridian]),
                        type = ConnectionType.LINE
                    ))
                }
            }

            return PointGroup(points, connections, isClosed = false)
        }
    }
}