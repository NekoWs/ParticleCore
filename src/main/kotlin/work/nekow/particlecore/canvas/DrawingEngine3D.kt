package work.nekow.particlecore.canvas

import work.nekow.particlecore.canvas.utils.Connection
import work.nekow.particlecore.canvas.utils.ConnectionType
import work.nekow.particlecore.canvas.utils.FillMethod
import work.nekow.particlecore.canvas.utils.Point3D
import work.nekow.particlecore.canvas.utils.PointGroup
import kotlin.math.abs
import kotlin.math.max

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
                val triangles = FillAlgorithms3D.triangulatePlanarPolygon(group.points)
                FillAlgorithms3D.fillTriangles(triangles, fillDensity)
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
        val center = group.center()

        val points = mutableListOf<Point3D>()
        val numContours = max(1, ((maxPoint.z - minPoint.z) / density).toInt())

        for (i in 0..numContours) {
            val z = minPoint.z + (maxPoint.z - minPoint.z) * i / numContours
            val contour = createContourAtHeight(group, z, density)
            points.addAll(contour)
        }

        return points
    }

    private fun createContourAtHeight(
        group: PointGroup,
        height: Double,
        density: Double
    ): List<Point3D> {
        // 在指定高度创建轮廓
        val contourPoints = mutableListOf<Point3D>()

        for (connection in group.connections) {
            val pointsAtHeight = connection.points.mapNotNull { point ->
                if (abs(point.z - height) < density) {
                    Point3D(point.x, point.y, height)
                } else null
            }

            if (pointsAtHeight.size >= 2) {
                val interpolated = DrawingAlgorithms3D.interpolatePolyline(
                    pointsAtHeight,
                    density,
                    connection.isClosed
                )
                contourPoints.addAll(interpolated)
            }
        }

        return contourPoints
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

        return points.distinct() // 移除重复点
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
            size: Double,
            precision: Double
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
    }
}