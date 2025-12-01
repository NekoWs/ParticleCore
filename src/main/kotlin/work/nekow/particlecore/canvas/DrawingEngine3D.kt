package work.nekow.particlecore.canvas

import work.nekow.particlecore.canvas.utils.*
import kotlin.math.*

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

    fun fillGroup(
        group: PointGroup,
        method: FillMethod = FillMethod.TRIANGULATION,
        density: Double? = null
    ): List<Point3D> {
        val fillDensity = density ?: (precision * 2)

        return when (method) {
            FillMethod.TRIANGULATION -> {
                if (group.isPlanar()) {
                    val triangles = FillAlgorithms3D.triangulatePlanarPolygon(group.points)
                    FillAlgorithms3D.fillTriangles(triangles, fillDensity)
                } else {
                    // 对于非平面多边形，使用体素填充
                    FillAlgorithms3D.voxelFill(group, fillDensity)
                }
            }

            FillMethod.VOXEL -> {
                FillAlgorithms3D.voxelFill(group, fillDensity)
            }

            FillMethod.SURFACE_SAMPLING -> {
                FillAlgorithms3D.surfaceSampling(group.connections, fillDensity)
            }

            FillMethod.CONTOUR -> {
                contourFill(group, fillDensity)
            }
        }
    }

    // 轮廓填充方法
    private fun contourFill(group: PointGroup, density: Double): List<Point3D> {
        if (!group.isClosed || group.points.isEmpty()) return emptyList()

        val (minPoint, maxPoint) = group.boundingBox()

        val points = mutableListOf<Point3D>()

        // 根据密度决定轮廓层数
        val contourSpacing = density * 2
        val numContours = max(1, ((maxPoint.z - minPoint.z) / contourSpacing).toInt())

        // 在多个高度上生成轮廓
        for (contourIndex in 0..numContours) {
            val height = minPoint.z + (maxPoint.z - minPoint.z) * contourIndex / numContours
            val contourPoints = generateContourAtHeight(group, height, density)
            points.addAll(contourPoints)
        }

        return points
    }

    private fun generateContourAtHeight(
        group: PointGroup,
        height: Double,
        tolerance: Double
    ): List<Point3D> {
        val contourPoints = mutableListOf<Point3D>()

        // 找到与指定高度相交的连接
        for (connection in group.connections) {
            val intersectionPoints = mutableListOf<Point3D>()

            for (i in 0 until connection.points.size - 1) {
                val p1 = connection.points[i]
                val p2 = connection.points[i + 1]

                // 检查线段是否与水平面相交
                if ((p1.z - height) * (p2.z - height) <= 0) {
                    // 计算交点
                    val t = (height - p1.z) / (p2.z - p1.z)
                    val x = p1.x + (p2.x - p1.x) * t
                    val y = p1.y + (p2.y - p1.y) * t
                    intersectionPoints.add(Point3D(x, y, height))
                }
            }

            // 如果找到了交点，将它们连接起来
            if (intersectionPoints.size >= 2) {
                val sortedPoints = sortPointsAlongContour(intersectionPoints)
                val interpolated = DrawingAlgorithms3D.interpolatePolyline(
                    sortedPoints,
                    tolerance,
                    connection.isClosed
                )
                contourPoints.addAll(interpolated)
            }
        }

        return contourPoints
    }

    private fun sortPointsAlongContour(points: List<Point3D>): List<Point3D> {
        if (points.size <= 2) return points

        // 简单的点排序：按角度排序（假设轮廓大致是凸的）
        val center = points.reduce { acc, point -> acc + point } / points.size.toDouble()
        return points.sortedBy { point ->
            atan2(point.y - center.y, point.x - center.x)
        }
    }

    fun renderGroup(
        group: PointGroup,
        renderEdges: Boolean = true,
        renderFill: Boolean = true
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

        if (renderFill && group.isClosed) {
            val fillPoints = fillGroup(group, FillMethod.TRIANGULATION)
            points.addAll(fillPoints)
        }

        // 使用集合去重，避免重复点
        return points.distinctBy { point ->
            Triple(point.x.roundTo(precision), point.y.roundTo(precision), point.z.roundTo(precision))
        }
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