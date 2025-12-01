package work.nekow.particlecore.canvas

import work.nekow.particlecore.canvas.utils.*
import kotlin.math.*
import kotlin.random.Random

@Suppress("unused")
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

        fun fillCubeFaces(
            cube: PointGroup,
            density: Double
        ): List<Point3D> {
            // 立方体的6个面
            val faces = listOf(
                // 底面 (z = min)
                listOf(0, 1, 2, 3),
                // 顶面 (z = max)
                listOf(4, 5, 6, 7),
                // 前面 (y = min)
                listOf(0, 1, 5, 4),
                // 后面 (y = max)
                listOf(3, 2, 6, 7),
                // 左面 (x = min)
                listOf(0, 3, 7, 4),
                // 右面 (x = max)
                listOf(1, 2, 6, 5)
            )

            val points = mutableListOf<Point3D>()

            faces.forEach { faceIndices ->
                val facePoints = faceIndices.map { cube.points[it] }
                if (facePoints.size >= 3) {
                    // 三角剖分并填充每个面
                    val triangles = triangulatePlanarPolygon(facePoints)
                    val faceFillPoints = fillTriangles(triangles, density)
                    points.addAll(faceFillPoints)
                }
            }

            return points
        }

        // 通用填充方法
        fun fill3DShape(
            group: PointGroup,
            density: Double,
            method: FillMethod3D = FillMethod3D.FACE_BY_FACE
        ): List<Point3D> = when (method) {
            FillMethod3D.FACE_BY_FACE -> fillByFaces(group, density)
            FillMethod3D.VOXEL -> voxelFill(group, density)
            FillMethod3D.CONTOUR -> contourFill(group, density)
        }

        fun fillByFaces(group: PointGroup, density: Double): List<Point3D> {
            val points = mutableListOf<Point3D>()

            // 检测并提取所有平面面
            val faces = extractFacesFromConnections(group.connections)

            faces.forEach { facePoints ->
                if (facePoints.size >= 3) {
                    val triangles = triangulatePlanarPolygon(facePoints)
                    val faceFillPoints = fillTriangles(triangles, density)
                    points.addAll(faceFillPoints)
                }
            }

            return points
        }

        private fun extractFacesFromConnections(connections: List<Connection>): List<List<Point3D>> {
            val faces = mutableListOf<List<Point3D>>()

            // 简单的边遍历算法来找到封闭的面
            val edgeMap = mutableMapOf<Point3D, MutableSet<Point3D>>()

            // 构建边映射
            connections.forEach { connection ->
                for (i in 0 until connection.points.size - 1) {
                    val p1 = connection.points[i]
                    val p2 = connection.points[i + 1]

                    edgeMap.getOrPut(p1) { mutableSetOf() }.add(p2)
                    edgeMap.getOrPut(p2) { mutableSetOf() }.add(p1)
                }

                if (connection.isClosed) {
                    val p1 = connection.points.last()
                    val p2 = connection.points.first()
                    edgeMap.getOrPut(p1) { mutableSetOf() }.add(p2)
                    edgeMap.getOrPut(p2) { mutableSetOf() }.add(p1)
                }
            }

            // 查找循环（面的边界）
            val visitedEdges = mutableSetOf<Pair<Point3D, Point3D>>()

            edgeMap.forEach { (start, neighbors) ->
                neighbors.forEach { neighbor ->
                    val edge = if (start.hashCode() < neighbor.hashCode())
                        start to neighbor else neighbor to start

                    if (edge !in visitedEdges) {
                        val face = findFaceCycle(start, neighbor, edgeMap)
                        if (face.size >= 3) {
                            faces.add(face)
                            // 标记边为已访问
                            for (i in face.indices) {
                                val p1 = face[i]
                                val p2 = face[(i + 1) % face.size]
                                val visitedEdge = if (p1.hashCode() < p2.hashCode())
                                    p1 to p2 else p2 to p1
                                visitedEdges.add(visitedEdge)
                            }
                        }
                    }
                }
            }

            return faces
        }

        private fun findFaceCycle(
            start: Point3D,
            next: Point3D,
            edgeMap: Map<Point3D, Set<Point3D>>
        ): List<Point3D> {
            val path = mutableListOf(start, next)
            var current = next

            while (current != start && path.size < edgeMap.size) {
                val neighbors = edgeMap[current] ?: break

                // 找到下一个顶点（使用右手法则）
                val prev = path[path.size - 2]
                val candidates = neighbors.filter { it != prev }

                if (candidates.isEmpty()) break

                // 选择使得转向最小的顶点
                val nextVertex = if (candidates.size == 1) {
                    candidates.first()
                } else {
                    // 简单的启发式：选择角度最小的
                    candidates.minByOrNull {
                        angleBetween(prev, current, it)
                    } ?: candidates.first()
                }

                path.add(nextVertex)
                current = nextVertex
            }

            return if (current == start) path else emptyList()
        }

        private fun angleBetween(a: Point3D, b: Point3D, c: Point3D): Double {
            val v1 = a - b
            val v2 = c - b
            val dot = v1.dot(v2)
            val length1 = sqrt(v1.x.pow(2) + v1.y.pow(2) + v1.z.pow(2))
            val length2 = sqrt(v2.x.pow(2) + v2.y.pow(2) + v2.z.pow(2))

            return if (length1 > 0 && length2 > 0) {
                acos(dot / (length1 * length2))
            } else {
                Double.MAX_VALUE
            }
        }

        // 添加：轮廓填充方法（用于3D形状）
        fun contourFill(group: PointGroup, density: Double): List<Point3D> {
            val points = mutableListOf<Point3D>()
            val (minPoint, maxPoint) = group.boundingBox()

            // 在多个Z高度上生成切片
            val numSlices = max(1, ((maxPoint.z - minPoint.z) / density).toInt())

            for (slice in 0..numSlices) {
                val z = minPoint.z + (maxPoint.z - minPoint.z) * slice / numSlices
                val slicePoints = generateSlice(group, z, density)
                points.addAll(slicePoints)
            }

            return points
        }

        // 查找所有与水平面相交的边
        fun intersectionPoints(connection: Connection, height: Double): List<Point3D> {
            val intersections = mutableListOf<Point3D>()

            for (i in 0 until connection.points.size - 1) {
                val p1 = connection.points[i]
                val p2 = connection.points[i + 1]

                // 检查线段是否与平面 z = constant 相交
                if ((p1.z - height) * (p2.z - height) <= 0) {
                    // 计算交点
                    val t = (height - p1.z) / (p2.z - p1.z)
                    val x = p1.x + (p2.x - p1.x) * t
                    val y = p1.y + (p2.y - p1.y) * t
                    intersections.add(Point3D(x, y, height))
                }
            }
            return intersections
        }

        private fun generateSlice(
            group: PointGroup,
            z: Double,
            tolerance: Double
        ): List<Point3D> {
            val slicePoints = mutableListOf<Point3D>()
            val epsilon = tolerance * 0.1

            // 查找所有与水平面相交的边
            val intersections = mutableListOf<Point3D>()

            group.connections.forEach { connection ->
                intersections.addAll(intersectionPoints(connection, z))
            }
            // 连接交点形成轮廓
            if (intersections.size >= 2) {
                // 按角度排序（假设轮廓是凸的）
                val center = intersections.reduce { acc, p -> acc + p } / intersections.size.toDouble()
                val sorted = intersections.sortedBy { p ->
                    atan2(p.y - center.y, p.x - center.x)
                }

                // 插值轮廓线
                for (i in sorted.indices) {
                    val p1 = sorted[i]
                    val p2 = sorted[(i + 1) % sorted.size]
                    val segmentPoints = DrawingAlgorithms3D.interpolateLine(p1, p2, tolerance)
                    slicePoints.addAll(segmentPoints)
                }
            }

            return slicePoints
        }
    }
}