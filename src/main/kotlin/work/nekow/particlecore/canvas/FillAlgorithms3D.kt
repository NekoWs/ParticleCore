package work.nekow.particlecore.canvas

import work.nekow.particlecore.canvas.utils.Connection
import work.nekow.particlecore.canvas.utils.ConnectionType
import work.nekow.particlecore.canvas.utils.Point3D
import work.nekow.particlecore.canvas.utils.PointGroup
import work.nekow.particlecore.canvas.utils.Triangle
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.random.Random

class FillAlgorithms3D {
    companion object {
        // 平面多边形三角剖分（耳切法）
        fun triangulatePlanarPolygon(points: List<Point3D>): List<Triangle> {
            if (points.size < 3) return emptyList()

            val indices = points.indices.toMutableList()
            val triangles = mutableListOf<Triangle>()

            while (indices.size > 3) {
                var earFound = false

                for (i in indices.indices) {
                    val prev = indices[(i - 1 + indices.size) % indices.size]
                    val curr = indices[i]
                    val next = indices[(i + 1) % indices.size]

                    if (isEar(points, indices, i)) {
                        triangles.add(Triangle(points[prev], points[curr], points[next]))
                        indices.removeAt(i)
                        earFound = true
                        break
                    }
                }

                if (!earFound) break // 避免无限循环
            }

            if (indices.size == 3) {
                triangles.add(
                    Triangle(
                        points[indices[0]],
                        points[indices[1]],
                        points[indices[2]]
                    )
                )
            }

            return triangles
        }

        private fun isEar(
            points: List<Point3D>,
            indices: List<Int>,
            index: Int
        ): Boolean {
            val n = indices.size
            val prev = indices[(index - 1 + n) % n]
            val curr = indices[index]
            val next = indices[(index + 1) % n]

            // 检查三角形是否包含其他点
            val triangle = listOf(points[prev], points[curr], points[next])

            for (i in indices.indices) {
                if (i != (index - 1 + n) % n && i != index && i != (index + 1) % n) {
                    if (isPointInTriangle(points[indices[i]], triangle)) {
                        return false
                    }
                }
            }

            return true
        }

        private fun isPointInTriangle(point: Point3D, triangle: List<Point3D>): Boolean {
            val (p1, p2, p3) = triangle

            // 计算重心坐标
            val v0 = p2 - p1
            val v1 = p3 - p1
            val v2 = point - p1

            val dot00 = v0.dot(v0)
            val dot01 = v0.dot(v1)
            val dot02 = v0.dot(v2)
            val dot11 = v1.dot(v1)
            val dot12 = v1.dot(v2)

            val invDenom = 1.0 / (dot00 * dot11 - dot01 * dot01)
            val u = (dot11 * dot02 - dot01 * dot12) * invDenom
            val v = (dot00 * dot12 - dot01 * dot02) * invDenom

            return (u >= 0) && (v >= 0) && (u + v < 1)
        }

        // 三角形网格填充
        fun fillTriangles(triangles: List<Triangle>, density: Double): List<Point3D> {
            val points = mutableListOf<Point3D>()

            triangles.forEach { triangle ->
                val area = triangle.area()
                val numPoints = max(1, (area / (density * density)).toInt())

                repeat(numPoints) {
                    // 重心坐标随机采样
                    var r1 = Random.nextDouble()
                    var r2 = Random.nextDouble()

                    if (r1 + r2 > 1) {
                        r1 = 1 - r1
                        r2 = 1 - r2
                    }

                    val point = triangle.p1 * (1 - r1 - r2) + triangle.p2 * r1 + triangle.p3 * r2
                    points.add(point)
                }
            }

            return points
        }

        // 体素化填充（用于复杂 3D 形状）
        fun voxelFill(
            group: PointGroup,
            voxelSize: Double
        ): List<Point3D> {
            if (group.points.isEmpty()) return emptyList()

            val (minPoint, maxPoint) = group.boundingBox()

            val xSteps = ceil((maxPoint.x - minPoint.x) / voxelSize).toInt()
            val ySteps = ceil((maxPoint.y - minPoint.y) / voxelSize).toInt()
            val zSteps = ceil((maxPoint.z - minPoint.z) / voxelSize).toInt()

            val points = mutableListOf<Point3D>()

            for (i in 0..xSteps) {
                for (j in 0..ySteps) {
                    for (k in 0..zSteps) {
                        val point = Point3D(
                            minPoint.x + i * voxelSize,
                            minPoint.y + j * voxelSize,
                            minPoint.z + k * voxelSize
                        )

                        if (isPointInPolyhedron(point, group.points)) {
                            points.add(point)
                        }
                    }
                }
            }

            return points
        }

        private fun isPointInPolyhedron(point: Point3D, vertices: List<Point3D>): Boolean {
            // 简化版：假设是凸多面体，使用重心坐标法
            if (vertices.size < 4) return false

            // 使用射线法
            val ray = Point3D(1.0, 0.0, 0.0)
            var intersectionCount = 0

            // 假设我们已经有了三角剖分
            val triangles = triangulatePlanarPolygon(vertices)

            triangles.forEach { triangle ->
                if (rayTriangleIntersection(point, ray, triangle)) {
                    intersectionCount++
                }
            }

            return intersectionCount % 2 == 1
        }

        private fun rayTriangleIntersection(
            origin: Point3D,
            direction: Point3D,
            triangle: Triangle
        ): Boolean {
            // Möller-Trumbore 算法
            val edge1 = triangle.p2 - triangle.p1
            val edge2 = triangle.p3 - triangle.p1
            val h = direction.cross(edge2)
            val a = edge1.dot(h)

            if (abs(a) < 1e-8) return false

            val f = 1.0 / a
            val s = origin - triangle.p1
            val u = f * s.dot(h)

            if (u !in 0.0..1.0) return false

            val q = s.cross(edge1)
            val v = f * direction.dot(q)

            if (v < 0.0 || u + v > 1.0) return false

            val t = f * edge2.dot(q)
            return t > 1e-8
        }

        // 表面采样填充（用于曲面）
        fun surfaceSampling(
            connections: List<Connection>,
            density: Double
        ): List<Point3D> {
            val points = mutableListOf<Point3D>()

            connections.forEach { connection ->
                when (connection.type) {
                    ConnectionType.LINE -> {
                        for (i in 0 until connection.points.size - 1) {
                            val segmentPoints = DrawingAlgorithms3D.interpolateLine(
                                connection.points[i],
                                connection.points[i + 1],
                                density
                            )
                            points.addAll(segmentPoints)
                        }
                    }

                    ConnectionType.BEZIER_CURVE -> {
                        val controlPoints = connection.controlPoints ?: connection.points
                        val curvePoints = DrawingAlgorithms3D.bezierCurve(
                            controlPoints,
                            density
                        )
                        points.addAll(curvePoints)
                    }

                    else -> {
                        // 其他类型暂时使用线性插值
                        val linePoints = DrawingAlgorithms3D.interpolatePolyline(
                            connection.points,
                            density,
                            connection.isClosed
                        )
                        points.addAll(linePoints)
                    }
                }
            }

            return points
        }
    }
}